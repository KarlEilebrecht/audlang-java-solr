//@formatter:off
/*
 * DataFieldTest
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

import de.calamanari.adl.DeepCopyUtils;
import de.calamanari.adl.cnv.tps.ConfigException;
import de.calamanari.adl.solr.SolrTestBase;

import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_STRING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class DataFieldTest extends SolrTestBase {

    @Test
    void testBasics() {

        assertThrows(ConfigException.class, () -> new DataField(null, null, null, false));
        assertThrows(ConfigException.class, () -> new DataField(null, null, null, true));

        assertNotNull(new DataField(NODE_TYPE_1, FIELD_NAME_1, SOLR_STRING, false));
        assertNotNull(new DataField(NODE_TYPE_1, FIELD_NAME_1, SOLR_STRING, true));

        assertThrows(ConfigException.class, () -> new DataField(NODE_TYPE_1, FIELD_NAME_1, null, false));

        for (String badName : BAD_SOLR_NAME_EXAMPLES) {
            assertThrows(ConfigException.class, () -> new DataField(badName, FIELD_NAME_1, SOLR_STRING, false));
            assertThrows(ConfigException.class, () -> new DataField(NODE_TYPE_1, badName, SOLR_STRING, false));
        }

    }

    @Test
    void testSerialization() {

        DataField field = new DataField(NODE_TYPE_1, FIELD_NAME_1, SOLR_STRING, true);

        DataField field2 = DeepCopyUtils.deepCopy(field);

        assertEquals(field, field2);

    }

}
