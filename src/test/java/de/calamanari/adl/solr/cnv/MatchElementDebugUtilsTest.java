//@formatter:off
/*
 * MatchElementDebugUtilsTest
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

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import de.calamanari.adl.CombinedExpressionType;

import static de.calamanari.adl.solr.cnv.ConversionTestUtils.createDryTestContext;
import static de.calamanari.adl.solr.cnv.ConversionTestUtils.matchTreeOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class MatchElementDebugUtilsTest {

    private SolrConversionProcessContext ctx = createDryTestContext();

    @Test
    void testAppendTreeLevelMembersDebugString() {

        StringBuilder sb = new StringBuilder();

        MatchTreeElement mte1 = matchTreeOf("(age=3 or taste=good) and (color=red or taste=bad)", ctx);

        MatchElementDebugUtils.appendTreeLevelMembersDebugString(sb, 0, "TestElement", CombinedExpressionType.AND, Arrays.asList(mte1));

        assertEquals("""
                TestElement (
                        CombinedMatchTreeElement __[combiType=AND (node1)] (
                                CombinedMatchTreeElement __[combiType=OR (node1)] (
                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=age = 3, matchInstruction=DEFAULT]
                                    OR
                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=taste = good, matchInstruction=DEFAULT]
                                )
                            AND
                                CombinedMatchTreeElement __[combiType=OR (node1)] (
                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=color = red, matchInstruction=DEFAULT]
                                    OR
                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=taste = bad, matchInstruction=DEFAULT]
                                )
                        )
                )""", sb.toString());

        MatchTreeElement mte2 = matchTreeOf("((field1_s=true and field2_s=true) or (field3_s=true and field4_s=true)) "
                + "and ((field5_s=true and field6_s=true) or (field7_s=true and field8_s=true))", ctx);

        sb.setLength(0);

        MatchElementDebugUtils.appendTreeLevelMembersDebugString(sb, 0, "TestElement", CombinedExpressionType.AND, Arrays.asList(mte1, mte2));

        assertEquals(
                """
                        TestElement (
                                CombinedMatchTreeElement __[combiType=AND (node1)] (
                                        CombinedMatchTreeElement __[combiType=OR (node1)] (
                                                SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=age = 3, matchInstruction=DEFAULT]
                                            OR
                                                SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=taste = good, matchInstruction=DEFAULT]
                                        )
                                    AND
                                        CombinedMatchTreeElement __[combiType=OR (node1)] (
                                                SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=color = red, matchInstruction=DEFAULT]
                                            OR
                                                SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=taste = bad, matchInstruction=DEFAULT]
                                        )
                                )
                            AND
                                CombinedMatchTreeElement __[combiType=AND (node1)] (
                                        CombinedMatchTreeElement __[combiType=OR (node1)] (
                                                CombinedMatchTreeElement __[combiType=AND (node1)] (
                                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field1_s = true, matchInstruction=DEFAULT]
                                                    AND
                                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field2_s = true, matchInstruction=DEFAULT]
                                                )
                                            OR
                                                CombinedMatchTreeElement __[combiType=AND (node1)] (
                                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field3_s = true, matchInstruction=DEFAULT]
                                                    AND
                                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field4_s = true, matchInstruction=DEFAULT]
                                                )
                                        )
                                    AND
                                        CombinedMatchTreeElement __[combiType=OR (node1)] (
                                                CombinedMatchTreeElement __[combiType=AND (node1)] (
                                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field5_s = true, matchInstruction=DEFAULT]
                                                    AND
                                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field6_s = true, matchInstruction=DEFAULT]
                                                )
                                            OR
                                                CombinedMatchTreeElement __[combiType=AND (node1)] (
                                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field7_s = true, matchInstruction=DEFAULT]
                                                    AND
                                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field8_s = true, matchInstruction=DEFAULT]
                                                )
                                        )
                                )
                        )""",
                sb.toString());

        sb.setLength(0);

        MatchElementDebugUtils.appendTreeLevelMembersDebugString(sb, 0, "TestElement", CombinedExpressionType.OR, Arrays.asList(mte1, mte2));

        assertEquals(
                """
                        TestElement (
                                CombinedMatchTreeElement __[combiType=AND (node1)] (
                                        CombinedMatchTreeElement __[combiType=OR (node1)] (
                                                SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=age = 3, matchInstruction=DEFAULT]
                                            OR
                                                SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=taste = good, matchInstruction=DEFAULT]
                                        )
                                    AND
                                        CombinedMatchTreeElement __[combiType=OR (node1)] (
                                                SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=color = red, matchInstruction=DEFAULT]
                                            OR
                                                SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=taste = bad, matchInstruction=DEFAULT]
                                        )
                                )
                            OR
                                CombinedMatchTreeElement __[combiType=AND (node1)] (
                                        CombinedMatchTreeElement __[combiType=OR (node1)] (
                                                CombinedMatchTreeElement __[combiType=AND (node1)] (
                                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field1_s = true, matchInstruction=DEFAULT]
                                                    AND
                                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field2_s = true, matchInstruction=DEFAULT]
                                                )
                                            OR
                                                CombinedMatchTreeElement __[combiType=AND (node1)] (
                                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field3_s = true, matchInstruction=DEFAULT]
                                                    AND
                                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field4_s = true, matchInstruction=DEFAULT]
                                                )
                                        )
                                    AND
                                        CombinedMatchTreeElement __[combiType=OR (node1)] (
                                                CombinedMatchTreeElement __[combiType=AND (node1)] (
                                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field5_s = true, matchInstruction=DEFAULT]
                                                    AND
                                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field6_s = true, matchInstruction=DEFAULT]
                                                )
                                            OR
                                                CombinedMatchTreeElement __[combiType=AND (node1)] (
                                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field7_s = true, matchInstruction=DEFAULT]
                                                    AND
                                                        SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field8_s = true, matchInstruction=DEFAULT]
                                                )
                                        )
                                )
                        )""",
                sb.toString());

        sb.setLength(0);

        MatchElementDebugUtils.appendTreeLevelMembersDebugString(sb, 0, "TestElement", CombinedExpressionType.AND, Collections.emptyList());

        assertEquals("""
                TestElement ()""", sb.toString());

        sb.setLength(0);

        MatchElementDebugUtils.appendTreeLevelMembersDebugString(sb, 0, "TestElement", CombinedExpressionType.AND, null);

        assertEquals("""
                TestElement (null)""", sb.toString());

    }

    @Test
    void testCreateListDebugString() {
        MatchTreeElement mte1 = matchTreeOf("(age=3 or taste=good) and (color=red or taste=bad)", ctx);

        MatchTreeElement mte2 = matchTreeOf("((field1_s=true and field2_s=true) or (field3_s=true and field4_s=true)) "
                + "and ((field5_s=true and field6_s=true) or (field7_s=true and field8_s=true))", ctx);

        assertEquals("""

                [
                    CombinedMatchTreeElement __[combiType=AND (node1)] (
                            CombinedMatchTreeElement __[combiType=OR (node1)] (
                                    SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=age = 3, matchInstruction=DEFAULT]
                                OR
                                    SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=taste = good, matchInstruction=DEFAULT]
                            )
                        AND
                            CombinedMatchTreeElement __[combiType=OR (node1)] (
                                    SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=color = red, matchInstruction=DEFAULT]
                                OR
                                    SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=taste = bad, matchInstruction=DEFAULT]
                            )
                    )
                ]
                """, MatchElementDebugUtils.createListDebugString(Arrays.asList(mte1)));

        assertEquals("""

                [
                    CombinedMatchTreeElement __[combiType=AND (node1)] (
                            CombinedMatchTreeElement __[combiType=OR (node1)] (
                                    SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=age = 3, matchInstruction=DEFAULT]
                                OR
                                    SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=taste = good, matchInstruction=DEFAULT]
                            )
                        AND
                            CombinedMatchTreeElement __[combiType=OR (node1)] (
                                    SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=color = red, matchInstruction=DEFAULT]
                                OR
                                    SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=taste = bad, matchInstruction=DEFAULT]
                            )
                    ),

                    CombinedMatchTreeElement __[combiType=AND (node1)] (
                            CombinedMatchTreeElement __[combiType=OR (node1)] (
                                    CombinedMatchTreeElement __[combiType=AND (node1)] (
                                            SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field1_s = true, matchInstruction=DEFAULT]
                                        AND
                                            SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field2_s = true, matchInstruction=DEFAULT]
                                    )
                                OR
                                    CombinedMatchTreeElement __[combiType=AND (node1)] (
                                            SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field3_s = true, matchInstruction=DEFAULT]
                                        AND
                                            SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field4_s = true, matchInstruction=DEFAULT]
                                    )
                            )
                        AND
                            CombinedMatchTreeElement __[combiType=OR (node1)] (
                                    CombinedMatchTreeElement __[combiType=AND (node1)] (
                                            SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field5_s = true, matchInstruction=DEFAULT]
                                        AND
                                            SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field6_s = true, matchInstruction=DEFAULT]
                                    )
                                OR
                                    CombinedMatchTreeElement __[combiType=AND (node1)] (
                                            SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field7_s = true, matchInstruction=DEFAULT]
                                        AND
                                            SingleMatchWrapper __[nodeType=node1, type=VALUE_MATCH, matchExpression=field8_s = true, matchInstruction=DEFAULT]
                                    )
                            )
                    )
                ]
                """, MatchElementDebugUtils.createListDebugString(Arrays.asList(mte1, mte2)));

        assertEquals("""

                []
                """, MatchElementDebugUtils.createListDebugString(Collections.emptyList()));

        assertEquals("""

                null
                """, MatchElementDebugUtils.createListDebugString(null));

    }

}
