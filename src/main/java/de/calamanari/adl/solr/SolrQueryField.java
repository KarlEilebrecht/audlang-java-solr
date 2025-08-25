//@formatter:off
/*
 * SolrQueryField
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

import java.io.Serializable;

import de.calamanari.adl.solr.config.ConfigUtils;

/**
 * A {@link SolrQueryField} is a Solr-field qualified by the nodeType of the document that contains it.
 * 
 * @param nodeType node type of the document that contains the field
 * @param fieldName name of the Solr-field
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public record SolrQueryField(String nodeType, String fieldName) implements Serializable {

    /**
     * @param nodeType node type of the document that contains the field, must comply to {@link ConfigUtils#isValidSolrName(String)}
     * @param fieldName name of the Solr-field, must comply to {@link ConfigUtils#isValidSolrName(String)}
     */
    public SolrQueryField {
        if (!ConfigUtils.isValidSolrName(nodeType) || !ConfigUtils.isValidSolrName(fieldName)) {
            throw new IllegalArgumentException(String.format("%s%ngiven: nodeType=%s, fieldName=%s", SolrFormatConstants.SOLR_NAMING_DEBUG_INFO, nodeType, fieldName));
        }
    }
}
