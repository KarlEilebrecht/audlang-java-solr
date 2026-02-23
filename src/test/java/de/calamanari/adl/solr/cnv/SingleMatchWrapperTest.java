//@formatter:off
/*
 * SingleMatchWrapperTest
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

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.calamanari.adl.irl.MatchExpression;
import de.calamanari.adl.irl.MatchOperator;
import de.calamanari.adl.solr.SolrTestBase;

import static de.calamanari.adl.cnv.StandardConversions.parseCoreExpression;
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
class SingleMatchWrapperTest extends SolrTestBase {

    static final Logger LOGGER = LoggerFactory.getLogger(SingleMatchWrapperTest.class);

    @Test
    void testBasics() {

        MatchExpression match = (MatchExpression) parseCoreExpression("a=1");

        assertCanConstruct(NODE_TYPE_1, match, DEFAULT);
        assertCanConstruct(NODE_TYPE_1, match, NEGATE);
        assertCanConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_LEFT_ARG_HAS_ANY_VALUE);
        assertCannotConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_RIGHT_ARG_HAS_ANY_VALUE);
        assertCannotConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_BOTH_ARGS_HAVE_ANY_VALUE);

        match = (MatchExpression) parseCoreExpression("a < 1");

        assertCanConstruct(NODE_TYPE_1, match, DEFAULT);
        assertCanConstruct(NODE_TYPE_1, match, NEGATE);
        assertCanConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_LEFT_ARG_HAS_ANY_VALUE);
        assertCannotConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_RIGHT_ARG_HAS_ANY_VALUE);
        assertCannotConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_BOTH_ARGS_HAVE_ANY_VALUE);

        match = (MatchExpression) parseCoreExpression("a > 1");

        assertCanConstruct(NODE_TYPE_1, match, DEFAULT);
        assertCanConstruct(NODE_TYPE_1, match, NEGATE);
        assertCanConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_LEFT_ARG_HAS_ANY_VALUE);
        assertCannotConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_RIGHT_ARG_HAS_ANY_VALUE);
        assertCannotConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_BOTH_ARGS_HAVE_ANY_VALUE);

        match = (MatchExpression) parseCoreExpression("a CONTAINS xyz");

        assertCanConstruct(NODE_TYPE_1, match, DEFAULT);
        assertCanConstruct(NODE_TYPE_1, match, NEGATE);
        assertCanConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_LEFT_ARG_HAS_ANY_VALUE);
        assertCannotConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_RIGHT_ARG_HAS_ANY_VALUE);
        assertCannotConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_BOTH_ARGS_HAVE_ANY_VALUE);

    }

    @Test
    void testBasics2() {
        MatchExpression match = (MatchExpression) parseCoreExpression("a=@b");

        assertCanConstruct(NODE_TYPE_1, match, DEFAULT);
        assertCanConstruct(NODE_TYPE_1, match, NEGATE);
        assertCanConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_LEFT_ARG_HAS_ANY_VALUE);
        assertCanConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_RIGHT_ARG_HAS_ANY_VALUE);
        assertCanConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_BOTH_ARGS_HAVE_ANY_VALUE);

        match = (MatchExpression) parseCoreExpression("a IS UNKNOWN");

        assertCanConstruct(NODE_TYPE_1, match, DEFAULT);
        assertCanConstruct(NODE_TYPE_1, match, NEGATE);
        assertCannotConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_LEFT_ARG_HAS_ANY_VALUE);
        assertCannotConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_RIGHT_ARG_HAS_ANY_VALUE);
        assertCannotConstruct(NODE_TYPE_1, match, NEGATE_VERIFY_BOTH_ARGS_HAVE_ANY_VALUE);

    }

    @Test
    void testSpecial() {
        MatchExpression match = (MatchExpression) parseCoreExpression("a=@b");

        assertCannotConstruct(null, null, null);
        assertCannotConstruct(NODE_TYPE_1, match, null);
        assertCannotConstruct(null, match, DEFAULT);
        assertCannotConstruct(NODE_TYPE_1, null, DEFAULT);

    }

    private static void assertCannotConstruct(String nodeType, MatchExpression match, MatchInstruction matchInstruction) {
        assertThrows(IllegalArgumentException.class, () -> new SingleMatchWrapper(nodeType, match, matchInstruction, false));
        assertThrows(IllegalArgumentException.class, () -> new SingleMatchWrapper(nodeType, match, matchInstruction, true));
    }

    private static void assertCanConstruct(String nodeType, MatchExpression match, MatchInstruction matchInstruction) {

        for (boolean isGroupingEligible : Arrays.asList(false, true)) {
            SingleMatchWrapper wrapper = new SingleMatchWrapper(nodeType, match, matchInstruction, isGroupingEligible);
            assertEquals(nodeType, wrapper.nodeType());
            assertEquals(nodeType, wrapper.commonNodeType());
            assertEquals(deriveExpectedMatchWrapperType(match), wrapper.type());
            assertEquals(matchInstruction, wrapper.matchInstruction());
            assertEquals(match, wrapper.firstMember());
            assertFalse(wrapper.members().isEmpty());
            assertEquals(match, wrapper.members().get(0));
            assertEquals(match.argName(), wrapper.argName());
            assertEquals(match.referencedArgName(), wrapper.referencedArgName());
            assertEquals(match.operator(), wrapper.operator());
            assertTrue(wrapper.childElements().isEmpty());
            assertTrue(wrapper.isLeafElement());
            assertEquals(matchInstruction.isNegation(), wrapper.isNegation());
            assertEquals(match.referencedArgName() != null, wrapper.isReferenceMatch());
            assertEquals(matchInstruction.isNegation(), wrapper.containsAnyNegation());
            assertEquals(isGroupingEligible, wrapper.isGroupingEligible());
        }
    }

    private static MatchWrapperType deriveExpectedMatchWrapperType(MatchExpression match) {
        MatchOperator operator = match.operator();

        if (match.referencedArgName() == null) {
            if (operator == MatchOperator.IS_UNKNOWN) {
                return MatchWrapperType.ANY_VALUE_MATCH;
            }
            else {
                return MatchWrapperType.VALUE_MATCH;
            }
        }
        else {
            return MatchWrapperType.REF_MATCH;
        }
    }

}
