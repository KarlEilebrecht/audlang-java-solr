//@formatter:off
/*
 * ArgFieldAssignmentTest
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

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.calamanari.adl.cnv.tps.ArgMetaInfo;
import de.calamanari.adl.cnv.tps.ConfigException;
import de.calamanari.adl.cnv.tps.DefaultAdlType;
import de.calamanari.adl.solr.DefaultAdlSolrType;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class ArgFieldAssignmentTest {

    @Test
    void testBasics() {

        DataField field1 = new DataField("doc1", "field1", DefaultAdlSolrType.SOLR_STRING, false);

        ArgMetaInfo metaInfo1 = new ArgMetaInfo("arg", DefaultAdlType.STRING, false, false);

        ArgFieldAssignment afa = new ArgFieldAssignment(metaInfo1, field1, true);

        assertEquals(field1, afa.field());
        assertEquals(metaInfo1, afa.arg());
        assertTrue(afa.isMultiDoc());

        DataField field2 = new DataField("doc2", "field2", DefaultAdlSolrType.SOLR_STRING, true);

        afa = new ArgFieldAssignment(metaInfo1, field2, false);

        assertEquals(field2, afa.field());

        ArgMetaInfo metaInfo2 = new ArgMetaInfo("arg", DefaultAdlType.STRING, true, false);

        assertNotEquals(metaInfo2, afa.arg());

        metaInfo2 = new ArgMetaInfo("arg", DefaultAdlType.STRING, false, false);

        afa = new ArgFieldAssignment(metaInfo2, field1, true);

        assertEquals(field1, afa.field());

        assertEquals(metaInfo1, afa.arg());

        afa = new ArgFieldAssignment(metaInfo2, field2, false);

        ArgMetaInfo metaInfo3 = new ArgMetaInfo("arg", DefaultAdlType.STRING, false, true);

        assertEquals(field2, afa.field());

        assertEquals(metaInfo3, afa.arg());
        assertFalse(afa.isMultiDoc());

    }

    @Test
    void testSpecialCase() {

        DataField field1 = new DataField("doc1", "field1", DefaultAdlSolrType.SOLR_STRING, false);

        ArgMetaInfo metaInfo1 = new ArgMetaInfo("arg", DefaultAdlType.STRING, false, false);

        assertThrows(IllegalArgumentException.class, () -> new ArgFieldAssignment(metaInfo1, null, false));
        assertThrows(IllegalArgumentException.class, () -> new ArgFieldAssignment(null, field1, false));
        assertThrows(IllegalArgumentException.class, () -> new ArgFieldAssignment(null, null, false));

        ArgMetaInfo metaInfoBool = new ArgMetaInfo("arg2", DefaultAdlType.BOOL, false, false);

        DataField colDate = new DataField("doc1", "field2", DefaultAdlSolrType.SOLR_DATE, false);
        assertThrows(ConfigException.class, () -> new ArgFieldAssignment(metaInfoBool, colDate, false));

        DataField colDouble = new DataField("doc1", "field3", DefaultAdlSolrType.SOLR_DOUBLE, false);
        assertThrows(ConfigException.class, () -> new ArgFieldAssignment(metaInfoBool, colDouble, true));

    }

}
