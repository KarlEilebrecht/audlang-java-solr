//@formatter:off
/*
 * SolrFilterQueryBuilderTest
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

package de.calamanari.adl.solr.cnv;

import static de.calamanari.adl.solr.cnv.ConversionTestUtils.createDryTestContext;
import static de.calamanari.adl.solr.cnv.ConversionTestUtils.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.calamanari.adl.FormatStyle;
import de.calamanari.adl.solr.SolrConditionType;
import de.calamanari.adl.solr.SolrFilterQuery;
import de.calamanari.adl.solr.SolrQueryField;
import de.calamanari.adl.solr.SolrTestBase;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class SolrFilterQueryBuilderTest extends SolrTestBase {

    static final Logger LOGGER = LoggerFactory.getLogger(SolrFilterQueryBuilderTest.class);

    private ResettableScpContext ctx;

    @BeforeEach
    void before() {
        ctx = createDryTestContext();
        ctx.setMatchFilterFactory(new DefaultMatchFilterFactory(ctx));
    }

    @Test
    void testBasicErrors() {

        SolrFilterQueryBuilder builder = new SolrFilterQueryBuilder(ctx);

        assertThrows(IllegalArgumentException.class, builder::getResult);

        assertThrows(IllegalStateException.class, builder::closeBrace);

        assertThrows(IllegalStateException.class, builder::endJoin);

        builder.openBrace();

        // brace not closed
        assertThrows(IllegalStateException.class, builder::getResult);

        builder.closeBrace();

        // no fields, yet
        assertThrows(IllegalArgumentException.class, builder::getResult);

        builder.reset();

        SolrFilterQuery filterOnNode3 = filter("flag_b = 1");

        // wrong node type
        assertThrows(IllegalStateException.class, () -> builder.appendFilterQuery(filterOnNode3));

        builder.reset();

        // join with main node type
        assertThrows(IllegalStateException.class, () -> builder.startDependentJoin(NODE_TYPE_1));
        assertThrows(IllegalStateException.class, () -> builder.startNestedJoin(NODE_TYPE_1));

        builder.reset();

        builder.startDependentJoin(NODE_TYPE_2);

        // wrong sub-join
        assertThrows(IllegalStateException.class, () -> builder.startDependentJoin(NODE_TYPE_2));
        assertThrows(IllegalStateException.class, () -> builder.startNestedJoin(NODE_TYPE_2));
        assertThrows(IllegalStateException.class, () -> builder.startDependentJoin(NODE_TYPE_3));
        assertThrows(IllegalStateException.class, () -> builder.startNestedJoin(NODE_TYPE_3));

        assertThrows(IllegalStateException.class, builder::getResult);

        builder.reset();

        builder.startDependentJoin(NODE_TYPE_2);
        assertThrows(IllegalStateException.class, builder::closeBrace);

        builder.reset();

        builder.startDependentJoin(NODE_TYPE_2);

        builder.openBrace();

        // brace not closed
        assertThrows(IllegalStateException.class, builder::endJoin);

    }

    @Test
    void testPrettyPrint() {

        SolrFilterQueryBuilder builder = new SolrFilterQueryBuilder(ctx);

        assertEquals(builder.getMainNodeType(), builder.getCurrentNodeType());

        builder.append("--- anything additional ---\n");

        SolrFilterQuery result = prepareTestQuery(builder);

        assertEquals(
                """
                        --- anything additional ---
                        color:red
                        AND (
                            taste:bad
                            OR (
                                {!join from=main_id to=id v="flag_b\\:TRUE"}
                                AND (
                                    {!parent which="node_type:node1" v="imps1_l\\:1000000\\
                        \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ AND\\ _query_\\:\\"\\\\\\{\\\\\\!frange\\\\\\ l=1\\\\\\ u=1\\\\\\}if\\\\\\(and\\\\\\(exists\\\\\\(imps2_l\\\\\\),exists\\\\\\(imps3_l\\\\\\),eq\\\\\\(imps2_l,imps3_l\\\\\\)\\\\\\),1,0\\\\\\)\\""}
                                    OR age:{18 TO *]
                                )
                            )
                        )
                        AND NOT {!parent which="node_type:node1" v="clicks_l\\:100"}""",
                result.queryString());

        builder.reset();

    }

    @Test
    void testInline() {

        ctx.setStyle(FormatStyle.INLINE);

        SolrFilterQueryBuilder builder = new SolrFilterQueryBuilder(ctx);

        SolrFilterQuery result = prepareTestQuery(builder);

        // @formatter:off
        assertEquals("color:red AND (taste:bad OR ({!join from=main_id to=id v=\"flag_b\\:TRUE\"} "
                             + "AND ({!parent which=\"node_type:node1\" v=\"imps1_l\\:1000000\\ "
                                         + "AND\\ _query_\\:\\\"\\\\\\{\\\\\\!frange\\\\\\ l=1\\\\\\ u=1\\\\\\}if\\\\\\("
                                               + "and\\\\\\(exists\\\\\\(imps2_l\\\\\\),exists\\\\\\(imps3_l\\\\\\),eq\\\\\\(imps2_l,imps3_l\\\\\\)\\\\\\),1,0\\\\\\)\\\"\"} "
                                         + "OR age:{18 TO *]))) "
                                         + "AND NOT {!parent which=\"node_type:node1\" v=\"clicks_l\\:100\"}", result.queryString());
        // @formatter:on

    }

    private SolrFilterQuery prepareTestQuery(SolrFilterQueryBuilder builder) {
        builder.appendFilterQuery(filter("color = red"));

        builder.appendAND();

        builder.openBrace();

        builder.appendFilterQuery(filter("taste = bad"));

        builder.appendOR();

        builder.openBrace();

        builder.startDependentJoin(NODE_TYPE_3);
        builder.appendFilterQuery(filter("flag_b = 1"));
        builder.endJoin();

        builder.appendAND();

        builder.openBrace();

        builder.startNestedJoin(NODE_TYPE_2);

        builder.appendFilterQuery(filter("imps1_l = 1000000"));

        builder.appendAND();

        builder.appendFilterQuery(filter("imps2_l = @imps3_l"));

        builder.endJoin();

        builder.appendOR();

        builder.appendFilterQuery(filter("age > 18"));

        builder.closeBrace();

        builder.closeBrace();

        builder.closeBrace();

        builder.appendAND();

        builder.appendNOT();

        builder.startNestedJoin(NODE_TYPE_2);

        builder.appendFilterQuery(filter("clicks_l = 100"));

        builder.endJoin();

        SolrFilterQuery res = builder.getResult();

        assertEquals(Arrays.asList(SolrConditionType.CMP_VALUE, SolrConditionType.CMP_RANGE, SolrConditionType.FRANGE), res.conditionTypes());

        assertEquals(8, res.fields().size());

        assertTrue(res.fields().contains(new SolrQueryField(NODE_TYPE_1, "color")));
        assertTrue(res.fields().contains(new SolrQueryField(NODE_TYPE_1, "taste")));
        assertTrue(res.fields().contains(new SolrQueryField(NODE_TYPE_1, "age")));

        assertTrue(res.fields().contains(new SolrQueryField(NODE_TYPE_2, "imps1_l")));
        assertTrue(res.fields().contains(new SolrQueryField(NODE_TYPE_2, "imps2_l")));
        assertTrue(res.fields().contains(new SolrQueryField(NODE_TYPE_2, "imps3_l")));
        assertTrue(res.fields().contains(new SolrQueryField(NODE_TYPE_2, "clicks_l")));

        assertTrue(res.fields().contains(new SolrQueryField(NODE_TYPE_3, "flag_b")));

        assertEquals(Arrays.asList(NODE_TYPE_1, NODE_TYPE_2, NODE_TYPE_3), res.nodeTypesInvolved());

        return res;

    }

    private SolrFilterQuery filter(String expression) {

        return ctx.getMatchFilterFactory().createMatchFilter(wrap(expression, ctx));

    }

}
