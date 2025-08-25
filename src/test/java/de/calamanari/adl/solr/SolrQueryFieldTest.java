//@formatter:off
/*
 * SolrQueryFieldTest
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.calamanari.adl.DeepCopyUtils;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class SolrQueryFieldTest extends SolrTestBase {

    @Test
    void testBasics() {

        assertThrows(IllegalArgumentException.class, () -> new SolrQueryField(null, null));

        assertNotNull(new SolrQueryField(NODE_TYPE_1, FIELD_NAME_1));

        for (String badName : BAD_SOLR_NAME_EXAMPLES) {
            assertThrows(IllegalArgumentException.class, () -> new SolrQueryField(badName, FIELD_NAME_1));
            assertThrows(IllegalArgumentException.class, () -> new SolrQueryField(NODE_TYPE_1, badName));
        }

    }

    @Test
    void testSerialization() {

        SolrQueryField field = new SolrQueryField(NODE_TYPE_1, FIELD_NAME_1);

        SolrQueryField field2 = DeepCopyUtils.deepCopy(field);

        assertEquals(field, field2);

    }

}
