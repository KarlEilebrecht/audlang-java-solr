//@formatter:off
/*
 * FilterField
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

import de.calamanari.adl.cnv.TemplateParameterUtils;
import de.calamanari.adl.cnv.tps.AdlFormattingException;
import de.calamanari.adl.cnv.tps.ConfigException;
import de.calamanari.adl.irl.MatchOperator;
import de.calamanari.adl.solr.AdlSolrType;
import de.calamanari.adl.solr.SolrFormatConstants;

import static de.calamanari.adl.solr.config.ConfigUtils.isValidSolrName;

/**
 * A {@link FilterField} is an extra condition (field in a document along with a filter value) to be matched independently from a particular query.
 * <p>
 * For example if there is an indicator field <b><code>isActive</code></b> in a document, then only documents should be considered with isActive:true.
 * <p>
 * <b>Note:</b> The {@link #filterValue} is considered to be an audlang string, formatted by the formatter of the specified field type. Thus, you <b>must
 * not</b> prematurely escape the filter value if the provided string contains characters conflicting with Solr.
 * 
 * @param nodeType identifies the root or a child document this field belongs to
 * @param fieldName solr field name
 * @param fieldType solr field type
 * @param filterValue fixed filter value as a condition to be included in the query, or <code>${argName}</code> to fill-in the argument name
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public record FilterField(String nodeType, String fieldName, AdlSolrType fieldType, String filterValue) implements AdlSolrField {

    /**
     * Fixed value that can be specified as a filterValue to tell the system to replace it at runtime with the current argName from the expression.
     * <p>
     * This variable is highly dynamic and only available (of interest) during the conversion phase.
     */
    public static final String VAR_ARG_NAME = "argName";

    /**
     * Reference to the variable {@value #VAR_ARG_NAME}
     */
    public static final String ARG_NAME_PLACEHOLDER = "${" + VAR_ARG_NAME + "}";

    /**
     * @param nodeType identifies the root or a child document this field belongs to
     * @param fieldName solr field name
     * @param fieldType solr field type
     * @param filterValue fixed filter value as a condition to be included in the query, or <code>${argName}</code> to fill-in the argument name
     */
    public FilterField {
        if (!isValidSolrName(nodeType) || !isValidSolrName(fieldName) || fieldType == null || filterValue == null) {
            throw new ConfigException(String.format(
                    "None of the arguments can be null and nodeType/fieldName must be valid Solr identifiers, given: nodeType=%s, fieldName=%s, fieldType=%s, filterValue=%s.%n%s",
                    nodeType, fieldName, fieldType, filterValue, SolrFormatConstants.SOLR_NAMING_DEBUG_INFO));
        }
        try {
            // probing to avoid later surprises
            String value = createNativeValue(nodeType, fieldName, fieldType, filterValue);
            if (value.isBlank()) {
                throw new ConfigException(String.format(
                        "The given filterValue formatted in the given way would result in a broken blank value (>>%s<<), given: nodeType=%s, fieldName=%s, fieldType=%s, filterValue=%s.",
                        value, nodeType, fieldName, fieldType, filterValue));
            }
        }
        catch (AdlFormattingException ex) {
            throw new ConfigException(String.format(
                    "The given filterValue cannot be formatted with the specified type, given: nodeType=%s, fieldName=%s, fieldType=%s, filterValue=%s.",
                    nodeType, fieldName, fieldType, filterValue), ex);
        }

    }

    /**
     * @return the native filter value to be included in the Solr-query
     */
    private static String createNativeValue(String nodeType, String fieldName, AdlSolrType fieldType, String filterValue) {
        return TemplateParameterUtils.containsAnyVariables(filterValue) ? filterValue
                : fieldType.getFormatter().format("<FilterColumn:" + nodeType + "." + fieldName + ">", filterValue, MatchOperator.EQUALS);
    }

    /**
     * @return the native filter condition to be included in the Solr-query
     */
    private static String createNativeCondition(String nodeType, String fieldName, AdlSolrType fieldType, String filterValue) {
        return fieldName + ":" + createNativeValue(nodeType, fieldName, fieldType, filterValue);
    }

    /**
     * @return the native filter condition to be included in the Solr-query
     */
    public String nativeCondition() {
        return createNativeCondition(nodeType, fieldName, fieldType, filterValue);
    }

    @Override
    public String toString() {
        return "?{ " + nativeCondition() + " }";
    }

}
