//@formatter:off
/*
 * AdlSolrField
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

import de.calamanari.adl.solr.AdlSolrType;

/**
 * Interface to for Solr fields, introduced to handle {@link DataField}s and {@link FilterField}s in a common way.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public interface AdlSolrField extends Serializable {

    /**
     * Identifies the root or a child document structure this field belongs to
     * 
     * @return name of nodeType
     */
    String nodeType();

    /**
     * @return name of the Solr field
     */
    String fieldName();

    /**
     * @return type of the Solr field
     */
    AdlSolrType fieldType();

}
