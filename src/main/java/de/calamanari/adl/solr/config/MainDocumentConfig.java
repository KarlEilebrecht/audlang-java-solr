//@formatter:off
/*
 * BaseDocumentConfig
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

package de.calamanari.adl.solr.config;

import static de.calamanari.adl.solr.config.ConfigUtils.assertContextNotNull;
import static de.calamanari.adl.solr.config.ConfigUtils.assertValidArgName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import de.calamanari.adl.AudlangMessage;
import de.calamanari.adl.CommonErrors;
import de.calamanari.adl.ProcessContext;
import de.calamanari.adl.cnv.tps.ArgMetaInfoLookup;
import de.calamanari.adl.cnv.tps.ConfigException;
import de.calamanari.adl.cnv.tps.LookupException;
import de.calamanari.adl.solr.config.ConfigBuilderInterfaces.MainDocumentAddFilterFieldOrFieldOrAutoMappingOrSubConfig;
import de.calamanari.adl.solr.config.ConfigBuilderInterfaces.MainDocumentConfigBuilder;

/**
 * A {@link MainDocumentConfig} describes a central document with the data about an individual with optional nested or dependent documents (to be joined).
 * <p>
 * The main document is the one that defines the entire id-space, each id optionally referenced implicitly (from any nested documents) or explicitly via the
 * reference field <code><b>main_id</b></code> in dependent documents.
 * <p>
 * Be aware that we solely rely on this 2-level-max relationship, means nested or dependent documents will be joined with the main document. More complex
 * operations like sub-nesting or joins of dependents is not supported.
 * 
 * @param nodeType document structure, value stored in the Solr-field <code><b>"node_type"</b></code> required
 * @param documentFilters static filters to be applied to this document type independent from the current query, may be empty or null
 * @param argFieldMap static assignments of arg names to Solr-fields may be empty or null
 * @param autoMappingPolicy dynamic assignments of arg names to Solr-fields, may be null
 * @param subDocumentConfigs nested or dependent document configurations, may be null
 * @param guardianLookup logical data model to restrict argNames and associated types, optional, unique across main-config or null
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public record MainDocumentConfig(String nodeType, List<FilterField> documentFilters, Map<String, ArgFieldAssignment> argFieldMap,
        AutoMappingPolicy autoMappingPolicy, List<SubDocumentConfig> subDocumentConfigs, ArgMetaInfoLookup guardianLookup)
        implements SolrMappingConfig, NodeTypeMetaInfo {

    /**
     * Entry point for fluently setting up a main-document config (with optional sub-document configs) in the the physical data model and along with it the
     * logical data model (mapped argNames)
     * <p>
     * This is more for testing convenience. The recommended way is first creating a logical data model with all the argNames and then calling
     * {@link #forNodeType(String, ArgMetaInfoLookup)} to setup the nodeType(s) and create a mapping.
     * 
     * @param nodeType Solr document's value in field <code><b>node_type</b></code>
     * @return builder
     */
    public static MainDocumentAddFilterFieldOrFieldOrAutoMappingOrSubConfig forNodeType(String nodeType) {
        return new Builder(nodeType, null);
    }

    /**
     * Entry point for setting up a main-document with optional sub-configs and for mapping it to existing argNames
     * 
     * @param nodeType Solr document's value in field <code><b>node_type</b></code>
     * @param argMetaInfoLookup (logical data model with all the argNames)
     * @return builder
     */
    public static MainDocumentAddFilterFieldOrFieldOrAutoMappingOrSubConfig forNodeType(String nodeType, ArgMetaInfoLookup argMetaInfoLookup) {
        return new Builder(nodeType, argMetaInfoLookup);
    }

    /**
     * @param nodeType document structure, value stored in the Solr-field <code><b>"node_type"</b></code> required
     * @param documentFilters static filters to be applied to this document type independent from the current query, may be empty or null
     * @param argFieldMap static assignments of arg names to Solr-fields may be empty or null
     * @param autoMappingPolicy dynamic assignments of arg names to Solr-fields, may be null
     * @param subDocumentConfigs nested or dependent document configurations, may be null
     * @param guardianLookup logical data model to restrict argNames and associated types, optional, must be unique across main-config or null
     */
    public MainDocumentConfig(String nodeType, List<FilterField> documentFilters, Map<String, ArgFieldAssignment> argFieldMap,
            AutoMappingPolicy autoMappingPolicy, List<SubDocumentConfig> subDocumentConfigs, ArgMetaInfoLookup guardianLookup) {

        ConfigValidationUtils.validateRequiredDocumentConfigFields(nodeType, SolrDocumentNature.MAIN, documentFilters, argFieldMap);
        ConfigValidationUtils.validateDocumentFilters(nodeType, SolrDocumentNature.MAIN, documentFilters, argFieldMap);
        ConfigValidationUtils.validateArgFieldMap(nodeType, SolrDocumentNature.MAIN, documentFilters, argFieldMap);
        ConfigValidationUtils.validateUniqueFieldTypes(nodeType, SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigs);
        ConfigValidationUtils.validateSubDocumentConfigs(nodeType, documentFilters, argFieldMap, subDocumentConfigs);
        ConfigValidationUtils.validateNoStaticFieldStealing(nodeType, documentFilters, argFieldMap, subDocumentConfigs);
        ConfigValidationUtils.validateGuardianLookups(nodeType, documentFilters, argFieldMap, subDocumentConfigs, guardianLookup);

        List<SubDocumentConfig> subDocumentConfigsTemp = (subDocumentConfigs == null || subDocumentConfigs.isEmpty()) ? Collections.emptyList()
                : new ArrayList<>(subDocumentConfigs);

        Map<String, ArgFieldAssignment> tempArgFieldMap = new TreeMap<>(argFieldMap);

        this.nodeType = nodeType;
        this.documentFilters = documentFilters == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(documentFilters));
        this.argFieldMap = Collections.unmodifiableMap(tempArgFieldMap);
        this.autoMappingPolicy = autoMappingPolicy == null ? DefaultAutoMappingPolicy.NONE : autoMappingPolicy;
        this.subDocumentConfigs = subDocumentConfigsTemp.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(subDocumentConfigsTemp);
        this.guardianLookup = guardianLookup;
    }

    @Override
    public int numberOfNodeTypes() {
        return subDocumentConfigs.size() + 1;
    }

    @Override
    public SolrDocumentNature documentNature() {
        return SolrDocumentNature.MAIN;
    }

    @Override
    public boolean contains(String argName) {
        assertValidArgName(argName);
        return (guardianLookup == null || guardianLookup.contains(argName))
                && (argFieldMap.containsKey(argName) || subDocumentConfigs.stream().anyMatch(tc -> tc.contains(argName)));
    }

    /**
     * This method tries to find the assignment <b>first</b> in the sub-configurations in the order they were configured and <b>finally</b> in the main
     * configuration (this).<br>
     * The idea is that the sub-configurations are probably more specific which might be expressed as name prefixes or suffixes. By checking the main config
     * <i>last</i> we create a plausible handler chain with the main auto-mapper as the <i>generic fallback</i> if none of the sub-configurations (in order of
     * configuration) contained a mapping for it.
     */
    @Override
    public ArgFieldAssignment lookupAssignment(String argName, ProcessContext ctx) {
        assertContextNotNull(ctx);
        assertValidArgName(argName);
        assertArgNameInGuardianLookupIfPresent(argName);

        try {
            Optional<SubDocumentConfig> subConfig = subDocumentConfigs.stream().filter(tc -> tc.contains(argName)).findFirst();

            // first check sub-configs then the main config
            if (subConfig.isPresent()) {
                return subConfig.get().lookupAssignment(argName, ctx);
            }
            else {
                ArgFieldAssignment res = argFieldMap.get(argName);
                if (res == null && autoMappingPolicy.isApplicable(argName)) {
                    res = autoMappingPolicy.map(argName, ctx);
                }
                if (res == null) {
                    throw new LookupException("No meta data available for argName=" + argName,
                            AudlangMessage.argMsg(CommonErrors.ERR_3000_MAPPING_FAILED, argName));
                }
                else if (guardianLookup != null) {
                    // ensure we remain consistent with guardian lookup (logical data model) even if the auto-mapper returns anything else
                    // except for "isCollection" which will be inherited bottom-up from the underlying field, see ArgFieldAssignment
                    res = new ArgFieldAssignment(guardianLookup.lookup(argName), res.field(), res.isMultiDoc());
                }
                return res;
            }
        }
        catch (ConfigException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            AudlangMessage userMessage = AudlangMessage.argMsg(CommonErrors.ERR_3000_MAPPING_FAILED, argName);
            throw new LookupException(String.format("An (auto-)mapping failed unexpectedly while being applied to argName=%s.", argName), ex, userMessage);
        }

    }

    @Override
    public NodeTypeMetaInfo lookupNodeTypeMetaInfo(String argName, ProcessContext ctx) {
        assertContextNotNull(ctx);
        assertValidArgName(argName);
        assertArgNameInGuardianLookupIfPresent(argName);

        try {
            Optional<SubDocumentConfig> subConfig = subDocumentConfigs.stream().filter(tc -> tc.contains(argName)).findAny();

            // first check sub-configs then the main config
            if (subConfig.isPresent()) {
                return subConfig.get();
            }
            else {
                if (argFieldMap.containsKey(argName) || autoMappingPolicy.isApplicable(argName)) {
                    return this;
                }
                AudlangMessage userMessage = AudlangMessage.argMsg(CommonErrors.ERR_3000_MAPPING_FAILED, argName);
                throw new LookupException(String.format("No meta data available for argName=%s.", argName), userMessage);
            }
        }
        catch (ConfigException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            AudlangMessage userMessage = AudlangMessage.argMsg(CommonErrors.ERR_3000_MAPPING_FAILED, argName);
            throw new LookupException(String.format("An (auto-)mapping failed unexpectedly while being applied to argName=%s.", argName), ex, userMessage);
        }

    }

    @Override
    public List<NodeTypeMetaInfo> allNodeTypeMetaInfos() {
        List<NodeTypeMetaInfo> res = new ArrayList<>(subDocumentConfigs.size() + 1);
        res.add(this);
        res.addAll(subDocumentConfigs);
        return Collections.unmodifiableList(res);
    }

    @Override
    public NodeTypeMetaInfo mainNodeTypeMetaInfo() {
        return this;
    }

    /**
     * If there is a {@link #guardianLookup} we check if it contains the argName and otherwise throw an exception
     * 
     * @param argName
     */
    private void assertArgNameInGuardianLookupIfPresent(String argName) {
        if (guardianLookup != null && !guardianLookup.contains(argName)) {
            throw new LookupException(String.format("No meta data available for argName=%s", argName));
        }
    }

    /**
     * Fluent builder implementation to describe and map a main-configuration step by step.
     * <p>
     * <b>Note:</b> This class parameterizes the super-class with itself to keep some common code in the parent while staying compatible to individual
     * interfaces.
     * 
     * @see AbstractDocumentConfigBuilder
     */
    private static class Builder extends AbstractDocumentConfigBuilder<Builder> implements MainDocumentConfigBuilder {

        private Builder(String nodeType, ArgMetaInfoLookup argMetaInfoLookup) {
            super(nodeType, argMetaInfoLookup);
        }

        @Override
        public MainDocumentConfig get() {
            return super.createMainDocumentConfig();
        }

    }

}
