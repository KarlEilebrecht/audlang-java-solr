//@formatter:off
/*
 * SolrDocumentMetaInfo
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
import java.util.List;

/**
 * This interface defines methods that return additional meta data about a Solr document aside from its data fields.
 * <p>
 * Every document instance must carry a {@link #nodeType()} information (usually the solr-field <code><b>"node_type"</b></code>) which identifies its structure
 * and related meta data.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public interface NodeTypeMetaInfo extends Serializable {

    /**
     * @return name of the node type of this document, usually stored in the Solr-field <code><b>"node_type"</b></code>, NOT NULL
     */
    String nodeType();

    /**
     * @return the document characteristics of this node type.
     */
    SolrDocumentNature documentNature();

    /**
     * @return list of static filter conditions related to this document type
     */
    List<FilterField> documentFilters();

}
