//@formatter:off
/*
 * QueryType
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
 * The query type tells what to query from Solr (documents, ids or count).
 * <p>
 * In Solr this meta-information is completely independent from the query definition (other than for example in SQL), so this enum is more an indicator
 * throughout the process for what purpose a query was created, along with suggestions how to properly setup the final Solr-Query.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public enum DefaultQueryType {

    /**
     * Returns the matching root-level documents without special sorting
     * <p>
     * <b>Suggestions for the Solr-query:</b>
     * <ul>
     * <li>Set the fields to be returned to <b><code>"*"</code></b>, e.g., <code>solrQuery.setFields("*");</code></li>
     * <li>Don't forget to adjust the max number of returned documents (only 10 by default), e.g., <code>solrQuery.setRows(10_000);</code></li>
     * </ul>
     */
    SELECT_DOCUMENTS,

    /**
     * Returns the matching root-level document IDs in ascending order
     * <p>
     * <b>Suggestions for the Solr-query:</b>
     * <ul>
     * <li>Set the sort field name to the id-field and an ordering directive, e.g., <code>solrQuery.setSort("id", ORDER.desc);</code></li>
     * <li>Set the fields to be returned to <b><code>"*"</code></b>, e.g., <code>solrQuery.setFields("*");</code></li>
     * <li>Don't forget to adjust the max number of returned documents (only 10 by default), e.g., <code>solrQuery.setRows(10_000);</code></li>
     * </ul>
     */
    SELECT_DOCUMENTS_ORDERED,

    /**
     * Returns the matching root-level documents, each including its nested data
     * <p>
     * <b>Clarification:</b> This won't change the number of returned documents but potentially their structure.
     * <p>
     * <b>Suggestions for the Solr-query:</b>
     * <ul>
     * <li>Set the fields to be returned to <b><code>"*", "[child]"</code></b>, e.g., <code>solrQuery.setFields("*", "[child]");</code></li>
     * <li>Don't forget to adjust the max number of returned documents (only 10 by default), e.g., <code>solrQuery.setRows(10_000);</code></li>
     * <li>You may use the same approach to query <i>IDs only</i> from the root document and its nested documents. To do so, you must include the names of any
     * collection fields in the field list, e.g., <code>solrQuery.setFields("id", "fact_data", "[child]");</code><br>
     * To include <i>selected details</i> for the root and any nested documents, just add the fields to the list which acts as a global filter (white-list) for
     * a single result entry.<br>
     * </li>
     * </ul>
     */
    SELECT_DOCUMENTS_WITH_NESTING,

    /**
     * Returns the matching root-level documents, each including its nested data
     * <p>
     * <b>Clarification:</b> This won't change the number of returned documents but potentially their structure.
     * <p>
     * <b>Suggestions for the Solr-query:</b>
     * <ul>
     * <li>Set the sort field name to the id-field and an ordering directive, e.g., <code>solrQuery.setSort("id", ORDER.desc);</code></li>
     * <li>Set the fields to be returned to <b><code>"*", "[child]"</code></b>, e.g., <code>solrQuery.setFields("*", "[child]");</code></li>
     * <li>Don't forget to adjust the max number of returned documents (only 10 by default), e.g., <code>solrQuery.setRows(10_000);</code></li>
     * <li>You may use the same approach to query <i>IDs only</i> from the root document and its nested documents. To do so, you must add the name of any
     * <i>collection field to be included</i> in the field list, e.g., <code>solrQuery.setFields("id", "fact_data", "[child]");</code>.<br>
     * To include <i>selected details</i> for the root and any nested documents, just add the fields to the list which acts as a global filter (white-list) for
     * a single result entry.<br>
     * Be aware that the given sorting directive will be only applied to the root-level.</li>
     * </ul>
     */
    SELECT_DOCUMENTS_ORDERED_WITH_NESTING,

    /**
     * Returns the matching root-level document IDs (uniqueKey in Solr's managed schema) without special sorting
     * <p>
     * <b>Suggestions for the Solr-query:</b>
     * <ul>
     * <li>Set the fields to be returned to the id-field, e.g., <code>solrQuery.setFields("id");</code></li>
     * <li>Don't forget to adjust the max number of returned documents (only 10 by default), e.g., <code>solrQuery.setRows(10_000);</code></li>
     * </ul>
     */
    SELECT_IDS,

    /**
     * Returns the matching root-level document IDs (uniqueKey in Solr's managed schema) in ascending order
     * <p>
     * <b>Suggestions for the Solr-query:</b>
     * <ul>
     * <li>Set the sort field name to the id-field and an ordering directive, e.g., <code>solrQuery.setSort("id", ORDER.desc);</code></li>
     * <li>Set the fields to be returned to the id-field, e.g., <code>solrQuery.setFields("id");</code></li>
     * <li>Don't forget to adjust the max number of returned documents (only 10 by default), e.g., <code>solrQuery.setRows(10_000);</code></li>
     * </ul>
     */
    SELECT_IDS_ORDERED,

    /**
     * A query that returns the number of matching root-level documents
     * <p>
     * <b>Suggestions for the Solr-query:</b>
     * <ul>
     * <li>Don't forget to set the max number of returned documents to zero to prevent unnecessary data transfer to the client, e.g.,
     * <code>solrQuery.setRows(0);</code></li>
     * </ul>
     */
    SELECT_COUNT;
}
