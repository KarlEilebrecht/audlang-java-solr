//@formatter:off
/*
 * MatchInstruction
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

/**
 * This enumeration identifies how a match must be executed (simply matched or as a negation subtracted from <i>all</i>.
 * <p>
 * The {@link MatchInstruction} is an additional instruction how to convert an expression into a Solr-expression.<br>
 * It allows re-creating the equivalent of an Audlang STRICT vs. non-STRICT NOT (see
 * <a href="https://github.com/KarlEilebrecht/audlang-spec/blob/main/doc/AudienceDefinitionLanguageSpecification.md#5-negation">§5 Audlang Spec</a>).<br>
 * This addresses the problem that Solr only can <i>subtract</i> sets (which is non-STRICT by definition). Whenever a STRICT negation is required we must
 * exclude the matches which don't have <i>any</i> value for the related Solr-field.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public enum MatchInstruction {

    /**
     * Positive comparison, a condition restricts the number of matching Solr-documents
     */
    DEFAULT,

    /**
     * Non-strict negation, means: We can just subtract the result of the positive expression from the total result which implicitly includes the cases where
     * any of the arguments is unknown (has no value at all, means Solr <i>dynamic fields</i> or missing sub-documents).
     */
    NEGATE,

    /**
     * Strict negation of the left argument, right non-strict: we must additionally verify that the Solr-field mapped to the left argument has <i>any</i> value.
     */
    NEGATE_VERIFY_LEFT_ARG_HAS_ANY_VALUE,

    /**
     * Strict negation of the right argument, left non-strict: we must additionally verify that the Solr-field mapped to the right argument has <i>any</i>
     * value.
     */
    NEGATE_VERIFY_RIGHT_ARG_HAS_ANY_VALUE,

    /**
     * Strict negation, means: we must additionally verify that the Solr-fields mapped to arguments of the negated match have <i>any</i> value.
     */
    NEGATE_VERIFY_BOTH_ARGS_HAVE_ANY_VALUE;

    /**
     * @return true if this is a negation
     */
    public boolean isNegation() {
        return this != DEFAULT;
    }

    /**
     * @return true if this instruction requires any "co-match" to verify any of the arguments' existence
     */
    public boolean requiresIsNotUnknownVerification() {
        return this == NEGATE_VERIFY_BOTH_ARGS_HAVE_ANY_VALUE || this == NEGATE_VERIFY_LEFT_ARG_HAS_ANY_VALUE || this == NEGATE_VERIFY_RIGHT_ARG_HAS_ANY_VALUE;
    }

}
