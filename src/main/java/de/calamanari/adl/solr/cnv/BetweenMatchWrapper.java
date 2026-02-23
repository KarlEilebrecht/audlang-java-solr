//@formatter:off
/*
 * BetweenMatchWrapper
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
import java.util.List;

import de.calamanari.adl.irl.MatchExpression;
import de.calamanari.adl.irl.MatchOperator;

import static de.calamanari.adl.solr.cnv.MatchWrapperType.VALUE_GTE_AND_LTE_MATCH;
import static de.calamanari.adl.solr.cnv.MatchWrapperType.VALUE_GTE_AND_LT_MATCH;
import static de.calamanari.adl.solr.cnv.MatchWrapperType.VALUE_GT_AND_LTE_MATCH;
import static de.calamanari.adl.solr.cnv.MatchWrapperType.VALUE_GT_AND_LT_MATCH;
import static de.calamanari.adl.solr.cnv.MatchWrapperType.VALUE_MATCH;
import static de.calamanari.adl.solr.cnv.MatchWrapperType.VALUE_OR_EQ_MATCH;

/**
 * A {@link BetweenMatchWrapper} is an aggregation of two match wrappers representing a lower bound and an upper bound to match a field against.
 * <p>
 * <b>Important:</b> A <i>between</i> operation is a non-trivial combination of conditions that can accidentally <b>change the semantics of a query</b> if the
 * underlying field is a collection or <i>multi-doc</i> assignment. For example: given the collection-field <b><code>temperature</code></b>, the two
 * Solr-queries <b><code>temperature:[20 TO *] AND temperature:[* TO 30]</code></b> vs. <b><code>temperature:[20 TO 30]</code></b> may return different
 * results.<br>
 * In terms of Audlang, the <i>first</i> query's result is the expected outcome. See also <a href=
 * "https://github.com/KarlEilebrecht/audlang-spec/blob/main/doc/AudienceDefinitionLanguageSpecification.md#74-between-on-collection-attributes"><i>Audlang Spec
 * §7.4 Between on Collection Attributes</i></a>.
 * 
 * @param left greater than [or equals] value match
 * @param right less than [or equals] value match
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public record BetweenMatchWrapper(MatchWrapper left, MatchWrapper right) implements MatchWrapper {

    /**
     * @param left greater than [or equals] value match
     * @param right less than [or equals] value match
     */
    public BetweenMatchWrapper {
        if (left == null || right == null) {
            throw new IllegalArgumentException(String.format("Members of BETWEEN must not be null, given: left=%s, right=%s", left, right));
        }

        if (!left.argName().equals(right.argName()) || !left.nodeType().equals(right.nodeType())) {
            throw new IllegalArgumentException(String.format("Members of BETWEEN relate to the same argument, given: left=%s, right=%s", left, right));
        }

        if (left.matchInstruction() != right.matchInstruction()) {
            throw new IllegalArgumentException(String.format("Members of BETWEEN must have the same match instruction, given: left=%s, right=%s", left, right));
        }

        if (left.isReferenceMatch() || right.isReferenceMatch()) {
            throw new IllegalArgumentException(String.format("Members of BETWEEN must be value matches, given: left=%s, right=%s", left, right));
        }

        if ((left.type() != VALUE_MATCH && left.type() != VALUE_OR_EQ_MATCH) || (right.type() != VALUE_MATCH && right.type() != VALUE_OR_EQ_MATCH)
                || left.firstMember().operator() != MatchOperator.GREATER_THAN || right.firstMember().operator() != MatchOperator.LESS_THAN) {
            throw new IllegalArgumentException(
                    String.format("Members of BETWEEN must form a conditions like 'lower <[=] value <[=] upper', given: left=%s, right=%s", left, right));
        }

        if (left.firstMember().operand().value().equals(right.firstMember().operand().value())) {
            throw new IllegalArgumentException(String.format("Members of BETWEEN must have different values (bounds), given: left=%s, right=%s", left, right));
        }
    }

    @Override
    public boolean isGroupingEligible() {
        return left.isGroupingEligible() && right.isGroupingEligible();
    }

    @Override
    public String nodeType() {
        return left.nodeType();
    }

    @Override
    public MatchWrapperType type() {
        MatchWrapperType typeLeft = left.type();
        MatchWrapperType typeRight = right.type();
        if (typeLeft == VALUE_MATCH && typeRight == VALUE_MATCH) {
            // greater than AND less than
            return VALUE_GT_AND_LT_MATCH;
        }
        else if (typeLeft == VALUE_OR_EQ_MATCH && typeRight == VALUE_MATCH) {
            // greater than or equals AND less than
            return VALUE_GTE_AND_LT_MATCH;
        }
        else if (typeLeft == VALUE_MATCH && typeRight == VALUE_OR_EQ_MATCH) {
            // greater than AND less than or equals
            return VALUE_GT_AND_LTE_MATCH;
        }
        else {
            // greater than or equals AND less than or equals
            return VALUE_GTE_AND_LTE_MATCH;
        }
    }

    @Override
    public List<MatchExpression> members() {
        List<MatchExpression> res = new ArrayList<>();
        res.addAll(left.members());
        res.addAll(right.members());
        return res;
    }

    @Override
    public MatchExpression firstMember() {
        return left.firstMember();
    }

    @Override
    public MatchInstruction matchInstruction() {
        return left.matchInstruction();
    }

    /**
     * @return value of the left expression, which is the lower bound value
     */
    public String lowerBound() {
        return left.firstMember().operand().value();
    }

    /**
     * @return value of the left expression, which is the lower bound value
     */
    public String upperBound() {
        return right.firstMember().operand().value();
    }

    @Override
    public String toString() {
        return "BetweenMatchWrapper " + createDebugIndicator() + "[nodeType=" + nodeType() + ", type=" + type() + ", argName=" + left.argName() + ", members="
                + members() + ", matchInstruction=" + matchInstruction() + "]";
    }

}
