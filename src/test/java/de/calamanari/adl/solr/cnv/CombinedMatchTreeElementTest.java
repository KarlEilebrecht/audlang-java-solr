//@formatter:off
/*
 * CombinedMatchTreeElementTest
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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.calamanari.adl.CombinedExpressionType;
import de.calamanari.adl.irl.CombinedExpression;
import de.calamanari.adl.irl.CoreExpression;
import de.calamanari.adl.irl.MatchExpression;
import de.calamanari.adl.irl.NegationExpression;
import de.calamanari.adl.solr.SolrTestBase;

import static de.calamanari.adl.CombinedExpressionType.AND;
import static de.calamanari.adl.CombinedExpressionType.OR;
import static de.calamanari.adl.solr.cnv.ConversionTestUtils.expr;
import static de.calamanari.adl.solr.cnv.ConversionTestUtils.exprList;
import static de.calamanari.adl.solr.cnv.MatchInstruction.DEFAULT;
import static de.calamanari.adl.solr.cnv.MatchInstruction.NEGATE;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class CombinedMatchTreeElementTest extends SolrTestBase {
    static final Logger LOGGER = LoggerFactory.getLogger(CombinedMatchTreeElementTest.class);

    @Test
    void testBasics() {

        assertCanConstruct(AND, mteList(NODE_TYPE_1, "a=1", "b=2"), NODE_TYPE_1);
        assertCanConstruct(AND, mteList(NODE_TYPE_1, "a=1", "b=2"), null);
        assertCanConstruct(OR, mteList(NODE_TYPE_1, "a=1", "b=2", "c=4"));

        assertCannotConstruct(null, null, null);
        assertCannotConstruct(AND, null, null);
        assertCannotConstruct(AND, Collections.emptyList(), null);
        assertCannotConstruct(AND, mteList(NODE_TYPE_1, "a=1"), null);
        assertCannotConstruct(OR, mteList(NODE_TYPE_1, "a=1", "b=2", "c=4"), NODE_TYPE_2);

    }

    @Test
    void testComplex() {
        assertCanConstruct(AND, mteList(NODE_TYPE_1, "(a=1 and c!=3) or e=@f", "b=2"));

        CombinedMatchTreeElement cmte = new CombinedMatchTreeElement(AND, mteList(NODE_TYPE_1, "(a=1 and c!=3) or e=@f", "b=2"));

        assertTrue(cmte.containsAnyNegation());

        cmte = new CombinedMatchTreeElement(OR, Arrays.asList(mte(NODE_TYPE_1, "a=1"), mte(NODE_TYPE_2, "b=2")));

        assertNull(cmte.commonNodeType());

        MultiMatchWrapper notInClause = new MultiMatchWrapper(NODE_TYPE_1, exprList("k = 1", "k = 90", "k = 45"), NEGATE, false);

        cmte = new CombinedMatchTreeElement(OR, Arrays.asList(mte(NODE_TYPE_1, "a=1"), mte(NODE_TYPE_1, "b=2"), notInClause));

        assertEquals(NODE_TYPE_1, cmte.commonNodeType());
        assertTrue(cmte.containsAnyNegation());

    }

    @Test
    void testMemberMismatch() {

        for (MatchInstruction instruction : MatchInstruction.values()) {
            SingleMatchWrapper left = new SingleMatchWrapper(NODE_TYPE_1, expr("b = @k"), instruction, false);
            SingleMatchWrapper right = new SingleMatchWrapper(NODE_TYPE_1, expr("c = @k"), DEFAULT, false);
            List<MatchTreeElement> members = Arrays.asList(left, right);

            boolean isGroupingEligible = members.stream().allMatch(MatchTreeElement::isGroupingEligible);
            boolean containsAnyNegation = members.stream().anyMatch(MatchTreeElement::containsAnyNegation);

            if (instruction == DEFAULT) {
                CombinedMatchTreeElement cmte = new CombinedMatchTreeElement(AND, members, NODE_TYPE_1, isGroupingEligible, containsAnyNegation);
                assertFalse(cmte.containsAnyNegation());
                cmte = new CombinedMatchTreeElement(OR, members, NODE_TYPE_1, isGroupingEligible, containsAnyNegation);
                assertFalse(cmte.containsAnyNegation());
            }
            else {
                assertThrows(IllegalArgumentException.class, () -> new CombinedMatchTreeElement(AND, members, NODE_TYPE_1, isGroupingEligible, false));
                assertThrows(IllegalArgumentException.class, () -> new CombinedMatchTreeElement(OR, members, NODE_TYPE_1, isGroupingEligible, false));

            }

        }

        SingleMatchWrapper left = new SingleMatchWrapper(NODE_TYPE_1, expr("b = 1"), DEFAULT, true);
        SingleMatchWrapper right = new SingleMatchWrapper(NODE_TYPE_1, expr("c = 1"), DEFAULT, false);
        List<MatchTreeElement> members = Arrays.asList(left, right);

        assertThrows(IllegalArgumentException.class, () -> new CombinedMatchTreeElement(AND, members, NODE_TYPE_1, true, false));
        assertThrows(IllegalArgumentException.class, () -> new CombinedMatchTreeElement(OR, members, NODE_TYPE_1, true, false));

        SingleMatchWrapper left1 = new SingleMatchWrapper(NODE_TYPE_1, expr("b = 1"), DEFAULT, false);
        SingleMatchWrapper right2 = new SingleMatchWrapper(NODE_TYPE_2, expr("c = 1"), DEFAULT, false);
        List<MatchTreeElement> members2 = Arrays.asList(left1, right2);

        assertThrows(IllegalArgumentException.class, () -> new CombinedMatchTreeElement(AND, members2, null, true, false));
        assertThrows(IllegalArgumentException.class, () -> new CombinedMatchTreeElement(OR, members2, null, true, false));

    }

    private static void assertCannotConstruct(CombinedExpressionType combiType, List<MatchTreeElement> childElements, String commonNodeType) {
        boolean isGroupingEligible = childElements != null && childElements.stream().allMatch(MatchTreeElement::isGroupingEligible);
        boolean containsAnyNegation = childElements != null && childElements.stream().anyMatch(MatchTreeElement::containsAnyNegation);
        assertThrows(IllegalArgumentException.class,
                () -> new CombinedMatchTreeElement(combiType, childElements, commonNodeType, isGroupingEligible, containsAnyNegation));
    }

    private static void assertCanConstruct(CombinedExpressionType combiType, List<MatchTreeElement> childElements) {
        assertCanConstruct(combiType, childElements, null);
    }

    private static void assertCanConstruct(CombinedExpressionType combiType, List<MatchTreeElement> childElements, String commonNodeType) {

        boolean isGroupingEligible = childElements != null && childElements.stream().allMatch(MatchTreeElement::isGroupingEligible);
        boolean containsAnyNegation = childElements != null && childElements.stream().anyMatch(MatchTreeElement::containsAnyNegation);

        CombinedMatchTreeElement cmte = new CombinedMatchTreeElement(combiType, childElements, commonNodeType, isGroupingEligible, containsAnyNegation);
        CombinedMatchTreeElement cmte2 = new CombinedMatchTreeElement(combiType, childElements);

        assertEquals(cmte, cmte2);
        assertEquals(combiType, cmte.combiType());
        assertEquals(childElements, cmte.childElements());
        if (commonNodeType != null) {
            assertEquals(commonNodeType, cmte.commonNodeType());
        }
        else if (cmte.commonNodeType() != null) {
            assertTrue(childElements.stream().allMatch(child -> cmte.commonNodeType().equals(child.commonNodeType())));
        }
        boolean expectNegation = childElements.stream().anyMatch(MatchTreeElement::containsAnyNegation);
        assertEquals(expectNegation, cmte.containsAnyNegation());
        assertFalse(cmte.isLeafElement());

    }

    /**
     * @param nodeType
     * @param expression
     * @return a list with mtes all of the same node type
     */
    private static List<MatchTreeElement> mteList(String nodeType, String... expression) {
        return Arrays.stream(expression).map(e -> mte(nodeType, e)).toList();
    }

    private static MatchTreeElement mte(String nodeType, String expression) {
        return mte(nodeType, expr(expression));
    }

    private static MatchTreeElement mte(String nodeType, CoreExpression expression) {
        switch (expression) {
        case MatchExpression match:
            return new SingleMatchWrapper(nodeType, match, DEFAULT, false);
        case NegationExpression neg:
            return new SingleMatchWrapper(nodeType, neg.delegate(), NEGATE, false);
        case CombinedExpression cmb:
            return new CombinedMatchTreeElement(cmb.combiType(), cmb.childExpressions().stream().map(e -> mte(nodeType, e)).toList());
        default:
            throw new IllegalArgumentException(String.format("nodeType=%s, expression=%s", nodeType, expression));
        }

    }

}
