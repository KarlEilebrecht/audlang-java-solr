//@formatter:off
/*
 * ConventionUtilsTest
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

import static de.calamanari.adl.cnv.tps.DefaultAdlType.BOOL;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.DATE;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.DECIMAL;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.INTEGER;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.STRING;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_BOOLEAN;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_DATE;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_DOUBLE;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_FLOAT;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_INTEGER;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_LONG;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_STRING;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class ConventionUtilsTest {

    @Test
    void testDetermineGenericArgType() {
        assertEquals(INTEGER, ConventionUtils.determineGenericArgType("arg_i"));
        assertEquals(INTEGER, ConventionUtils.determineGenericArgType("arg_is"));
        assertEquals(STRING, ConventionUtils.determineGenericArgType("arg_s"));
        assertEquals(STRING, ConventionUtils.determineGenericArgType("arg_ss"));
        assertEquals(INTEGER, ConventionUtils.determineGenericArgType("arg_l"));
        assertEquals(INTEGER, ConventionUtils.determineGenericArgType("arg_ls"));
        assertEquals(BOOL, ConventionUtils.determineGenericArgType("arg_b"));
        assertEquals(BOOL, ConventionUtils.determineGenericArgType("arg_bs"));
        assertEquals(DECIMAL, ConventionUtils.determineGenericArgType("arg_f"));
        assertEquals(DECIMAL, ConventionUtils.determineGenericArgType("arg_fs"));
        assertEquals(DECIMAL, ConventionUtils.determineGenericArgType("arg_d"));
        assertEquals(DECIMAL, ConventionUtils.determineGenericArgType("arg_ds"));
        assertEquals(DATE, ConventionUtils.determineGenericArgType("arg_dt"));
        assertEquals(DATE, ConventionUtils.determineGenericArgType("arg_dts"));

        assertEquals(STRING, ConventionUtils.determineGenericArgType("a_s"));
        assertEquals(STRING, ConventionUtils.determineGenericArgType("a_ss"));
        assertNull(ConventionUtils.determineGenericArgType("_s"));
        assertNull(ConventionUtils.determineGenericArgType("_ss"));

        assertNull(ConventionUtils.determineGenericArgType("foo"));
        assertNull(ConventionUtils.determineGenericArgType(""));
        assertNull(ConventionUtils.determineGenericArgType(null));
    }

    @Test
    void testDetermineGenericSolrFieldType() {
        assertEquals(SOLR_INTEGER, ConventionUtils.determineGenericSolrFieldType("arg_i"));
        assertEquals(SOLR_INTEGER, ConventionUtils.determineGenericSolrFieldType("arg_is"));
        assertEquals(SOLR_STRING, ConventionUtils.determineGenericSolrFieldType("arg_s"));
        assertEquals(SOLR_STRING, ConventionUtils.determineGenericSolrFieldType("arg_ss"));
        assertEquals(SOLR_LONG, ConventionUtils.determineGenericSolrFieldType("arg_l"));
        assertEquals(SOLR_LONG, ConventionUtils.determineGenericSolrFieldType("arg_ls"));
        assertEquals(SOLR_BOOLEAN, ConventionUtils.determineGenericSolrFieldType("arg_b"));
        assertEquals(SOLR_BOOLEAN, ConventionUtils.determineGenericSolrFieldType("arg_bs"));
        assertEquals(SOLR_FLOAT, ConventionUtils.determineGenericSolrFieldType("arg_f"));
        assertEquals(SOLR_FLOAT, ConventionUtils.determineGenericSolrFieldType("arg_fs"));
        assertEquals(SOLR_DOUBLE, ConventionUtils.determineGenericSolrFieldType("arg_d"));
        assertEquals(SOLR_DOUBLE, ConventionUtils.determineGenericSolrFieldType("arg_ds"));
        assertEquals(SOLR_DATE, ConventionUtils.determineGenericSolrFieldType("arg_dt"));
        assertEquals(SOLR_DATE, ConventionUtils.determineGenericSolrFieldType("arg_dts"));

        assertEquals(SOLR_STRING, ConventionUtils.determineGenericSolrFieldType("a_s"));
        assertEquals(SOLR_STRING, ConventionUtils.determineGenericSolrFieldType("a_ss"));
        assertNull(ConventionUtils.determineGenericSolrFieldType("_s"));
        assertNull(ConventionUtils.determineGenericSolrFieldType("_ss"));

        assertNull(ConventionUtils.determineGenericSolrFieldType("foo"));
        assertNull(ConventionUtils.determineGenericSolrFieldType(""));
        assertNull(ConventionUtils.determineGenericSolrFieldType(null));

    }

    @Test
    void testDetermineGenericIsCollection() {
        assertFalse(ConventionUtils.determineGenericIsCollection("arg_i"));
        assertTrue(ConventionUtils.determineGenericIsCollection("arg_is"));
        assertFalse(ConventionUtils.determineGenericIsCollection("arg_s"));
        assertTrue(ConventionUtils.determineGenericIsCollection("arg_ss"));
        assertFalse(ConventionUtils.determineGenericIsCollection("arg_l"));
        assertTrue(ConventionUtils.determineGenericIsCollection("arg_ls"));
        assertFalse(ConventionUtils.determineGenericIsCollection("arg_b"));
        assertTrue(ConventionUtils.determineGenericIsCollection("arg_bs"));
        assertFalse(ConventionUtils.determineGenericIsCollection("arg_f"));
        assertTrue(ConventionUtils.determineGenericIsCollection("arg_fs"));
        assertFalse(ConventionUtils.determineGenericIsCollection("arg_d"));
        assertTrue(ConventionUtils.determineGenericIsCollection("arg_ds"));
        assertFalse(ConventionUtils.determineGenericIsCollection("arg_dt"));
        assertTrue(ConventionUtils.determineGenericIsCollection("arg_dts"));

        assertTrue(ConventionUtils.determineGenericIsCollection("a_ss"));
        assertFalse(ConventionUtils.determineGenericIsCollection("arg"));
        assertFalse(ConventionUtils.determineGenericIsCollection("_s"));
        assertFalse(ConventionUtils.determineGenericIsCollection("_ss"));

        assertFalse(ConventionUtils.determineGenericIsCollection(""));
        assertFalse(ConventionUtils.determineGenericIsCollection(null));

    }

    @Test
    void testResolveArgTypeFromFieldType() {

        assertEquals(INTEGER, ConventionUtils.resolveArgTypeFromFieldType(SOLR_INTEGER));
        assertEquals(STRING, ConventionUtils.resolveArgTypeFromFieldType(SOLR_STRING));
        assertEquals(INTEGER, ConventionUtils.resolveArgTypeFromFieldType(SOLR_LONG));
        assertEquals(BOOL, ConventionUtils.resolveArgTypeFromFieldType(SOLR_BOOLEAN));
        assertEquals(DECIMAL, ConventionUtils.resolveArgTypeFromFieldType(SOLR_FLOAT));
        assertEquals(DECIMAL, ConventionUtils.resolveArgTypeFromFieldType(SOLR_DOUBLE));
        assertEquals(DATE, ConventionUtils.resolveArgTypeFromFieldType(SOLR_DATE));

        assertNull(ConventionUtils.resolveArgTypeFromFieldType(null));

    }
}
