//@formatter:off
/*
 * EmbeddedSolrServerUtils
 * Copyright 2025 Karl Eilebrecht
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"):
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//@formatter:on

package de.calamanari.adl.solr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.servlet.SolrDispatchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.calamanari.adl.solr.config.MainDocumentConfig;
import de.calamanari.adl.solr.config.SolrMappingConfig;
import de.calamanari.adl.solr.config.SubDocumentConfig;

import static de.calamanari.adl.cnv.tps.DefaultAdlType.DATE;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.STRING;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_BOOLEAN;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_DATE;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_DOUBLE;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_FLOAT;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_INTEGER;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_LONG;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_STRING;

/**
 * Utilities for creating and feeding an embedded Solr server in the target directory of the maven project.<br>
 * Therefore it considers the configuration below src/test/resources/solr.<br>
 * Initially, the collection will be fed from the specified JSON file located in src/test/resources/solr/exampledocs.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class EmbeddedSolrServerUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedSolrServerUtils.class);

    /**
     * Project root directory
     */
    private static final Path PROJECT_ROOT_PATH = Path.of(System.getProperty("user.dir"));

    /**
     * Maven build's target directory of the project
     */
    private static final Path PROJECT_TARGET_PATH = PROJECT_ROOT_PATH.resolve("target");

    /**
     * This is where we place the embedded server instances (each in a dedicated directory with a random name)
     */
    private static final Path SOLR_INSTALL_PATH = PROJECT_TARGET_PATH.resolve("embedded-solr-servers");

    /**
     * Directory with all test resources for setup and feeding of embedded solr servers
     */
    private static final Path SOLR_RESOURCES_PATH = PROJECT_ROOT_PATH.resolve("src").resolve("test").resolve("resources").resolve("solr");

    /**
     * Solr server configurations (each collection in a sub-directory)
     */
    private static final Path SOLR_CONFIGSETS_PATH = SOLR_RESOURCES_PATH.resolve("configsets");

    /**
     * Directory with JSON-documents for initial feeding of the embedded instance
     */
    private static final Path SOLR_EXAMPLEDOCS_PATH = SOLR_RESOURCES_PATH.resolve("exampledocs");

    /**
     * Type reference for quickly de-serializing JSON into a more convenient object structure.<br>
     * We expect the JSON to consist of a List of documents (Map<String, Object), possibly nested. This relies on the behavior of the Jackson ObjectMapper.
     */
    private static final TypeReference<List<Map<String, Object>>> JSON_FEED_DOCUMENT_STRUCTURE = new TypeReference<>() {
        // nothing to do
    };

    /**
     * This sets the limit on returned documents from the default (10) to @value for each query
     */
    public static final int MAX_RETURNED_DOCS = 10_000;

    /**
     * {@value}
     */
    public static final String NODE_TYPE_PROFILE = "profile";

    /**
     * {@value}
     */
    public static final String NODE_TYPE_FACT = "fact";

    /**
     * {@value}
     */
    public static final String NODE_TYPE_SURVEY = "survey";

    /**
     * {@value}
     */
    public static final String NODE_TYPE_POS = "pos";

    /**
     * The main query is always <b><code>{@value}</code></b> (all documents)
     * <p>
     * Solr distinguishes between the main query (more flexible, but also with potentially expensive scoring) and a filter query (boolean criteria). Because we
     * are not doing any fuzzy queries at all here), we only rely on filter queries.
     */
    private static final String FIXED_MAIN_QUERY = SolrFormatConstants.QUERY_ALL_DOCUMENTS;

    /**
     * The base filter query is always <b><code>{@value}</code></b> (all documents of type profile)
     * <p>
     * So we cannot accidentally return anything else and Solr will cache this filter query.
     */
    private static final String BASE_FILTER_QUERY = SolrFormatConstants.DEFAULT_NODE_TYPE_FIELD_NAME + ":" + NODE_TYPE_PROFILE;

    /**
     * @return mapping for the hybrid test document setup
     */
    public static SolrMappingConfig createHybridMappingConfig() {
        // @formatter:off
        return MainDocumentConfig.forNodeType(NODE_TYPE_PROFILE)
                                      .dataField("provider", SOLR_STRING)
                                          .mappedToArgName("provider")
                                      .dataField("country", SOLR_STRING)
                                          .mappedToArgName("home-country")
                                      .dataField("city", SOLR_STRING)
                                          .mappedToArgName("home-city")
                                      .dataField("demcode", SOLR_INTEGER)
                                          .mappedToArgName("demCode")
                                      .dataField("gender", SOLR_STRING)
                                          .mappedToArgName("gender")
                                      .dataField("omscore", SOLR_FLOAT)
                                          .mappedToArgName("omScore")
                                      .dataField("updtime", SOLR_DATE)
                                          .mappedToArgName("upd1")
                                      .dataField("upddate", SOLR_DATE)
                                          .mappedToArgName("upd2")
                                      .dataField("tntcode", SOLR_INTEGER)
                                          .mappedToArgName("tntCode")
                                      .dataField("bstate", SOLR_BOOLEAN)
                                          .mappedToArgName("bState")
                                      .dataField("scode", SOLR_INTEGER)
                                          .mappedToArgName("sCode")
                                      .dataField("bicode", SOLR_LONG)
                                          .mappedToArgName("biCode")
                                      .dataField("ncode", SOLR_DOUBLE)
                                          .mappedToArgName("nCode")
                                      .dataField("flt_sports_ss")
                                          .mappedToArgName("sports")
                                      .dataField("flt_hobbies_ss")
                                          .mappedToArgName("hobbies")
                                      .dataField("flt_sizecm_i")
                                          .mappedToArgName("sizeCM")
                                      .dataField("flt_bodytempcelsius_d")
                                          .mappedToArgName("bodyTempCelsius")
                                      .dataField("flt_clubmember_b")
                                          .mappedToArgName("clubMember")
                                      .dataField("flt_anniversarydate_dt")
                                          .mappedToArgName("anniverseryDate")
                                      .dataField("flt_false1_b")
                                          .mappedToArgName("false1")
                                      .dataField("flt_false2_b")
                                          .mappedToArgName("false2")
                                      .dataField("flt_true1_b")
                                          .mappedToArgName("true1")
                                      .dataField("flt_true2_b")
                                          .mappedToArgName("true2")
                                      .dataField("flt_true_true_bs")
                                          .mappedToArgName("trueTrue")
                                      .dataField("flt_false_false_bs")
                                          .mappedToArgName("falseFalse")
                                      .dataField("flt_true_false1_bs")
                                          .mappedToArgName("trueFalse1")
                                      .dataField("flt_true_false2_bs")
                                          .mappedToArgName("trueFalse2")
                                      .subConfig(
                                          SubDocumentConfig.forNodeType(NODE_TYPE_FACT)
                                              .nestedMultiDoc()
                                              .dataField("fct_provider_s")
                                                  .mappedToArgName("fact.provider").notMultiDoc()
                                              .dataField("fct_contacttime_dts", SOLR_DATE)
                                                  .mappedToArgName("fact.contactTime.ts", DATE)
                                              .dataField("fct_contacttime2_dts", SOLR_DATE)
                                                  .mappedToArgName("fact.contactTime2.dt", DATE)
                                              .dataField("fct_contactcode_ss", SOLR_STRING)
                                                  .mappedToArgName("fact.contactCode.str", STRING)
                                              .autoMapped(s-> (s.startsWith("fact.") && s.endsWith(".str") && s.length() > 9) ? "fct_" + s.substring(5, s.length()-4).toLowerCase()+"_s" : null)
                                              .autoMapped(s-> (s.startsWith("fact.") && s.endsWith(".int") && s.length() > 9) ? "fct_" + s.substring(5, s.length()-4).toLowerCase()+"_i" : null)
                                              .autoMapped(s-> (s.startsWith("fact.") && s.endsWith(".dec") && s.length() > 9) ? "fct_" + s.substring(5, s.length()-4).toLowerCase()+"_d" : null)
                                              .autoMapped(s-> (s.startsWith("fact.") && s.endsWith(".flg") && s.length() > 9) ? "fct_" + s.substring(5, s.length()-4).toLowerCase()+"_b" : null)
                                              .autoMapped(s-> (s.startsWith("fact.") && s.endsWith(".dt") && s.length() > 8) ? "fct_" + s.substring(5, s.length()-3).toLowerCase()+"_dt" : null)
                                              .autoMapped(s-> (s.startsWith("fact.") && s.endsWith(".ts") && s.length() > 8) ? "fct_" + s.substring(5, s.length()-3).toLowerCase()+"_dt" : null)
                                              .autoMapped(s-> (s.startsWith("fact.") && s.endsWith(".flgs") && s.length() > 8) ? "fct_" + s.substring(5, s.length()-5).toLowerCase()+"_bs" : null)
                                          .get())
                                      .subConfig(
                                          SubDocumentConfig.forNodeType(NODE_TYPE_SURVEY)
                                              .dependent()
                                              .filteredBy("tenant", SOLR_INTEGER, "${tenant}")
                                              .dataField("srv_favcolor_ss")
                                                  .mappedToArgName("q.favColor.str")
                                              .autoMapped(s-> (s.startsWith("q.") && s.endsWith(".str") && s.length() > 6) ? "srv_" +s.substring(2, s.length()-4).toLowerCase() + "_s" : null)
                                              .autoMapped(s-> (s.startsWith("q.") && s.endsWith(".int") && s.length() > 6) ? "srv_" +s.substring(2, s.length()-4).toLowerCase() + "_i" : null)
                                              .autoMapped(s-> (s.startsWith("q.") && s.endsWith(".flg") && s.length() > 6) ? "srv_" +s.substring(2, s.length()-4).toLowerCase() + "_b" : null)
                                          .get())
                                      .subConfig(
                                          SubDocumentConfig.forNodeType(NODE_TYPE_POS)
                                              .dependent()
                                              .dataField("pos_invdate_dt")
                                                  .mappedToArgName("pos.date")
                                              .dataField("pos_invdate_dt")
                                                  .mappedToArgName("pos.anyDate").multiDoc()
                                              .dataField("pos_description_s")
                                                  .mappedToArgName("pos.name")
                                              .dataField("pos_quantity_i")
                                                  .mappedToArgName("pos.quantity")
                                              .dataField("pos_uprice_d")
                                                  .mappedToArgName("pos.unitPrice")
                                              .dataField("pos_country_s")
                                                  .mappedToArgName("pos.country")
                                          .get())
                                  .get();
        // @formatter:on

    }

    /**
     * @return mapping for the hybrid test document setup, profile only
     */
    public static SolrMappingConfig createHybridMappingConfigProfileOnly() {
        MainDocumentConfig base = (MainDocumentConfig) createHybridMappingConfig();

        return new MainDocumentConfig(base.nodeType(), base.documentFilters(), base.argFieldMap(), base.autoMappingPolicy(), Collections.emptyList(),
                base.guardianLookup());

    }

    /**
     * @return mapping for the hybrid test document setup, profile with facts
     */
    public static SolrMappingConfig createHybridMappingConfigProfileAndFacts() {
        MainDocumentConfig base = (MainDocumentConfig) createHybridMappingConfig();

        SubDocumentConfig factsConfig = base.subDocumentConfigs().stream().filter(sdc -> sdc.nodeType().equals("fact")).findFirst()
                .orElseThrow(() -> new IllegalStateException("no facts?"));

        return new MainDocumentConfig(base.nodeType(), base.documentFilters(), base.argFieldMap(), base.autoMappingPolicy(), Arrays.asList(factsConfig),
                base.guardianLookup());

    }

    /**
     * @return mapping for the hybrid test document setup, profile with pos-data
     */
    public static SolrMappingConfig createHybridMappingConfigProfileAndPos() {
        MainDocumentConfig base = (MainDocumentConfig) createHybridMappingConfig();

        SubDocumentConfig factsConfig = base.subDocumentConfigs().stream().filter(sdc -> sdc.nodeType().equals("pos")).findFirst()
                .orElseThrow(() -> new IllegalStateException("no pos?"));

        return new MainDocumentConfig(base.nodeType(), base.documentFilters(), base.argFieldMap(), base.autoMappingPolicy(), Arrays.asList(factsConfig),
                base.guardianLookup());

    }

    /**
     * @return mapping for the hybrid test document setup, profile with facts not multi-row
     */
    public static SolrMappingConfig createHybridMappingConfigProfileAndFactsNotMultiDoc() {
        MainDocumentConfig base = (MainDocumentConfig) createHybridMappingConfig();

        // @formatter:off
        SubDocumentConfig factsNotMultiDoc = SubDocumentConfig.forNodeType(NODE_TYPE_FACT)
                            .nested()
                            .dataField("fct_provider_s")
                                .mappedToArgName("fact.provider")
                            .dataField("fct_contacttime_dts", SOLR_DATE)
                                .mappedToArgName("fact.contactTime.ts", DATE)
                            .dataField("fct_contacttime2_dts", SOLR_DATE)
                                .mappedToArgName("fact.contactTime2.dt", DATE)
                            .dataField("fct_contactcode_ss", SOLR_STRING)
                                .mappedToArgName("fact.contactCode.str", STRING)
                            .autoMapped(s-> (s.startsWith("fact.") && s.endsWith(".str") && s.length() > 9) ? "fct_" + s.substring(5, s.length()-4).toLowerCase()+"_s" : null)
                            .autoMapped(s-> (s.startsWith("fact.") && s.endsWith(".int") && s.length() > 9) ? "fct_" + s.substring(5, s.length()-4).toLowerCase()+"_i" : null)
                            .autoMapped(s-> (s.startsWith("fact.") && s.endsWith(".dec") && s.length() > 9) ? "fct_" + s.substring(5, s.length()-4).toLowerCase()+"_d" : null)
                            .autoMapped(s-> (s.startsWith("fact.") && s.endsWith(".flg") && s.length() > 9) ? "fct_" + s.substring(5, s.length()-4).toLowerCase()+"_b" : null)
                            .autoMapped(s-> (s.startsWith("fact.") && s.endsWith(".dt") && s.length() > 8) ? "fct_" + s.substring(5, s.length()-3).toLowerCase()+"_dt" : null)
                            .autoMapped(s-> (s.startsWith("fact.") && s.endsWith(".ts") && s.length() > 8) ? "fct_" + s.substring(5, s.length()-3).toLowerCase()+"_dt" : null)
                            .autoMapped(s-> (s.startsWith("fact.") && s.endsWith(".flgs") && s.length() > 8) ? "fct_" + s.substring(5, s.length()-5).toLowerCase()+"_bs" : null)
                          .get();        
        
        //@formatter:on

        return new MainDocumentConfig(base.nodeType(), base.documentFilters(), base.argFieldMap(), base.autoMappingPolicy(), Arrays.asList(factsNotMultiDoc),
                base.guardianLookup());

    }

    /**
     * @return mapping for the hybrid test document setup, all pos-fields marked multi-doc
     */
    public static SolrMappingConfig createHybridMappingConfigMakePosDataAllMultiDoc() {

        MainDocumentConfig base = (MainDocumentConfig) createHybridMappingConfig();

        List<SubDocumentConfig> updatedSubConfigs = new ArrayList<>();

        for (SubDocumentConfig subConfig : base.subDocumentConfigs()) {
            if (subConfig.nodeType().equals(NODE_TYPE_POS)) {
                // @formatter:off
                updatedSubConfigs.add(SubDocumentConfig.forNodeType(NODE_TYPE_POS)
                                              .dependentMultiDoc()
                                              .dataField("pos_invdate_dt")
                                                  .mappedToArgName("pos.date")
                                              .dataField("pos_invdate_dt")
                                                  .mappedToArgName("pos.anyDate")
                                              .dataField("pos_description_s")
                                                  .mappedToArgName("pos.name")
                                              .dataField("pos_quantity_i")
                                                  .mappedToArgName("pos.quantity")
                                              .dataField("pos_uprice_d")
                                                  .mappedToArgName("pos.unitPrice")
                                              .dataField("pos_country_s")
                                                  .mappedToArgName("pos.country")
                                          .get());
                // @formatter:on
            }
            else {
                updatedSubConfigs.add(subConfig);
            }
        }

        return new MainDocumentConfig(base.nodeType(), base.documentFilters(), base.argFieldMap(), base.autoMappingPolicy(), updatedSubConfigs,
                base.guardianLookup());

    }

    /**
     * Converts a single Map (means the content of a single JSON object) into a single SolrInputDocument.
     * <p>
     * Each field (mapping) becomes a SolrInputField, where the key stays as-is.
     * <p>
     * Be aware that the magic around child document indexing entirely happens inside Solr. <br>
     * Thus we can hand-over the document structure as is after recursively handling all inner maps (the child documents) in the exact same way before assigning
     * them as field values.
     * 
     * @param map input mappings
     * @return solr (child) document
     */
    private static SolrInputDocument convertMapRecursively(Map<?, ?> map) {
        Map<String, SolrInputField> fieldMap = new LinkedHashMap<>(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object effectiveValue = entry.getValue();
            if (effectiveValue instanceof Map childMap) {
                effectiveValue = convertMapRecursively(childMap);
            }
            else if (effectiveValue instanceof List effectiveValueList && !effectiveValueList.isEmpty() && effectiveValueList.get(0) instanceof Map) {
                @SuppressWarnings("unchecked")
                List<Map<?, ?>> effectiveValueListCasted = effectiveValueList;
                effectiveValue = effectiveValueListCasted.stream().map(EmbeddedSolrServerUtils::convertMapRecursively).toList();
            }
            SolrInputField field = new SolrInputField((String) entry.getKey());
            field.setValue(effectiveValue);
            fieldMap.put(field.getName(), field);
        }
        return new SolrInputDocument(fieldMap);
    }

    /**
     * Creates a <b>new, independent</b> embedded solr instance, even when called with the same parameters to avoid tests interfering with each other.
     * 
     * @param name the core name of the solr instance
     * @param configSetName the plain name of the config set (direct sibling of {@link #SOLR_CONFIGSETS_PATH})
     * @param modules the names of optional modules to be loaded
     * @return embedded solr server, even in a test you should finally call {@linkplain EmbeddedSolrServer#close()}
     */
    @SuppressWarnings("resource")
    public static EmbeddedSolrServer createNewServerInstance(String name, String configSetName, String... modules) {

        EmbeddedSolrServer res = null;
        try {
            Path solrHomePath = SOLR_INSTALL_PATH.resolve(UUID.randomUUID().toString());
            Files.createDirectories(solrHomePath);

            LOGGER.trace("Creating embedded solr-server instance '{}' with config-set '{}' at {} ...", name, configSetName, solrHomePath);
            System.setProperty(SolrDispatchFilter.SOLR_INSTALL_DIR_ATTRIBUTE, SOLR_INSTALL_PATH.toString());

            NodeConfig.NodeConfigBuilder builder = new NodeConfig.NodeConfigBuilder(configSetName, solrHomePath)
                    .setConfigSetBaseDirectory(SOLR_CONFIGSETS_PATH.toString());

            if (modules != null && modules.length > 0) {
                LOGGER.trace("""
                        Solr-modules {} will be picked up from {}.

                        E.g., to use the solr 'scripting'-module, configure the maven-build to create a sub-directory
                        named 'scripting/lib' and to copy (see maven-dependency-plugin) the scripting artifact (jar)
                        to this location during build-phase 'validate'.""", modules, SOLR_INSTALL_PATH.resolve("modules"));
                builder.setModules(Arrays.stream(modules).collect(Collectors.joining(",")));
            }

            res = new EmbeddedSolrServer(builder.build(), name);
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName(name);
            createRequest.setConfigSet(configSetName);
            res.request(createRequest);
        }
        catch (SolrServerException | IOException | RuntimeException ex) {
            if (res != null) {
                try {
                    res.close();
                }
                catch (IOException | RuntimeException exClose) {
                    LOGGER.error("Unable to properly close embedded solr-server", exClose);
                }
            }
            if (!(ex instanceof RuntimeException)) {
                throw new RuntimeException(ex);
            }
        }
        return res;
    }

    /**
     * Feeds the data from the specified JSON feed file (to be located in {@link #SOLR_EXAMPLEDOCS_PATH})
     * 
     * @param solrClient (the embedded solr server created beforehand)
     * @param seedJsonFileName plain file name of the JSON file with the documents to be loaded (null means none)
     */
    public static void feedDocuments(SolrClient solrClient, String seedJsonFileName) {
        if (seedJsonFileName != null) {
            Path seedJsonFilePath = SOLR_EXAMPLEDOCS_PATH.resolve(seedJsonFileName);
            if (Files.exists(seedJsonFilePath)) {

                try {

                    List<Map<String, Object>> documents = new ObjectMapper().readValue(seedJsonFilePath.toFile(), JSON_FEED_DOCUMENT_STRUCTURE);
                    List<SolrInputDocument> solrInputDocuments = documents.stream().map(EmbeddedSolrServerUtils::convertMapRecursively).toList();
                    solrClient.add(solrInputDocuments);
                    solrClient.commit();
                }
                catch (SolrServerException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            else {
                throw new RuntimeException(String.format("Unable to load seed json file '%s' (%s does not exist)", seedJsonFileName, seedJsonFilePath));
            }
        }

    }

    /**
     * Queries the documents filtered by the given query string.
     * <p>
     * <b>Important:</b> This method applies the <i>fixed main query</i> <code><b>{@value #FIXED_MAIN_QUERY}</b></code>, and it won't consider any other
     * documents besides those of type <i>profile</i>.
     * 
     * @param solrClient
     * @param query
     * @return document list (number of results limited to {@value #MAX_RETURNED_DOCS} max)
     */
    public static SolrDocumentList queryDocs(SolrClient solrClient, String query) {
        return queryDocsInternal(solrClient, query, false);

    }

    /**
     * Queries the documents filtered by the given query string.
     * <p>
     * <b>Important:</b> This method applies the <i>fixed main query</i> <code><b>{@value #FIXED_MAIN_QUERY}</b></code>, and it won't consider any other
     * documents besides those of type <i>profile</i>.
     * 
     * @param solrClient
     * @param query
     * @param countQuery true to suppress data transfer
     * @return document list (number of results limited to {@value #MAX_RETURNED_DOCS} max)
     */
    private static SolrDocumentList queryDocsInternal(SolrClient solrClient, String query, boolean countQuery) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(FIXED_MAIN_QUERY);
        solrQuery.setFilterQueries(BASE_FILTER_QUERY, query);
        if (countQuery) {
            // avoid transferring any documents to the client as we are only interested in the number of matches
            solrQuery.setRows(0);
        }
        else {
            solrQuery.setRows(MAX_RETURNED_DOCS);
        }
        try {
            QueryResponse response = solrClient.query(solrQuery);
            return response.getResults();
        }
        catch (SolrServerException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Queries the documents filtered by the query definition.
     * 
     * @param solrClient
     * @param query
     * @param queryType determines what to return
     * @return document list (number of results limited to {@value #MAX_RETURNED_DOCS} max)
     */
    public static SolrDocumentList queryDocs(SolrClient solrClient, SolrQueryDefinition query, DefaultQueryType queryType) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query.mainQueryString());
        solrQuery.setFilterQueries(query.filterQueries().stream().map(SolrFilterQuery::queryString).toArray(String[]::new));
        solrQuery.setRows(MAX_RETURNED_DOCS);
        switch (queryType) {
        case SELECT_COUNT:
            solrQuery.setRows(0);
            break;
        case SELECT_DOCUMENTS:
            solrQuery.setFields("*");
            break;
        case SELECT_DOCUMENTS_ORDERED:
            solrQuery.setSort(query.uniqueKeyFieldName(), ORDER.asc);
            solrQuery.setFields("*");
            break;
        case SELECT_DOCUMENTS_ORDERED_WITH_NESTING:
            solrQuery.setSort(query.uniqueKeyFieldName(), ORDER.asc);
            solrQuery.setFields("*", "[child]");
            break;
        case SELECT_DOCUMENTS_WITH_NESTING:
            solrQuery.setFields("*", "[child]");
            break;
        case SELECT_IDS:
            solrQuery.setFields(query.uniqueKeyFieldName());
            break;
        case SELECT_IDS_ORDERED:
            solrQuery.setSort(query.uniqueKeyFieldName(), ORDER.asc);
            solrQuery.setFields(query.uniqueKeyFieldName());
            break;
        }

        try {
            QueryResponse response = solrClient.query(solrQuery);
            return response.getResults();
        }
        catch (SolrServerException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Queries the number of documents matched by the given query string.
     * <p>
     * <b>Important:</b> This method applies the <i>fixed main query</i> <code><b>{@value #FIXED_MAIN_QUERY}</b></code>, and it won't consider any other
     * documents besides those of type <i>profile</i>.
     * 
     * @param solrClient
     * @param query
     * @return number of matching documents
     */
    public static long queryCount(SolrClient solrClient, String query) {
        return queryDocsInternal(solrClient, query, true).getNumFound();
    }

    /**
     * Queries the number of documents matched by the given query definition.
     * 
     * @param solrClient
     * @param query
     * @return number of matching documents
     */
    public static long queryCount(SolrClient solrClient, SolrQueryDefinition query) {
        return queryDocs(solrClient, query, DefaultQueryType.SELECT_COUNT).getNumFound();
    }

    /**
     * Queries the document ids filtered by the given query string.
     * <p>
     * <b>Important:</b> This method applies the <i>fixed main query</i> <code><b>{@value #FIXED_MAIN_QUERY}</b></code>, and it won't consider any other
     * documents besides those of type <i>profile</i>.
     * 
     * @param solrClient
     * @param query
     * @return document id list
     */
    public static List<String> queryIds(SolrClient solrClient, String query) {
        return queryDocs(solrClient, query).stream().map(e -> e.getFieldValue(SolrFormatConstants.DEFAULT_UNIQUE_KEY_FIELD_NAME)).map(String::valueOf).toList();
    }

    /**
     * Queries the document ids filtered by the given query definition.
     * 
     * @param solrClient
     * @param query
     * @return document id list
     */
    public static List<String> queryIds(SolrClient solrClient, SolrQueryDefinition query) {
        return queryDocs(solrClient, query, DefaultQueryType.SELECT_IDS).stream().map(e -> e.getFieldValue(query.uniqueKeyFieldName())).map(String::valueOf)
                .toList();
    }

    /**
     * Queries the document ids as integer filtered by the given query string (of course this only works if the ids are numeric).
     * <p>
     * <b>Important:</b> This method applies the <i>fixed main query</i> <code><b>{@value #FIXED_MAIN_QUERY}</b></code>, and it won't consider any other
     * documents besides those of type <i>profile</i>.
     * 
     * @param solrClient
     * @param query
     * @return document ids as integers (for convenient display and comparison)
     */
    public static List<Integer> queryIntIds(SolrClient solrClient, String query) {
        return queryDocs(solrClient, query).stream().map(e -> e.getFieldValue(SolrFormatConstants.DEFAULT_UNIQUE_KEY_FIELD_NAME)).map(String::valueOf)
                .map(Integer::parseInt).toList();
    }

    /**
     * Queries the document ids as integer filtered by the given query string (of course this only works if the ids are numeric).
     * 
     * @param solrClient
     * @param query
     * @return document id list
     */
    public static List<Integer> queryIntIds(SolrClient solrClient, SolrQueryDefinition query) {
        return queryIds(solrClient, query).stream().map(Integer::valueOf).toList();
    }

    /**
     * Queries the document ids as integer filtered by the given query string (of course this only works if the ids are numeric).
     * <p>
     * <b>Important:</b> This method applies the <i>fixed main query</i> <code><b>{@value #FIXED_MAIN_QUERY}</b></code>, and it won't consider any other
     * documents besides those of type <i>profile</i>.
     * 
     * @param solrClient
     * @param query
     * @return document ids as integers, sorted ascending (for convenient display and comparison)
     */
    public static List<Integer> queryIntIdsSorted(SolrClient solrClient, String query) {
        return queryDocs(solrClient, query).stream().map(e -> e.getFieldValue(SolrFormatConstants.DEFAULT_UNIQUE_KEY_FIELD_NAME)).map(String::valueOf)
                .map(Integer::parseInt).sorted().toList();
    }

    /**
     * Queries the document ids as integer filtered by the given query string (of course this only works if the ids are numeric).
     * 
     * @param solrClient
     * @param query
     * @return document id list
     */
    public static List<Integer> queryIntIdsSorted(SolrClient solrClient, SolrQueryDefinition query) {
        return queryIds(solrClient, query).stream().map(Integer::valueOf).sorted().toList();
    }

}
