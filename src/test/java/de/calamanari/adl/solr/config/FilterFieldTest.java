//@formatter:off
/*
 * FilterFieldTest
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

import org.junit.jupiter.api.Test;

import de.calamanari.adl.cnv.tps.ConfigException;
import de.calamanari.adl.solr.SolrTestBase;

import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_INTEGER;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_STRING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class FilterFieldTest extends SolrTestBase {

    @Test
    void testBasics() {

        assertThrows(ConfigException.class, () -> new FilterField(null, FIELD_NAME_3, SOLR_STRING, "value"));
        assertThrows(ConfigException.class, () -> new FilterField(null, FIELD_NAME_3, SOLR_STRING, "value"));
        assertThrows(ConfigException.class, () -> new FilterField(NODE_TYPE_1, null, SOLR_STRING, "value"));
        assertThrows(ConfigException.class, () -> new FilterField(NODE_TYPE_1, FIELD_NAME_3, null, "value"));
        assertThrows(ConfigException.class, () -> new FilterField(NODE_TYPE_1, FIELD_NAME_3, SOLR_STRING, null));
        assertThrows(ConfigException.class, () -> new FilterField(NODE_TYPE_1, FIELD_NAME_3, SOLR_STRING, ""));
        assertThrows(ConfigException.class, () -> new FilterField(NODE_TYPE_1, FIELD_NAME_3, SOLR_INTEGER, "not-a-number"));

        FilterField field = new FilterField(NODE_TYPE_1, FIELD_NAME_3, SOLR_STRING, "some value");

        assertEquals(FIELD_NAME_3 + ":some\\ value", field.nativeCondition());

        field = new FilterField(NODE_TYPE_1, FIELD_NAME_3, SOLR_STRING, "${someSystemVariable}");

        assertEquals(FIELD_NAME_3 + ":${someSystemVariable}", field.nativeCondition());

        assertEquals("?{ " + FIELD_NAME_3 + ":${someSystemVariable} }", field.toString());
    }

}
