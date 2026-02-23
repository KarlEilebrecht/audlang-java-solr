//@formatter:off
/*
 * MultiMatchWrapperTest
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.calamanari.adl.irl.MatchExpression;
import de.calamanari.adl.irl.MatchOperator;
import de.calamanari.adl.solr.SolrTestBase;

import static de.calamanari.adl.solr.cnv.ConversionTestUtils.exprList;
import static de.calamanari.adl.solr.cnv.MatchInstruction.DEFAULT;
import static de.calamanari.adl.solr.cnv.MatchInstruction.NEGATE;
import static de.calamanari.adl.solr.cnv.MatchInstruction.NEGATE_VERIFY_BOTH_ARGS_HAVE_ANY_VALUE;
import static de.calamanari.adl.solr.cnv.MatchInstruction.NEGATE_VERIFY_LEFT_ARG_HAS_ANY_VALUE;
import static de.calamanari.adl.solr.cnv.MatchInstruction.NEGATE_VERIFY_RIGHT_ARG_HAS_ANY_VALUE;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class MultiMatchWrapperTest extends SolrTestBase {

    @Test
    void testBasics() {

        assertCanConstruct(NODE_TYPE_1, exprList("a > 1", "a = 1"), DEFAULT);
        assertCanConstruct(NODE_TYPE_1, exprList("a > 1", "a = 1"), NEGATE);
        assertCanConstruct(NODE_TYPE_1, exprList("a > 1", "a = 1"), NEGATE_VERIFY_LEFT_ARG_HAS_ANY_VALUE);
        assertCannotConstruct(NODE_TYPE_1, exprList("a > 1", "a = 1"), NEGATE_VERIFY_RIGHT_ARG_HAS_ANY_VALUE);
        assertCannotConstruct(NODE_TYPE_1, exprList("a > 1", "a = 1"), NEGATE_VERIFY_BOTH_ARGS_HAVE_ANY_VALUE);

        assertCanConstruct(NODE_TYPE_1, exprList("a = 1", "a = 1"), DEFAULT);
        assertCannotConstruct(NODE_TYPE_1, exprList("a CONTAINS xyz", "a = 1"), DEFAULT);

        assertCanConstruct(NODE_TYPE_1, exprList("a < 1", "a = 1"), DEFAULT);
        assertCanConstruct(NODE_TYPE_1, exprList("a < 1", "a = 1"), NEGATE);
        assertCanConstruct(NODE_TYPE_1, exprList("a < 1", "a = 1"), NEGATE_VERIFY_LEFT_ARG_HAS_ANY_VALUE);

        assertCanConstruct(NODE_TYPE_1, exprList("a < @b", "a = @b"), DEFAULT);
        assertCanConstruct(NODE_TYPE_1, exprList("a < @b", "a = @b"), NEGATE);
        assertCanConstruct(NODE_TYPE_1, exprList("a < @b", "a = @b"), NEGATE_VERIFY_LEFT_ARG_HAS_ANY_VALUE);
        assertCanConstruct(NODE_TYPE_1, exprList("a < @b", "a = @b"), NEGATE_VERIFY_RIGHT_ARG_HAS_ANY_VALUE);
        assertCanConstruct(NODE_TYPE_1, exprList("a < @b", "a = @b"), NEGATE_VERIFY_BOTH_ARGS_HAVE_ANY_VALUE);

        assertCanConstruct(NODE_TYPE_1, exprList("a = 1", "a = 2"), DEFAULT);
        assertCanConstruct(NODE_TYPE_1, exprList("a = 1", "a = 2", "a = 3"), DEFAULT);
        assertCannotConstruct(NODE_TYPE_1, exprList("a = 1", "b = 2"), DEFAULT);
        assertCannotConstruct(NODE_TYPE_1, exprList("a > 1", "a > 2"), DEFAULT);
        assertCannotConstruct(NODE_TYPE_1, exprList("a > 1", "a = @b"), DEFAULT);
        assertCannotConstruct(NODE_TYPE_1, exprList("a > @b", "a = 1"), DEFAULT);

    }

    @Test
    void testSpecial() {

        List<MatchExpression> expressions = exprList("a > 1");

        assertCannotConstruct(null, null, null);
        assertCannotConstruct(NODE_TYPE_1, expressions, null);
        assertCannotConstruct(null, expressions, DEFAULT);
        assertCannotConstruct(NODE_TYPE_1, null, DEFAULT);
        assertCannotConstruct(NODE_TYPE_1, Collections.emptyList(), DEFAULT);

    }

    private static void assertCannotConstruct(String nodeType, List<MatchExpression> members, MatchInstruction matchInstruction) {
        assertThrows(IllegalArgumentException.class, () -> new MultiMatchWrapper(nodeType, members, matchInstruction, false));
        assertThrows(IllegalArgumentException.class, () -> new MultiMatchWrapper(nodeType, members, matchInstruction, true));
    }

    private static void assertCanConstruct(String nodeType, List<MatchExpression> members, MatchInstruction matchInstruction) {

        for (boolean isGroupingEligible : Arrays.asList(false, true)) {

            MultiMatchWrapper wrapper = new MultiMatchWrapper(nodeType, members, matchInstruction, isGroupingEligible);

            List<MatchExpression> expectedMembers = new ArrayList<>(members);
            Collections.sort(expectedMembers);

            assertEquals(nodeType, wrapper.nodeType());
            assertEquals(nodeType, wrapper.commonNodeType());
            assertEquals(deriveExpectedMatchWrapperType(members), wrapper.type());
            assertEquals(matchInstruction, wrapper.matchInstruction());
            assertEquals(expectedMembers.get(0), wrapper.firstMember());
            assertFalse(wrapper.members().isEmpty());
            assertEquals(expectedMembers.get(0), wrapper.members().get(0));
            assertEquals(expectedMembers.get(0).argName(), wrapper.argName());
            assertEquals(expectedMembers.get(0).referencedArgName(), wrapper.referencedArgName());
            assertEquals(expectedMembers.get(0).operator(), wrapper.operator());
            assertTrue(wrapper.childElements().isEmpty());
            assertTrue(wrapper.isLeafElement());
            assertEquals(matchInstruction.isNegation(), wrapper.isNegation());
            assertEquals(expectedMembers.get(0).referencedArgName() != null, wrapper.isReferenceMatch());
            assertEquals(matchInstruction.isNegation(), wrapper.containsAnyNegation());
            assertEquals(isGroupingEligible, wrapper.isGroupingEligible());
        }
    }

    private static MatchWrapperType deriveExpectedMatchWrapperType(List<MatchExpression> members) {

        MatchExpression firstMember = members.get(0);
        if (firstMember.referencedArgName() != null) {
            return MatchWrapperType.REF_OR_EQ_MATCH;
        }
        if (firstMember.operator() == MatchOperator.LESS_THAN || firstMember.operator() == MatchOperator.GREATER_THAN) {
            return MatchWrapperType.VALUE_OR_EQ_MATCH;
        }
        else {
            return MatchWrapperType.MULTI_VALUE_MATCH;
        }

    }

}
