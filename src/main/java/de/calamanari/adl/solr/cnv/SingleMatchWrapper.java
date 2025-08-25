//@formatter:off
/*
 * SingleMatchWrapper
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

import java.util.Collections;
import java.util.List;

import de.calamanari.adl.irl.MatchExpression;
import de.calamanari.adl.irl.MatchOperator;

/**
 * Wraps a {@link MatchExpression} to later compare a Solr-field against a value or referenced field with a single operation.
 * <p>
 * <b>Remark:</b> Here, a <code>NOT {@link MatchOperator#IS_UNKNOWN}</code> becomes a <code>{@link MatchWrapperType#ANY_VALUE_MATCH}</code> while a
 * <code>{@link MatchOperator#IS_UNKNOWN}</code> becomes a <code>{@link MatchWrapperType#ANY_VALUE_MATCH}</code> with a negative {@link MatchInstruction}.
 * <p>
 * <b>Important:</b> In case of a reference match the calling component is responsible for verifying that both fields are related to the given node type!
 * 
 * @param nodeType the node type the resulting expression will be related to
 * @param matchExpression NOT NULL
 * @param matchInstruction NOT NULL
 * @param isGroupingEligible true if this element resp. its match condition can be combined with other conditions within the same join
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public record SingleMatchWrapper(String nodeType, MatchExpression matchExpression, MatchInstruction matchInstruction, boolean isGroupingEligible)
        implements MatchWrapper {

    /**
     * @param nodeType the node type the resulting expression will be related to, NOT NULL
     * @param matchExpression NOT NULL
     * @param matchInstruction NOT NULL
     * @param isGroupingEligible true if this element resp. its match condition can be combined with other conditions within the same join
     */
    public SingleMatchWrapper {
        if (nodeType == null || matchExpression == null || matchInstruction == null) {
            throw new IllegalArgumentException(
                    String.format("Arguments must not be null, given: nodeType=%s, matchExpression=%s, matchInstruction=%s, isGroupingEligible=%s", nodeType,
                            matchExpression, matchInstruction, isGroupingEligible));
        }
        if (matchExpression.operator() == MatchOperator.IS_UNKNOWN
                && !(matchInstruction == MatchInstruction.DEFAULT || matchInstruction == MatchInstruction.NEGATE)) {
            throw new IllegalArgumentException(String.format(
                    "Match instruction mismatch, IS_UNKNOWN can only be specified in conjunction with DEFAULT or NEGATE, given: nodeType=%s, matchExpression=%s, matchInstruction=%s, isGroupingEligible=%s",
                    nodeType, matchExpression, matchInstruction, isGroupingEligible));
        }
        if ((matchInstruction == MatchInstruction.NEGATE_VERIFY_RIGHT_ARG_HAS_ANY_VALUE
                || matchInstruction == MatchInstruction.NEGATE_VERIFY_BOTH_ARGS_HAVE_ANY_VALUE) && matchExpression.referencedArgName() == null) {
            throw new IllegalArgumentException(String.format(
                    "Match instruction mismatch, %s can only be specified in conjunction with a reference match, given: nodeType=%s, matchExpression=%s, matchInstruction=%s, isGroupingEligible=%s",
                    matchInstruction, nodeType, matchExpression, matchInstruction, isGroupingEligible));

        }
    }

    @Override
    public MatchWrapperType type() {
        if (operator() == MatchOperator.IS_UNKNOWN) {
            return MatchWrapperType.ANY_VALUE_MATCH;
        }
        else if (isReferenceMatch()) {
            return MatchWrapperType.REF_MATCH;
        }
        else {
            return MatchWrapperType.VALUE_MATCH;
        }
    }

    @Override
    public MatchExpression firstMember() {
        return matchExpression;
    }

    @Override
    public List<MatchExpression> members() {
        return Collections.singletonList(matchExpression);
    }

    @Override
    public String toString() {
        return "SingleMatchWrapper " + createDebugIndicator() + "[nodeType=" + nodeType + ", type=" + type() + ", matchExpression=" + matchExpression
                + ", matchInstruction=" + matchInstruction + "]";
    }

}
