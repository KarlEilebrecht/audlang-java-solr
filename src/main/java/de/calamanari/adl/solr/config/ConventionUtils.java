//@formatter:off
/*
 * ConventionUtils
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

import static de.calamanari.adl.cnv.tps.DefaultAdlType.BOOL;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.DATE;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.DECIMAL;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.INTEGER;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.STRING;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_BOOLEAN;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_DATE;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_DOUBLE;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_FLOAT;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_INTEGER;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_LONG;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_STRING;

import de.calamanari.adl.cnv.tps.AdlType;
import de.calamanari.adl.cnv.tps.DefaultAdlType;
import de.calamanari.adl.solr.AdlSolrType;
import de.calamanari.adl.solr.DefaultAdlSolrType;
import de.calamanari.adl.solr.SolrFormatConstants;

/**
 * A couple of utility methods following common conventions.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class ConventionUtils {

    /**
     * Determines the argType based on the solr-suffix-conventions
     * <p>
     * Supported field name suffixes:
     * <ul>
     * <li><code>_i</code> -&gt; {@link DefaultAdlType#INTEGER}</li>
     * <li><code>_is</code> -&gt; {@link DefaultAdlType#INTEGER}</li>
     * <li><code>_s</code> -&gt; {@link DefaultAdlType#STRING}</li>
     * <li><code>_ss</code> -&gt; {@link DefaultAdlType#STRING}</li>
     * <li><code>_l</code> -&gt; {@link DefaultAdlType#INTEGER}</li>
     * <li><code>_ls</code> -&gt; {@link DefaultAdlType#INTEGER}</li>
     * <li><code>_b</code> -&gt; {@link DefaultAdlType#BOOL}</li>
     * <li><code>_bs</code> -&gt; {@link DefaultAdlType#BOOL}</li>
     * <li><code>_f</code> -&gt; {@link DefaultAdlType#DECIMAL}</li>
     * <li><code>_fs</code> -&gt; {@link DefaultAdlType#DECIMAL}</li>
     * <li><code>_d</code> -&gt; {@link DefaultAdlType#DECIMAL}</li>
     * <li><code>_ds</code> -&gt; {@link DefaultAdlType#DECIMAL}</li>
     * <li><code>_dt</code> -&gt; {@link DefaultAdlType#DATE}</li>
     * <li><code>_dts</code> -&gt; {@link DefaultAdlType#DATE}</li>
     * </ul>
     * 
     * @param name any name that adheres to the Solr dynamic field naming conventions
     * @return type of the audlang argument or null if detection failed
     */
    public static AdlType determineGenericArgType(String name) {
        switch (extractStandardTypeSuffix(name)) {
        case "_i", "_is":
            return INTEGER;
        case "_s", "_ss":
            return STRING;
        case "_l", "_ls":
            return INTEGER;
        case "_b", "_bs":
            return BOOL;
        case "_f", "_fs":
            return DECIMAL;
        case "_d", "_ds":
            return DECIMAL;
        case "_dt", "_dts":
            return DATE;
        default:
            return null;
        }
    }

    /**
     * Determines the Solr field type based in the Solr-suffix-conventions
     * <p>
     * Supported field name suffixes:
     * <ul>
     * <li><code>_i</code> -&gt; {@link DefaultAdlSolrType#SOLR_INTEGER}</li>
     * <li><code>_is</code> -&gt; {@link DefaultAdlSolrType#SOLR_INTEGER}</li>
     * <li><code>_s</code> -&gt; {@link DefaultAdlSolrType#SOLR_STRING}</li>
     * <li><code>_ss</code> -&gt; {@link DefaultAdlSolrType#SOLR_STRING}</li>
     * <li><code>_l</code> -&gt; {@link DefaultAdlSolrType#SOLR_LONG}</li>
     * <li><code>_ls</code> -&gt; {@link DefaultAdlSolrType#SOLR_LONG}</li>
     * <li><code>_b</code> -&gt; {@link DefaultAdlSolrType#SOLR_BOOLEAN}</li>
     * <li><code>_bs</code> -&gt; {@link DefaultAdlSolrType#SOLR_BOOLEAN}</li>
     * <li><code>_f</code> -&gt; {@link DefaultAdlSolrType#SOLR_FLOAT}</li>
     * <li><code>_fs</code> -&gt; {@link DefaultAdlSolrType#SOLR_FLOAT}</li>
     * <li><code>_d</code> -&gt; {@link DefaultAdlSolrType#SOLR_DOUBLE}</li>
     * <li><code>_ds</code> -&gt; {@link DefaultAdlSolrType#SOLR_DOUBLE}</li>
     * <li><code>_dt</code> -&gt; {@link DefaultAdlSolrType#SOLR_DATE}</li>
     * <li><code>_dts</code> -&gt; {@link DefaultAdlSolrType#SOLR_DATE}</li>
     * </ul>
     * 
     * @param name any name that adheres to the Solr dynamic field naming conventions
     * @return type of the Solr field or null if detection failed
     */
    public static AdlSolrType determineGenericSolrFieldType(String name) {
        switch (extractStandardTypeSuffix(name)) {
        case "_i", "_is":
            return SOLR_INTEGER;
        case "_s", "_ss":
            return SOLR_STRING;
        case "_l", "_ls":
            return SOLR_LONG;
        case "_b", "_bs":
            return SOLR_BOOLEAN;
        case "_f", "_fs":
            return SOLR_FLOAT;
        case "_d", "_ds":
            return SOLR_DOUBLE;
        case "_dt", "_dts":
            return SOLR_DATE;
        default:
            return null;
        }
    }

    /**
     * Determines whether the arg/field is a collection of values using the Solr-suffix-conventions.
     * <p>
     * Supported field name suffixes:
     * <ul>
     * <li><code>_is</code> list of integers</li>
     * <li><code>_ss</code> list of strings</li>
     * <li><code>_ls</code> list of longs</li>
     * <li><code>_bs</code> list of booleans</li>
     * <li><code>_fs</code> list of floats</li>
     * <li><code>_ds</code> list of doubles</li>
     * <li><code>_dts</code> list of dates</li>
     * </ul>
     * 
     * @param name any name that adheres to the Solr dynamic field naming conventions
     * @return true if this is a collection (false by default)
     */
    public static boolean determineGenericIsCollection(String name) {
        return SolrFormatConstants.SOLR_STANDARD_COLLECTION_SUFFIXES.contains(extractStandardTypeSuffix(name));
    }

    /**
     * Performs a <i>bottom-up</i> type resolution from the solr-type (base type) to the Audlang type.
     * <ul>
     * <li>{@link DefaultAdlSolrType#SOLR_INTEGER} -&gt; {@link DefaultAdlType#INTEGER}</li>
     * <li>{@link DefaultAdlSolrType#SOLR_STRING} -&gt; {@link DefaultAdlType#STRING}</li>
     * <li>{@link DefaultAdlSolrType#SOLR_LONG} -&gt; {@link DefaultAdlType#INTEGER}</li>
     * <li>{@link DefaultAdlSolrType#SOLR_BOOLEAN} -&gt; {@link DefaultAdlType#BOOL}</li>
     * <li>{@link DefaultAdlSolrType#SOLR_FLOAT} {@link DefaultAdlType#DECIMAL}</li>
     * <li>{@link DefaultAdlSolrType#SOLR_DOUBLE} -&gt; {@link DefaultAdlType#DECIMAL}</li>
     * <li>{@link DefaultAdlSolrType#SOLR_DATE} -&gt; {@link DefaultAdlType#DATE}</li>
     * </ul>
     * 
     * @param fieldType Solr field type to be mapped to an Audlang type
     * @return audlang argType or null if resolution failed
     */
    public static AdlType resolveArgTypeFromFieldType(AdlSolrType fieldType) {
        if (fieldType == null) {
            return null;
        }
        AdlSolrType baseFieldType = fieldType.getBaseType();
        switch (baseFieldType) {
        case SOLR_STRING:
            return STRING;
        case SOLR_INTEGER, SOLR_LONG:
            return INTEGER;
        case SOLR_FLOAT, SOLR_DOUBLE:
            return DECIMAL;
        case SOLR_BOOLEAN:
            return BOOL;
        case SOLR_DATE:
            return DATE;
        default:
            return null;
        }
    }

    /**
     * Parses the suffix (part beginning with the <i>last</i> underscore) from the given name
     * 
     * @param name any name that adheres to the Solr dynamic field naming conventions
     * @return suffix or empty string if there is no suffix, never null
     */
    public static String extractStandardTypeSuffix(String name) {
        String res = "";
        if (name != null) {
            int lastUnderscoreIdx = name.lastIndexOf('_');
            if (lastUnderscoreIdx > 0 && lastUnderscoreIdx < name.length() - 1) {
                res = name.substring(lastUnderscoreIdx);
            }
        }
        return res;
    }

    private ConventionUtils() {
        // utilities
    }

}
