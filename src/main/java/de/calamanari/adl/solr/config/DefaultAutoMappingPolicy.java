//@formatter:off
/*
 * DefaultAutoMappingPolicy
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
import static de.calamanari.adl.solr.config.ConventionUtils.determineGenericArgType;
import static de.calamanari.adl.solr.config.ConventionUtils.determineGenericIsCollection;
import static de.calamanari.adl.solr.config.ConventionUtils.determineGenericSolrFieldType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.calamanari.adl.AudlangMessage;
import de.calamanari.adl.CommonErrors;
import de.calamanari.adl.ProcessContext;
import de.calamanari.adl.cnv.tps.AdlType;
import de.calamanari.adl.cnv.tps.ArgMetaInfo;
import de.calamanari.adl.cnv.tps.ArgMetaInfoLookup;
import de.calamanari.adl.cnv.tps.ConfigException;
import de.calamanari.adl.solr.AdlSolrType;
import de.calamanari.adl.solr.SolrFormatConstants;

/**
 * The {@link DefaultAutoMappingPolicy} dynamically creates {@link ArgFieldAssignment} based on meta-information parsed from a given argName.
 * <p>
 * With its default settings, it just applies some solar field naming conventions (see for example {@link #determineSolrFieldType(String, String)}) connect both
 * worlds.
 * <p>
 * There are two ways to customize the policies. The easiest option is setting a different local name extractor function. The extractor also decides (return not
 * null if not responsible) whether a local name (field name) can be parsed and so the argName can be handled by this policy or not.<br>
 * If this is not flexible enough, you can sub-class this policy to adjust the behavior to your needs.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class DefaultAutoMappingPolicy implements AutoMappingPolicy {

    private static final long serialVersionUID = -352375809217258551L;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAutoMappingPolicy.class);

    /**
     * Dynamic variable that can be specified as a filterValue to tell the system to replace it at runtime with the value of the <i>global variable</i>
     * <code>argName.local</code> (if present). By default this value will be set by this policy right before processing the filter condition setup (parameter
     * creation), so the value will be the result of this instances extractor function.
     */
    public static final String VAR_ARG_NAME_LOCAL = "argName.local";

    /**
     * Reference to the variable {@value #VAR_ARG_NAME_LOCAL}
     */
    public static final String ARG_NAME_LOCAL_PLACEHOLDER = "${" + VAR_ARG_NAME_LOCAL + "}";

    /**
     * Configuration element if no policy is defined to avoid null
     */
    public static final AutoMappingPolicy NONE = new AutoMappingPolicy() {

        private static final long serialVersionUID = 1234932766875792126L;

        @Override
        public ArgFieldAssignment map(String argName, ProcessContext settings) {
            throw new ConfigException("Auto-mapping error: NONE-mapper called for argName=" + argName,
                    AudlangMessage.argMsg(CommonErrors.ERR_3000_MAPPING_FAILED, argName));
        }

        @Override
        public boolean isApplicable(String argName) {
            return false;
        }

        /**
         * @return singleton instance in JVM
         */
        Object readResolve() {
            return NONE;
        }
    };

    /**
     * The nodeType (document type) this policy belongs to
     */
    protected final String nodeType;

    /**
     * The function that extracts the {@value #ARG_NAME_LOCAL_PLACEHOLDER} from the argName
     */
    protected final LocalArgNameExtractor extractorFunction;

    /**
     * If this property is not null, the type of the argName will be fixed, otherwise it will be derived somehow from the argName
     */
    protected final AdlType argTypeOverride;

    /**
     * If this property is not null, the type of the Solr-field will be fixed, otherwise it will be derived somehow from the argName
     */
    protected final AdlSolrType fieldTypeOverride;

    /**
     * If this property is not null, the field's isCollection setting will be true/false, otherwise it will derived somehow from the argName
     */
    protected final Boolean isCollectionOverride;

    /**
     * true, if each auto-created assignment shall be isMultiDoc, means there may be multiple nested/dependent Solr-documents, and not every document contains
     * the mapped field
     */
    protected final boolean isMultiDoc;

    /**
     * If this property is not null, then this lookup acts as a guard in {@link #isApplicable(String)} to restrict which argNames are allowed
     */
    protected final ArgMetaInfoLookup argMetaLookup;

    /**
     * Creates a new policy object.
     * 
     * @param nodeType identifies the document type the policy is related to (might be relevant for sub-class implementations)
     * @param extractorFunction extracts the argName.local (should be the Solr-field name), if null, uses {@link LocalArgNameExtractor#validateOnly()}
     * @param argTypeOverride fixed arg type information (from configuration), if null, delegates to {@link #determineArgType(String, String)}
     * @param fieldTypeOverride fixed Solr field type information (from configuration), if null, delegates to {@link #determineSolrFieldType(String, String)}
     * @param isCollectionOverride fixed setting whether such fields are collections or not, if null, delegates to
     *            {@link #determineIsCollection(String, String)}
     * @param isMultiDoc configures the auto-created assignments, see {@link ArgFieldAssignment}
     * @param argMetaLookup this optional lookup acts as a guard to restrict which argNames are allowed, or null if there is no logical data model
     */
    public DefaultAutoMappingPolicy(String nodeType, LocalArgNameExtractor extractorFunction, AdlType argTypeOverride, AdlSolrType fieldTypeOverride,
            Boolean isCollectionOverride, boolean isMultiDoc, ArgMetaInfoLookup argMetaLookup) {
        if (!ConfigUtils.isValidSolrName(nodeType)) {
            throw new IllegalArgumentException(String.format(
                    "%s%ngiven: nodeType=%s, localArgNameExtractorFunction=%s, argTypeOverride=%s, fieldTypeOverride=%s, isCollectionOverride=%s, isMultiDoc=%s, argMetaLookup=<%s>",
                    SolrFormatConstants.SOLR_NAMING_DEBUG_INFO, nodeType, (extractorFunction == null ? "null" : extractorFunction.getClass().getSimpleName()),
                    argTypeOverride, fieldTypeOverride, isCollectionOverride, isMultiDoc,
                    argMetaLookup == null ? "null" : argMetaLookup.getClass().getSimpleName()));
        }
        if (argMetaLookup != null && argTypeOverride != null) {
            throw new ConfigException(String.format(
                    "Cannot have argMetaLookup and argTypeOverride at the same time (either rely on a logical model OR a fixed argType).%n"
                            + "given: nodeType=%s, localArgNameExtractorFunction=%s, argTypeOverride=%s, fieldTypeOverride=%s, isCollectionOverride=%s, isMultiDoc=%s, argMetaLookup=<%s>",
                    nodeType, (extractorFunction == null ? "null" : extractorFunction.getClass().getSimpleName()), argTypeOverride, fieldTypeOverride,
                    isCollectionOverride, isMultiDoc, argMetaLookup.getClass().getSimpleName()));
        }
        this.nodeType = nodeType;
        this.argTypeOverride = argTypeOverride;
        this.fieldTypeOverride = fieldTypeOverride;
        this.isCollectionOverride = isCollectionOverride;
        this.argMetaLookup = argMetaLookup;
        this.isMultiDoc = isMultiDoc;
        this.extractorFunction = createExtractorFunctionWithTypeResolutionCheck(
                extractorFunction != null ? extractorFunction : LocalArgNameExtractor.validateOnly());
    }

    /**
     * Wraps the given extractor with a check whether this policy will also be able to determine the argType and the Solr fieldType type based on the argName
     * (look-ahead).
     * <p>
     * This ensures consistency with the contract of the {@link #isApplicable(String)} method which should never return a non-null value if the policy is unable
     * to deal with the mapping.
     * 
     * @param wrappedExtractorFunction
     * @return given extractor wrapped with type checking
     */
    private LocalArgNameExtractor createExtractorFunctionWithTypeResolutionCheck(LocalArgNameExtractor baseExtractorFunction) {
        if (argTypeOverride != null && fieldTypeOverride != null) {
            return baseExtractorFunction;
        }
        return argName -> extractArgNameWithTypeResolutionCheck(argName, baseExtractorFunction);

    }

    /**
     * Before calling the base extractor checks whether the argName is allowed and if the types related to the argName can be resolved properly.
     * 
     * @param argName candidate
     * @param baseExtractorFunction
     * @return local argName or null if not applicable
     */
    private String extractArgNameWithTypeResolutionCheck(String argName, LocalArgNameExtractor baseExtractorFunction) {
        if (argName == null) {
            return null;
        }

        String argNameLocal = baseExtractorFunction.apply(argName);
        if (argNameLocal != null) {
            AdlType argType = determineArgTypeGuarded(argName, argNameLocal);
            if (argType != null) {
                AdlSolrType fieldType = fieldTypeOverride != null ? fieldTypeOverride : determineSolrFieldType(argName, argNameLocal);
                if (fieldType != null) {
                    return argNameLocal;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isApplicable(String argName) {
        String localArgName = extractorFunction.apply(argName);

        boolean valid = ConfigUtils.isValidSolrName(localArgName);
        if (localArgName != null && !valid) {
            LOGGER.debug("An auto-mapping policy for nodeType={} applied to argName={} returned an invalid localArgName={} - SKIPPED!", nodeType, argName,
                    localArgName);
        }
        return valid;

    }

    /**
     * This TEMPLATE METHOD allows sub-classes to compute and set further global variables which can then be referenced inside the mapping process.
     * <p>
     * <b>Example:</b> Let the argName be structured <code>org.section.<b>department</b></code> and you want to pass the <i>department</i> as a filter column
     * value.<br>
     * Simply extract (parse) the qualifier from the argName in <i>this method</i> and put it as a global variable <i>department</i> into the settings.<br>
     * Now you can reference it in a filter column value, e.g., <code>SEC/${department}</code> like any other variable.
     * <p>
     * By default this method calls the extractor function and sets the global variable {@value #ARG_NAME_LOCAL_PLACEHOLDER} to the value extracted from the
     * argName.
     * 
     * @param argName
     * @param ctx to obtain and set variables
     */
    protected void prepareVariables(String argName, ProcessContext ctx) {
        assertContextNotNull(ctx);
        String argNameLocal = extractorFunction.apply(argName);
        ctx.getGlobalVariables().put(VAR_ARG_NAME_LOCAL, argNameLocal);
    }

    /**
     * Resolves the type of the given argName with the following priority chain:
     * <ol>
     * <li>If {@link #argTypeOverride} is not null, return this fixed type.</li>
     * <li>If {@link #argMetaLookup} is not null, return the type mapped to the argName or return null if the argName is not defined in the logical model.</li>
     * <li>Delegate to {@link #determineArgType(String, String)}</li>
     * </ol>
     * 
     * @param argName
     * @param argNameLocal
     * @return resolved type or null if impossible
     */
    private AdlType determineArgTypeGuarded(String argName, String argNameLocal) {
        AdlType res = null;
        if (argTypeOverride != null) {
            res = argTypeOverride;
        }
        else if (argMetaLookup != null) {
            if (argMetaLookup.contains(argName)) {
                res = argMetaLookup.typeOf(argName);
            }
        }
        else {
            res = determineArgType(argName, argNameLocal);
        }
        return res;
    }

    /**
     * This TEMPLATE METHOD derives the audlang field type from the given name information
     * <p>
     * The default implementation delegates to {@link ConventionUtils#determineGenericArgType(String)} with the argNameLocal as parameter.
     * <p>
     * Sub-classes may apply a different strategy.
     * 
     * @param argName audlang name
     * @param argNameLocal extracted name (might be modified by the extractor)
     * @return resolved type or null if impossible
     */
    protected AdlType determineArgType(String argName, String argNameLocal) {
        return determineGenericArgType(argNameLocal);
    }

    /**
     * This TEMPLATE METHOD derives the Solr field type from the given name information
     * <p>
     * The default implementation delegates to {@link ConventionUtils#determineGenericSolrFieldType(String)} with the argNameLocal as parameter.
     * <p>
     * Sub-classes may apply a different strategy.
     * 
     * @param argName audlang name
     * @param argNameLocal extracted name (might be modified by the extractor)
     * @return resolved type or null if impossible
     */
    protected AdlSolrType determineSolrFieldType(String argName, String argNameLocal) {
        return determineGenericSolrFieldType(argNameLocal);
    }

    /**
     * This TEMPLATE METHOD derives the information whether the arg/field is a collection from the given name information
     * <p>
     * The default implementation delegates to {@link ConventionUtils#determineGenericIsCollection(String)} with the argNameLocal as parameter.
     * <p>
     * Sub-classes may apply a different strategy.
     * 
     * @param argName audlang name
     * @param argNameLocal extracted name (might be modified by the extractor)
     * @return true if the arg/field is a collection
     */
    protected boolean determineIsCollection(String argName, String argNameLocal) {
        return determineGenericIsCollection(argNameLocal);
    }

    @Override
    public ArgFieldAssignment map(String argName, ProcessContext ctx) {
        if (!isApplicable(argName)) {
            // this is rather an implementation error because the code cannot be reached under normal conditions
            throw new ConfigException("Auto-mapping error: this mapper is not applicable to argName=" + argName,
                    AudlangMessage.argMsg(CommonErrors.ERR_3000_MAPPING_FAILED, argName));
        }
        this.prepareVariables(argName, ctx);

        String argNameLocal = extractorFunction.apply(argName);

        AdlType argType = determineArgTypeGuarded(argName, argNameLocal);
        if (argType == null) {
            throw new ConfigException(String.format("AdlType resolution error: Unable to auto-detect the type from the argName. Given: nodeType=%s, argName=%s",
                    nodeType, argName), AudlangMessage.argMsg(CommonErrors.ERR_3000_MAPPING_FAILED, argName));
        }

        AdlSolrType fieldType = fieldTypeOverride != null ? fieldTypeOverride : determineSolrFieldType(argName, argNameLocal);

        if (fieldType == null) {
            throw new ConfigException(
                    String.format("SolrType resolution error: Unable to auto-detect the Solr field type from the argName. Given: nodeType=%s, argName=%s",
                            nodeType, argName),
                    AudlangMessage.argMsg(CommonErrors.ERR_3000_MAPPING_FAILED, argName));
        }

        boolean isCollection = isCollectionOverride != null ? isCollectionOverride : determineIsCollection(argName, argNameLocal);
        ArgMetaInfo argMetaInfo = new ArgMetaInfo(argName, argType, false, isCollection);
        DataField field = new DataField(nodeType, argNameLocal, fieldType, isCollection);
        return new ArgFieldAssignment(argMetaInfo, field, isMultiDoc);
    }

}
