//@formatter:off
/*
 * MatchWrapper
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

import java.util.List;

import de.calamanari.adl.irl.MatchExpression;
import de.calamanari.adl.irl.MatchOperator;
import de.calamanari.adl.irl.Operand;

/**
 * A {@link MatchWrapper} wraps {@link MatchExpression}(s) <i>related to the same argName</i> as a preparation step for creating Solr-expressions.
 * <p>
 * If a match wrapper wraps a reference match, then this must be its only member, see {@link #referencedArgName()}.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public interface MatchWrapper extends MatchTreeElement {

    /**
     * Returns the node type the underlying expression is related to
     * 
     * @return node type
     */
    String nodeType();

    /**
     * @return type of this wrapper
     */
    MatchWrapperType type();

    /**
     * @return member(s) NOT EMPTY, NOT NULL
     */
    List<MatchExpression> members();

    /**
     * @return match instruction
     */
    MatchInstruction matchInstruction();

    /**
     * @return true if the set of results after applying this filter should be <i>subtracted</i> from the total result.
     */
    default boolean isNegation() {
        return matchInstruction().isNegation();
    }

    /**
     * Shorthand for <code>members().get(0)</code>
     * 
     * @return first member, never null
     */
    default MatchExpression firstMember() {
        return members().get(0);
    }

    /**
     * @return operator of the first member
     */
    default MatchOperator operator() {
        return firstMember().operator();
    }

    /**
     * @return the (left) argument name, never null
     */
    default String argName() {
        return firstMember().argName();
    }

    /**
     * @return true if this wraps a reference match between two arguments
     */
    default boolean isReferenceMatch() {
        Operand operand = firstMember().operand();
        return operand != null && operand.isReference();
    }

    /**
     * @return if {@link #isReferenceMatch()} then this method returns the value of the operand of the only member
     */
    default String referencedArgName() {
        if (isReferenceMatch()) {
            return firstMember().operand().value();
        }
        return null;
    }

    @Override
    default String commonNodeType() {
        return nodeType();
    }

    @Override
    default boolean containsAnyNegation() {
        return isNegation();
    }

    @Override
    default boolean isMissingDocumentIncluded() {
        return isNegation() && matchInstruction() == MatchInstruction.NEGATE;
    }

}
