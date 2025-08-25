//@formatter:off
/*
 * DefaultMatchTreeHelperTest
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

import static de.calamanari.adl.solr.cnv.ConversionTestUtils.assertEqualsIgnoreElementOrder;
import static de.calamanari.adl.solr.cnv.ConversionTestUtils.createDryTestContext;
import static de.calamanari.adl.solr.cnv.ConversionTestUtils.matchTreeOf;
import static de.calamanari.adl.solr.cnv.ConversionTestUtils.wrap;
import static de.calamanari.adl.solr.cnv.MatchElementDebugUtils.createListDebugString;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.calamanari.adl.CombinedExpressionType;
import de.calamanari.adl.solr.SolrTestBase;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class DefaultMatchTreeHelperTest extends SolrTestBase {
    static final Logger LOGGER = LoggerFactory.getLogger(DefaultMatchTreeHelperTest.class);

    private SolrConversionProcessContext ctx = createDryTestContext();

    private DefaultMatchTreeHelper helper = new DefaultMatchTreeHelper(ctx);

    @Test
    void testConsolidateUnaffected() {
        SingleMatchWrapper smw = enforceIsGroupingEligible(wrap("age=3", ctx));

        assertSame(smw, helper.consolidateMatchTree(smw));

        smw = wrap("STRICT age!=3", ctx);

        MatchTreeElement root = enforceIsGroupingEligible(matchTreeOf("age=3 and color=red and taste=bad", ctx));
        assertSame(root, helper.consolidateMatchTree(root));

        root = enforceIsGroupingEligible(matchTreeOf("age=3 or color=red or taste=bad", ctx));
        assertSame(root, helper.consolidateMatchTree(root));

        root = enforceIsGroupingEligible(matchTreeOf("(age=3 or taste=good) and (color=red or taste=bad)", ctx));

        assertSame(root, helper.consolidateMatchTree(root));

        root = enforceIsGroupingEligible(matchTreeOf(
                "((field1_s=true and field2_s=true) or (field3_s=true and field4_s=true)) and ((field5_s=true and field6_s=true) or (field7_s=true and field8_s=true))",
                ctx));

        assertSame(root, helper.consolidateMatchTree(root));

        root = enforceIsGroupingEligible(matchTreeOf("story_s contains any of (the, quick, brown, fox)", ctx));

        assertSame(root, helper.consolidateMatchTree(root));

        root = enforceIsGroupingEligible(matchTreeOf("fav_color_s=blue and fav_color_s=red and fav_color_s=black", ctx));

        assertSame(root, helper.consolidateMatchTree(root));

    }

    @Test
    void testConsolidateHybridToTheEnd() {

        MatchTreeElement root = enforceIsGroupingEligible(matchTreeOf("(age=3 or other_node_field_i=6) and (color=red or taste=bad)", ctx));
        MatchTreeElement consolidated = helper.consolidateMatchTree(root);

        assertNotEquals(root, consolidated);
        assertEqualsIgnoreElementOrder(root, consolidated);

    }

    @Test
    void testConsolidateMultiValueOrCombining() {

        assertEquals("MultiMatchWrapper *_[nodeType=node1, members=[age = 5, age = 6], matchInstruction=DEFAULT]", consolidate("age any of (5,6)").toString());

        assertEquals("MultiMatchWrapper *_[nodeType=node1, members=[age = 5, age = 6, age = 7, age = 8, age = 9], matchInstruction=DEFAULT]",
                consolidate("age any of (5,6,7,8,9)").toString());

        // @formatter:off
        assertEquals(
                """
                
                CombinedMatchTreeElement *_[combiType=OR (node1)] (
                        MultiMatchWrapper *_[nodeType=node1, members=[age = 5, age = 6], matchInstruction=DEFAULT]
                    OR
                        MultiMatchWrapper *_[nodeType=node1, members=[color = pink, color = red], matchInstruction=DEFAULT]
                )
                """,
                consolidate("age any of (5,6) OR color any of (red, pink)").toDebugString());
        // @formatter:off
        
        
        // @formatter:on

        assertEquals("""

                CombinedMatchTreeElement *_[combiType=AND (node1)] (
                        MultiMatchWrapper *_[nodeType=node1, members=[age = 5, age = 6], matchInstruction=DEFAULT]
                    AND
                        MultiMatchWrapper *_[nodeType=node1, members=[color = pink, color = red], matchInstruction=DEFAULT]
                )
                """, consolidate("age any of (5,6) AND color any of (red, pink)").toDebugString());
        // @formatter:on

    }

    @Test
    void testConsolidateMultiValueAndNotCombiningStrict() {

        assertEquals("MultiMatchWrapper *![nodeType=node1, members=[age = 5, age = 6], matchInstruction=NEGATE]",
                consolidate("age strict not any of (5,6)").toString());

        assertEquals("MultiMatchWrapper *![nodeType=node1, members=[age = 5, age = 6, age = 7, age = 8, age = 9], matchInstruction=NEGATE]",
                consolidate("age strict not any of (5,6,7,8,9)").toString());

        assertEquals("""

                CombinedMatchTreeElement *![combiType=OR (node1)] (
                        MultiMatchWrapper *![nodeType=node1, members=[age = 5, age = 6], matchInstruction=NEGATE]
                    OR
                        MultiMatchWrapper *![nodeType=node1, members=[color = pink, color = red], matchInstruction=NEGATE]
                )
                """, consolidate("age strict not any of (5,6) OR color strict not any of (red, pink)").toDebugString());

        assertEquals("""

                CombinedMatchTreeElement *![combiType=AND (node1)] (
                        MultiMatchWrapper *![nodeType=node1, members=[age = 5, age = 6], matchInstruction=NEGATE]
                    AND
                        MultiMatchWrapper *![nodeType=node1, members=[color = pink, color = red], matchInstruction=NEGATE]
                )
                """, consolidate("age strict not any of (5,6) AND color strict not any of (red, pink)").toDebugString());

    }

    @Test
    void testConsolidateMultiValueAndNotCombiningDefault() {

        // @formatter:off
        assertEquals(
                """

                CombinedMatchTreeElement *![combiType=OR (node1)] (
                        MultiMatchWrapper *![nodeType=node1, members=[age = 5, age = 6], matchInstruction=NEGATE]
                    OR
                        SingleMatchWrapper *![nodeType=node1, type=ANY_VALUE_MATCH, matchExpression=age IS UNKNOWN, matchInstruction=NEGATE]
                )                
                """,
                consolidate("age not any of (5,6)").toDebugString());
        // @formatter:on

        // @formatter:off
        assertEquals(
                """

                CombinedMatchTreeElement *![combiType=OR (node1)] (
                        MultiMatchWrapper *![nodeType=node1, members=[age = 5, age = 6, age = 7, age = 8, age = 9], matchInstruction=NEGATE]
                    OR
                        SingleMatchWrapper *![nodeType=node1, type=ANY_VALUE_MATCH, matchExpression=age IS UNKNOWN, matchInstruction=NEGATE]
                )                
                """,
                consolidate("age not any of (5,6,7,8,9)").toDebugString());
        // @formatter:on

        // @formatter:off
        assertEquals(
                """

                CombinedMatchTreeElement *![combiType=OR (node1)] (
                        MultiMatchWrapper *![nodeType=node1, members=[age = 5, age = 6], matchInstruction=NEGATE]
                    OR
                        SingleMatchWrapper *![nodeType=node1, type=ANY_VALUE_MATCH, matchExpression=age IS UNKNOWN, matchInstruction=NEGATE]
                    OR
                        MultiMatchWrapper *![nodeType=node1, members=[color = pink, color = red], matchInstruction=NEGATE]
                    OR
                        SingleMatchWrapper *![nodeType=node1, type=ANY_VALUE_MATCH, matchExpression=color IS UNKNOWN, matchInstruction=NEGATE]
                )                
                """,
                consolidate("age not any of (5,6) OR color not any of (red, pink)").toDebugString());
        // @formatter:on

        // @formatter:off
        assertEquals("""

                CombinedMatchTreeElement *![combiType=AND (node1)] (
                        CombinedMatchTreeElement *![combiType=OR (node1)] (
                                MultiMatchWrapper *![nodeType=node1, members=[age = 5, age = 6], matchInstruction=NEGATE]
                            OR
                                SingleMatchWrapper *![nodeType=node1, type=ANY_VALUE_MATCH, matchExpression=age IS UNKNOWN, matchInstruction=NEGATE]
                        )
                    AND
                        CombinedMatchTreeElement *![combiType=OR (node1)] (
                                MultiMatchWrapper *![nodeType=node1, members=[color = pink, color = red], matchInstruction=NEGATE]
                            OR
                                SingleMatchWrapper *![nodeType=node1, type=ANY_VALUE_MATCH, matchExpression=color IS UNKNOWN, matchInstruction=NEGATE]
                        )
                )                
                """,
                consolidate("age not any of (5,6) AND color not any of (red, pink)").toDebugString());
        // @formatter:on

    }

    @Test
    void testConsolidateMultiValueMixed() {

        // @formatter:off

        assertEquals(
                """

                CombinedMatchTreeElement *![combiType=AND (node1)] (
                        MultiMatchWrapper *_[nodeType=node1, members=[age = 8, age = 9], matchInstruction=DEFAULT]
                    AND
                        MultiMatchWrapper *![nodeType=node1, members=[age = 5, age = 6], matchInstruction=NEGATE]
                )
                """,
                consolidate("age strict not any of (5,6) AND age any of (8,9)").toDebugString());

        // @formatter:on

        // @formatter:off

        assertEquals(
                """

                CombinedMatchTreeElement *![combiType=AND (node1)] (
                        MultiMatchWrapper *_[nodeType=node1, members=[age = 8, age = 9], matchInstruction=DEFAULT]
                    AND
                        MultiMatchWrapper *![nodeType=node1, members=[age = 5, age = 6], matchInstruction=NEGATE]
                )
                """,
                consolidate("age not any of (5,6) AND age any of (8,9)").toDebugString());

        // @formatter:on

        // @formatter:off

        assertEquals(
                """

                CombinedMatchTreeElement *![combiType=OR (node1)] (
                        MultiMatchWrapper *_[nodeType=node1, members=[age = 8, age = 9], matchInstruction=DEFAULT]
                    OR
                        MultiMatchWrapper *![nodeType=node1, members=[age = 5, age = 6], matchInstruction=NEGATE]
                    OR
                        SingleMatchWrapper *![nodeType=node1, type=ANY_VALUE_MATCH, matchExpression=age IS UNKNOWN, matchInstruction=NEGATE]
                )
                """,
                consolidate("age not any of (5,6) OR age any of (8,9)").toDebugString());

        // @formatter:on

    }

    @Test
    void testConsolidateLtGtEqCombining() {
        assertEquals("MultiMatchWrapper *_[nodeType=node1, members=[age > 18, age = 18], matchInstruction=DEFAULT]", consolidate("age >= 18").toString());
        assertEquals("MultiMatchWrapper *_[nodeType=node1, members=[age < 18, age = 18], matchInstruction=DEFAULT]", consolidate("age <= 18").toString());

        assertEquals("MultiMatchWrapper *![nodeType=node1, members=[age > 18, age = 18], matchInstruction=NEGATE]",
                consolidate("STRICT NOT age >= 18").toString());
        assertEquals("MultiMatchWrapper *![nodeType=node1, members=[age < 18, age = 18], matchInstruction=NEGATE]",
                consolidate("STRICT NOT age <= 18").toString());

        // @formatter:off

        assertEquals(
                """

                CombinedMatchTreeElement *_[combiType=OR (node1)] (
                        MultiMatchWrapper *_[nodeType=node1, members=[age < 5, age = 5], matchInstruction=DEFAULT]
                    OR
                        MultiMatchWrapper *_[nodeType=node1, members=[age > 18, age = 18], matchInstruction=DEFAULT]
                )
                """,
                consolidate("age >= 18 OR age <= 5").toDebugString());

        // @formatter:on

        assertEquals("MultiMatchWrapper *![nodeType=node1, members=[age > 18, age = 18], matchInstruction=NEGATE]",
                consolidate("STRICT NOT age >= 18").toString());
        assertEquals("MultiMatchWrapper *![nodeType=node1, members=[age < 18, age = 18], matchInstruction=NEGATE]",
                consolidate("STRICT NOT age <= 18").toString());

    }

    @Test
    void testConsolidateBetweenCombiningPositive() {
        // @formatter:off

        assertEquals(
                "BetweenMatchWrapper *_[nodeType=node1, type=VALUE_GTE_AND_LTE_MATCH, argName=age, "
                        + "members=[age > 18, age = 18, age < 20, age = 20], matchInstruction=DEFAULT]",
                consolidate("age >= 18 AND age <= 20").toString());

        // @formatter:on

        // @formatter:off

        assertEquals(
                "BetweenMatchWrapper *_[nodeType=node1, type=VALUE_GT_AND_LTE_MATCH, argName=age, "
                        + "members=[age > 18, age < 20, age = 20], matchInstruction=DEFAULT]",
                consolidate("age > 18 AND age <= 20").toString());

        // @formatter:on

        // @formatter:off

        assertEquals(
                "BetweenMatchWrapper *_[nodeType=node1, type=VALUE_GT_AND_LT_MATCH, argName=age, "
                        + "members=[age > 18, age < 20], matchInstruction=DEFAULT]",
                consolidate("age > 18 AND age < 20").toString());

        // @formatter:on

        // @formatter:off

        assertEquals(
                "BetweenMatchWrapper *_[nodeType=node1, type=VALUE_GTE_AND_LT_MATCH, argName=age, "
                        + "members=[age > 18, age = 18, age < 20], matchInstruction=DEFAULT]",
                consolidate("age >= 18 AND age < 20").toString());

        // @formatter:on

        // @formatter:off

        assertEquals(
                "BetweenMatchWrapper *_[nodeType=node1, type=VALUE_GTE_AND_LTE_MATCH, argName=age, "
                        + "members=[age > 18, age = 18, age < 20, age = 20], matchInstruction=DEFAULT]",
                consolidate("age BETWEEN(18,20)").toString());

        // @formatter:on

    }

    @Test
    void testConsolidateBetweenCombiningNegative() {
        // @formatter:off

        assertEquals(
                "CombinedMatchTreeElement *![combiType=OR (node1), childElements=["
                        + "BetweenMatchWrapper *![nodeType=node1, type=VALUE_GTE_AND_LTE_MATCH, argName=age, "
                            + "members=[age > 18, age = 18, age < 20, age = 20], matchInstruction=NEGATE], "
                        + "SingleMatchWrapper *![nodeType=node1, type=ANY_VALUE_MATCH, matchExpression=age IS UNKNOWN, matchInstruction=NEGATE]]]",
                consolidate("NOT (age >= 18 AND age <= 20)").toString());

        // @formatter:on

        // @formatter:off

        assertEquals(
                "BetweenMatchWrapper *![nodeType=node1, type=VALUE_GTE_AND_LTE_MATCH, argName=age, "
                        + "members=[age > 18, age = 18, age < 20, age = 20], matchInstruction=NEGATE]",
                consolidate("STRICT NOT (age >= 18 AND age <= 20)").toString());

        // @formatter:on

        // @formatter:off

        assertEquals(
                "BetweenMatchWrapper *![nodeType=node1, type=VALUE_GT_AND_LTE_MATCH, argName=age, "
                        + "members=[age > 18, age < 20, age = 20], matchInstruction=NEGATE]",
                consolidate("STRICT NOT (age > 18 AND age <= 20)").toString());

        // @formatter:on

        // @formatter:off

        assertEquals(
                "BetweenMatchWrapper *![nodeType=node1, type=VALUE_GT_AND_LT_MATCH, argName=age, "
                        + "members=[age > 18, age < 20], matchInstruction=NEGATE]",
                consolidate("STRICT NOT (age > 18 AND age < 20)").toString());

        // @formatter:on

        // @formatter:off

        assertEquals(
                "BetweenMatchWrapper *![nodeType=node1, type=VALUE_GTE_AND_LT_MATCH, argName=age, "
                        + "members=[age > 18, age = 18, age < 20], matchInstruction=NEGATE]",
                consolidate("STRICT NOT (age >= 18 AND age < 20)").toString());

        // @formatter:on

        // @formatter:off

        assertEquals(
                "BetweenMatchWrapper *![nodeType=node1, type=VALUE_GTE_AND_LTE_MATCH, argName=age, "
                        + "members=[age > 18, age = 18, age < 20, age = 20], matchInstruction=NEGATE]",
                consolidate("age STRICT NOT BETWEEN(18,20)").toString());

        // @formatter:on

    }

    @Test
    void testConsolidateBetweenForbidden() {
        // @formatter:off

        assertEquals(
                """

                CombinedMatchTreeElement *_[combiType=AND (node1)] (
                        MultiMatchWrapper *_[nodeType=node1, members=[numbers < 17, numbers = 17], matchInstruction=DEFAULT]
                    AND
                        MultiMatchWrapper *_[nodeType=node1, members=[numbers > 8, numbers = 8], matchInstruction=DEFAULT]
                )
                """,
                consolidate("numbers BETWEEN(8,17)").toDebugString());

        // @formatter:on

        // @formatter:off

        assertEquals(
                """

                CombinedMatchTreeElement *![combiType=OR (node1)] (
                        MultiMatchWrapper *![nodeType=node1, members=[numbers < 17, numbers = 17], matchInstruction=NEGATE]
                    OR
                        MultiMatchWrapper *![nodeType=node1, members=[numbers > 8, numbers = 8], matchInstruction=NEGATE]
                )
                """,
                consolidate("numbers STRICT NOT BETWEEN(8,17)").toDebugString());

        // @formatter:on

        // @formatter:off

        assertEquals(
                """

                CombinedMatchTreeElement __[combiType=AND (node3)] (
                        MultiMatchWrapper __[nodeType=node3, members=[anyPurchaseDate < 2014-08-15, anyPurchaseDate = 2014-08-15], matchInstruction=DEFAULT]
                    AND
                        MultiMatchWrapper __[nodeType=node3, members=[anyPurchaseDate > 2014-08-12, anyPurchaseDate = 2014-08-12], matchInstruction=DEFAULT]
                )
                """,
                consolidate("anyPurchaseDate BETWEEN(2014-08-12, 2014-08-15)").toDebugString());

        // @formatter:on

    }

    @Test
    void testConsolidateMixedCombining() {

        // @formatter:off

        assertEquals(
                """

                CombinedMatchTreeElement *_[combiType=OR (node1)] (
                        MultiMatchWrapper *_[nodeType=node1, members=[age < 18, age = 18], matchInstruction=DEFAULT]
                    OR
                        MultiMatchWrapper *_[nodeType=node1, members=[age = 59, age = 89], matchInstruction=DEFAULT]
                )
                """,
                consolidate("age <= 18 OR age any of (59, 89)").toDebugString());

        // @formatter:on

        // @formatter:off

        assertEquals(
                """

                CombinedMatchTreeElement *![combiType=AND (node1)] (
                        MultiMatchWrapper *![nodeType=node1, members=[age < 18, age = 18], matchInstruction=NEGATE]
                    AND
                        MultiMatchWrapper *![nodeType=node1, members=[age = 59, age = 89], matchInstruction=NEGATE]
                )
                """,
                consolidate("STRICT NOT age <= 18 AND age STRICT NOT any of (59, 89)").toDebugString());
        // @formatter:on

    }

    @Test
    void testConsolidateDates() {

        // @formatter:off

        assertEquals(
                """

                CombinedMatchTreeElement *_[combiType=OR (node1)] (
                        SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=date_of_birth = 2024-03-11, matchInstruction=DEFAULT]
                    OR
                        SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=date_of_birth = 2024-03-12, matchInstruction=DEFAULT]
                )
                """,
                consolidate("date_of_birth any of (2024-03-11, 2024-03-12)").toDebugString());
        // @formatter:on

        assertEquals("MultiMatchWrapper *_[nodeType=node1, members=[date_of_birth > 2024-03-11, date_of_birth = 2024-03-11], matchInstruction=DEFAULT]",
                consolidate("date_of_birth >= 2024-03-11").toString());

        ctx.getGlobalFlags().add(SolrConversionDirective.DISABLE_DATE_TIME_ALIGNMENT);

        assertEquals("MultiMatchWrapper *_[nodeType=node1, members=[date_of_birth = 2024-03-11, date_of_birth = 2024-03-12], matchInstruction=DEFAULT]",
                consolidate("date_of_birth any of (2024-03-11, 2024-03-12)").toString());

        ctx.getGlobalFlags().remove(SolrConversionDirective.DISABLE_DATE_TIME_ALIGNMENT);

        // @formatter:off

        assertEquals(
                """

                CombinedMatchTreeElement *_[combiType=OR (node1)] (
                        SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=date_of_birth = 2024-03-11, matchInstruction=DEFAULT]
                    OR
                        SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=date_of_birth = 2024-03-12, matchInstruction=DEFAULT]
                )
                """,
                consolidate("date_of_birth any of (2024-03-11, 2024-03-12)").toDebugString());
        // @formatter:on

    }

    @Test
    void testSafetyAssertion() {

        SingleMatchWrapper smw1 = wrap("age = 17", ctx);
        SingleMatchWrapper smw2 = wrap("STRICT NOT age > 19", ctx);
        List<SingleMatchWrapper> emptyList = Collections.emptyList();

        assertThrows(IllegalStateException.class,
                () -> DefaultMatchTreeHelper.assertSameMatchInstructionInMultiMatch(smw1, smw2, CombinedExpressionType.AND, emptyList));
    }

    @Test
    void testCreateNodeTypeMatchTreeElementGroupBasics() {

        SingleMatchWrapper smw1 = wrap("age = 17", ctx);
        SingleMatchWrapper smw2 = wrap("STRICT NOT age > 19", ctx);

        assertEquals(Collections.singletonList(smw1), helper.createExecutionGroups(smw1));
        assertEquals(Collections.singletonList(smw2), helper.createExecutionGroups(smw2));

        // @formatter:off

        assertEquals(
                """

                [
                    NodeTypeMatchTreeElementGroup [combiType=AND node1] (
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=age = 18, matchInstruction=DEFAULT]
                        AND
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=color = red, matchInstruction=DEFAULT]
                    )
                ]
                """,
                createListDebugString(group("age = 18 and color = red")));
        // @formatter:on

        // @formatter:off

        assertEquals("""

                [
                    NodeTypeMatchTreeElementGroup [combiType=OR node1] (
                            CombinedMatchTreeElement *_[combiType=AND (node1)] (
                                    SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=age = 18, matchInstruction=DEFAULT]
                                AND
                                    SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=color = red, matchInstruction=DEFAULT]
                            )
                        OR
                            CombinedMatchTreeElement *_[combiType=AND (node1)] (
                                    SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=date_of_birth > 2020-01-01, matchInstruction=DEFAULT]
                                AND
                                    SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=taste = bad, matchInstruction=DEFAULT]
                            )
                    )
                ]
                """,
                createListDebugString(group("(age = 18 and color = red) OR (taste = bad and date_of_birth > 2020-01-01)")));
        // @formatter:on

        List<MatchElement> grouped = group("age = 18 and (flag1_b=1 OR (flag2_b=0 AND contact_date = 2024-09-01))");

        // @formatter:off

        assertEquals("""

                [
                    SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=age = 18, matchInstruction=DEFAULT],
                
                    CombinedMatchTreeElement *_[combiType=OR (node3)] (
                            CombinedMatchTreeElement *_[combiType=AND (node3)] (
                                    SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=contact_date = 2024-09-01, matchInstruction=DEFAULT]
                                AND
                                    SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=flag2_b = 0, matchInstruction=DEFAULT]
                            )
                        OR
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=flag1_b = 1, matchInstruction=DEFAULT]
                    )
                ]
                """,
                createListDebugString(grouped));
        // @formatter:on

        List<MatchElement> subGrouped0 = helper.createExecutionGroups((MatchTreeElement) grouped.get(0));

        assertEquals("[SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=age = 18, matchInstruction=DEFAULT]]", subGrouped0.toString());

        List<MatchElement> subGrouped1 = helper.createExecutionGroups((MatchTreeElement) grouped.get(1));

        // @formatter:off

        assertEquals(
                """

                [
                    NodeTypeMatchTreeElementGroup [combiType=OR node3] (
                            CombinedMatchTreeElement *_[combiType=AND (node3)] (
                                    SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=contact_date = 2024-09-01, matchInstruction=DEFAULT]
                                AND
                                    SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=flag2_b = 0, matchInstruction=DEFAULT]
                            )
                        OR
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=flag1_b = 1, matchInstruction=DEFAULT]
                    )
                ]
                """,
                createListDebugString(subGrouped1));

        // @formatter:on

        grouped = group("age = 18 and color!=red and taste != bad");

        // @formatter:off

        assertEquals(
                """

                [
                    NodeTypeMatchTreeElementGroup [combiType=AND node1] (
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=age = 18, matchInstruction=DEFAULT]
                        AND
                            CombinedMatchTreeElement *![combiType=OR (node1)] (
                                    SingleMatchWrapper *![nodeType=node1, type=VALUE_MATCH, matchExpression=color = red, matchInstruction=NEGATE]
                                OR
                                    SingleMatchWrapper *![nodeType=node1, type=ANY_VALUE_MATCH, matchExpression=color IS UNKNOWN, matchInstruction=NEGATE]
                            )
                        AND
                            CombinedMatchTreeElement *![combiType=OR (node1)] (
                                    SingleMatchWrapper *![nodeType=node1, type=VALUE_MATCH, matchExpression=taste = bad, matchInstruction=NEGATE]
                                OR
                                    SingleMatchWrapper *![nodeType=node1, type=ANY_VALUE_MATCH, matchExpression=taste IS UNKNOWN, matchInstruction=NEGATE]
                            )
                    )
                ]
                """,
                createListDebugString(grouped));
        // @formatter:on

        grouped = group("(age = 18 and color!=red and taste != bad) OR (flag1_b = 1 AND contact_date_dt != 2025-01-01)");

        // @formatter:off

        assertEquals("""

                [
                    CombinedMatchTreeElement *![combiType=AND (node1)] (
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=age = 18, matchInstruction=DEFAULT]
                        AND
                            CombinedMatchTreeElement *![combiType=OR (node1)] (
                                    SingleMatchWrapper *![nodeType=node1, type=VALUE_MATCH, matchExpression=color = red, matchInstruction=NEGATE]
                                OR
                                    SingleMatchWrapper *![nodeType=node1, type=ANY_VALUE_MATCH, matchExpression=color IS UNKNOWN, matchInstruction=NEGATE]
                            )
                        AND
                            CombinedMatchTreeElement *![combiType=OR (node1)] (
                                    SingleMatchWrapper *![nodeType=node1, type=VALUE_MATCH, matchExpression=taste = bad, matchInstruction=NEGATE]
                                OR
                                    SingleMatchWrapper *![nodeType=node1, type=ANY_VALUE_MATCH, matchExpression=taste IS UNKNOWN, matchInstruction=NEGATE]
                            )
                    ),
                
                    CombinedMatchTreeElement _![combiType=AND (<HYBRID>)] (
                            CombinedMatchTreeElement *![combiType=OR (node1)] (
                                    SingleMatchWrapper *![nodeType=node1, type=VALUE_MATCH, matchExpression=contact_date_dt = 2025-01-01, matchInstruction=NEGATE]
                                OR
                                    SingleMatchWrapper *![nodeType=node1, type=ANY_VALUE_MATCH, matchExpression=contact_date_dt IS UNKNOWN, matchInstruction=NEGATE]
                            )
                        AND
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=flag1_b = 1, matchInstruction=DEFAULT]
                    )
                ]
                """,
                createListDebugString(grouped));
        // @formatter:on

        grouped = group("age = 18 OR color!=red OR taste != bad OR (flag1_b = 1 AND contact_date_dt != 2025-01-01)");

        // @formatter:off

        assertEquals(
                """

                [
                    NodeTypeMatchTreeElementGroup [combiType=OR node1] (
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=age = 18, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *![nodeType=node1, type=VALUE_MATCH, matchExpression=color = red, matchInstruction=NEGATE]
                        OR
                            SingleMatchWrapper *![nodeType=node1, type=ANY_VALUE_MATCH, matchExpression=color IS UNKNOWN, matchInstruction=NEGATE]
                        OR
                            SingleMatchWrapper *![nodeType=node1, type=VALUE_MATCH, matchExpression=taste = bad, matchInstruction=NEGATE]
                        OR
                            SingleMatchWrapper *![nodeType=node1, type=ANY_VALUE_MATCH, matchExpression=taste IS UNKNOWN, matchInstruction=NEGATE]
                    ),
                
                    CombinedMatchTreeElement _![combiType=AND (<HYBRID>)] (
                            CombinedMatchTreeElement *![combiType=OR (node1)] (
                                    SingleMatchWrapper *![nodeType=node1, type=VALUE_MATCH, matchExpression=contact_date_dt = 2025-01-01, matchInstruction=NEGATE]
                                OR
                                    SingleMatchWrapper *![nodeType=node1, type=ANY_VALUE_MATCH, matchExpression=contact_date_dt IS UNKNOWN, matchInstruction=NEGATE]
                            )
                        AND
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=flag1_b = 1, matchInstruction=DEFAULT]
                    )
                ]
                """,
                createListDebugString(grouped));

        // @formatter:on

    }

    @Test
    void testCreateNodeTypeMatchTreeElementGroupOneLevelMixed() {

        List<MatchElement> grouped = group("a_s = foo OR b_i != 3 OR c_b = 1 OR d_s = bar OR e_i = 4 OR f_f = 0.8");

        // @formatter:off

        assertEquals(
                """

                [
                    NodeTypeMatchTreeElementGroup [combiType=OR node1] (
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=a_s = foo, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=d_s = bar, matchInstruction=DEFAULT]
                    ),
                
                    SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=e_i = 4, matchInstruction=DEFAULT],
                
                    SingleMatchWrapper _![nodeType=node2, type=VALUE_MATCH, matchExpression=b_i = 3, matchInstruction=NEGATE],
                
                    SingleMatchWrapper _![nodeType=node2, type=ANY_VALUE_MATCH, matchExpression=b_i IS UNKNOWN, matchInstruction=NEGATE],
                
                    NodeTypeMatchTreeElementGroup [combiType=OR node3] (
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=c_b = 1, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=f_f = 0.8, matchInstruction=DEFAULT]
                    )
                ]
                """,
                createListDebugString(grouped));

        // @formatter:on

        grouped = group("a_s = foo OR b_i = 3 OR c_b = 1 OR d_s = bar OR e_i = 4 OR f_f = 0.8");

        // @formatter:off

        assertEquals(
                """

                [
                    NodeTypeMatchTreeElementGroup [combiType=OR node1] (
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=a_s = foo, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=d_s = bar, matchInstruction=DEFAULT]
                    ),
                
                    NodeTypeMatchTreeElementGroup [combiType=OR node2] (
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=b_i = 3, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=e_i = 4, matchInstruction=DEFAULT]
                    ),
                
                    NodeTypeMatchTreeElementGroup [combiType=OR node3] (
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=c_b = 1, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=f_f = 0.8, matchInstruction=DEFAULT]
                    )
                ]
                """,
                createListDebugString(grouped));

        // @formatter:on

        grouped = group("a_s = foo AND b_i = 3 AND c_b = 1 AND d_s = bar AND e_i = 4 AND f_f = 0.8");

        // @formatter:off

        assertEquals(
                """

                [
                    NodeTypeMatchTreeElementGroup [combiType=AND node1] (
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=a_s = foo, matchInstruction=DEFAULT]
                        AND
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=d_s = bar, matchInstruction=DEFAULT]
                    ),
                
                    NodeTypeMatchTreeElementGroup [combiType=AND node2] (
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=b_i = 3, matchInstruction=DEFAULT]
                        AND
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=e_i = 4, matchInstruction=DEFAULT]
                    ),
                
                    NodeTypeMatchTreeElementGroup [combiType=AND node3] (
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=c_b = 1, matchInstruction=DEFAULT]
                        AND
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=f_f = 0.8, matchInstruction=DEFAULT]
                    )
                ]
                """,
                createListDebugString(grouped));

        // @formatter:on

        grouped = group("a_s = foo AND b_i != 3 AND c_b = 1 AND d_s = bar AND e_i = 4 AND f_f = 0.8");

        // @formatter:off

        assertEquals(
                """

                [
                    NodeTypeMatchTreeElementGroup [combiType=AND node1] (
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=a_s = foo, matchInstruction=DEFAULT]
                        AND
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=d_s = bar, matchInstruction=DEFAULT]
                    ),
                
                    SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=e_i = 4, matchInstruction=DEFAULT],
                
                    CombinedMatchTreeElement _![combiType=OR (node2)] (
                            SingleMatchWrapper _![nodeType=node2, type=VALUE_MATCH, matchExpression=b_i = 3, matchInstruction=NEGATE]
                        OR
                            SingleMatchWrapper _![nodeType=node2, type=ANY_VALUE_MATCH, matchExpression=b_i IS UNKNOWN, matchInstruction=NEGATE]
                    ),
                
                    NodeTypeMatchTreeElementGroup [combiType=AND node3] (
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=c_b = 1, matchInstruction=DEFAULT]
                        AND
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=f_f = 0.8, matchInstruction=DEFAULT]
                    )
                ]
                """,
               createListDebugString(grouped));

        // @formatter:on

        grouped = group("a_s = foo OR b_i != 3 OR c_b = 1 OR d_s = bar OR e_i = 4 OR (f_f = 0.8 AND STRICT g_f!=0.7) OR h_f=1.3");

        // @formatter:off

        assertEquals(
                """

                [
                    NodeTypeMatchTreeElementGroup [combiType=OR node1] (
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=a_s = foo, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=d_s = bar, matchInstruction=DEFAULT]
                    ),
                
                    SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=e_i = 4, matchInstruction=DEFAULT],
                
                    SingleMatchWrapper _![nodeType=node2, type=VALUE_MATCH, matchExpression=b_i = 3, matchInstruction=NEGATE],
                
                    SingleMatchWrapper _![nodeType=node2, type=ANY_VALUE_MATCH, matchExpression=b_i IS UNKNOWN, matchInstruction=NEGATE],
                
                    NodeTypeMatchTreeElementGroup [combiType=OR node3] (
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=c_b = 1, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=h_f = 1.3, matchInstruction=DEFAULT]
                    ),
                
                    CombinedMatchTreeElement _![combiType=AND (node3)] (
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=f_f = 0.8, matchInstruction=DEFAULT]
                        AND
                            SingleMatchWrapper _![nodeType=node3, type=VALUE_MATCH, matchExpression=g_f = 0.7, matchInstruction=NEGATE]
                    )
                ]
                """,
                createListDebugString(grouped));

        // @formatter:on

    }

    @Test
    void testCreateNodeTypeMatchTreeElementGroupOneLevelMixedMultiDoc() {

        List<MatchElement> grouped = group("a_s = foo OR b_i = 3 OR c_b = 1 OR d_s = bar OR e_i = 4 OR f_f = 0.8");

        // @formatter:off

        assertEquals(
                """

                [
                    NodeTypeMatchTreeElementGroup [combiType=OR node1] (
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=a_s = foo, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=d_s = bar, matchInstruction=DEFAULT]
                    ),
                
                    NodeTypeMatchTreeElementGroup [combiType=OR node2] (
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=b_i = 3, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=e_i = 4, matchInstruction=DEFAULT]
                    ),
                
                    NodeTypeMatchTreeElementGroup [combiType=OR node3] (
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=c_b = 1, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=f_f = 0.8, matchInstruction=DEFAULT]
                    )
                ]
                """,
                createListDebugString(grouped));

        // @formatter:on

        grouped = group("a_s = foo OR b_md_i = 3 OR c_b = 1 OR d_s = bar OR e_i = 4 OR f_f = 0.8");

        // @formatter:off

        assertEquals(
                """

                [
                    NodeTypeMatchTreeElementGroup [combiType=OR node1] (
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=a_s = foo, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=d_s = bar, matchInstruction=DEFAULT]
                    ),
                
                    NodeTypeMatchTreeElementGroup [combiType=OR node2] (
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=b_md_i = 3, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=e_i = 4, matchInstruction=DEFAULT]
                    ),
                
                    NodeTypeMatchTreeElementGroup [combiType=OR node3] (
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=c_b = 1, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=f_f = 0.8, matchInstruction=DEFAULT]
                    )
                ]
                """,
                createListDebugString(grouped));

        // @formatter:on

        grouped = group("b_i = 1 AND c_i = 2 AND (d_i = 4 OR d_i = 5 OR d_i = 6 OR (e_i = 7 AND f_i = 7))");

        // @formatter:off

        assertEquals(
                """

                [
                    NodeTypeMatchTreeElementGroup [combiType=AND node2] (
                            CombinedMatchTreeElement *_[combiType=OR (node2)] (
                                    CombinedMatchTreeElement *_[combiType=AND (node2)] (
                                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=e_i = 7, matchInstruction=DEFAULT]
                                        AND
                                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=f_i = 7, matchInstruction=DEFAULT]
                                    )
                                OR
                                    MultiMatchWrapper *_[nodeType=node2, members=[d_i = 4, d_i = 5, d_i = 6], matchInstruction=DEFAULT]
                            )
                        AND
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=b_i = 1, matchInstruction=DEFAULT]
                        AND
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=c_i = 2, matchInstruction=DEFAULT]
                    )
                ]
                """,
                createListDebugString(grouped));

        // @formatter:on

        grouped = group("b_i = 1 AND c_i = 2 AND (d_i = 4 OR d_i = 5 OR d_i = 6 OR (e_i = 7 AND f_md_i = 7))");

        // @formatter:off

        assertEquals(
                """

                [
                    CombinedMatchTreeElement __[combiType=OR (node2)] (
                            CombinedMatchTreeElement __[combiType=AND (node2)] (
                                    SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=e_i = 7, matchInstruction=DEFAULT]
                                AND
                                    SingleMatchWrapper __[nodeType=node2, type=VALUE_MATCH, matchExpression=f_md_i = 7, matchInstruction=DEFAULT]
                            )
                        OR
                            MultiMatchWrapper *_[nodeType=node2, members=[d_i = 4, d_i = 5, d_i = 6], matchInstruction=DEFAULT]
                    ),
                
                    NodeTypeMatchTreeElementGroup [combiType=AND node2] (
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=b_i = 1, matchInstruction=DEFAULT]
                        AND
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=c_i = 2, matchInstruction=DEFAULT]
                    )
                ]
                """,
                createListDebugString(grouped));

        // @formatter:on

        grouped = group("b_i = 1 AND c_i = 2 AND (d_md_i = 4 OR d_md_i = 5 OR d_md_i = 6 OR (e_i = 7 AND f_i = 7))");

        // @formatter:off

        assertEquals(
                """

                [
                    CombinedMatchTreeElement __[combiType=OR (node2)] (
                            CombinedMatchTreeElement *_[combiType=AND (node2)] (
                                    SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=e_i = 7, matchInstruction=DEFAULT]
                                AND
                                    SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=f_i = 7, matchInstruction=DEFAULT]
                            )
                        OR
                            MultiMatchWrapper __[nodeType=node2, members=[d_md_i = 4, d_md_i = 5, d_md_i = 6], matchInstruction=DEFAULT]
                    ),
                
                    NodeTypeMatchTreeElementGroup [combiType=AND node2] (
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=b_i = 1, matchInstruction=DEFAULT]
                        AND
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=c_i = 2, matchInstruction=DEFAULT]
                    )
                ]
                """,
                createListDebugString(grouped));

        // @formatter:on

    }

    @Test
    void testCreateNodeTypeMatchTreeElementGroupCannotGroup() {

        List<MatchElement> grouped = group("(a_s = foo OR b_i != 3) AND (c_b = 1 OR d_s = bar) AND (e_i = 4 OR f_f = 0.8)");

        // @formatter:off

        assertEquals(
                """

                [
                    CombinedMatchTreeElement __[combiType=OR (<HYBRID>)] (
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=d_s = bar, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=c_b = 1, matchInstruction=DEFAULT]
                    ),
                
                    CombinedMatchTreeElement __[combiType=OR (<HYBRID>)] (
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=e_i = 4, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=f_f = 0.8, matchInstruction=DEFAULT]
                    ),
                
                    CombinedMatchTreeElement _![combiType=OR (<HYBRID>)] (
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=a_s = foo, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper _![nodeType=node2, type=VALUE_MATCH, matchExpression=b_i = 3, matchInstruction=NEGATE]
                        OR
                            SingleMatchWrapper _![nodeType=node2, type=ANY_VALUE_MATCH, matchExpression=b_i IS UNKNOWN, matchInstruction=NEGATE]
                    )
                ]
                """,
                createListDebugString(grouped));

        // @formatter:on

        grouped = group("(a_s = foo AND b_i != 3) OR (c_b = 1 AND d_s = bar) OR (e_i = 4 AND f_f = 0.8)");

        // @formatter:off

        assertEquals(
                """

                [
                    CombinedMatchTreeElement __[combiType=AND (<HYBRID>)] (
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=d_s = bar, matchInstruction=DEFAULT]
                        AND
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=c_b = 1, matchInstruction=DEFAULT]
                    ),
                
                    CombinedMatchTreeElement __[combiType=AND (<HYBRID>)] (
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=e_i = 4, matchInstruction=DEFAULT]
                        AND
                            SingleMatchWrapper *_[nodeType=node3, type=VALUE_MATCH, matchExpression=f_f = 0.8, matchInstruction=DEFAULT]
                    ),
                
                    CombinedMatchTreeElement _![combiType=AND (<HYBRID>)] (
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=a_s = foo, matchInstruction=DEFAULT]
                        AND
                            CombinedMatchTreeElement _![combiType=OR (node2)] (
                                    SingleMatchWrapper _![nodeType=node2, type=VALUE_MATCH, matchExpression=b_i = 3, matchInstruction=NEGATE]
                                OR
                                    SingleMatchWrapper _![nodeType=node2, type=ANY_VALUE_MATCH, matchExpression=b_i IS UNKNOWN, matchInstruction=NEGATE]
                            )
                    )
                ]
                """,
                createListDebugString(grouped));

        // @formatter:on

        grouped = group("a_s = foo AND (d_s = bar OR e_i = 4)");

        // @formatter:off

        assertEquals(
                """

                [
                    SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=a_s = foo, matchInstruction=DEFAULT],
                
                    CombinedMatchTreeElement __[combiType=OR (<HYBRID>)] (
                            SingleMatchWrapper *_[nodeType=node1, type=VALUE_MATCH, matchExpression=d_s = bar, matchInstruction=DEFAULT]
                        OR
                            SingleMatchWrapper *_[nodeType=node2, type=VALUE_MATCH, matchExpression=e_i = 4, matchInstruction=DEFAULT]
                    )
                ]
                """,
                createListDebugString(grouped));

        // @formatter:on

    }

    private MatchTreeElement consolidate(String expression) {
        return helper.consolidateMatchTree(matchTreeOf(expression, ctx));
    }

    private List<MatchElement> group(String expression) {
        MatchTreeElement root = consolidate(expression);
        return helper.createExecutionGroups(root);
    }

    private static <T extends MatchTreeElement> T enforceIsGroupingEligible(T element) {

        if (element.isGroupingEligible()) {
            return element;
        }

        switch (element) {
        case SingleMatchWrapper smw:
            @SuppressWarnings("unchecked")
            T smwReplaced = (T) new SingleMatchWrapper(smw.nodeType(), smw.matchExpression(), smw.matchInstruction(), true);
            return smwReplaced;
        case MultiMatchWrapper mmw:
            @SuppressWarnings("unchecked")
            T mmwReplaced = (T) new MultiMatchWrapper(mmw.nodeType(), mmw.members(), mmw.matchInstruction(), true);
            return mmwReplaced;
        case CombinedMatchTreeElement cmte:
            @SuppressWarnings("unchecked")
            T cmteReplaced = (T) new CombinedMatchTreeElement(cmte.combiType(),
                    cmte.childElements().stream().map(DefaultMatchTreeHelperTest::enforceIsGroupingEligible).toList());
            if (cmte.isGroupingEligible()) {
                return element;
            }
            else {
                return cmteReplaced;
            }
        default:
            throw new IllegalArgumentException("unexpected: " + element);
        }

    }

}
