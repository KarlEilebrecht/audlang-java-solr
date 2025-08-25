//@formatter:off
/*
 * ConfigValidationUtils
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.calamanari.adl.AudlangMessage;
import de.calamanari.adl.CommonErrors;
import de.calamanari.adl.cnv.TemplateParameterUtils;
import de.calamanari.adl.cnv.tps.ArgMetaInfoLookup;
import de.calamanari.adl.cnv.tps.ConfigException;
import de.calamanari.adl.solr.SolrFormatConstants;

/**
 * Central methods for validating main and sub document configurations.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class ConfigValidationUtils {

    private ConfigValidationUtils() {
        // utilities
    }

    /**
     * Performs some essential non-null and solr name checks.
     * 
     * @param nodeType to check name
     * @param documentNature required
     * @param documentFilters for debugging
     * @param argFieldMap for debugging
     */
    public static void validateRequiredDocumentConfigFields(String nodeType, SolrDocumentNature documentNature, List<FilterField> documentFilters,
            Map<String, ArgFieldAssignment> argFieldMap) {
        if (!ConfigUtils.isValidSolrName(nodeType)) {
            throw new ConfigException(String.format("Invalid nodeName, given: nodeType=%s, documentNature=%s, documentFilters=%s, argFieldMap=%s%n%s", nodeType,
                    documentNature, documentFilters, argFieldMap, SolrFormatConstants.SOLR_NAMING_DEBUG_INFO));
        }
        if (documentNature == null) {
            throw new ConfigException(
                    String.format("Argument documentNature must not be null, given: nodeType=%s, documentNature=%s, documentFilters=%s, argFieldMap=%s",
                            nodeType, documentNature, documentFilters, argFieldMap));
        }
    }

    /**
     * Ensures no nulls or contradictions (nodeTypes etc.) are sneaking in.
     * 
     * @param nodeType for debugging
     * @param documentNature for debugging
     * @param documentFilters for debugging
     * @param argFieldMap to be checked
     */
    public static void validateArgFieldMap(String nodeType, SolrDocumentNature documentNature, List<FilterField> documentFilters,
            Map<String, ArgFieldAssignment> argFieldMap) {
        if (argFieldMap == null) {
            throw new ConfigException(String.format(
                    "The parameter argFieldMap can be empty but must not be null, given: nodeType=%s, documentNature=%s, documentFilters=%s, argFieldMap=%s",
                    nodeType, documentNature, documentFilters, argFieldMap));
        }
        for (Map.Entry<String, ArgFieldAssignment> entry : argFieldMap.entrySet()) {
            String argName = entry.getKey();
            if (!ConfigUtils.isValidArgName(argName)) {
                throw new ConfigException(String.format(
                        "Argument names in argFieldMap must not be null or empty, given: nodeType=%s, documentNature=%s, documentFilters=%s, argFieldMap=%s",
                        nodeType, documentNature, documentFilters, argFieldMap));
            }
            if (entry.getValue() == null) {
                throw new ConfigException(String.format(
                        "Assignments in argFieldMap must not be null, given: (%s=null) with nodeType=%s, documentNature=%s, documentFilters=%s, argFieldMap=%s",
                        argName, nodeType, documentNature, documentFilters, argFieldMap));
            }
            if (!argName.equals(entry.getValue().arg().argName())) {
                throw new ConfigException(String.format(
                        "Argument name mismatch in argFieldMap (expected same argument name), given: nodeType=%s,  documentNature=%s, documentFilters=%s, argFieldMap=%s",
                        nodeType, documentNature, documentFilters, argFieldMap), AudlangMessage.argMsg(CommonErrors.ERR_3000_MAPPING_FAILED, argName));
            }
            if (!entry.getValue().field().nodeType().equals(nodeType)) {
                throw new ConfigException(
                        String.format("Fields must all be in the same nodeType, given: nodeType=%s, documentNature=%s, documentFilters=%s, argFieldMap=%s",
                                nodeType, documentNature, documentFilters, argFieldMap));
            }

        }
    }

    /**
     * Checks some basic rules on the filters (e.g. duplicates and illegal argument references)
     * 
     * @param nodeType
     * @param documentNature for debugging
     * @param documentFilters to be checked
     * @param argFieldMap
     */
    public static void validateDocumentFilters(String nodeType, SolrDocumentNature documentNature, List<FilterField> documentFilters,
            Map<String, ArgFieldAssignment> argFieldMap) {

        if (documentFilters == null) {
            return;
        }

        Set<String> allDataFieldNames = argFieldMap == null ? Collections.emptySet()
                : argFieldMap.values().stream().map(ArgFieldAssignment::field).map(DataField::fieldName).collect(Collectors.toCollection(HashSet::new));

        Map<String, FilterField> documentFilterFields = new HashMap<>();
        for (FilterField filterField : documentFilters) {
            if (filterField == null) {
                throw new ConfigException(
                        String.format("Illegal null-entry in documentFilters list, given: nodeType=%s, documentNature=%s, documentFilters=%s, argFieldMap=%s",
                                nodeType, documentNature, documentFilters, argFieldMap));
            }
            else if (allDataFieldNames.contains(filterField.fieldName())) {
                throw new ConfigException(String.format(
                        "A data field (%s) must not be specified as filter field, given: nodeType=%s, documentNature=%s, documentFilters=%s, argFieldMap=%s",
                        filterField.fieldName(), nodeType, documentNature, documentFilters, argFieldMap));
            }
            else if (documentFilterFields.putIfAbsent(filterField.fieldName(), filterField) != null) {
                throw new ConfigException(
                        String.format("Duplicate document filter field %s detected, given: nodeType=%s, documentNature=%s, documentFilters=%s, argFieldMap=%s",
                                filterField.fieldName(), nodeType, documentNature, documentFilters, argFieldMap));
            }
            List<String> variableNames = TemplateParameterUtils.extractVariableNames(filterField.filterValue());
            if (variableNames.contains(DefaultAutoMappingPolicy.VAR_ARG_NAME_LOCAL) || variableNames.contains(FilterField.VAR_ARG_NAME)) {
                throw new ConfigException(String.format(
                        "Cannot reference variable 'argName' in document filter (only global variables allowed), given: nodeType=%s, documentNature=%s, documentFilters=%s, argFieldMap=%s",
                        nodeType, documentNature, documentFilters, argFieldMap));
            }
            else if (!filterField.nodeType().equals(nodeType)) {
                throw new ConfigException(String.format(
                        "Filter fields must all be in the same nodeType, given: nodeType=%s, documentNature=%s, documentFilters=%s, argFieldMap=%s", nodeType,
                        documentNature, documentFilters, argFieldMap));

            }
        }
    }

    /**
     * Checks the sub configurations against the main settings to apply some rules
     * 
     * @param nodeType of the main config
     * @param documentFilters of the main config
     * @param argFieldMap of the main config
     * @param subDocumentConfigs to be validated
     */
    public static void validateSubDocumentConfigs(String nodeType, List<FilterField> documentFilters, Map<String, ArgFieldAssignment> argFieldMap,
            List<SubDocumentConfig> subDocumentConfigs) {

        if (subDocumentConfigs == null) {
            return;
        }

        Set<String> subNodeTypes = HashSet.newHashSet(subDocumentConfigs.size());
        Map<String, ArgFieldAssignment> allArgFieldMap = new HashMap<>(argFieldMap);
        for (SubDocumentConfig config : subDocumentConfigs) {
            if (config == null) {
                throw new ConfigException(String.format(
                        "Sub-document configuration entry null, given: main nodeType=%s, documentFilters=%s, argFieldMap=%s, subDocumentConfigs=%s", nodeType,
                        documentFilters, argFieldMap, subDocumentConfigs));
            }
            String subNodeType = config.nodeType();
            if (subNodeType.equals(nodeType)) {
                throw new ConfigException(String.format(
                        "Cannot have a sub-document configuration with the same nodeType=%s as the main document, given: main nodeType=%s, documentFilters=%s, argFieldMap=%s, subDocumentConfigs=%s",
                        subNodeType, nodeType, documentFilters, argFieldMap, subDocumentConfigs));
            }
            if (subNodeTypes.contains(subNodeType)) {
                throw new ConfigException(String.format(
                        "Duplicate definition of sub-nodeType %s in sub-configs detected, given: main nodeType=%s, documentFilters=%s, argFieldMap=%s, subDocumentConfigs=%s",
                        subNodeType, nodeType, documentFilters, argFieldMap, subDocumentConfigs));
            }
            subNodeTypes.add(subNodeType);
            ArgFieldAssignment duplicateAssignment = ConfigValidationUtils.detectDuplicateArgNameMapping(config, allArgFieldMap);
            if (duplicateAssignment != null) {
                throw new ConfigException(String.format(
                        "Duplicate mapping detected for argName=%s to nodeType=%s (already mapped to nodeType=%s, fieldName=%s), given: main nodeType=%s, documentFilters=%s, argFieldMap=%s, subDocumentConfigs=%s",
                        duplicateAssignment.arg().argName(), subNodeType, duplicateAssignment.field().nodeType(), duplicateAssignment.field().fieldName(),
                        nodeType, documentFilters, argFieldMap, subDocumentConfigs));

            }
            allArgFieldMap.putAll(config.argFieldMap());
        }
    }

    /**
     * Checks that fields referenced multiple times are referenced using the same Solr-field-type.
     * <p>
     * In Solr field-type mappings are global, so if the same field gets mapped in different (sub-)documents, the types must the same. Two Solr-types are
     * treated "same" if they have the same base type.
     * 
     * @param nodeType info only
     * @param documentNature info only
     * @param documentFilters check filter fields if not null
     * @param argFieldMap check data fields if not null
     * @param subDocumentConfigs check sub-documents if not null
     */
    public static void validateUniqueFieldTypes(String nodeType, SolrDocumentNature documentNature, List<FilterField> documentFilters,
            Map<String, ArgFieldAssignment> argFieldMap, List<SubDocumentConfig> subDocumentConfigs) {
        ConfigValidationUtils.validateCollectFieldTypeMappings(nodeType, documentNature, documentFilters, argFieldMap, subDocumentConfigs, new HashMap<>());
    }

    /**
     * Checks that fields referenced multiple times are referenced using the same Solr-field-type.
     * <p>
     * In Solr field-type mappings are global, so if the same field gets mapped in different (sub-)documents, the types must be the same. Two Solr-types are
     * treated "same" if they have the same base type.
     * 
     * @param nodeType info only
     * @param documentNature info only
     * @param documentFilters check filter fields (null ignored)
     * @param argFieldMap check data fields (null ignored)
     * @param subDocumentConfigs check sub-documents as well (null ignored)
     * @param collectedMappings field name to Solr-field mappings collected top-down
     */
    private static void validateCollectFieldTypeMappings(String nodeType, SolrDocumentNature documentNature, List<FilterField> documentFilters,
            Map<String, ArgFieldAssignment> argFieldMap, List<SubDocumentConfig> subDocumentConfigs, Map<String, AdlSolrField> collectedMappings) {

        if (documentFilters != null) {
            for (AdlSolrField currentField : documentFilters) {
                ConfigValidationUtils.validateCollectCurrentFieldTypeMapping(nodeType, documentNature, documentFilters, argFieldMap, subDocumentConfigs,
                        currentField, collectedMappings);
            }
        }
        if (argFieldMap != null) {
            argFieldMap.values().stream().map(ArgFieldAssignment::field).forEach(f -> ConfigValidationUtils.validateCollectCurrentFieldTypeMapping(nodeType,
                    documentNature, documentFilters, argFieldMap, subDocumentConfigs, f, collectedMappings));
        }
        if (subDocumentConfigs != null) {
            subDocumentConfigs.stream().forEach(sdc -> validateCollectFieldTypeMappings(sdc.nodeType(), sdc.documentNature(), sdc.documentFilters(),
                    sdc.argFieldMap(), null, collectedMappings));
        }
    }

    /**
     * Checks if was a previous mapping for the same Solr field and if so if the type is the same.
     * <p>
     * Field definitions in a Solr-schema are <i>global</i>, so the type cannot vary from document to document for the same field name. Two Solr-types are
     * treated "same" if they have the same base type.
     * 
     * @param nodeType
     * @param documentNature
     * @param documentFilters
     * @param argFieldMap
     * @param subDocumentConfigs
     * @param currentField
     * @param collectedMappings
     */
    private static void validateCollectCurrentFieldTypeMapping(String nodeType, SolrDocumentNature documentNature, List<FilterField> documentFilters,
            Map<String, ArgFieldAssignment> argFieldMap, List<SubDocumentConfig> subDocumentConfigs, AdlSolrField currentField,
            Map<String, AdlSolrField> collectedMappings) {
        AdlSolrField prevMapping = collectedMappings.putIfAbsent(currentField.fieldName(), currentField);
        if (prevMapping != null && !(prevMapping.fieldType().getBaseType().equals(currentField.fieldType().getBaseType()))) {
            throw new ConfigException(String.format("""
                    Incompatible duplicate type mapping detected: nodeType=%s, fieldName=%s, fieldType=%s (previously mapped at nodeType=%s with fieldType=%s)
                    Field definitions in a Solr-schema are global, a field's type is fixed and cannot vary from document to document.

                    given:
                    nodeType=%s, documentNature=%s, documentFilters=%s, argFieldMap=%s, subDocumentConfigs=%s
                    """, nodeType, currentField.fieldName(), currentField.fieldType(), prevMapping.nodeType(), prevMapping.fieldType(), nodeType,
                    documentNature, documentFilters, argFieldMap, subDocumentConfigs));
        }
    }

    /**
     * Checks whether static field mappings will be stolen by auto-mapping.
     * <p>
     * The resolution process <b>first</b> considers the sub-configurations in order of appearance in the configuration and <b>finally</b> the main
     * configuration. This means in particular:
     * <ul>
     * <li>Static argName mappings defined in the main configuration could be stolen by auto-mappers defined in any sub-configuration.</li>
     * <li>Static argName mappings defined in a sub-configuration (nested or dependent) could be stolen by auto-mappers of any previous sub-configuration</li>
     * </ul>
     * 
     * @param nodeType null ignored
     * @param documentFilters debug only
     * @param argFieldMap null ignored
     * @param subDocumentConfigs null ignored
     */
    public static void validateNoStaticFieldStealing(String nodeType, List<FilterField> documentFilters, Map<String, ArgFieldAssignment> argFieldMap,
            List<SubDocumentConfig> subDocumentConfigs) {

        argFieldMap = argFieldMap == null ? Collections.emptyMap() : argFieldMap;
        subDocumentConfigs = subDocumentConfigs == null ? Collections.emptyList() : subDocumentConfigs;

        // check the main config fields against all sub config because the main config is the fallback (comes last)
        for (String argName : argFieldMap.keySet()) {
            String errMsg = ConfigValidationUtils.createMessageOnFieldStealing(subDocumentConfigs, nodeType, argName);
            if (errMsg != null) {
                throw new ConfigException(String.format("%s%ngiven: nodeType=%s, documentFilters=%s, argFieldMap=%s, subDocumentConfigs=%s", errMsg, nodeType,
                        documentFilters, argFieldMap, subDocumentConfigs));
            }
        }

        // check sub-configs in order of appearance against predecessors
        List<SubDocumentConfig> previousConfigs = new ArrayList<>(subDocumentConfigs.size());
        for (SubDocumentConfig currentConfig : subDocumentConfigs) {
            for (String argName : currentConfig.argFieldMap().keySet()) {
                String errMsg = ConfigValidationUtils.createMessageOnFieldStealing(previousConfigs, currentConfig.nodeType(), argName);
                if (errMsg != null) {
                    throw new ConfigException(String.format("%s%ngiven: nodeType=%s, documentFilters=%s, argFieldMap=%s, subDocumentConfigs=%s", errMsg,
                            nodeType, documentFilters, argFieldMap, subDocumentConfigs));
                }
            }
            previousConfigs.add(currentConfig);
        }

    }

    /**
     * Verifies that <i>if</i> there is a logical model (guardianLookup) then this must be <i>the same</i> for the main config and all its sub-documents.
     * <p>
     * This shall protect us from weird setup mistakes.
     * 
     * @param nodeType for info
     * @param documentFilters for info
     * @param argFieldMap for info
     * @param subDocumentConfigs to be checked
     * @param guardianLookup main lookup
     */
    public static void validateGuardianLookups(String nodeType, List<FilterField> documentFilters, Map<String, ArgFieldAssignment> argFieldMap,
            List<SubDocumentConfig> subDocumentConfigs, ArgMetaInfoLookup guardianLookup) {

        if (subDocumentConfigs == null) {
            return;
        }
        ArgMetaInfoLookup guard = guardianLookup;
        for (SubDocumentConfig subConfig : subDocumentConfigs) {
            ArgMetaInfoLookup subGuard = subConfig.guardianLookup();
            if ((guard == null && subGuard != null) || (guard != null && !guard.equals(subGuard))) {
                throw new ConfigException(String.format(
                        "Inconsistent guardianLookup settings detected for nodeType=%s (logical model must be the same or null across main config)%n"
                                + "given: nodeType=%s, documentFilters=%s, argFieldMap=%s, subDocumentConfigs=%s",
                        subConfig.nodeType(), nodeType, documentFilters, argFieldMap, subDocumentConfigs));
            }
        }
    }

    /**
     * Returns an an error message string if the given argName gets consumed (mapper applicable) by any of the previous configs.
     * 
     * @param previousConfigs
     * @param currentNodeType
     * @param argName
     * @return error message string or null if no consumption
     */
    private static String createMessageOnFieldStealing(List<SubDocumentConfig> previousConfigs, String currentNodeType, String argName) {
        for (SubDocumentConfig previousConfig : previousConfigs) {
            if (previousConfig.contains(argName)) {
                return String.format("""
                        Field stealing detected! Auto-mapping must not hide static mappings.
                        The argName=%s statically mapped to nodeType=%s (victim) will be auto-mapped prematurely to nodeType=%s.
                        Resolution FIRST considers the sub-configurations (in order of appearance in the configuration) and
                        FINALLY checks the main document configuration.
                        Change your auto-mapper to ignore this field or remove the static mapping.
                        """, argName, currentNodeType, previousConfig.nodeType());
            }
        }
        return null;
    }

    /**
     * Checks whether the argument name is already mapped by the sub-config
     * 
     * @param currentSubConfig
     * @param allArgFieldMap all previous assignments
     * @return existing assignment
     */
    private static ArgFieldAssignment detectDuplicateArgNameMapping(SubDocumentConfig currentSubConfig, Map<String, ArgFieldAssignment> allArgFieldMap) {
        for (String argName : currentSubConfig.argFieldMap().keySet()) {
            ArgFieldAssignment prevAssignment = allArgFieldMap.get(argName);
            if (prevAssignment != null) {
                return prevAssignment;
            }
        }
        return null;
    }

}
