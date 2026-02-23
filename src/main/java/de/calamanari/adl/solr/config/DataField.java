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

import de.calamanari.adl.cnv.tps.ConfigException;
import de.calamanari.adl.solr.AdlSolrType;
import de.calamanari.adl.solr.SolrFormatConstants;

import static de.calamanari.adl.solr.config.ConfigUtils.isValidSolrName;

/**
 * A {@link DataField} describes a single field in a solr document. Solr defines field characteristics (name/type, collection or not) <i>globally</i>, however,
 * for mapping and lookup we define it narrowed to its nodeType. However, you still cannot have field 'fooBar' in one document and another field with the same
 * name but different type in a nested document.
 * <p>
 * The properties {@link #nodeType} and {@link #fieldName} will be validated against some rules, see {@link ConfigUtils#isValidSolrName(String)}
 * 
 * @param nodeType identifies the root or a child document this field belongs to
 * @param fieldName solr field name
 * @param fieldType solr field type
 * @param isCollection tells whether this field may hold multiple values (Solr-collection)
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public record DataField(String nodeType, String fieldName, AdlSolrType fieldType, boolean isCollection) implements AdlSolrField {

    /**
     * @param nodeType identifies the root or a child document this field belongs to
     * @param fieldName solr field name
     * @param fieldType solr field type
     * @param isCollection tells whether this field may hold multiple values (Solr-collection)
     */
    public DataField {
        if (!isValidSolrName(nodeType) || !isValidSolrName(fieldName) || fieldType == null) {
            throw new ConfigException(String.format(
                    "Arguments nodeType and fieldName must be valid Solr identifiers, fieldType is mandatory, given: nodeType=%s, fieldName=%s, fieldType=%s%n%s",
                    nodeType, fieldName, fieldType, SolrFormatConstants.SOLR_NAMING_DEBUG_INFO));
        }
    }

}
