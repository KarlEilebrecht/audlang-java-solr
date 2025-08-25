//@formatter:off
/*
 * ConfigBuilderInterfaces
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

import java.util.function.BiFunction;

import de.calamanari.adl.cnv.tps.AdlType;
import de.calamanari.adl.cnv.tps.ArgMetaInfoLookup;
import de.calamanari.adl.cnv.tps.DefaultAdlType;
import de.calamanari.adl.solr.AdlSolrType;
import de.calamanari.adl.solr.DefaultAdlSolrType;

/**
 * This class contains the interfaces for the fluent builder flows for creating fields and document configurations for Solr.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class ConfigBuilderInterfaces {

    private ConfigBuilderInterfaces() {
        // only interfaces for builders
    }

    /**
     * Template interface for documentation
     */
    public static interface TemplateDocumentSetNature {

        /**
         * Tells that this sub-config describes a nested document, the parent is the main-document (no sub-nesting).
         * 
         * @return builder
         * @see SolrDocumentNature#NESTED
         */
        Object nested();

        /**
         * Tells that this sub-config describes a nested document, the parent is the main-document (no sub-nesting).
         * <p>
         * Additionally, sets <b><code>isMultiDoc=true</code></b> for all fields in the sub-configuration (if not explicitly specified on the assignment).
         * 
         * @return builder
         * @see SolrDocumentNature#NESTED
         * @see ArgFieldAssignment
         */
        Object nestedMultiDoc();

        /**
         * Tells that this sub-config describes a <i>dependent</i> document. These documents exist independently from the main document and will be joined at
         * runtime via their <code><b>main_id</b></code> field which points to the main-document's <code><b>id</b></code>.
         * 
         * @return builder
         * @see SolrDocumentNature#DEPENDENT
         */
        Object dependent();

        /**
         * Tells that this sub-config describes a <i>dependent</i> document. These documents exist independently from the main document and will be joined at
         * runtime via their <code><b>main_id</b></code> field which points to the main-document's <code><b>id</b></code>.
         * <p>
         * Additionally, sets <b><code>isMultiDoc=true</code></b> for all fields in the sub-configuration (if not explicitly specified on the assignment).
         * 
         * @return builder
         * @see SolrDocumentNature#DEPENDENT
         * @see ArgFieldAssignment
         */
        Object dependentMultiDoc();

    }

    /**
     * Template interface for documentation
     */
    public static interface TemplateDocumentAddField {

        /**
         * Defines a data field of the given Solr type.
         * <p>
         * If the given field-type is null, then this method behaves like {@link #dataField(String)}.
         * 
         * @see DataField
         * @param fieldName Solr field name
         * @param fieldType Solr type of the field
         * @return builder
         */
        Object dataField(String fieldName, AdlSolrType fieldType);

        /**
         * Defines a data field of the given name. Type will be derived from the given fieldName according to Solr conventions
         * {@link ConventionUtils#determineGenericSolrFieldType(String)}
         * 
         * @see DataField
         * @param fieldName Solr field name
         * @return builder
         */
        Object dataField(String fieldName);
    }

    /**
     * Template interface for documentation
     */
    public static interface TemplateDocumentAddFieldStep1 {

        /**
         * Maps the Solr-field to an attribute (argName) of the logical data model.<br>
         * You can map the same field to multiple attributes but not vice-versa.
         * <p>
         * This method is meant for setting up the logical and physical data model at the same time.
         * <p>
         * The preferred way should be first setting up an {@link ArgMetaInfoLookup} with the argName's meta data and configuring the table with
         * {@link #mappedToArgName(String)}.
         * <p>
         * If parameter argType is null, then this method behaves like {@link #mappedToArgName(String)}.
         * 
         * @param argName name of the attribute used in expressions that should be mapped to the Solr-field
         * @param argType type of the mapped argument in the logical data model
         * @return builder
         */
        Object mappedToArgName(String argName, AdlType argType);

        /**
         * Maps the Solr-field to an attribute (argName) of the logical data model which has been configured in advance or you want to auto-detect the argument
         * type based on the type of the Solr-field.<br>
         * You can map the same field to multiple attributes but not vice-versa.
         * <p>
         * If there is no logical data model configured, the argType will be resolved <i>bottom-up</i> from the Solr fieldType (see
         * {@link ConventionUtils#resolveArgTypeFromFieldType(AdlSolrType)}).
         * 
         * @param argName name of the attribute used in expressions that should be mapped to the Solr-field
         * @return builder
         */
        Object mappedToArgName(String argName);

    }

    /**
     * Template interface for documentation
     */
    public static interface TemplateDocumentAddFieldStep2 {

        /**
         * Tells that this field is a collection, same as <code>collection(<b>true</b>)</code>
         * 
         * @see DataField
         * @return builder
         */
        Object asCollection();

        /**
         * Tells that this field is a collection or not
         * <p>
         * This method <b>only</b> exists for the weird case that you want to mark a field <i>single-value</i> which would be otherwise marked as collection
         * according to naming convention.
         * 
         * @see DataField
         * @return builder
         */
        Object notAsCollection();

    }

    /**
     * Template interface for documentation
     */
    public static interface TemplateDocumentSetMultiDoc {

        /**
         * Tells that the argument to field assignment shall be <b><code>isMultiDoc=true</code></b>.
         * <p>
         * If not explicitly configured, the isMultiDoc property will be inherited from the sub-document (e.g., dependentMultiDoc()).
         * 
         * @see ArgFieldAssignment
         * @return builder
         */
        Object multiDoc();

        /**
         * Tells that the argument to field assignment shall be <b><code>isMultiDoc=false</code></b>.
         * <p>
         * If not explicitly configured, the isMultiDoc property will be inherited from the sub-document (e.g., dependentMultiDoc()).
         * 
         * @see ArgFieldAssignment
         * @return builder
         */
        Object notMultiDoc();

    }

    /**
     * Template interface for documentation
     */
    public static interface TemplateDocumentAddFilterField {

        /**
         * Defines an <i>extra filter</i> on a solr-field of the same document. This is meant to restrict the documents to be considered independently from the
         * current query (e.g., a system tenant, or "is_active"-marker).
         * <p>
         * <b>Note:</b> The actual <i>filterValue</i> (either hard-coded or from a variable) must be an ADL-value.
         * <p>
         * <b>Example:</b>
         * <ul>
         * <li><b>correct:</b> <code>filteredBy("active", SOLR_BOOLEAN, <b>"1"</b>)</code></li>
         * <li>incorrect: <code>filteredBy("active", SOLR_BOOLEAN, <s>"TRUE"</s>)</code></li>
         * </ul>
         * 
         * @see DataField
         * 
         * @param fieldName Solr field name
         * @param fieldType type of the filter field for query generation
         * @param filterValue the filter, a <b>fixed value or variable placeholder ${varName}</b> to include into any query on the current document type
         * @return builder
         */
        Object filteredBy(String fieldName, AdlSolrType fieldType, String filterValue);

    }

    /**
     * Template interface for documentation
     */
    public static interface TemplateDocumentAddAutoMapping {

        /**
         * Maps argNames in a generic way (see {@link ConventionUtils}) to Solr-columns based on the local name returned by the given extractor function.
         * <p>
         * <b>Example:</b> Given argName is <code><b>srv.answer</b></code> then the extractor function might be
         * <code>argName -> argName.startsWith("srv.") ? (argName.substring(4) + "_ss") : null</code> which results the local argName
         * <code><b>answer_ss</b></code>.
         * <p>
         * By applying the Solr-conventions we get
         * <ul>
         * <li>Solr-field-name: <code><b>answer_ss</b></code></li>
         * <li>argType: {@link DefaultAdlType#STRING}</li>
         * <li>Solr field type: {@link DefaultAdlSolrType#SOLR_STRING}</li>
         * <li>isCollection: <code><b>true</b></code></li>
         * </ul>
         * <b>Important:</b>
         * <ul>
         * <li><i>First</i> all sub-configuration's auto-mappers will be considered before <i>finally</i> calling the main configuration's auto-mapper(s).<br>
         * By re-ordering the sub-configurations you can create a priority chain of auto-mappers.</li>
         * <li>The extractor must return a valid Solr name ({@link ConfigUtils#isValidSolrName(String)}), otherwise this auto-mapping rule will be skipped.</li>
         * </ul>
         * <p>
         * <b>Remark:</b> For better clarity, the argument 'extractor' must not be null. If you really want to achieve a pass-through extractor (greedy!) then
         * specify {@link LocalArgNameExtractor#validateOnly()}.
         * 
         * @param extractor instruction how to derive the local argName (Solr dynamic field name) from the given argName NOT NULL
         * @return builder
         * @see DefaultAutoMappingPolicy
         */
        Object autoMapped(LocalArgNameExtractor extractor);

        /**
         * Same as {@link #autoMapped(LocalArgNameExtractor)} with the option to set argType, adlSolrType and isCollection to fixed values.
         * 
         * @param extractor instruction how to derive the local argName (Solr dynamic field name) from the given argName, NOT NULL
         * @param argType fixed argument type (null means derived)
         * @param adlSolrType fixed Solr-type (null means derived)
         * @param isCollection fixed collection setting (null means derived)
         * @return builder
         * @see DefaultAutoMappingPolicy
         */
        Object autoMapped(LocalArgNameExtractor extractor, AdlType argType, AdlSolrType adlSolrType, Boolean isCollection);

        /**
         * Performs auto-mapping using the custom policy returned by the policy-creator. The arguments of the function are <b><code>nodeType</code></b> and the
         * optional logical data model (lookup may be null) of the surrounding configuration. <br>
         * Be aware that the given function gets executed immediately (inside the configuration builder, other than {@link #autoMapped(LocalArgNameExtractor)}
         * and {@link #autoMapped(LocalArgNameExtractor, AdlType, AdlSolrType, Boolean)}) and thus won't be subject to serialization.
         * <p>
         * <b>Important:</b><i>First</i> all sub-configuration's auto-mappers will be considered before <i>finally</i> calling the main configuration's
         * auto-mapper(s).<br>
         * By re-ordering the sub-configurations you can create a priority chain of auto-mappers.
         * 
         * @param policyCreator custom policy creator
         * @return builder
         */
        Object autoMapped(BiFunction<String, ArgMetaInfoLookup, AutoMappingPolicy> policyCreator);

    }

    /**
     * Fluent API to build a {@link SubDocumentConfig}
     */
    public static interface SubDocumentExit {

        /**
         * @return the sub document configuration
         */
        SubDocumentConfig get();

    }

    /**
     * Fluent API to build a {@link SubDocumentConfig}
     */
    public static interface SubDocumentSetNature extends TemplateDocumentSetNature {

        @Override
        SubDocumentAddFilterFieldOrFieldOrAutoMapping nested();

        @Override
        SubDocumentAddFilterFieldOrFieldOrAutoMapping dependent();

        @Override
        SubDocumentAddFilterFieldOrFieldOrAutoMapping nestedMultiDoc();

        @Override
        SubDocumentAddFilterFieldOrFieldOrAutoMapping dependentMultiDoc();

    }

    /**
     * Fluent API to add field to a sub-document
     */
    public static interface SubDocumentAddField extends TemplateDocumentAddField {

        @Override
        SubDocumentAddFieldStep1 dataField(String fieldName, AdlSolrType fieldType);

        @Override
        SubDocumentAddFieldStep1 dataField(String fieldName);
    }

    /**
     * Fluent API to build a {@link DataField} of a sub-config
     */
    public static interface SubDocumentAddFieldStep1 extends TemplateDocumentAddFieldStep1 {

        @Override
        SubDocumentAddFieldStep2OrStep3OrAddFieldOrAutoMappingOrExit mappedToArgName(String argName, AdlType argType);

        @Override
        SubDocumentAddFieldStep2OrStep3OrAddFieldOrAutoMappingOrExit mappedToArgName(String argName);

    }

    /**
     * Fluent API to build a {@link DataField} of a sub-config
     */
    public static interface SubDocumentAddFieldStep2 extends TemplateDocumentAddFieldStep2 {

        @Override
        SubDocumentAddFieldStep3OrAddFieldOrAutoMappingOrExit asCollection();

        @Override
        SubDocumentAddFieldStep3OrAddFieldOrAutoMappingOrExit notAsCollection();

    }

    /**
     * Fluent API to build a {@link DataField} of a sub-config
     */
    public static interface SubDocumentAddFieldStep3 extends TemplateDocumentSetMultiDoc {

        @Override
        SubDocumentAddFieldOrAutoMappingOrExit multiDoc();

        @Override
        SubDocumentAddFieldOrAutoMappingOrExit notMultiDoc();

    }

    /**
     * Fluent API to build a {@link FilterField} of a sub-config
     */
    public static interface SubDocumentAddFilterField extends TemplateDocumentAddFilterField {

        @Override
        SubDocumentAddFilterFieldOrFieldOrAutoMapping filteredBy(String fieldName, AdlSolrType fieldType, String filterValue);

    }

    /**
     * Fluent API to build a {@link SubDocumentConfig}
     */
    public static interface SubDocumentAddAutoMapping extends TemplateDocumentAddAutoMapping {

        @Override
        SubDocumentAddAutoMappingStep2OrAutoMappingOrExit autoMapped(LocalArgNameExtractor extractor);

        @Override
        SubDocumentAddAutoMappingStep2OrAutoMappingOrExit autoMapped(LocalArgNameExtractor extractor, AdlType argType, AdlSolrType adlSolrType,
                Boolean isCollection);

        @Override
        SubDocumentAddAutoMappingOrExit autoMapped(BiFunction<String, ArgMetaInfoLookup, AutoMappingPolicy> policyCreator);

    }

    /**
     * Fluent API to build a {@link DataField} of a sub-config
     */
    public static interface SubDocumentAddAutoMappingStep2 extends TemplateDocumentSetMultiDoc {

        @Override
        SubDocumentAddAutoMappingOrExit multiDoc();

        @Override
        SubDocumentAddAutoMappingOrExit notMultiDoc();

    }

    /**
     * Fluent API to build a {@link SubDocumentConfig}
     */
    public static interface SubDocumentAddFieldOrAutoMapping extends SubDocumentAddField, SubDocumentAddAutoMapping {
        // combined
    }

    /**
     * Fluent API to build a {@link SubDocumentConfig}
     */
    public static interface SubDocumentAddFieldStep2OrStep3OrAddFieldOrAutoMappingOrExit
            extends SubDocumentAddFieldStep2, SubDocumentAddFieldStep3, SubDocumentAddField, SubDocumentAddAutoMapping, SubDocumentExit {
        // combined
    }

    /**
     * Fluent API to build a {@link SubDocumentConfig}
     */
    public static interface SubDocumentAddFieldStep3OrAddFieldOrAutoMappingOrExit
            extends SubDocumentAddFieldStep3, SubDocumentAddField, SubDocumentAddAutoMapping, SubDocumentExit {
        // combined
    }

    /**
     * Fluent API to build a {@link SubDocumentConfig}
     */
    public static interface SubDocumentAddAutoMappingOrExit extends SubDocumentAddAutoMapping, SubDocumentExit {
        // combined
    }

    /**
     * Fluent API to build a {@link SubDocumentConfig}
     */
    public static interface SubDocumentAddAutoMappingStep2OrAutoMappingOrExit
            extends SubDocumentAddAutoMappingStep2, SubDocumentAddAutoMapping, SubDocumentExit {
        // combined
    }

    /**
     * Fluent API to build a {@link SubDocumentConfig}
     */
    public static interface SubDocumentAddFieldOrAutoMappingOrExit extends SubDocumentAddField, SubDocumentAddAutoMappingOrExit {
        // combined
    }

    /**
     * Fluent API to build a {@link SubDocumentConfig}
     */
    public static interface SubDocumentAddFilterFieldOrFieldOrAutoMapping extends SubDocumentAddFilterField, SubDocumentAddField, SubDocumentAddAutoMapping {
        // combined
    }

    /**
     * Fluent API to build a {@link SubDocumentConfig}
     */
    public static interface SubDocumentConfigBuilder
            extends SubDocumentSetNature, SubDocumentAddField, SubDocumentAddFieldStep1, SubDocumentAddFieldStep2OrStep3OrAddFieldOrAutoMappingOrExit,
            SubDocumentAddFieldStep3OrAddFieldOrAutoMappingOrExit, SubDocumentAddAutoMappingOrExit, SubDocumentAddAutoMappingStep2OrAutoMappingOrExit,
            SubDocumentAddFilterFieldOrFieldOrAutoMapping, SubDocumentAddFieldOrAutoMapping, SubDocumentAddFieldOrAutoMappingOrExit {
        // tagging interface to centralize the knowledge about the required combination here
    }

    /**
     * Fluent API to build a {@link MainDocumentConfig}
     */
    public static interface MainDocumentExit {

        /**
         * @return the sub document configuration
         */
        public MainDocumentConfig get();

    }

    /**
     * Fluent API to build a {@link MainDocumentConfig}
     */
    public static interface MainDocumentAddField extends TemplateDocumentAddField {

        @Override
        MainDocumentAddFieldStep1 dataField(String fieldName, AdlSolrType fieldType);

        @Override
        MainDocumentAddFieldStep1 dataField(String fieldName);

    }

    /**
     * Fluent API to build a {@link DataField} of a main-config
     */
    public static interface MainDocumentAddFieldStep1 extends TemplateDocumentAddFieldStep1 {

        @Override
        MainDocumentAddFieldStep2OrFieldOrAutoMappingOrSubConfigOrExit mappedToArgName(String argName, AdlType argType);

        @Override
        MainDocumentAddFieldStep2OrFieldOrAutoMappingOrSubConfigOrExit mappedToArgName(String argName);

    }

    /**
     * Fluent API to build a {@link DataField} of a main-config
     */
    public static interface MainDocumentAddFieldStep2 extends TemplateDocumentAddFieldStep2 {

        @Override
        MainDocumentAddFieldOrAutoMappingOrSubConfigOrExit asCollection();

        @Override
        MainDocumentAddFieldOrAutoMappingOrSubConfigOrExit notAsCollection();

    }

    /**
     * Fluent API to build a {@link FilterField} of a main-config
     */
    public static interface MainDocumentAddFilterField extends TemplateDocumentAddFilterField {

        @Override
        MainDocumentAddFilterFieldOrFieldOrAutoMappingOrSubConfig filteredBy(String fieldName, AdlSolrType fieldType, String filterValue);

    }

    /**
     * Fluent API to build a {@link MainDocumentConfig}
     */
    public static interface MainDocumentAddAutoMapping extends TemplateDocumentAddAutoMapping {

        @Override
        MainDocumentAddAutoMappingOrSubConfigOrExit autoMapped(LocalArgNameExtractor extractor);

        @Override
        MainDocumentAddAutoMappingOrSubConfigOrExit autoMapped(LocalArgNameExtractor extractor, AdlType argType, AdlSolrType adlSolrType, Boolean isCollection);

        @Override
        MainDocumentAddAutoMappingOrSubConfigOrExit autoMapped(BiFunction<String, ArgMetaInfoLookup, AutoMappingPolicy> policyCreator);

        /**
         * Performs auto-mapping fully relying on conventions. It behaves exactly like {@link #autoMapped(LocalArgNameExtractor)} with the identity as extractor
         * function.
         * <p>
         * Because it is greedy, it is the last in a chain of auto-mappers. Keep in mind that all sub-config's (auto-)mappers are executed <i>before</i> the
         * main-config's mapping!
         * 
         * @return builder
         */
        MainDocumentAddSubConfigOrExit defaultAutoMapped();

    }

    /**
     * Fluent API to build a {@link MainDocumentConfig}
     */
    public static interface MainDocumentAddSubConfig {

        /**
         * Adds the given sub-configuration to the main configuration
         * 
         * @param subDocumentConfig to be added
         * @return builder
         */
        MainDocumentAddSubConfigOrExit subConfig(SubDocumentConfig subDocumentConfig);

    }

    /**
     * Fluent API to build a {@link MainDocumentConfig}
     */
    public static interface MainDocumentAddFieldOrAutoMapping extends MainDocumentAddField, MainDocumentAddAutoMapping {
        // combined
    }

    /**
     * Fluent API to build a {@link MainDocumentConfig}
     */
    public static interface MainDocumentAddFieldStep2OrFieldOrAutoMappingOrSubConfigOrExit
            extends MainDocumentAddFieldStep2, MainDocumentAddField, MainDocumentAddAutoMapping, MainDocumentAddSubConfig, MainDocumentExit {
        // combined
    }

    /**
     * Fluent API to build a {@link MainDocumentConfig}
     */
    public static interface MainDocumentAddAutoMappingOrSubConfigOrExit extends MainDocumentAddAutoMapping, MainDocumentAddSubConfig, MainDocumentExit {
        // combined
    }

    /**
     * Fluent API to build a {@link MainDocumentConfig}
     */
    public static interface MainDocumentAddFilterFieldOrFieldOrAutoMappingOrSubConfig
            extends MainDocumentAddFilterField, MainDocumentAddField, MainDocumentAddAutoMapping, MainDocumentAddSubConfig {
        // combined
    }

    /**
     * Fluent API to build a {@link MainDocumentConfig}
     */
    public static interface MainDocumentAddFieldOrAutoMappingOrSubConfigOrExit
            extends MainDocumentAddField, MainDocumentAddAutoMapping, MainDocumentAddSubConfig, MainDocumentExit {
        // combined
    }

    /**
     * Fluent API to build a {@link MainDocumentConfig}
     */
    public static interface MainDocumentAddSubConfigOrExit extends MainDocumentAddSubConfig, MainDocumentExit {
        // combined
    }

    /**
     * Fluent API to build a {@link MainDocumentConfig}
     */
    public static interface MainDocumentConfigBuilder extends MainDocumentAddField, MainDocumentAddFieldStep1, MainDocumentAddFieldStep2,
            MainDocumentAddAutoMappingOrSubConfigOrExit, MainDocumentAddSubConfigOrExit, MainDocumentAddFilterFieldOrFieldOrAutoMappingOrSubConfig,
            MainDocumentAddFieldOrAutoMappingOrSubConfigOrExit, MainDocumentAddFieldStep2OrFieldOrAutoMappingOrSubConfigOrExit {
        // tagging interface to centralize the knowledge about the required combination here
    }

}
