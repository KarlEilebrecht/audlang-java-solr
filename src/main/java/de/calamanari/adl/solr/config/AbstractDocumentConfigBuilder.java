//@formatter:off
/*
 * AbstractDocumentConfigBuilder
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

import de.calamanari.adl.AudlangMessage;
import de.calamanari.adl.CommonErrors;
import de.calamanari.adl.cnv.tps.AdlType;
import de.calamanari.adl.cnv.tps.ArgMetaInfo;
import de.calamanari.adl.cnv.tps.ArgMetaInfoLookup;
import de.calamanari.adl.cnv.tps.ConfigException;
import de.calamanari.adl.solr.AdlSolrType;
import de.calamanari.adl.solr.SolrFormatConstants;

/**
 * Implementation backing the fluent configuration builders for {@link MainDocumentConfig} and {@link SubDocumentConfig}.
 * <p>
 * This class avoids code duplication by putting some common logic in the abstract parent class, so the same code can be reused behind different builder
 * interfaces.
 * 
 * @param <T> concrete builder, selects the correct interface
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public abstract class AbstractDocumentConfigBuilder<T extends AbstractDocumentConfigBuilder<T>> {

    /**
     * builder instance (concrete class is parameterized with itself (T))
     */
    private final T builder;

    /**
     * The nodeType of the current document
     */
    protected String nodeType;

    /**
     * the lookup (if present) or null if this builder implicitly builds the logical data model
     */
    protected final ArgMetaInfoLookup argMetaInfoLookup;

    /**
     * mappings collected for the current document
     */
    protected final Map<String, ArgFieldAssignment> argFieldMap = new TreeMap<>();

    /**
     * List of all sub-document configs
     */
    protected List<SubDocumentConfig> subDocumentConfigs = new ArrayList<>();

    /**
     * optional filters the current (sub-)document
     */
    protected List<FilterField> documentFilters = new ArrayList<>();

    /**
     * current field name
     */
    protected String fieldName = null;

    /**
     * current field type
     */
    protected AdlSolrType fieldType = null;

    /**
     * Configured setting for isCollection of a field. If null, auto-mapping shall make the decision
     */
    protected Boolean isCollection = null;

    /**
     * argName mapped to the current field
     */
    protected String mappedArgName;

    /**
     * type of the arg mapped to the current field
     */
    protected AdlType mappedArgType;

    /**
     * List of all the policies for the current document in order of configuration
     */
    protected List<AutoMappingPolicy> autoMappingPolicies = new ArrayList<>();

    /**
     * nature of a the current sub-document
     */
    protected SolrDocumentNature documentNature = null;

    /**
     * Indicates that the field building has started, this is only to distinguish between the initial state (no pending field) and the working state
     */
    protected boolean havePendingField = false;

    /**
     * If not null then there is a pending auto-mapping to be created
     */
    protected AutoMappingConfig pendingAutoMappingConfig = null;

    /**
     * multi-doc marker (default setting)
     */
    protected boolean isMultiDocDefault = false;

    /**
     * multi-doc marker
     */
    protected boolean isMultiDoc = false;

    /**
     * Creates a new builder for the given node type with optional meta-data
     * 
     * @param nodeType
     * @param argMetaInfoLookup optional meta model, may be null
     */
    protected AbstractDocumentConfigBuilder(String nodeType, ArgMetaInfoLookup argMetaInfoLookup) {
        this.argMetaInfoLookup = argMetaInfoLookup;
        // need explicit cast, otherwise the compiler does not understand
        // that T must be the "type of this"
        @SuppressWarnings("unchecked")
        T instance = (T) this;
        this.builder = instance;
        this.nodeType = nodeType;
    }

    /**
     * @see ConfigBuilderInterfaces.TemplateDocumentAddFilterField#filteredBy(String, AdlSolrType, String)
     */
    public T filteredBy(String fieldName, AdlSolrType fieldType, String filterValue) {
        documentFilters.add(new FilterField(nodeType, fieldName, fieldType, filterValue));
        return builder;
    }

    /**
     * @see ConfigBuilderInterfaces.TemplateDocumentAddAutoMapping#autoMapped(LocalArgNameExtractor)
     */
    public T autoMapped(LocalArgNameExtractor extractor) {
        return this.autoMapped(extractor, null, null, null);
    }

    /**
     * @see ConfigBuilderInterfaces.TemplateDocumentAddAutoMapping#autoMapped(LocalArgNameExtractor, AdlType, AdlSolrType, Boolean)
     */
    public T autoMapped(LocalArgNameExtractor extractor, AdlType argType, AdlSolrType adlSolrType, Boolean isCollection) {
        addPendingFieldOrAutoMapping();
        if (extractor == null) {
            throw new ConfigException(String.format("The argument 'extractor' must not be null (nodeType=%s).", nodeType));
        }
        pendingAutoMappingConfig = new AutoMappingConfig(extractor, argType, adlSolrType, isCollection);
        return builder;
    }

    /**
     * @see ConfigBuilderInterfaces.TemplateDocumentAddAutoMapping#autoMapped(BiFunction)
     */
    public T autoMapped(BiFunction<String, ArgMetaInfoLookup, AutoMappingPolicy> policyCreator) {
        addPendingFieldOrAutoMapping();
        if (policyCreator == null) {
            throw new ConfigException(String.format("The argument policyCreator must not be null (nodeType=%s).", nodeType));

        }
        AutoMappingPolicy policy = policyCreator.apply(nodeType, argMetaInfoLookup);
        if (policy == null) {
            throw new ConfigException(String.format("The policy returned by the given policyCreator was null (nodeType=%s, argMetaInfoLookup=<%s>).", nodeType,
                    argMetaInfoLookup == null ? "null" : argMetaInfoLookup.getClass().getSimpleName()));

        }
        autoMappingPolicies.add(policy);
        return builder;

    }

    /**
     * @see ConfigBuilderInterfaces.MainDocumentAddAutoMapping#defaultAutoMapped()
     */
    public T defaultAutoMapped() {
        return autoMapped(LocalArgNameExtractor.validateOnly());
    }

    /**
     * @see ConfigBuilderInterfaces.TemplateDocumentSetMultiDoc#multiDoc()
     */
    public T multiDoc() {
        isMultiDoc = true;
        return builder;
    }

    /**
     * @see ConfigBuilderInterfaces.TemplateDocumentSetMultiDoc#notMultiDoc()
     */
    public T notMultiDoc() {
        isMultiDoc = false;
        return builder;
    }

    /**
     * @see ConfigBuilderInterfaces.MainDocumentAddSubConfig#subConfig(SubDocumentConfig)
     */
    public T subConfig(SubDocumentConfig subDocumentConfig) {
        addPendingFieldOrAutoMapping();
        this.subDocumentConfigs.add(subDocumentConfig);
        return builder;
    }

    /**
     * @see ConfigBuilderInterfaces.TemplateDocumentSetNature#nested()
     */
    public T nested() {
        this.documentNature = SolrDocumentNature.NESTED;
        this.isMultiDocDefault = false;
        this.isMultiDoc = false;
        return builder;
    }

    /**
     * @see ConfigBuilderInterfaces.TemplateDocumentSetNature#dependent()
     */
    public T dependent() {
        this.documentNature = SolrDocumentNature.DEPENDENT;
        this.isMultiDocDefault = false;
        this.isMultiDoc = false;
        return builder;
    }

    /**
     * @see ConfigBuilderInterfaces.TemplateDocumentSetNature#nestedMultiDoc()
     */
    public T nestedMultiDoc() {
        this.documentNature = SolrDocumentNature.NESTED;
        this.isMultiDocDefault = true;
        this.isMultiDoc = true;
        return builder;
    }

    /**
     * @see ConfigBuilderInterfaces.TemplateDocumentSetNature#dependentMultiDoc()
     */
    public T dependentMultiDoc() {
        this.documentNature = SolrDocumentNature.DEPENDENT;
        this.isMultiDocDefault = true;
        this.isMultiDoc = true;
        return builder;
    }

    /**
     * @see ConfigBuilderInterfaces.TemplateDocumentAddField#dataField(String, AdlSolrType)
     */
    public T dataField(String fieldName, AdlSolrType fieldType) {
        addPendingFieldOrAutoMapping();
        if (fieldName == null) {
            throw new ConfigException(
                    String.format("The argument fieldName for an explicitly defined dataField(fieldName=%s, fieldType=%s) must not be null (nodeType=%s).",
                            fieldName, fieldType, nodeType),
                    AudlangMessage.msg(CommonErrors.ERR_3000_MAPPING_FAILED));
        }
        if (fieldType == null) {
            fieldType = ConventionUtils.determineGenericSolrFieldType(fieldName);
            if (fieldType == null) {
                throw new ConfigException(String.format(
                        "Unable to derive the Solr fieldType from fieldName=%s (nodeType=%s) by applying standard solar conventions (supported suffixes: %s).",
                        fieldName, nodeType, SolrFormatConstants.SOLR_STANDARD_SUFFIXES));
            }
        }
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.havePendingField = true;
        return builder;
    }

    /**
     * @see ConfigBuilderInterfaces.TemplateDocumentAddField#dataField(String)
     */
    public T dataField(String fieldName) {
        return dataField(fieldName, null);
    }

    /**
     * @see ConfigBuilderInterfaces.TemplateDocumentAddFieldStep1#mappedToArgName(String, AdlType)
     */
    public T mappedToArgName(String argName, AdlType argType) {
        if (argName == null) {
            throw new ConfigException(String.format(
                    "The argument argName for an explicitly mapped ((fieldName=%s, fieldType=%s) mappedToArgName(argName=%s, argType=%s)) must not be null (nodeType=%s).",
                    fieldName, fieldType, argName, argType, nodeType), AudlangMessage.msg(CommonErrors.ERR_3000_MAPPING_FAILED));
        }

        if (argType == null && argMetaInfoLookup != null) {
            argType = argMetaInfoLookup.typeOf(argName);
        }

        if (argType == null) {
            argType = ConventionUtils.resolveArgTypeFromFieldType(fieldType);
        }

        if (argType == null) {
            throw new ConfigException(
                    String.format("Unable to resolve the argType for argName=%s from the Solr field (fieldName=%s, fieldType=%s, nodeType=%s).", argName,
                            fieldName, fieldType, nodeType));
        }

        boolean autoDetectedCollectionFlag = ConventionUtils.determineGenericIsCollection(fieldName);
        if (autoDetectedCollectionFlag) {
            // if the type was previously specified explicitly (not auto-detected),
            // it could happen that the suffix indicates a collection, but it is not.
            // Thus, we double-check if the auto-detected type matches the current type
            // This way the suffix "_ss" for a field that for whatever reason is an integer value
            // won't be automatically identified as a collection
            AdlSolrType autoDetectedType = ConventionUtils.determineGenericSolrFieldType(fieldName);
            if (autoDetectedType != null && autoDetectedType.getBaseType().equals(fieldType.getBaseType())) {
                this.isCollection = true;
            }
        }

        this.mappedArgType = argType;
        this.mappedArgName = argName;
        return builder;

    }

    /**
     * @see ConfigBuilderInterfaces.TemplateDocumentAddFieldStep1#mappedToArgName(String)
     */
    public T mappedToArgName(String argName) {
        return mappedToArgName(argName, null);
    }

    /**
     * @see ConfigBuilderInterfaces.TemplateDocumentAddFieldStep2#asCollection()
     */
    public T asCollection() {
        this.isCollection = true;
        return builder;
    }

    /**
     * @see ConfigBuilderInterfaces.TemplateDocumentAddFieldStep2#notAsCollection()
     */
    public T notAsCollection() {
        this.isCollection = false;
        return builder;
    }

    /**
     * Takes the collected data and adds a new field, resets the builder for the next field or auto-mapping
     */
    protected void addPendingFieldOrAutoMapping() {

        if (havePendingField) {
            if (isCollection == null) {
                isCollection = ConventionUtils.determineGenericIsCollection(fieldName);
            }
            DataField field = new DataField(nodeType, fieldName, fieldType, isCollection);
            ArgMetaInfo argMetaInfo = new ArgMetaInfo(mappedArgName, mappedArgType, false, isCollection);

            ArgFieldAssignment assignment = new ArgFieldAssignment(argMetaInfo, field, isMultiDoc);

            ArgFieldAssignment existingAssignment = argFieldMap.get(mappedArgName);
            if (existingAssignment != null) {
                throw new ConfigException(
                        String.format("Duplicate mapping detected mappedToArgName(%s) : (1) %s vs. (2) %s ", mappedArgName, existingAssignment, assignment));
            }
            argFieldMap.put(mappedArgName, assignment);
        }
        if (pendingAutoMappingConfig != null) {
            autoMappingPolicies.add(new DefaultAutoMappingPolicy(nodeType, pendingAutoMappingConfig.extractor(), pendingAutoMappingConfig.argType(),
                    pendingAutoMappingConfig.adlSolrType(), pendingAutoMappingConfig.isCollection(), isMultiDoc, argMetaInfoLookup));
            pendingAutoMappingConfig = null;
        }
        fieldName = null;
        fieldType = null;
        mappedArgName = null;
        mappedArgType = null;
        isCollection = false;
        havePendingField = false;
        isMultiDoc = isMultiDocDefault;
    }

    /**
     * @return Sub-document config based on the current settings
     */
    public SubDocumentConfig createSubDocumentConfig() {
        addPendingFieldOrAutoMapping();
        return new SubDocumentConfig(nodeType, documentNature, documentFilters, argFieldMap,
                autoMappingPolicies.isEmpty() ? DefaultAutoMappingPolicy.NONE : new CompositeAutoMappingPolicy(autoMappingPolicies), argMetaInfoLookup);
    }

    /**
     * @return Main-document config based on the current settings
     */
    public MainDocumentConfig createMainDocumentConfig() {
        addPendingFieldOrAutoMapping();
        return new MainDocumentConfig(nodeType, documentFilters, argFieldMap,
                autoMappingPolicies.isEmpty() ? DefaultAutoMappingPolicy.NONE : new CompositeAutoMappingPolicy(autoMappingPolicies), subDocumentConfigs,
                argMetaInfoLookup);
    }

    /**
     * Stores the parameters for an auto-mapper (postponed construction within the builder's flow)
     * 
     * @param extractor pending extractor function
     * @param argType type of the argName to be auto-mapped
     * @param adlSolrType type of the Solr field to be auto-mapped
     * @param isCollection multi-value Solr-field or not
     * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
     */
    protected static record AutoMappingConfig(LocalArgNameExtractor extractor, AdlType argType, AdlSolrType adlSolrType, Boolean isCollection) {
    }

}
