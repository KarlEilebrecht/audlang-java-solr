//@formatter:off
/*
 * SolrDocumentNature
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

package de.calamanari.adl.solr.config;

import java.io.Serializable;

/**
 * Different {@link SolrDocumentNature}s define the characteristics of supported Solr documents (node types).
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public enum SolrDocumentNature implements Serializable {

    /**
     * The set of document instances of this node type builds the base for all queries and especially the source of all ID-queries.
     * <p>
     * Per individual (id) there must exist a single main document with the basic attributes.
     * <p>
     * In other words: selecting the ids of <i>main documents</i> will return the <i>entirety</i> of all individuals.
     */
    MAIN,

    /**
     * This node type describes a type of Solr-documents nested within the main document.
     * <p>
     * The features of nested documents can be included in queries to be executed as Solr Block-Join-queries. While being stored as separate documents nested
     * documents don't exist independently from the main document. Re-indexing always affects the main document including all nested documents.
     * <p>
     * The main difference to {@link #DEPENDENT} is runtime-query-performance because Solr builds supporting structures for the block-join during indexing,
     * which leads to faster query execution. However, the downside is the additional indexing overhead.
     */
    NESTED,

    /**
     * This node type describes an additional document type residing <i>besides</i> the main document.
     * <p>
     * Dependent documents usually have surrogate ids and reference the main document through the <code><b>"main_id"</b></code> for joining. These documents
     * exist completely independent from the main document.
     * <p>
     * The features of these documents can be included in queries to be executed as Join-queries.
     */
    DEPENDENT;

}
