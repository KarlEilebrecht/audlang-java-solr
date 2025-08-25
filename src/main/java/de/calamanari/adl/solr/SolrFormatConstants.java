//@formatter:off
/*
 * SqlFormatConstants
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A list of Solr-generation related words and phrases etc.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class SolrFormatConstants {

    /**
     * According to <a href="https://solr.apache.org/guide/solr/latest/query-guide/standard-query-parser.html">Solr Documentation</a> the following characters
     * must be escaped in queries: <b><code>{@value}</code></b><br>
     * This applies to keys and values while it is strongly encouraged not have any keys (field names) with special characters.
     * <p>
     * Although not mentioned in the documentation, additionally the backslash itself and all whitespace characters must be escaped.
     * <p>
     * See <code>org.apache.solr.client.solrj.util.ClientUtils</code> (part of solr-solrj.jar) for more information on this topic
     */
    public static final String SOLR_SPECIAL_CHARACTERS = "+-&|!(){}[]^\"~*?:/";

    /**
     * Constant for cases where we need a dummy argument name for method calls (relevant for debugging only)
     */
    public static final String ARGNAME_DUMMY = "<ARGNAME>";

    /**
     * Solr-query string (main query) for querying all documents: <b><code>{@value}</code></b>
     */
    public static final String QUERY_ALL_DOCUMENTS = "*:*";

    /**
     * Default Solr-field for the unique key: <b><code>{@value}</code></b>
     */
    public static final String DEFAULT_UNIQUE_KEY_FIELD_NAME = "id";

    /**
     * Default Solr-field for the nodeType in a document: <b><code>{@value}</code></b>
     */
    public static final String DEFAULT_NODE_TYPE_FIELD_NAME = "node_type";

    /**
     * Default Solr-field for the main document key reference in a dependent document: <b><code>{@value}</code></b>
     */
    public static final String DEFAULT_DEPENDENT_MAIN_KEY_FIELD_NAME = "main_id";

    /**
     * {@value}
     */
    public static final String TRUE = "TRUE";

    /**
     * {@value}
     */
    public static final String FALSE = "FALSE";

    /**
     * {@value}
     */
    public static final String NOT = "NOT";

    /**
     * {@value}
     */
    public static final String AND = "AND";

    /**
     * {@value}
     */
    public static final String OR = "OR";

    /**
     * Example: <code><b>and</b>(<i>condition1</i>,<i>condition2</i>[,...])</code>
     * <p>
     * See also: <a href="https://solr.apache.org/guide/solr/latest/query-guide/function-queries.html#and-function">Solr and-function</a>
     */
    public static final String FUNC_AND = "and";

    /**
     * Example: <code><b>or</b>(<i>condition1</i>,<i>condition2</i>[,...])</code>
     * <p>
     * See also: <a href="https://solr.apache.org/guide/solr/latest/query-guide/function-queries.html#or-function">Solr or-function</a>
     */
    public static final String FUNC_OR = "or";

    /**
     * Example: <code><b>gt</b>(<i>field</i>,<i>field or constant</i>)</code>
     */
    public static final String FUNC_GREATER_THAN = "gt";

    /**
     * Example: <code><b>gte</b>(<i>field</i>,<i>field or constant</i>)</code>
     */
    public static final String FUNC_GREATER_THAN_OR_EQUALS = "gte";

    /**
     * Example: <code><b>lt</b>(<i>field</i>,<i>field or constant</i>)</code>
     */
    public static final String FUNC_LESS_THAN = "lt";

    /**
     * Example: <code><b>lte</b>(<i>field</i>,<i>field or constant</i>)</code>
     */
    public static final String FUNC_LESS_THAN_OR_EQUALS = "lte";

    /**
     * Example: <code><b>eq</b>(<i>field</i>,<i>field or constant</i>)</code>
     */
    public static final String FUNC_EQUALS = "eq";

    /**
     * Example: <code><b>exists</b>(<i>field</i>)</code>
     */
    public static final String FUNC_EXISTS = "exists";

    /**
     * Example: <code><b>ms</b>(<i>date</i>)</code> (number of milliseconds since January 1, 1970 UTC)
     * <p>
     * See also: <a href="https://solr.apache.org/guide/solr/latest/query-guide/function-queries.html#ms-function">Solr ms-function</a>
     */
    public static final String FUNC_MS = "ms";

    /**
     * Example: <code><b>div</b>(<i>dividend</i>,<i>divisor</i>)</code>
     */
    public static final String FUNC_DIV = "div";

    /**
     * Example: <code><b>mul</b>(<i>factor1</i>,<i>factor2</i>)</code>
     */
    public static final String FUNC_MUL = "mul";

    /**
     * Example: <code><b>floor</b>(<i>floating-point value</i>)</code> (strip decimals)
     */
    public static final String FUNC_FLOOR = "floor";

    /**
     * Example: <code><b>sub</b>(<i>field</i>,<i>field or constant</i>)</code> (left <i>minus</i> right)
     */
    public static final String FUNC_SUB = "sub";

    /**
     * Example: <code><b>if</b>(<i>test</i>,1,0)</code> (modulus)
     * <p>
     * See also: <a href="https://solr.apache.org/guide/solr/latest/query-guide/function-queries.html#if-function">Solr if-function</a>
     */
    public static final String FUNC_IF = "if";

    /**
     * Example: <code>if(<i>test</i><b>,1,0</b>)</code>
     * <p>
     * See also: <a href="https://solr.apache.org/guide/solr/latest/query-guide/function-queries.html#if-function">Solr if-function</a>
     */
    public static final String IF_VALUES_0_1 = ",1,0";

    /**
     * {@value}
     */
    public static final String DAY_IN_MILLISECONDS = "86400000";

    /**
     * {@value}
     */
    public static final String INLINE_QUERY = "_query_";

    /**
     * {@value}
     */
    public static final String DOUBLE_QUOTES = "\"";

    /**
     * {@value}
     */
    public static final char OPEN_BRACE = '(';

    /**
     * {@value}
     */
    public static final char CLOSE_BRACE = ')';

    /**
     * {@value}
     */
    public static final char COMMA = ',';

    /**
     * {@value}
     */
    public static final char COLON = ':';

    /**
     * {@value}
     */
    public static final char ASTERISK = '*';

    /**
     * Naming conventions
     * <p>
     * To be included in error messages
     */
    public static final String SOLR_NAMING_DEBUG_INFO = """
            Field names and node types can only contain the following characters 'a'-'z', 'A'-'Z', '0'-'9' or the underscore '_'.
            Names must not start with a digit and can neither start nor end with an underscore.
            """;

    /**
     * All known standard suffixes indicating solr fields that can hold a single value
     */
    public static final Set<String> SOLR_STANDARD_SINGLE_VALUE_SUFFIXES = Collections.unmodifiableSet(
            Arrays.stream(new String[] { "_i", "_l", "_d", "_f", "_b", "_s", "_dt" }).collect(Collectors.toCollection(() -> new TreeSet<String>())));

    /**
     * All known standard suffixes indicating solr fields that can hold multiple values (collections)
     */
    public static final Set<String> SOLR_STANDARD_COLLECTION_SUFFIXES = Collections.unmodifiableSet(
            Arrays.stream(new String[] { "_is", "_ls", "_ds", "_fs", "_bs", "_ss", "_dts" }).collect(Collectors.toCollection(() -> new TreeSet<String>())));

    /**
     * All known standard suffixes indicating solr fields that can hold multiple values (collections)
     */
    public static final Set<String> SOLR_STANDARD_SUFFIXES;
    static {
        List<String> temp = new ArrayList<>();
        temp.addAll(SolrFormatConstants.SOLR_STANDARD_SINGLE_VALUE_SUFFIXES);
        temp.addAll(SolrFormatConstants.SOLR_STANDARD_SINGLE_VALUE_SUFFIXES);
        SOLR_STANDARD_SUFFIXES = Collections.unmodifiableSet(new TreeSet<>(temp));
    }

    private SolrFormatConstants() {
        // Constants
    }
}
