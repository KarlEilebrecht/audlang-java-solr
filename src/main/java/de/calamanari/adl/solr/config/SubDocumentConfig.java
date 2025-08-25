//@formatter:off
/*
 * SubDocumentConfig
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
import java.util.TreeMap;

import de.calamanari.adl.AudlangMessage;
import de.calamanari.adl.CommonErrors;
import de.calamanari.adl.ProcessContext;
import de.calamanari.adl.cnv.tps.ArgMetaInfo;
import de.calamanari.adl.cnv.tps.ArgMetaInfoLookup;
import de.calamanari.adl.cnv.tps.ConfigException;
import de.calamanari.adl.cnv.tps.LookupException;
import de.calamanari.adl.solr.config.ConfigBuilderInterfaces.SubDocumentConfigBuilder;
import de.calamanari.adl.solr.config.ConfigBuilderInterfaces.SubDocumentSetNature;

/**
 * A {@link SubDocumentConfig} describes a node type that is either a nested document inside the main document or a dependent document referencing the main
 * document (for joining).
 * 
 * @param nodeType document structure, value stored in the Solr-field <code><b>"node_type"</b></code> required
 * @param documentNature kind of sub-document (nested or dependent) required, <b>not</b> {@link SolrDocumentNature#MAIN}
 * @param documentFilters static filters to be applied to this document type independent from the current query, may be empty or null
 * @param argFieldMap static assignments of arg names to Solr-fields may be empty or null
 * @param autoMappingPolicy dynamic assignments of arg names to Solr-fields may be null
 * @param guardianLookup logical data model to restrict argNames and associated types, optional, may be null
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public record SubDocumentConfig(String nodeType, SolrDocumentNature documentNature, List<FilterField> documentFilters,
        Map<String, ArgFieldAssignment> argFieldMap, AutoMappingPolicy autoMappingPolicy, ArgMetaInfoLookup guardianLookup) implements NodeTypeMetaInfo {

    /**
     * Entry point for fluently setting up a sub-document config (without main) in the the physical data model and along with it the logical data model (mapped
     * argNames)
     * <p>
     * This is more for testing convenience. The recommended way is first creating a logical data model with all the argNames and then calling
     * {@link #forNodeType(String, ArgMetaInfoLookup)} to setup the nodeType(s) and create a mapping.
     * 
     * @param nodeType Solr document's value in field <code><b>node_type</b></code>
     * @return builder
     */
    public static SubDocumentSetNature forNodeType(String nodeType) {
        return new Builder(nodeType, null);
    }

    /**
     * Entry point for setting up a sub-document (without main) and for mapping it to existing argNames
     * 
     * @param nodeType Solr document's value in field <code><b>node_type</b></code>
     * @param argMetaInfoLookup (logical data model with all the argNames)
     * @return builder
     */
    public static SubDocumentSetNature forNodeType(String nodeType, ArgMetaInfoLookup argMetaInfoLookup) {
        return new Builder(nodeType, argMetaInfoLookup);
    }

    /**
     * @param nodeType document structure, value stored in the Solr-field <code><b>"node_type"</b></code> required
     * @param documentNature kind of sub-document (nested or dependent) required, <b>not</b> {@link SolrDocumentNature#MAIN}
     * @param documentFilters static filters to be applied to this document type independent from the current query, may be empty or null
     * @param argFieldMap static assignments of arg names to Solr-fields may be empty or null
     * @param autoMappingPolicy dynamic assignments of arg names to Solr-fields may be null
     * @param guardianLookup logical data model to restrict argNames and associated types, optional, must be unique across main-config or null
     */
    public SubDocumentConfig(String nodeType, SolrDocumentNature documentNature, List<FilterField> documentFilters, Map<String, ArgFieldAssignment> argFieldMap,
            AutoMappingPolicy autoMappingPolicy, ArgMetaInfoLookup guardianLookup) {

        ConfigValidationUtils.validateRequiredDocumentConfigFields(nodeType, documentNature, documentFilters, argFieldMap);
        ConfigValidationUtils.validateDocumentFilters(nodeType, documentNature, documentFilters, argFieldMap);
        ConfigValidationUtils.validateArgFieldMap(nodeType, documentNature, documentFilters, argFieldMap);
        ConfigValidationUtils.validateUniqueFieldTypes(nodeType, documentNature, documentFilters, argFieldMap, null);

        if (documentNature == SolrDocumentNature.MAIN) {
            throw new ConfigException(String.format(
                    "Cannot create a sub-document configuration with documentNature=MAIN, given: nodeType=%s, documentNature=%s, documentFilters=%s, argFieldMap=%s",
                    nodeType, documentNature, documentFilters, argFieldMap));
        }

        Map<String, ArgFieldAssignment> tempMap = new TreeMap<>(argFieldMap);

        this.nodeType = nodeType;
        this.documentNature = documentNature;
        this.documentFilters = Collections.unmodifiableList(documentFilters == null ? Collections.emptyList() : new ArrayList<>(documentFilters));
        this.argFieldMap = Collections.unmodifiableMap(tempMap);
        this.autoMappingPolicy = autoMappingPolicy == null ? DefaultAutoMappingPolicy.NONE : autoMappingPolicy;
        this.guardianLookup = guardianLookup;
    }

    /**
     * Determines the column assignment for the given argName.
     * 
     * @param argName
     * @param ctx
     * @return the configured {@link ArgFieldAssignment} if {@link #contains(String)}=true, otherwise throws exception
     * @throws LookupException if there is no assignment for the given argName
     * @throws IllegalArgumentException if argName was null
     */
    public ArgFieldAssignment lookupAssignment(String argName, ProcessContext ctx) {
        assertContextNotNull(ctx);
        assertValidArgName(argName);
        ArgFieldAssignment assignment = null;

        ArgMetaInfo confirmedArgMetaInfo = null;
        if (guardianLookup != null && guardianLookup.contains(argName)) {
            confirmedArgMetaInfo = guardianLookup.lookup(argName);
        }

        if (guardianLookup == null || confirmedArgMetaInfo != null) {
            assignment = argFieldMap.get(argName);
            if (assignment == null && autoMappingPolicy.isApplicable(argName)) {
                assignment = autoMappingPolicy.map(argName, ctx);
                if (assignment != null && !assignment.field().nodeType().equals(this.nodeType)) {
                    throw new LookupException(String.format(
                            "Inconsistent auto-mapping detected: The argName=%s was mapped to %s, a field that does not belong to this nodeType (%s).", argName,
                            assignment, this.nodeType), AudlangMessage.argMsg(CommonErrors.ERR_3000_MAPPING_FAILED, argName));
                }
            }
        }
        if (assignment == null) {
            throw new LookupException("No meta data available for argName=" + argName);
        }
        else if (confirmedArgMetaInfo != null) {
            // ensure we remain consistent with guardian lookup (logical data model) even if the auto-mapper returns anything else
            // except for "isCollection" which will be inherited bottom-up from the underlying field, see ArgFieldAssignment
            assignment = new ArgFieldAssignment(confirmedArgMetaInfo, assignment.field(), assignment.isMultiDoc());
        }
        return assignment;
    }

    /**
     * @param argName
     * @return true if this lookup contains the given argument
     */
    public boolean contains(String argName) {
        assertValidArgName(argName);
        return (guardianLookup == null || guardianLookup.contains(argName)) && (argFieldMap.containsKey(argName) || autoMappingPolicy.isApplicable(argName));
    }

    /**
     * Fluent builder implementation to describe and map a sub-configuration (without main) step by step.
     * <p>
     * <b>Note:</b> This class parameterizes the super-class with itself to keep some common code in the parent while staying compatible to individual
     * interfaces.
     * 
     * @see AbstractDocumentConfigBuilder
     */
    private static class Builder extends AbstractDocumentConfigBuilder<Builder> implements SubDocumentConfigBuilder {

        private Builder(String nodeType, ArgMetaInfoLookup argMetaInfoLookup) {
            super(nodeType, argMetaInfoLookup);
        }

        @Override
        public SubDocumentConfig get() {
            return super.createSubDocumentConfig();
        }

    }
}
