//@formatter:off
/*
 * BetweenMatchWrapperTest
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

import static de.calamanari.adl.cnv.StandardConversions.parseCoreExpression;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.calamanari.adl.CombinedExpressionType;
import de.calamanari.adl.irl.CombinedExpression;
import de.calamanari.adl.irl.CoreExpression;
import de.calamanari.adl.irl.MatchExpression;
import de.calamanari.adl.irl.NegationExpression;
import de.calamanari.adl.solr.SolrTestBase;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class BetweenMatchWrapperTest extends SolrTestBase {

    @Test
    void testBasics() {
        assertCanConstruct(mw("a > 0"), mw("a < 2"));
        assertCanConstruct(mw("a >= 0"), mw("a < 2"));
        assertCanConstruct(mw("a >= 0"), mw("a <= 2"));
        assertCanConstruct(mw("a > 0"), mw("a <= 2"));

        assertCanConstruct(mw("a > 0", MatchInstruction.NEGATE), mw("a < 2", MatchInstruction.NEGATE));
        assertCanConstruct(mw("a >= 0", MatchInstruction.NEGATE), mw("a < 2", MatchInstruction.NEGATE));
        assertCanConstruct(mw("a >= 0", MatchInstruction.NEGATE), mw("a <= 2", MatchInstruction.NEGATE));
        assertCanConstruct(mw("a > 0", MatchInstruction.NEGATE), mw("a <= 2", MatchInstruction.NEGATE));

    }

    @Test
    void testErrors() {

        assertCannotConstruct(null, null);
        assertCannotConstruct(mw("a > 0"), null);
        assertCannotConstruct(null, mw("a < 0"));
        assertCannotConstruct(mw("a > 0"), mw("a < 0"));
        assertCannotConstruct(mw("a >= 0"), mw("a <= 0"));
        assertCannotConstruct(mw("a < 2"), mw("a > 0"));
        assertCannotConstruct(mw("a <= 2"), mw("a >= 0"));
        assertCannotConstruct(mw("a ANY OF (1,2,3)"), mw("a >= 0"));
        assertCannotConstruct(mw("a >= 0"), mw("a = 4"));
        assertCannotConstruct(mw("a > 0"), mw("b < 2"));

        assertCannotConstruct(mw("a > 0"), mw(NODE_TYPE_2, "a < 2"));
        assertCannotConstruct(mw("a > 0"), mw("a < 2", MatchInstruction.NEGATE));
        assertCannotConstruct(mw("a > 0", MatchInstruction.NEGATE), mw("a < 2"));

        assertCannotConstruct(mw("a > 0"), mw("a = 2"));

    }

    private static void assertCannotConstruct(MatchWrapper left, MatchWrapper right) {
        assertThrows(IllegalArgumentException.class, () -> new BetweenMatchWrapper(left, right));
    }

    private static void assertCanConstruct(MatchWrapper left, MatchWrapper right) {

        BetweenMatchWrapper bmw = new BetweenMatchWrapper(left, right);

        assertEquals(left, bmw.left());
        assertEquals(right, bmw.right());

        assertEquals(left.argName(), bmw.argName());
        assertEquals(right.argName(), bmw.argName());

        assertTrue(bmw.childElements().isEmpty());
        assertTrue(bmw.isLeafElement());
        assertFalse(bmw.isReferenceMatch());
        assertEquals(left.isGroupingEligible() && right.isGroupingEligible(), bmw.isGroupingEligible());
        assertEquals(left.isNegation() && right.isNegation(), bmw.containsAnyNegation());
        assertEquals(left.isNegation() && right.isNegation(), bmw.isNegation());
        assertSame(left.firstMember(), bmw.firstMember());
        assertSame(left.commonNodeType(), bmw.commonNodeType());

        assertEquals(left.firstMember().operand().value(), bmw.lowerBound());
        assertEquals(right.firstMember().operand().value(), bmw.upperBound());

        List<MatchExpression> expectedMembers = new ArrayList<>();
        expectedMembers.addAll(left.members());
        expectedMembers.addAll(right.members());
        assertEquals(expectedMembers, bmw.members());

        if (left.type() == MatchWrapperType.VALUE_MATCH && right.type() == MatchWrapperType.VALUE_OR_EQ_MATCH) {
            assertEquals(MatchWrapperType.VALUE_GT_AND_LTE_MATCH, bmw.type());
        }
        else if (left.type() == MatchWrapperType.VALUE_OR_EQ_MATCH && right.type() == MatchWrapperType.VALUE_OR_EQ_MATCH) {
            assertEquals(MatchWrapperType.VALUE_GTE_AND_LTE_MATCH, bmw.type());
        }
        else if (left.type() == MatchWrapperType.VALUE_OR_EQ_MATCH && right.type() == MatchWrapperType.VALUE_MATCH) {
            assertEquals(MatchWrapperType.VALUE_GTE_AND_LT_MATCH, bmw.type());
        }
        else {
            assertEquals(MatchWrapperType.VALUE_GT_AND_LT_MATCH, bmw.type());
        }

    }

    private static MatchWrapper mw(String expression) {
        return mw(null, expression, null);
    }

    private static MatchWrapper mw(String nodeType, String expression) {
        return mw(nodeType, expression, null);
    }

    private static MatchWrapper mw(String expression, MatchInstruction matchInstruction) {
        return mw(null, expression, matchInstruction);

    }

    private static MatchWrapper mw(String nodeType, String expression, MatchInstruction matchInstruction) {

        CoreExpression expr = parseCoreExpression(expression);

        if (matchInstruction == null) {
            matchInstruction = expr instanceof NegationExpression ? MatchInstruction.NEGATE : MatchInstruction.DEFAULT;
        }

        if (nodeType == null) {
            nodeType = NODE_TYPE_1;
        }

        switch (expr) {
        case MatchExpression match:
            return new SingleMatchWrapper(nodeType, match, matchInstruction, false);
        case NegationExpression neg:
            return new SingleMatchWrapper(nodeType, neg.delegate(), matchInstruction, true);
        case CombinedExpression cmbOr when cmbOr.combiType() == CombinedExpressionType.OR:
            return new MultiMatchWrapper(nodeType, cmbOr.childExpressions().stream().map(MatchExpression.class::cast).toList(), matchInstruction,
                    matchInstruction.isNegation());
        default:
            throw new IllegalArgumentException(expression);
        }

    }

}
