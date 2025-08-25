//@formatter:off
/*
 * BooleanReferenceMatchCase
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
 * This enumeration covers the cases when translating a boolean reference match into a combination of value comparisons on both sides.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public enum BooleanReferenceMatchCase {

    /**
     * Positive match, both fields can only carry a single value
     */
    LEFT_SINGLE_RIGHT_SINGLE,

    /**
     * Negative match, both fields can only carry a single value
     */
    LEFT_SINGLE_RIGHT_SINGLE_NEGATION,

    /**
     * Positive match, and right can carry multiple values.
     */
    LEFT_SINGLE_RIGHT_MULTI,

    /**
     * Negative match, and right can carry multiple values.
     */
    LEFT_SINGLE_RIGHT_MULTI_NEGATION,

    /**
     * Positive match, and left can carry multiple values.
     */
    LEFT_MULTI_RIGHT_SINGLE,

    /**
     * Negative match, and left can carry multiple values.
     */
    LEFT_MULTI_RIGHT_SINGLE_NEGATION,

    /**
     * Positive match, both fields can carry multiple values.
     */
    LEFT_MULTI_RIGHT_MULTI,

    /**
     * Negative match, both fields can carry multiple values.
     */
    LEFT_MULTI_RIGHT_MULTI_NEGATION;

    /**
     * Determines the case based on the flags
     * 
     * @param leftMulti left potentially has multiple values
     * @param rightMulti right potentially has multiple values
     * @param isNegation this is a negation
     * @return derived case
     */
    public static BooleanReferenceMatchCase from(boolean leftMulti, boolean rightMulti, boolean isNegation) {

        if (leftMulti && rightMulti) {
            return isNegation ? LEFT_MULTI_RIGHT_MULTI_NEGATION : LEFT_MULTI_RIGHT_MULTI;
        }
        else if (leftMulti) {
            return isNegation ? LEFT_MULTI_RIGHT_SINGLE_NEGATION : LEFT_MULTI_RIGHT_SINGLE;
        }
        else if (rightMulti) {
            return isNegation ? LEFT_SINGLE_RIGHT_MULTI_NEGATION : LEFT_SINGLE_RIGHT_MULTI;
        }
        else {
            return isNegation ? LEFT_SINGLE_RIGHT_SINGLE_NEGATION : LEFT_SINGLE_RIGHT_SINGLE;
        }

    }

}