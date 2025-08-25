//@formatter:off
/*
 * SolrConversionOverrides
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

import de.calamanari.adl.solr.SolrFormatConstants;
import de.calamanari.adl.solr.config.SolrDocumentNature;

/**
 * {@link SolrConversionOverrides} are names of global variables to override conventions in rare edge-cases (for example the name of the id-field in the
 * Solr-schema).
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public enum SolrConversionOverrides {

    /**
     * Changes the name of the ID-field in the Solr-documents, <code><b>{@value SolrFormatConstants#DEFAULT_UNIQUE_KEY_FIELD_NAME}</b></code> by default.
     */
    OVERRIDE_UNIQUE_KEY_FIELD_NAME,

    /**
     * Changes the name of the field in {@link SolrDocumentNature#DEPENDENT} Solr-documents which contains the referenced main-document id,
     * <code><b>{@value SolrFormatConstants#DEFAULT_DEPENDENT_MAIN_KEY_FIELD_NAME}</b></code> by default.
     */
    OVERRIDE_DEPENDENT_MAIN_KEY_FIELD_NAME,

    /**
     * Changes the name of the field in {@link SolrDocumentNature#DEPENDENT} Solr-documents which contains the node-type of a document,
     * <code><b>{@value SolrFormatConstants#DEFAULT_NODE_TYPE_FIELD_NAME}</b></code> by default.
     */
    OVERRIDE_NODE_TYPE_FIELD_NAME;

}
