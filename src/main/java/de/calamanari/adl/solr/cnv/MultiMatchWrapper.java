//@formatter:off
/*
 * MultiMatchWrapper
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import de.calamanari.adl.irl.MatchExpression;
import de.calamanari.adl.irl.MatchOperator;

/**
 * A {@link MultiMatchWrapper} encapsulates multiple {@link MatchExpression}s relating the the same argName(s) which should be translated into a single
 * Solr-operation, for example: <b><code>&lt;=, &gt;=</code></b> or the equivalent of an <b><code>ANY OF (<i>value1</i>, <i>value2</i>, ...)</code></b>.
 * <p>
 * Instances are deeply immutable.
 * 
 * @param nodeType the node type the later Solr-expression will be related to
 * @param members ({@link MatchOperator#LESS_THAN} or {@link MatchOperator#GREATER_THAN}) followed by a {@link MatchOperator#EQUALS} or at least two
 *            value-match-expressions {@link MatchOperator#EQUALS}
 * @param matchInstruction NOT NULL
 * @param isGroupingEligible true if this element resp. its match condition can be combined with other conditions within the same join
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public record MultiMatchWrapper(String nodeType, List<MatchExpression> members, MatchInstruction matchInstruction, boolean isGroupingEligible)
        implements MatchWrapper {

    /**
     * @param nodeType the node type the later Solr-expression will be related to
     * @param members ({@link MatchOperator#LESS_THAN} or {@link MatchOperator#GREATER_THAN}) followed by a {@link MatchOperator#EQUALS} or at least two
     *            value-match-expressions {@link MatchOperator#EQUALS}
     * @param matchInstruction NOT NULL
     * @param isGroupingEligible true if this element resp. its match condition can be combined with other conditions within the same join
     */
    public MultiMatchWrapper(String nodeType, List<MatchExpression> members, MatchInstruction matchInstruction, boolean isGroupingEligible) {
        if (nodeType == null || members == null || matchInstruction == null) {
            throw new IllegalArgumentException(
                    String.format("The arguments must not be null, given: nodeType=%s, members=%s, matchInstruction=%s, isGroupingEligible=%s", nodeType,
                            members, matchInstruction, isGroupingEligible));
        }
        if (members.size() < 2) {
            throw new IllegalArgumentException(String.format("At least two members expected (either ['>', '='], or all ['=', '=', ...]), given: "
                    + "nodeType=%s, members=%s, matchInstruction=%s, isGroupingEligible=%s", nodeType, members, matchInstruction, isGroupingEligible));
        }
        List<MatchExpression> temp = new ArrayList<>(members);

        if (!isLtGtEqCase(members)) {
            String argName = members.get(0).argName();
            if (!members.stream()
                    .allMatch(member -> member.argName().equals(argName) && member.operator() == MatchOperator.EQUALS && !member.operand().isReference())) {

                throw new IllegalArgumentException(String.format(
                        "Unable to construct multi-value-match from the given list of expressions (expected: all EQUALS-VALUE-matches with the same argName), "
                                + "given: nodeType=%s, members=%s, matchInstruction=%s, isGroupingEligible=%s",
                        nodeType, members, matchInstruction, isGroupingEligible));
            }
        }

        if ((matchInstruction == MatchInstruction.NEGATE_VERIFY_RIGHT_ARG_HAS_ANY_VALUE
                || matchInstruction == MatchInstruction.NEGATE_VERIFY_BOTH_ARGS_HAVE_ANY_VALUE) && members.get(0).referencedArgName() == null) {
            throw new IllegalArgumentException(String.format(
                    "Match instruction mismatch, %s can only be specified in conjunction with a reference match, "
                            + "given: nodeType=%s, members=%s, matchInstruction=%s, isGroupingEligible=%s",
                    matchInstruction, nodeType, members, matchInstruction, isGroupingEligible));

        }

        this.nodeType = nodeType;
        this.members = Collections.unmodifiableList(temp);
        this.matchInstruction = matchInstruction;
        this.isGroupingEligible = isGroupingEligible;
    }

    /**
     * @param members
     * @return true if this is a less/greater than or equals
     */
    private static boolean isLtGtEqCase(List<MatchExpression> members) {
        if (members.size() == 2) {
            MatchExpression left = members.get(0);
            MatchExpression right = members.get(1);
            return (left.argName().equals(right.argName()) && (left.operator() == MatchOperator.LESS_THAN || left.operator() == MatchOperator.GREATER_THAN)
                    && right.operator() == MatchOperator.EQUALS && Objects.equals(left.operand(), right.operand()));
        }
        return false;
    }

    @Override
    public MatchWrapperType type() {
        if (members.get(0).operator() == MatchOperator.LESS_THAN || members.get(0).operator() == MatchOperator.GREATER_THAN) {
            return isReferenceMatch() ? MatchWrapperType.REF_OR_EQ_MATCH : MatchWrapperType.VALUE_OR_EQ_MATCH;
        }
        else {
            return MatchWrapperType.MULTI_VALUE_MATCH;
        }
    }

    @Override
    public String toString() {
        return "MultiMatchWrapper " + createDebugIndicator() + "[nodeType=" + nodeType + ", members=" + members + ", matchInstruction=" + matchInstruction
                + "]";
    }

}
