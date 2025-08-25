//@formatter:off
/*
 * MatchWrapperType
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

import de.calamanari.adl.irl.MatchExpression;

/**
 * Type to distinguish wrappers without detail expression checking.
 * <p>
 * Additionally, the match wrapper type allows to introduce aggregated match operations (e.g., {@link #VALUE_OR_EQ_MATCH}) related to Solr.
 * <p>
 * <b>Important:</b> {@link MatchWrapperType}s are all <i>positive</i>. Negation is not subject to the type of a wrapper but an orthogonal property.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public enum MatchWrapperType {

    /**
     * Match <i>any</i> value for the related field (semantically NOT IS UNKNOWN).
     */
    ANY_VALUE_MATCH,

    /**
     * Match of a field against a single value
     */
    VALUE_MATCH,

    /**
     * LESS/GEATER THAN <i>OR EQUALS</i> match against a value
     */
    VALUE_OR_EQ_MATCH,

    /**
     * Between two values, excluding the values
     */
    VALUE_GT_AND_LT_MATCH,

    /**
     * Between two values or equal to the left value
     */
    VALUE_GTE_AND_LT_MATCH,

    /**
     * Between two values or equal to the left value
     */
    VALUE_GT_AND_LTE_MATCH,

    /**
     * Between two values or equal to the left value
     */
    VALUE_GTE_AND_LTE_MATCH,

    /**
     * Match of a field against multiple values (multiple {@link MatchExpression}s attached)
     */
    MULTI_VALUE_MATCH,

    /**
     * A match of a field against another field of the same document
     */
    REF_MATCH,

    /**
     * LESS/GEATER THAN <i>OR EQUALS</i> match against another field of the same document
     */
    REF_OR_EQ_MATCH;

}
