//@formatter:off
/*
 * TestBase
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

import static de.calamanari.adl.cnv.StandardConversions.parseCoreExpression;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.junit.jupiter.api.AfterAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import de.calamanari.adl.AudlangUserMessage;
import de.calamanari.adl.ConversionException;
import de.calamanari.adl.Flag;
import de.calamanari.adl.FormatStyle;
import de.calamanari.adl.solr.cnv.SolrExpressionConverter;
import de.calamanari.adl.solr.config.SolrMappingConfig;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public abstract class SolrTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrTestBase.class);

    private static final Object LOGGERS_CONF_SYNC = new Object();

    private static volatile boolean loggersConfigured = false;

    public static final List<String> BAD_SOLR_NAME_EXAMPLES = Collections.unmodifiableList(Arrays.asList(null, "", "bad name", "_bad", "bad_", "_bad_", "  "));

    public static final String NODE_TYPE_1 = "node1";
    public static final String NODE_TYPE_2 = "node2";
    public static final String NODE_TYPE_3 = "node3";

    public static final String FIELD_NAME_1 = "field1";
    public static final String FIELD_NAME_2 = "field2";
    public static final String FIELD_NAME_3 = "field3";

    public static final String ARG_NAME_1 = "arg1";
    public static final String ARG_NAME_2 = "arg2";
    public static final String ARG_NAME_3 = "arg3";

    /**
     * test server created and fed by {@link #initTestSolrServer(String)}, initially null
     */
    protected static EmbeddedSolrServer testServer = null;

    protected static SolrMappingConfig currentSolrMappingConfig = null;

    static {
        synchronized (LOGGERS_CONF_SYNC) {
            if (!loggersConfigured) {
                SLF4JBridgeHandler.removeHandlersForRootLogger();
                SLF4JBridgeHandler.install();
            }
        }
    }

    /**
     * Initializes the test server and loads the documents from the given seed file
     * <p>
     * If it was already initialized, the instance will be shutdown and replaced
     * <p>
     * <b>Important:</b> This method resets the {@link #currentSolrMappingConfig} to <b>null</b>, expecting the caller to configure the correct mapping.
     * 
     * @param seedJsonFileName plain json file name without path
     * @throws IOException
     */
    protected static void initTestSolrServer(String seedJsonFileName) throws IOException {
        if (testServer != null) {
            LOGGER.trace("Shutting down test for re-initialization.");
            testServer.close();
            testServer = null;
        }
        EmbeddedSolrServer server = EmbeddedSolrServerUtils.createNewServerInstance("Audlang", "audlang");
        try {
            EmbeddedSolrServerUtils.feedDocuments(server, seedJsonFileName);
            testServer = server;
        }
        catch (RuntimeException ex) {
            server.close();
        }

        currentSolrMappingConfig = null;
    }

    /**
     * Initializes the server and the given mapping config
     * 
     * @throws IOException
     */
    protected static void initTestServerWithHybridMapping(SolrMappingConfig config) throws IOException {
        initTestSolrServer("audlang-data-hybrid.json");

        currentSolrMappingConfig = config;

    }

    /**
     * Initializes the server and the current mapping config
     * 
     * @throws IOException
     */
    protected static void initTestServerWithHybridMapping() throws IOException {

        initTestServerWithHybridMapping(EmbeddedSolrServerUtils.createHybridMappingConfig());

    }

    @AfterAll
    protected static void shutdownServerAfterTest() {
        if (testServer != null) {
            try {
                LOGGER.trace("Shutting down test server after test.");
                testServer.close();
            }
            catch (IOException | RuntimeException ex) {
                LOGGER.error("Error during test server shutdown", ex);
            }
        }
    }

    /**
     * @param expression
     * @param globalVariables if null: sets the variable <b><code>tenent=17</code></b>
     * @param flags empty if null
     * @return list with all the IDs matching the given expression
     */
    protected static List<Integer> selectIds(String expression, Map<String, Serializable> globalVariables, Set<Flag> flags) {

        LOGGER.debug("Selecting IDs for: \n{}", expression);

        List<Integer> resPretty = EmbeddedSolrServerUtils.queryIntIdsSorted(testServer,
                createQueryDefinition(expression, globalVariables, flags, FormatStyle.PRETTY_PRINT));
        List<Integer> resInline = EmbeddedSolrServerUtils.queryIntIdsSorted(testServer,
                createQueryDefinition(expression, globalVariables, flags, FormatStyle.INLINE));

        assertEquals(resPretty, resInline);

        LOGGER.debug("Result: {}", resInline);

        return resInline;

    }

    /**
     * @param expression
     * @param globalVariables if null: sets the variable <b><code>tenent=17</code></b>
     * @return list with all the IDs matching the given expression
     */
    protected static List<Integer> selectIds(String expression, Map<String, Serializable> globalVariables) {
        return selectIds(expression, globalVariables, null);
    }

    /**
     * @param expression
     * @return list with all the IDs matching the given expression after setting the variable <b><code>tenent=17</code></b>
     */
    protected static List<Integer> selectIds(String expression) {
        return selectIds(expression, null, null);
    }

    /**
     * @param expected list with expected ids (ordered)
     * @param expression
     */
    protected static void assertQueryResult(List<Integer> expected, String expression) {
        assertEquals(expected, selectIds(expression));
    }

    protected static void assertThrowsErrorCode(AudlangUserMessage expectedError, Supplier<?> s) {
        ConversionException expected = null;
        try {
            s.get();
        }
        catch (ConversionException ex) {
            expected = ex;
        }

        assertNotNull(expected);
        assertEquals(expectedError.code(), expected.getUserMessage().code());

    }

    protected static void assertThrowsErrorCode(AudlangUserMessage expectedError, Runnable r) {
        assertThrowsErrorCode(expectedError, () -> {
            r.run();
            return 0;
        });
    }

    protected static void assertQueryDef(String expected, String expression) {
        assertEquals(expected, createQueryDefinition(expression, FormatStyle.PRETTY_PRINT).toExpressionDebugString().trim());
    }

    /**
     * @param expression
     * @param globalVariables if null: sets the variable <b><code>tenent=17</code></b>
     * @param flags empty if null
     * @return number of matching documents
     */
    protected static int selectCount(String expression, Map<String, Serializable> globalVariables, Set<Flag> flags) {

        LOGGER.debug("Selecting count for: \n{}", expression);

        long resInline = EmbeddedSolrServerUtils.queryCount(testServer, createQueryDefinition(expression, globalVariables, flags, FormatStyle.INLINE));
        long resPretty = EmbeddedSolrServerUtils.queryCount(testServer, createQueryDefinition(expression, globalVariables, flags, FormatStyle.PRETTY_PRINT));

        assertSame(resPretty, resInline);

        LOGGER.debug("Result: {}", resInline);

        return (int) resInline;

    }

    /**
     * @param expression
     * @param globalVariables if null: sets the variable <b><code>tenent=17</code></b>
     * @return number of matching documents
     */
    protected static int selectCount(String expression, Map<String, Serializable> globalVariables) {
        return selectCount(expression, globalVariables, null);
    }

    /**
     * @param expression
     * @return number of matching documents after setting the variable <b><code>tenent=17</code></b>
     */
    protected static int selectCount(String expression) {
        return selectCount(expression, null, null);
    }

    /**
     * Parses the expression to create a query definition for execution on Solr. Uses {@link #currentSolrMappingConfig}.
     * 
     * @param expression
     * @param globalVariables if null, sets the variable <b><code>tenent=17</code></b>
     * @param flags
     * @param style
     * @return query definition
     */
    protected static SolrQueryDefinition createQueryDefinition(String expression, Map<String, Serializable> globalVariables, Set<Flag> flags,
            FormatStyle style) {

        SolrExpressionConverter converter = new SolrExpressionConverter(currentSolrMappingConfig);

        converter.setStyle(style);

        if (flags != null) {
            converter.getInitialFlags().addAll(flags);
        }

        if (globalVariables != null) {
            converter.getInitialVariables().putAll(globalVariables);
        }
        else {
            converter.getInitialVariables().put("tenant", "17");
        }

        if (expression == null) {
            throw new IllegalArgumentException("expression must not be null");
        }

        SolrQueryDefinition res = converter.convert(parseCoreExpression(expression));
        LOGGER.debug("Created query definition: {}", res);
        return res;

    }

    protected static SolrQueryDefinition createQueryDefinition(String expression, Map<String, Serializable> globalVariables, FormatStyle style) {
        return createQueryDefinition(expression, globalVariables, null, style);
    }

    protected static SolrQueryDefinition createQueryDefinition(String expression, FormatStyle style) {
        return createQueryDefinition(expression, null, null, style);
    }

    /**
     * Utility for quickly creating a set of flags
     * 
     * @param flags
     * @return
     */
    protected static Set<Flag> flags(Flag... flags) {
        if (flags == null || flags.length == 0) {
            return Collections.emptySet();
        }
        Set<Flag> res = new TreeSet<>();
        res.addAll(Arrays.asList(flags));
        return res;

    }

    /**
     * Shorthand to get a global variables map with the mapping <code>"tenant"=value</code>
     * 
     * @param value
     * @return global variables map
     */
    protected static Map<String, Serializable> withTenant(int value) {
        return vars().put("tenant", value).get();
    }

    /**
     * @return variables map builder
     */
    protected static MapBuilder vars() {
        return new MapBuilder(new TreeMap<>());
    }

    /**
     * Utility for quickly setting up a global variables map
     */
    protected static record MapBuilder(Map<String, Serializable> get) {

        public MapBuilder put(String key, Serializable value) {
            get.put(key, value);
            return this;
        }

    }

    /**
     * Creates a list of Integers (for expected values)
     * 
     * @param values
     * @return Integer list
     */
    protected static List<Integer> list(int... values) {
        return Arrays.stream(values).boxed().toList();
    }

    /**
     * Logs the query definition after parsing
     * 
     * @param expression
     * @param style
     */
    protected static void showDefinition(String expression, FormatStyle style) {
        LOGGER.info("\n{}", createQueryDefinition(expression, style).toExpressionDebugString());
    }

    /**
     * Logs the query definition after parsing with pretty-print
     * 
     * @param expression
     */
    protected static void showDefinition(String expression) {
        LOGGER.info("\n{}", createQueryDefinition(expression, FormatStyle.PRETTY_PRINT).toExpressionDebugString());
    }

    /**
     * Logs the query definition after parsing with pretty-print
     * 
     * @param expression
     */
    protected static void showExecution(String expression) {

        SolrQueryDefinition def = createQueryDefinition(expression, FormatStyle.PRETTY_PRINT);

        StringBuilder sb = new StringBuilder();
        if (def.filterQueries().size() > 1) {

            for (SolrFilterQuery fq : def.filterQueries()) {
                sb.append("\n\n");
                sb.append(fq.queryString());
                sb.append("\nResult: " + EmbeddedSolrServerUtils.queryIntIdsSorted(testServer, fq.queryString()));
            }
        }
        sb.append("\n\n");
        sb.append(def.toExpressionDebugString());
        sb.append("\nResult: " + selectIds(expression));

        LOGGER.info("\n{}", sb);
    }

    protected static void showIds(String... expressions) {
        show(Arrays.stream(expressions).map(SolrTestBase::selectIds).toList());
    }

    /**
     * Logs the value to the command line
     * 
     * @param value
     */
    protected static void show(Object... value) {
        if (value == null) {
            LOGGER.info("\n>>>null<<<", value);
        }
        else if (value.length == 0) {
            LOGGER.info("\n>>>[]<<<", value);
        }
        else if (value.length == 1) {
            LOGGER.info("\n>>>\n{}\n<<<", value);
        }
        else {
            showRec(0, value);
        }
    }

    /**
     * Logs the value to the command line
     * 
     * @param value
     */
    protected static void show(Collection<?> value) {
        showRec(0, value);
    }

    private static void showRec(int indent, Object value) {

        StringBuilder sbIndent = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sbIndent.append("    ");
        }

        if (value instanceof Collection<?> coll && !isComplexCollection(coll)) {
            LOGGER.info("{}[", sbIndent);
            coll.forEach(e -> showRec(indent + 1, e));
            LOGGER.info("{}]", sbIndent);
        }
        else if (value != null && value.getClass().isArray() && !isComplexArray((Object[]) value)) {
            LOGGER.info("{}[", sbIndent);
            Arrays.stream((Object[]) value).forEach(e -> showRec(indent + 1, e));
            LOGGER.info("{}]", sbIndent);
        }
        else {
            LOGGER.info("{}{}", sbIndent, value);
        }
    }

    private static boolean isComplexArray(Object[] arr) {
        return arr.length == 0 || Number.class.isAssignableFrom(arr[0].getClass());
    }

    private static boolean isComplexCollection(Collection<?> coll) {
        return coll.isEmpty() || Number.class.isAssignableFrom(coll.iterator().next().getClass());
    }

}
