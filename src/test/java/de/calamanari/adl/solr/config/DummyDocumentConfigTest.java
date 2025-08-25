//@formatter:off
/*
 * DummyDocumentConfigTest
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

import static de.calamanari.adl.cnv.tps.DefaultAdlType.STRING;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_STRING;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import de.calamanari.adl.DeepCopyUtils;
import de.calamanari.adl.ProcessContext;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class DummyDocumentConfigTest {

    @Test
    void testBasics() {

        assertSame(DummyDocumentConfig.getInstance(), DummyDocumentConfig.getInstance());

        DummyDocumentConfig ddc = DummyDocumentConfig.getInstance();

        assertEquals(1, ddc.allNodeTypeMetaInfos().size());

        assertSame(ddc, ddc.allNodeTypeMetaInfos().get(0));

        assertTrue(ddc.contains("argName"));

        assertThrows(IllegalArgumentException.class, () -> ddc.contains(""));

        assertEquals(DummyDocumentConfig.DUMMY_NODE_TYPE, ddc.nodeType());

        assertEquals(SolrDocumentNature.MAIN, ddc.documentNature());

        assertEquals(Collections.emptyList(), ddc.documentFilters());

        ProcessContext emptyCtx = ProcessContext.empty();

        assertThrows(IllegalArgumentException.class, () -> ddc.lookupAssignment(null, emptyCtx));
        assertThrows(IllegalArgumentException.class, () -> ddc.lookupAssignment("argName", null));

        ArgFieldAssignment assignment = ddc.lookupAssignment("argName", emptyCtx);

        assertNotNull(assignment);
        assertEquals("argName", assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals("arg_name_s", assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        assertEquals(1, ddc.numberOfNodeTypes());

        assertThrows(IllegalArgumentException.class, () -> ddc.lookupNodeTypeMetaInfo(null, emptyCtx));
        assertThrows(IllegalArgumentException.class, () -> ddc.lookupNodeTypeMetaInfo("argName", null));

        assertSame(ddc, ddc.lookupNodeTypeMetaInfo("argName", emptyCtx));

        assertSame(DummyDocumentConfig.getInstance(), DummyDocumentConfig.getInstance().mainNodeTypeMetaInfo());
    }

    @Test
    void testCleanLowerCase() {

        assertThrows(IllegalArgumentException.class, () -> DummyDocumentConfig.cleanLowerCase(null));
        assertThrows(IllegalArgumentException.class, () -> DummyDocumentConfig.cleanLowerCase(""));

        assertEquals("arg_s", DummyDocumentConfig.cleanLowerCase("arg"));
        assertEquals("arg_s", DummyDocumentConfig.cleanLowerCase("Arg"));
        assertEquals("arg_s", DummyDocumentConfig.cleanLowerCase("ARG"));
        assertEquals("some_camel_case_arg_s", DummyDocumentConfig.cleanLowerCase("someCamelCaseArg"));
        assertEquals("some_camel_case_arg_s", DummyDocumentConfig.cleanLowerCase("SomeCamelCaseArg"));
        assertEquals("v_123_s", DummyDocumentConfig.cleanLowerCase("123"));
        assertEquals("v_123abc_s", DummyDocumentConfig.cleanLowerCase("123ABC"));
        assertEquals("node1_s", DummyDocumentConfig.cleanLowerCase("Node1"));
        assertEquals("src_123_s", DummyDocumentConfig.cleanLowerCase("src.123"));

    }

    @Test
    void testSerialization() {
        assertSame(DummyDocumentConfig.getInstance(), DeepCopyUtils.deepCopy(DummyDocumentConfig.getInstance()));
    }

    @Test
    void testSpecial() {
        assertEquals("some_camel_case_arg_s", DummyDocumentConfig.cleanLowerCase("someCamelCaseArg"));

    }

}
