//@formatter:off
/*
 * SolrConditionType
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

package de.calamanari.adl.solr;

/**
 * {@link SolrConditionType} identifies the type of operation(s) contained in a {@link SolrFilterQuery}.
 * <p>
 * This information indirectly tells about the expected complexity (cost) of the query when executed by Solr.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public enum SolrConditionType {

    /**
     * Check for <i>any</i> value, e.g., <code>color:*</code>
     */
    CMP_ANY,

    /**
     * Value comparison, e.g., <code>color:red</code>
     */
    CMP_VALUE,

    /**
     * Text contains comparison, e.g., <code>color:*red*</code>
     */
    CMP_TXT_CONTAINS,

    /**
     * Comparison against lower/upper bound(s), e.g., <code>number:[*:19]</code>
     */
    CMP_RANGE,

    /**
     * Solr function range query
     */
    FRANGE,

    /**
     * All main documents (with respect to node type filters)
     */
    ALL_DOCS,

    /**
     * All sub documents of a certain node type (with respect to node type filters)
     */
    ALL_SUB_DOCS;

}
