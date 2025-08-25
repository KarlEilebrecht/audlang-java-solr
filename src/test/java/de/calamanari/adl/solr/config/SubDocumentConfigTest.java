//@formatter:off
/*
 * SubDocumentConfigTest
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
import static de.calamanari.adl.cnv.tps.DefaultAdlType.INTEGER;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.STRING;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_BOOLEAN;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_DATE;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_INTEGER;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_STRING;
import static de.calamanari.adl.solr.cnv.ConversionTestUtils.simpleAssignment;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import de.calamanari.adl.DeepCopyUtils;
import de.calamanari.adl.ProcessContext;
import de.calamanari.adl.cnv.tps.ArgMetaInfoLookup;
import de.calamanari.adl.cnv.tps.ConfigException;
import de.calamanari.adl.cnv.tps.DefaultArgMetaInfoLookup;
import de.calamanari.adl.cnv.tps.LookupException;
import de.calamanari.adl.solr.DefaultAdlSolrType;
import de.calamanari.adl.solr.SolrTestBase;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class SubDocumentConfigTest extends SolrTestBase {

    @Test
    void testBasics() {

        // Note:
        // the detail validation (applicable to all documents) are covered by ConfigUtilsTest
        // Here we concentrate on the specific cases

        Map<String, ArgFieldAssignment> argFieldMapEmpty = Collections.emptyMap();

        assertNotNull(new SubDocumentConfig(NODE_TYPE_1, SolrDocumentNature.NESTED, null, argFieldMapEmpty, null, null));
        assertNotNull(new SubDocumentConfig(NODE_TYPE_1, SolrDocumentNature.DEPENDENT, null, argFieldMapEmpty, null, null));

        assertThrows(ConfigException.class, () -> new SubDocumentConfig(NODE_TYPE_1, SolrDocumentNature.MAIN, null, argFieldMapEmpty, null, null));

        SubDocumentConfig config = new SubDocumentConfig(NODE_TYPE_1, SolrDocumentNature.DEPENDENT, null, argFieldMapEmpty, null, null);

        assertEquals(NODE_TYPE_1, config.nodeType());

        assertEquals(SolrDocumentNature.DEPENDENT, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertEquals(Collections.emptyMap(), config.argFieldMap());

        assertEquals(DefaultAutoMappingPolicy.NONE, config.autoMappingPolicy());

        List<FilterField> documentFilters = new ArrayList<>();

        FilterField filterField = new FilterField(NODE_TYPE_1, FIELD_NAME_3, DefaultAdlSolrType.SOLR_INTEGER, "100");

        documentFilters.add(filterField);

        Map<String, ArgFieldAssignment> argFieldMap = new HashMap<>();

        ArgFieldAssignment afa1 = simpleAssignment(NODE_TYPE_1, ARG_NAME_1, FIELD_NAME_1);
        ArgFieldAssignment afa2 = simpleAssignment(NODE_TYPE_1, ARG_NAME_2, FIELD_NAME_2);

        argFieldMap.put(afa2.arg().argName(), afa2);
        argFieldMap.put(afa1.arg().argName(), afa1);

        AutoMappingPolicy policy = new DefaultAutoMappingPolicy(NODE_TYPE_1, s -> s.startsWith("int.") ? s.substring(5) : null, INTEGER, SOLR_INTEGER, null,
                false, null);

        config = new SubDocumentConfig(NODE_TYPE_1, SolrDocumentNature.NESTED, documentFilters, argFieldMap, policy, null);

        assertEquals(SolrDocumentNature.NESTED, config.documentNature());

        assertEquals(documentFilters, config.documentFilters());

        assertEquals(argFieldMap, config.argFieldMap());

        assertSame(policy, config.autoMappingPolicy());

    }

    @Test
    void testStaticMapping1() {
        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .dependent()
                                                    .dataField(FIELD_NAME_1, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.DEPENDENT, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertEquals(DefaultAutoMappingPolicy.NONE, config.autoMappingPolicy());

        ArgFieldAssignment assignment = config.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertFalse(assignment.isMultiDoc());
        assertEquals(FIELD_NAME_1, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        // @formatter:off
        SubDocumentConfig config2 = SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .nested()
                                                    .filteredBy(FIELD_NAME_3, SOLR_STRING, "active")
                                                    .dataField(FIELD_NAME_1, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.NESTED, config2.documentNature());

        assertEquals(1, config2.documentFilters().size());

        assertEquals(DefaultAutoMappingPolicy.NONE, config2.autoMappingPolicy());

        assignment = config2.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals(FIELD_NAME_1, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

    }

    @Test
    void testStaticMapping2() {
        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .dependent()
                                                    .dataField(FIELD_NAME_1, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                        .asCollection()
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.DEPENDENT, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertEquals(DefaultAutoMappingPolicy.NONE, config.autoMappingPolicy());

        ArgFieldAssignment assignment = config.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertTrue(assignment.arg().isCollection());
        assertEquals(FIELD_NAME_1, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertTrue(assignment.field().isCollection());

    }

    @Test
    void testStatiMapping3() {
        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .dependent()
                                                    .filteredBy(ARG_NAME_2, SOLR_INTEGER, "17")
                                                    .filteredBy(ARG_NAME_3, SOLR_BOOLEAN, "1")
                                                    .dataField(FIELD_NAME_1, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                        .asCollection()
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.DEPENDENT, config.documentNature());

        assertEquals(DefaultAutoMappingPolicy.NONE, config.autoMappingPolicy());

        assertEquals(2, config.documentFilters().size());

        FilterField filter = config.documentFilters().get(0);
        assertEquals(ARG_NAME_2, filter.fieldName());
        assertEquals(SOLR_INTEGER, filter.fieldType());
        assertEquals("17", filter.filterValue());

        filter = config.documentFilters().get(1);
        assertEquals(ARG_NAME_3, filter.fieldName());
        assertEquals(SOLR_BOOLEAN, filter.fieldType());
        assertEquals("1", filter.filterValue());

        ArgFieldAssignment assignment = config.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertTrue(assignment.arg().isCollection());
        assertEquals(FIELD_NAME_1, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertTrue(assignment.field().isCollection());

    }

    @Test
    void testStaticMapping4() {
        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .nested()
                                                    .dataField("multi_string_ss")
                                                        .mappedToArgName(ARG_NAME_1)
                                                    .dataField("mono_integer_i")
                                                        .mappedToArgName(ARG_NAME_2)
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.NESTED, config.documentNature());

        assertEquals(DefaultAutoMappingPolicy.NONE, config.autoMappingPolicy());

        assertEquals(Collections.emptyList(), config.documentFilters());

        ArgFieldAssignment assignment = config.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertTrue(assignment.arg().isCollection());
        assertEquals("multi_string_ss", assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertTrue(assignment.field().isCollection());

        assignment = config.lookupAssignment(ARG_NAME_2, ProcessContext.empty());
        assertEquals(ARG_NAME_2, assignment.arg().argName());
        assertEquals(INTEGER, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals("mono_integer_i", assignment.field().fieldName());
        assertEquals(SOLR_INTEGER, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

    }

    @Test
    void testStaticMapping5() {
        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .nested()
                                                    .dataField("multi_string_ss")
                                                        .mappedToArgName(ARG_NAME_1)
                                                        .notAsCollection()
                                                    .dataField("mono_integer_i")
                                                        .mappedToArgName(ARG_NAME_2, STRING)
                                                        .asCollection()
                                                    .dataField("b_ss", SOLR_BOOLEAN)
                                                        .mappedToArgName(ARG_NAME_3)
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.NESTED, config.documentNature());

        assertEquals(DefaultAutoMappingPolicy.NONE, config.autoMappingPolicy());

        assertEquals(Collections.emptyList(), config.documentFilters());

        ArgFieldAssignment assignment = config.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals("multi_string_ss", assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        assignment = config.lookupAssignment(ARG_NAME_2, ProcessContext.empty());
        assertEquals(ARG_NAME_2, assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertTrue(assignment.arg().isCollection());
        assertEquals("mono_integer_i", assignment.field().fieldName());
        assertEquals(SOLR_INTEGER, assignment.field().fieldType());
        assertTrue(assignment.field().isCollection());

        assignment = config.lookupAssignment(ARG_NAME_3, ProcessContext.empty());
        assertEquals(ARG_NAME_3, assignment.arg().argName());
        assertEquals(BOOL, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals("b_ss", assignment.field().fieldName());
        assertEquals(SOLR_BOOLEAN, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

    }

    @Test
    void testStaticMappingMultiDoc() {
        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .dependent()
                                                    .dataField(FIELD_NAME_1, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                        .asCollection()
                                                        .multiDoc()
                                                    .dataField(FIELD_NAME_2, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_2)
                                                        .multiDoc()
                                                    .dataField(FIELD_NAME_3, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_3)
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.DEPENDENT, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertEquals(DefaultAutoMappingPolicy.NONE, config.autoMappingPolicy());

        ArgFieldAssignment assignment = config.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertTrue(assignment.arg().isCollection());
        assertTrue(assignment.isMultiDoc());

        assignment = config.lookupAssignment(ARG_NAME_2, ProcessContext.empty());
        assertEquals(ARG_NAME_2, assignment.arg().argName());
        assertFalse(assignment.arg().isCollection());
        assertTrue(assignment.isMultiDoc());

        assignment = config.lookupAssignment(ARG_NAME_3, ProcessContext.empty());
        assertEquals(ARG_NAME_3, assignment.arg().argName());
        assertFalse(assignment.arg().isCollection());
        assertFalse(assignment.isMultiDoc());

    }

    @Test
    void testStaticMappingMultiDoc2() {
        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .dependentMultiDoc()
                                                    .dataField(FIELD_NAME_1, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                        .asCollection()
                                                        .notMultiDoc()
                                                    .dataField(FIELD_NAME_2, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_2)
                                                    .dataField(FIELD_NAME_3, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_3)
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.DEPENDENT, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertEquals(DefaultAutoMappingPolicy.NONE, config.autoMappingPolicy());

        ArgFieldAssignment assignment = config.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertTrue(assignment.arg().isCollection());
        assertFalse(assignment.isMultiDoc());

        assignment = config.lookupAssignment(ARG_NAME_2, ProcessContext.empty());
        assertEquals(ARG_NAME_2, assignment.arg().argName());
        assertFalse(assignment.arg().isCollection());
        assertTrue(assignment.isMultiDoc());

        assignment = config.lookupAssignment(ARG_NAME_3, ProcessContext.empty());
        assertEquals(ARG_NAME_3, assignment.arg().argName());
        assertFalse(assignment.arg().isCollection());
        assertTrue(assignment.isMultiDoc());

    }

    @Test
    void testStaticMappingMultiDoc3() {
        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .nestedMultiDoc()
                                                    .dataField(FIELD_NAME_1, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                        .asCollection()
                                                    .dataField(FIELD_NAME_2, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_2)
                                                        .notMultiDoc()
                                                    .dataField(FIELD_NAME_3, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_3)
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.NESTED, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertEquals(DefaultAutoMappingPolicy.NONE, config.autoMappingPolicy());

        ArgFieldAssignment assignment = config.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertTrue(assignment.arg().isCollection());
        assertTrue(assignment.isMultiDoc());

        assignment = config.lookupAssignment(ARG_NAME_2, ProcessContext.empty());
        assertEquals(ARG_NAME_2, assignment.arg().argName());
        assertFalse(assignment.arg().isCollection());
        assertFalse(assignment.isMultiDoc());

        assignment = config.lookupAssignment(ARG_NAME_3, ProcessContext.empty());
        assertEquals(ARG_NAME_3, assignment.arg().argName());
        assertFalse(assignment.arg().isCollection());
        assertTrue(assignment.isMultiDoc());

    }

    // Warning below ignored (difficult to avoid with fluent API testing)
    @Test
    @SuppressWarnings("java:S5778")
    void testStaticMappingWithMeta() {

        // @formatter:off
        ArgMetaInfoLookup logicalDataModel = DefaultArgMetaInfoLookup
            .withArg(ARG_NAME_1).ofType(DATE)
                .thatIsAlwaysKnown()
            .withArg(ARG_NAME_2).ofType(INTEGER)
            .withArg(ARG_NAME_3).ofType(BOOL)
            .get();
                
        // @formatter:on

        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1, logicalDataModel)
                                                    .dependent()
                                                    .dataField(FIELD_NAME_1, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                    .dataField(FIELD_NAME_2, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_2)
                                                    .dataField(FIELD_NAME_3, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_3)
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.DEPENDENT, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertEquals(DefaultAutoMappingPolicy.NONE, config.autoMappingPolicy());

        ArgFieldAssignment assignment = config.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertEquals(DATE, assignment.arg().type());
        assertTrue(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals(FIELD_NAME_1, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        assignment = config.lookupAssignment(ARG_NAME_2, ProcessContext.empty());
        assertEquals(ARG_NAME_2, assignment.arg().argName());
        assertEquals(INTEGER, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals(FIELD_NAME_2, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        assignment = config.lookupAssignment(ARG_NAME_3, ProcessContext.empty());
        assertEquals(ARG_NAME_3, assignment.arg().argName());
        assertEquals(BOOL, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals(FIELD_NAME_3, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        // @formatter:off
        assertThrows(ConfigException.class, () -> SubDocumentConfig.forNodeType(NODE_TYPE_1, logicalDataModel)
                                                    .dependent()
                                                    .dataField("unknown_field", SOLR_STRING)
                                                        .mappedToArgName("unknownArg")
                                                    .get());
        // @formatter:on

    }

    @Test
    void testAutoMapping1() {
        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .dependent()
                                                    .dataField(FIELD_NAME_1, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                    .autoMapped(s -> s.startsWith("int.") ? s.substring(4) + "_i" : null)
                                                    .autoMapped(s -> s.startsWith("bool.") ? s.substring(5) + "_b" : null)
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.DEPENDENT, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertNotNull(config.autoMappingPolicy());
        assertEquals(CompositeAutoMappingPolicy.class, config.autoMappingPolicy().getClass());

        ArgFieldAssignment assignment = config.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals(FIELD_NAME_1, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        assignment = config.lookupAssignment("int.count", ProcessContext.empty());
        assertEquals("int.count", assignment.arg().argName());
        assertEquals(INTEGER, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals("count_i", assignment.field().fieldName());
        assertEquals(SOLR_INTEGER, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        assignment = config.lookupAssignment("bool.active", ProcessContext.empty());
        assertEquals("bool.active", assignment.arg().argName());
        assertEquals(BOOL, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals("active_b", assignment.field().fieldName());
        assertEquals(SOLR_BOOLEAN, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

    }

    @Test
    void testAutoMapping2() {
        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .dependent()
                                                    .autoMapped(s -> s.startsWith("dates.") ? s.substring(6) + "_dts" : null)
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.DEPENDENT, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertNotNull(config.autoMappingPolicy());
        assertEquals(CompositeAutoMappingPolicy.class, config.autoMappingPolicy().getClass());

        ArgFieldAssignment assignment = config.lookupAssignment("dates.meeting", ProcessContext.empty());
        assertEquals("dates.meeting", assignment.arg().argName());
        assertEquals(DATE, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertTrue(assignment.arg().isCollection());
        assertEquals("meeting_dts", assignment.field().fieldName());
        assertEquals(SOLR_DATE, assignment.field().fieldType());
        assertTrue(assignment.field().isCollection());

    }

    @Test
    void testAutoMapping3() {
        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .dependent()
                                                    .dataField("special", SOLR_STRING)
                                                        .mappedToArgName("str.special", INTEGER)
                                                        .asCollection()
                                                    .autoMapped(s -> s.startsWith("str.") ? s.substring(4) + "_s" : null)
                                                    .autoMapped(s -> s.startsWith("bool.") ? s.substring(5) + "_b" : null)
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.DEPENDENT, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertNotNull(config.autoMappingPolicy());
        assertEquals(CompositeAutoMappingPolicy.class, config.autoMappingPolicy().getClass());

        ArgFieldAssignment assignment = config.lookupAssignment("str.special", ProcessContext.empty());
        assertEquals("str.special", assignment.arg().argName());
        assertEquals(INTEGER, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertTrue(assignment.arg().isCollection());
        assertEquals("special", assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertTrue(assignment.field().isCollection());

        assignment = config.lookupAssignment("str.other", ProcessContext.empty());
        assertEquals("str.other", assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals("other_s", assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        assignment = config.lookupAssignment("bool.active", ProcessContext.empty());
        assertEquals("bool.active", assignment.arg().argName());
        assertEquals(BOOL, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals("active_b", assignment.field().fieldName());
        assertEquals(SOLR_BOOLEAN, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

    }

    @Test
    void testAutoMapping4() {
        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .dependent()
                                                    .autoMapped(s -> s.startsWith("bool.") ? s.substring(5) : null, BOOL, SOLR_BOOLEAN, null)
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.DEPENDENT, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertNotNull(config.autoMappingPolicy());
        assertEquals(CompositeAutoMappingPolicy.class, config.autoMappingPolicy().getClass());

        ArgFieldAssignment assignment = config.lookupAssignment("bool.active", ProcessContext.empty());
        assertEquals("bool.active", assignment.arg().argName());
        assertEquals(BOOL, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals("active", assignment.field().fieldName());
        assertEquals(SOLR_BOOLEAN, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        assignment = config.lookupAssignment("bool.active_bs", ProcessContext.empty());
        assertEquals("bool.active_bs", assignment.arg().argName());
        assertEquals(BOOL, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertTrue(assignment.arg().isCollection());
        assertEquals("active_bs", assignment.field().fieldName());
        assertEquals(SOLR_BOOLEAN, assignment.field().fieldType());
        assertTrue(assignment.field().isCollection());

    }

    @Test
    void testAutoMapping5() {
        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .dependent()
                                                    .autoMapped(s -> s.startsWith("bool.") ? s.substring(5) : null, null, SOLR_BOOLEAN, false)
                                                    .autoMapped(s -> s.startsWith("str.") ? s.substring(4) : null, STRING, null, null)
                                                    .autoMapped(s -> s.startsWith("int.") ? s.substring(4) : null, INTEGER, null, false)
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.DEPENDENT, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertNotNull(config.autoMappingPolicy());
        assertEquals(CompositeAutoMappingPolicy.class, config.autoMappingPolicy().getClass());

        ArgFieldAssignment assignment = config.lookupAssignment("bool.active_bs", ProcessContext.empty());
        assertEquals("bool.active_bs", assignment.arg().argName());
        assertEquals(BOOL, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals("active_bs", assignment.field().fieldName());
        assertEquals(SOLR_BOOLEAN, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        assignment = config.lookupAssignment("str.counts_is", ProcessContext.empty());
        assertEquals("str.counts_is", assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertTrue(assignment.arg().isCollection());
        assertEquals("counts_is", assignment.field().fieldName());
        assertEquals(SOLR_INTEGER, assignment.field().fieldType());
        assertTrue(assignment.field().isCollection());

        assignment = config.lookupAssignment("int.count2_ss", ProcessContext.empty());
        assertEquals("int.count2_ss", assignment.arg().argName());
        assertEquals(INTEGER, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals("count2_ss", assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

    }

    @Test
    void testAutoMapping6() {
        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .nested()
                                                    .autoMapped((nodeType, ami) -> new DefaultAutoMappingPolicy(nodeType, 
                                                                                        s -> s.startsWith("bool.") ? s.substring(5) : null,
                                                                                        BOOL, 
                                                                                        SOLR_BOOLEAN, 
                                                                                        null, 
                                                                                        false,
                                                                                        null))
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.NESTED, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertNotNull(config.autoMappingPolicy());
        assertEquals(CompositeAutoMappingPolicy.class, config.autoMappingPolicy().getClass());

        ArgFieldAssignment assignment = config.lookupAssignment("bool.active", ProcessContext.empty());
        assertEquals("bool.active", assignment.arg().argName());
        assertEquals(BOOL, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals("active", assignment.field().fieldName());
        assertEquals(SOLR_BOOLEAN, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        assignment = config.lookupAssignment("bool.active_bs", ProcessContext.empty());
        assertEquals("bool.active_bs", assignment.arg().argName());
        assertEquals(BOOL, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertTrue(assignment.arg().isCollection());
        assertEquals("active_bs", assignment.field().fieldName());
        assertEquals(SOLR_BOOLEAN, assignment.field().fieldType());
        assertTrue(assignment.field().isCollection());

    }

    // Warning below ignored (difficult to avoid with fluent API testing)
    @Test
    @SuppressWarnings("java:S5778")
    void testAutoMapping7() {

        // @formatter:off
        ArgMetaInfoLookup logicalDataModel = DefaultArgMetaInfoLookup
            .withArg(ARG_NAME_1).ofType(DATE)
                .thatIsAlwaysKnown()
            .withArg("int.count").ofType(INTEGER)
            .withArg("bool.active").ofType(BOOL)
            .get();
                
        // @formatter:on

        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1, logicalDataModel)
                                                    .dependent()
                                                    .dataField(FIELD_NAME_1, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                    .autoMapped(s -> s.startsWith("int.") ? s.substring(4) + "_i" : null, null, null, true)
                                                    .autoMapped(s -> s.startsWith("bool.") ? s.substring(5) + "_b" : null)
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.DEPENDENT, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertNotNull(config.autoMappingPolicy());
        assertEquals(CompositeAutoMappingPolicy.class, config.autoMappingPolicy().getClass());

        ArgFieldAssignment assignment = config.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertEquals(DATE, assignment.arg().type());
        assertFalse(assignment.arg().isCollection());
        assertEquals(FIELD_NAME_1, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        assignment = config.lookupAssignment("int.count", ProcessContext.empty());
        assertEquals("int.count", assignment.arg().argName());
        assertEquals(INTEGER, assignment.arg().type());
        assertTrue(assignment.arg().isCollection());
        assertEquals("count_i", assignment.field().fieldName());
        assertEquals(SOLR_INTEGER, assignment.field().fieldType());
        assertTrue(assignment.field().isCollection());

        assignment = config.lookupAssignment("bool.active", ProcessContext.empty());
        assertEquals("bool.active", assignment.arg().argName());
        assertEquals(BOOL, assignment.arg().type());
        assertFalse(assignment.arg().isCollection());
        assertEquals("active_b", assignment.field().fieldName());
        assertEquals(SOLR_BOOLEAN, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        assertThrows(LookupException.class, () -> config.lookupAssignment("int.unknown1", ProcessContext.empty()));
        assertThrows(LookupException.class, () -> config.lookupAssignment("bool.unknown2", ProcessContext.empty()));

    }

    // Warning below ignored (difficult to avoid with fluent API testing)
    @Test
    @SuppressWarnings("java:S5778")
    void testAutoMapping8() {
        // @formatter:off
        assertThrows(ConfigException.class, () -> SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .nested()
                                                    .autoMapped((LocalArgNameExtractor)null)
                                                    .get());
        // @formatter:on

        // @formatter:off
        assertThrows(ConfigException.class, () -> SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .nested()
                                                    .autoMapped((BiFunction<String, ArgMetaInfoLookup, AutoMappingPolicy>)null)
                                                    .get());
        // @formatter:on

    }

    @Test
    void testAutoMappingMultiDoc() {
        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .dependent()
                                                    .dataField(FIELD_NAME_1, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                            .multiDoc()
                                                    .autoMapped(s -> s.startsWith("int.") ? s.substring(4) + "_i" : null)
                                                    .autoMapped(s -> s.startsWith("bool.") ? s.substring(5) + "_b" : null)
                                                        .multiDoc()
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.DEPENDENT, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertNotNull(config.autoMappingPolicy());
        assertEquals(CompositeAutoMappingPolicy.class, config.autoMappingPolicy().getClass());

        ArgFieldAssignment assignment = config.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertEquals(FIELD_NAME_1, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());
        assertTrue(assignment.isMultiDoc());

        assignment = config.lookupAssignment("int.count", ProcessContext.empty());
        assertEquals("int.count", assignment.arg().argName());
        assertEquals(INTEGER, assignment.arg().type());
        assertEquals("count_i", assignment.field().fieldName());
        assertEquals(SOLR_INTEGER, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());
        assertFalse(assignment.isMultiDoc());

        assignment = config.lookupAssignment("bool.active", ProcessContext.empty());
        assertEquals("bool.active", assignment.arg().argName());
        assertEquals(BOOL, assignment.arg().type());
        assertEquals("active_b", assignment.field().fieldName());
        assertEquals(SOLR_BOOLEAN, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());
        assertTrue(assignment.isMultiDoc());

    }

    // Warning below ignored (difficult to avoid with fluent API testing)
    @Test
    @SuppressWarnings("java:S5778")
    void testBadRemapping() {

        // @formatter:off
        assertThrows(ConfigException.class, () -> SubDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .nested()
                                                    .dataField(FIELD_NAME_1, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                    .dataField(FIELD_NAME_2, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                    .get());
        // @formatter:on

    }

    // Warning below ignored (difficult to avoid with fluent API testing)
    @Test
    @SuppressWarnings("java:S5778")
    void testSerialization() {

        // @formatter:off
        ArgMetaInfoLookup logicalDataModel = DefaultArgMetaInfoLookup
            .withArg(ARG_NAME_1).ofType(DATE)
                .thatIsAlwaysKnown()
            .withArg("int.count").ofType(INTEGER)
            .withArg("bool.active").ofType(BOOL)
            .get();
                
        // @formatter:on

        // @formatter:off
        SubDocumentConfig config = SubDocumentConfig.forNodeType(NODE_TYPE_1, logicalDataModel)
                                                    .dependent()
                                                    .dataField(FIELD_NAME_1, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                    .autoMapped(s -> s.startsWith("int.") ? s.substring(4) + "_i" : null, null, null, true)
                                                    .autoMapped((nodeType, ami) -> new DefaultAutoMappingPolicy(nodeType, 
                                                            s -> s.startsWith("bool.") ? s.substring(5) : null,
                                                            null, 
                                                            SOLR_BOOLEAN, 
                                                            null, 
                                                            false,
                                                            logicalDataModel))
                                                    .get();
        // @formatter:on

        SubDocumentConfig config2 = DeepCopyUtils.deepCopy(config);

        assertEquals(SolrDocumentNature.DEPENDENT, config2.documentNature());

        assertEquals(Collections.emptyList(), config2.documentFilters());

        assertNotNull(config2.autoMappingPolicy());
        assertEquals(CompositeAutoMappingPolicy.class, config2.autoMappingPolicy().getClass());

        ArgFieldAssignment assignment = config2.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertEquals(DATE, assignment.arg().type());
        assertFalse(assignment.arg().isCollection());
        assertEquals(FIELD_NAME_1, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        assignment = config2.lookupAssignment("int.count", ProcessContext.empty());
        assertEquals("int.count", assignment.arg().argName());
        assertEquals(INTEGER, assignment.arg().type());
        assertTrue(assignment.arg().isCollection());
        assertEquals("count_i", assignment.field().fieldName());
        assertEquals(SOLR_INTEGER, assignment.field().fieldType());
        assertTrue(assignment.field().isCollection());

        assignment = config2.lookupAssignment("bool.active", ProcessContext.empty());
        assertEquals("bool.active", assignment.arg().argName());
        assertEquals(BOOL, assignment.arg().type());
        assertFalse(assignment.arg().isCollection());
        assertEquals("active", assignment.field().fieldName());
        assertEquals(SOLR_BOOLEAN, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        assertThrows(LookupException.class, () -> config2.lookupAssignment("int.unknown1", ProcessContext.empty()));
        assertThrows(LookupException.class, () -> config2.lookupAssignment("bool.unknown2", ProcessContext.empty()));

    }

}
