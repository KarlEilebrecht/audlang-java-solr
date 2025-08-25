//@formatter:off
/*
 * ConfigUtils
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

import de.calamanari.adl.ProcessContext;
import de.calamanari.adl.solr.SolrFormatConstants;

/**
 * Set of utilities, mainly for validation to avoid duplication.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class ConfigUtils {

    /**
     * @param argName
     * @throws IllegalArgumentException if the argName is null or blank
     */
    public static void assertValidArgName(String argName) {
        if (!isValidArgName(argName)) {
            throw new IllegalArgumentException(String.format("Parameter argName must not be null or empty, given: %s", argName));
        }
    }

    /**
     * @param name node type or field name
     * @throws IllegalArgumentException if the name is NOT {@link #isValidSolrName(String)}
     */
    public static void assertValidSolrName(String name) {
        if (!isValidSolrName(name)) {
            throw new IllegalArgumentException(String.format("Node types and field names must be valid Solr-names, given: %s", name));
        }
    }

    /**
     * @param ctx process context to be checked
     * @throws IllegalArgumentException if the settings is null
     */
    public static void assertContextNotNull(ProcessContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException(
                    "The process context must not not be null, use ProcessContext.empty() instead to call this method without any settings.");
        }
    }

    /**
     * @param argName
     * @return true if the argName is acceptable (not null, not blank)
     */
    public static boolean isValidArgName(String argName) {
        return (argName != null && !argName.isBlank());
    }

    /**
     * Tests whether the given string is not blank and conforms to common
     * <a href="https://solr.apache.org/guide/solr/latest/indexing-guide/fields.html">Solr-conventions</a>.
     * <p>
     * As this is for the configuration part we are a bit stricter and enforce all these rules, plus we do not allow <i>any</i> field names with leading or
     * trailing underscores. So, not only reserved names like <code>_foo_bar_</code> are forbidden but additionally <code>_foo</code> and <code>bar_</code>.
     * <p>
     * Although, technically not required, we apply the same rules to the required nodeType field. The value of the nodeType field could be anything and even
     * contain whitespace but that only leads to complex escaping.
     * <p>
     * <i>{@value SolrFormatConstants#SOLR_NAMING_DEBUG_INFO}</i>
     * 
     * @param name a solr field name
     * @return true if the given string is not null and complies to the rules, otherwise false
     */
    public static boolean isValidSolrName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if ((i == 0 && !isSolrLetter(ch)) || (i == name.length() - 1 && !isSolrLetterOrDigit(ch)) || (ch != '_' && !isSolrLetterOrDigit(ch))) {
                return false;
            }
        }
        return true;
    }

    /**
     * The official Solr documentation mentions a set of valid characters and digits which might be enforced in future.
     * <p>
     * Here we strictly adhere to these rules.
     * 
     * @param ch
     * @return true if the given character is among 'a'-'z', 'A'-'Z' or '0'-'9'
     */
    public static boolean isSolrLetterOrDigit(char ch) {
        return isSolrLetter(ch) || (ch >= '0' && ch <= '9');
    }

    /**
     * The official Solr documentation mentions a set of valid characters in Solr-names which might be enforced in future.
     * <p>
     * Here we strictly adhere to these rules.
     * 
     * @param ch
     * @return true if the given character is among 'a'-'z', 'A'-'Z'
     */
    public static boolean isSolrLetter(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
    }

    private ConfigUtils() {
        // utilities
    }

}
