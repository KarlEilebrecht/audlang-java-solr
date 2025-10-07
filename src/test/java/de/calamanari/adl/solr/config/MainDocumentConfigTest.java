//@formatter:off
/*
 * MainDocumentConfigTest
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

import de.calamanari.adl.CommonErrors;
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
class MainDocumentConfigTest extends SolrTestBase {

    @Test
    void testBasics() {

        // Note:
        // the detail validation (applicable to all documents) are covered by ConfigUtilsTest
        // Here we concentrate on the specific cases

        Map<String, ArgFieldAssignment> argFieldMapEmpty = Collections.emptyMap();

        assertNotNull(new MainDocumentConfig(NODE_TYPE_1, null, argFieldMapEmpty, null, null, null));
        assertNotNull(new MainDocumentConfig(NODE_TYPE_1, null, argFieldMapEmpty, null, null, null));

        assertThrows(ConfigException.class, () -> new MainDocumentConfig(NODE_TYPE_1, null, null, null, null, null));

        MainDocumentConfig config = new MainDocumentConfig(NODE_TYPE_1, null, argFieldMapEmpty, null, null, null);

        assertEquals(NODE_TYPE_1, config.nodeType());

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

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

        List<SubDocumentConfig> subConfigs = new ArrayList<>();
        // @formatter:off
        subConfigs.add(SubDocumentConfig.forNodeType(NODE_TYPE_2)
                                            .nested()
                                            .dataField(FIELD_NAME_3, SOLR_INTEGER).mappedToArgName(ARG_NAME_3)
                                        .get());
        // @formatter:on

        config = new MainDocumentConfig(NODE_TYPE_1, documentFilters, argFieldMap, policy, subConfigs, null);

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

        assertEquals(documentFilters, config.documentFilters());

        assertEquals(argFieldMap, config.argFieldMap());

        assertSame(policy, config.autoMappingPolicy());

        assertEquals(subConfigs, config.subDocumentConfigs());

        assertSame(config, config.mainNodeTypeMetaInfo());

    }

    @Test
    void testStaticMapping1() {
        // @formatter:off
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                      .dataField(FIELD_NAME_1, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                      .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertEquals(DefaultAutoMappingPolicy.NONE, config.autoMappingPolicy());

        ArgFieldAssignment assignment = config.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals(FIELD_NAME_1, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        // @formatter:off
        MainDocumentConfig config2 = MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                           .filteredBy(FIELD_NAME_3, SOLR_STRING, "active")
                                                           .dataField(FIELD_NAME_1, SOLR_STRING)
                                                           .mappedToArgName(ARG_NAME_1)
                                                       .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.MAIN, config2.documentNature());

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
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                          .dataField(FIELD_NAME_1, SOLR_STRING)
                                                          .mappedToArgName(ARG_NAME_1)
                                                          .asCollection()
                                                      .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

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
    void testStaticMapping3() {
        // @formatter:off
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                          .filteredBy(ARG_NAME_2, SOLR_INTEGER, "17")
                                                          .filteredBy(ARG_NAME_3, SOLR_BOOLEAN, "1")
                                                          .dataField(FIELD_NAME_1, SOLR_STRING)
                                                              .mappedToArgName(ARG_NAME_1)
                                                              .asCollection()
                                                      .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

        assertEquals(DefaultAutoMappingPolicy.NONE, config.autoMappingPolicy());
        assertEquals(1, config.numberOfNodeTypes());

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
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .dataField("multi_string_ss")
                                                        .mappedToArgName(ARG_NAME_1)
                                                    .dataField("mono_integer_i")
                                                        .mappedToArgName(ARG_NAME_2)
                                                    .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

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
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1)
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

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

        assertEquals(DefaultAutoMappingPolicy.NONE, config.autoMappingPolicy());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertTrue(config.contains(ARG_NAME_1));
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
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1, logicalDataModel)
                                                          .dataField(FIELD_NAME_1, SOLR_STRING)
                                                              .mappedToArgName(ARG_NAME_1)
                                                          .dataField(FIELD_NAME_2, SOLR_STRING)
                                                              .mappedToArgName(ARG_NAME_2)
                                                          .dataField(FIELD_NAME_3, SOLR_STRING)
                                                              .mappedToArgName(ARG_NAME_3)
                                                      .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

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
        assertThrows(ConfigException.class, () -> MainDocumentConfig.forNodeType(NODE_TYPE_1, logicalDataModel)
                                                    .dataField("unknown_field", SOLR_STRING)
                                                        .mappedToArgName("unknownArg")
                                                    .get());
        // @formatter:on

    }

    @Test
    void testAutoMapping1() {
        // @formatter:off
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                          .dataField(FIELD_NAME_1, SOLR_STRING)
                                                              .mappedToArgName(ARG_NAME_1)
                                                          .autoMapped(s -> s.startsWith("int.") ? s.substring(4) + "_i" : null)
                                                          .autoMapped(s -> s.startsWith("bool.") ? s.substring(5) + "_b" : null)
                                                      .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

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
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                          .autoMapped(s -> s.startsWith("dates.") ? s.substring(6) + "_dts" : null)
                                                      .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

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
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                          .dataField("special", SOLR_STRING)
                                                              .mappedToArgName("str.special", INTEGER)
                                                              .asCollection()
                                                          .autoMapped(s -> s.startsWith("str.") ? s.substring(4) + "_s" : null)
                                                          .autoMapped(s -> s.startsWith("bool.") ? s.substring(5) + "_b" : null)
                                                      .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

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
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                          .autoMapped(s -> s.startsWith("bool.") ? s.substring(5) : null, BOOL, SOLR_BOOLEAN, null)
                                                      .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

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
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                          .autoMapped(s -> s.startsWith("bool.") ? s.substring(5) : null, null, SOLR_BOOLEAN, false)
                                                          .autoMapped(s -> s.startsWith("str.") ? s.substring(4) : null, STRING, null, null)
                                                          .autoMapped(s -> s.startsWith("int.") ? s.substring(4) : null, INTEGER, null, false)
                                                      .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

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
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                          .autoMapped((nodeType, _) -> new DefaultAutoMappingPolicy(nodeType, 
                                                                                        s -> s.startsWith("bool.") ? s.substring(5) : null,
                                                                                        BOOL, 
                                                                                        SOLR_BOOLEAN, 
                                                                                        null, 
                                                                                        false,
                                                                                        null))
                                                      .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

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
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1, logicalDataModel)
                                                          .dataField(FIELD_NAME_1, SOLR_STRING)
                                                              .mappedToArgName(ARG_NAME_1)
                                                          .autoMapped(s -> s.startsWith("int.") ? s.substring(4) + "_i" : null, null, null, true)
                                                          .autoMapped(s -> s.startsWith("bool.") ? s.substring(5) + "_b" : null)
                                                      .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

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
        assertThrows(ConfigException.class, () -> MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .autoMapped((LocalArgNameExtractor)null)
                                                    .get());
        // @formatter:on

        // @formatter:off
        assertThrows(ConfigException.class, () -> MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .autoMapped((BiFunction<String, ArgMetaInfoLookup, AutoMappingPolicy>)null)
                                                    .get());
        // @formatter:on

    }

    @Test
    void testWithSubConfig() {

        // @formatter:off
        ArgMetaInfoLookup logicalDataModel = DefaultArgMetaInfoLookup
            .withArg(ARG_NAME_1).ofType(STRING)
            .withArg(ARG_NAME_3).ofType(STRING)
            .withArg("int.count").ofType(INTEGER)
            .withArg("int.2.number").ofType(INTEGER).thatIsCollection()
            .withArg("bool.active").ofType(BOOL)
            .get();
                
        // @formatter:on

        // @formatter:off
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1, logicalDataModel)
                                                          .dataField(FIELD_NAME_1, SOLR_STRING)
                                                              .mappedToArgName(ARG_NAME_1)
                                                          .subConfig(SubDocumentConfig.forNodeType(NODE_TYPE_2, logicalDataModel)
                                                              .dependent()
                                                              .autoMapped(s -> s.startsWith("int.2.") ? s.substring(6) + "_i" : null)
                                                              .autoMapped(s -> s.startsWith("bool.") ? s.substring(5) + "_b" : null)
                                                          .get())
                                                          .subConfig(SubDocumentConfig.forNodeType(NODE_TYPE_3, logicalDataModel)
                                                              .dependent()
                                                              .dataField(FIELD_NAME_3, SOLR_STRING)
                                                                  .mappedToArgName(ARG_NAME_3)
                                                              .autoMapped(s -> s.startsWith("int.") ? s.substring(4) + "_i" : null)
                                                          .get())
                                                      .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertEquals(DefaultAutoMappingPolicy.NONE, config.autoMappingPolicy());

        assertTrue(config.contains(ARG_NAME_1));
        ArgFieldAssignment assignment = config.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertFalse(assignment.arg().isCollection());
        assertEquals(FIELD_NAME_1, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertEquals(NODE_TYPE_1, assignment.field().nodeType());

        assignment = config.lookupAssignment("int.count", ProcessContext.empty());
        assertEquals("int.count", assignment.arg().argName());
        assertEquals(INTEGER, assignment.arg().type());
        assertEquals("count_i", assignment.field().fieldName());
        assertEquals(SOLR_INTEGER, assignment.field().fieldType());
        assertEquals(NODE_TYPE_3, assignment.field().nodeType());

        assignment = config.lookupAssignment("int.2.number", ProcessContext.empty());
        assertEquals("int.2.number", assignment.arg().argName());
        assertEquals(INTEGER, assignment.arg().type());
        assertEquals("number_i", assignment.field().fieldName());
        assertEquals(SOLR_INTEGER, assignment.field().fieldType());
        assertEquals(NODE_TYPE_2, assignment.field().nodeType());

        assignment = config.lookupAssignment(ARG_NAME_3, ProcessContext.empty());
        assertEquals(ARG_NAME_3, assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertEquals(FIELD_NAME_3, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertEquals(NODE_TYPE_3, assignment.field().nodeType());

    }

    // Warning below ignored (difficult to avoid with fluent API testing)
    @Test
    @SuppressWarnings("java:S5778")
    void testWithSubConfig2() {

        // @formatter:off
        ArgMetaInfoLookup logicalDataModel = DefaultArgMetaInfoLookup
            .withArg(ARG_NAME_1).ofType(STRING)
            .withArg(ARG_NAME_3).ofType(STRING)
            .withArg("int.count").ofType(INTEGER)
            .withArg("int.2.number").ofType(INTEGER).thatIsCollection()
            .withArg("bool.active").ofType(BOOL)
            .get();
                
        // @formatter:on

        // @formatter:off
        assertThrows(ConfigException.class, () -> MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                                        .dataField(FIELD_NAME_1, SOLR_STRING)
                                                                            .mappedToArgName(ARG_NAME_1)
                                                                        .subConfig(SubDocumentConfig.forNodeType(NODE_TYPE_2, logicalDataModel)
                                                                            .dependent()
                                                                            .autoMapped(s -> s.startsWith("int.2.") ? s.substring(6) + "_i" : null)
                                                                            .autoMapped(s -> s.startsWith("bool.") ? s.substring(5) + "_b" : null)
                                                                        .get())
                                                                        .subConfig(SubDocumentConfig.forNodeType(NODE_TYPE_3, logicalDataModel)
                                                                            .dependent()
                                                                            .dataField(FIELD_NAME_3, SOLR_STRING)
                                                                                .mappedToArgName(ARG_NAME_3)
                                                                            .autoMapped(s -> s.startsWith("int.") ? s.substring(4) + "_i" : null)
                                                                        .get())
                                                                    .get());
        // @formatter:on

        // @formatter:off
        assertThrows(ConfigException.class, () -> MainDocumentConfig.forNodeType(NODE_TYPE_1, logicalDataModel)
                                                                        .dataField(FIELD_NAME_1, SOLR_STRING)
                                                                            .mappedToArgName(ARG_NAME_1)
                                                                        .subConfig(SubDocumentConfig.forNodeType(NODE_TYPE_2, logicalDataModel)
                                                                            .dependent()
                                                                            .autoMapped(s -> s.startsWith("int.2.") ? s.substring(6) + "_i" : null)
                                                                            .autoMapped(s -> s.startsWith("bool.") ? s.substring(5) + "_b" : null)
                                                                        .get())
                                                                        .subConfig(SubDocumentConfig.forNodeType(NODE_TYPE_3)
                                                                            .dependent()
                                                                            .dataField(FIELD_NAME_3, SOLR_STRING)
                                                                                .mappedToArgName(ARG_NAME_3)
                                                                            .autoMapped(s -> s.startsWith("int.") ? s.substring(4) + "_i" : null)
                                                                        .get())
                                                                    .get());
        // @formatter:on

        // @formatter:off
        assertThrows(ConfigException.class, () -> MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                                        .dataField(FIELD_NAME_1, SOLR_STRING)
                                                                            .mappedToArgName("int.dataField")
                                                                        .subConfig(SubDocumentConfig.forNodeType(NODE_TYPE_2)
                                                                            .dependent()
                                                                            .autoMapped(s -> s.startsWith("int.2.") ? s.substring(6) + "_i" : null)
                                                                            .autoMapped(s -> s.startsWith("bool.") ? s.substring(5) + "_b" : null)
                                                                        .get())
                                                                        .subConfig(SubDocumentConfig.forNodeType(NODE_TYPE_3)
                                                                            .dependent()
                                                                            .dataField(FIELD_NAME_3, SOLR_STRING)
                                                                                .mappedToArgName(ARG_NAME_3)
                                                                            .autoMapped(s -> s.startsWith("int.") ? s.substring(4) + "_i" : null)
                                                                        .get())
                                                                    .get());
        // @formatter:on

    }

    @Test
    void testWithSubConfig3() {

        // @formatter:off
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                          .dataField(FIELD_NAME_1, SOLR_STRING)
                                                              .mappedToArgName(ARG_NAME_1)
                                                          .autoMapped(s -> s.startsWith("int_") ? s.substring(4) + "_i" : null)
                                                          .defaultAutoMapped()
                                                          .subConfig(SubDocumentConfig.forNodeType(NODE_TYPE_3)
                                                              .dependentMultiDoc()
                                                              .dataField(FIELD_NAME_3, SOLR_STRING)
                                                                  .mappedToArgName(ARG_NAME_3, INTEGER)
                                                                  .notMultiDoc()
                                                              .autoMapped(s -> s.startsWith("int_x_") ? s.substring(6) + "_i" : null)
                                                          .get())
                                                      .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());
        assertEquals(2, config.numberOfNodeTypes());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertNotEquals(DefaultAutoMappingPolicy.NONE, config.autoMappingPolicy());

        ArgFieldAssignment assignment = config.lookupAssignment("int_x_number", ProcessContext.empty());
        assertEquals("int_x_number", assignment.arg().argName());
        assertEquals(INTEGER, assignment.arg().type());
        assertEquals("number_i", assignment.field().fieldName());
        assertEquals(SOLR_INTEGER, assignment.field().fieldType());
        assertEquals(NODE_TYPE_3, assignment.field().nodeType());
        assertTrue(assignment.isMultiDoc());

        assignment = config.lookupAssignment("int_number", ProcessContext.empty());
        assertEquals("int_number", assignment.arg().argName());
        assertEquals(INTEGER, assignment.arg().type());
        assertEquals("number_i", assignment.field().fieldName());
        assertEquals(SOLR_INTEGER, assignment.field().fieldType());
        assertEquals(NODE_TYPE_1, assignment.field().nodeType());

        assignment = config.lookupAssignment("foobar_s", ProcessContext.empty());
        assertEquals("foobar_s", assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertEquals("foobar_s", assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());

        assignment = config.lookupAssignment(ARG_NAME_3, ProcessContext.empty());
        assertEquals(ARG_NAME_3, assignment.arg().argName());
        assertEquals(INTEGER, assignment.arg().type());
        assertEquals(FIELD_NAME_3, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertEquals(NODE_TYPE_3, assignment.field().nodeType());
        assertFalse(assignment.isMultiDoc());

    }

    @Test
    void testWithSubConfig4() {

        // @formatter:off
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                          .dataField(FIELD_NAME_1, SOLR_STRING)
                                                              .mappedToArgName(ARG_NAME_1)
                                                          .autoMapped(s -> s.startsWith("int_") ? s.substring(4) + "_i" : null)
                                                          .defaultAutoMapped()
                                                          .subConfig(SubDocumentConfig.forNodeType(NODE_TYPE_3)
                                                              .dependentMultiDoc()
                                                              .dataField(FIELD_NAME_3, SOLR_STRING)
                                                                  .mappedToArgName(ARG_NAME_3, INTEGER)
                                                                  .notMultiDoc()
                                                              .autoMapped(s -> s.startsWith("int_x_") ? s.substring(6000) + "_i" : null)
                                                          .get())
                                                      .get();
        // @formatter:on

        ProcessContext ctx = ProcessContext.empty();
        assertThrows(LookupException.class, () -> config.lookupAssignment("fooBar", ctx));
        assertThrows(LookupException.class, () -> config.lookupAssignment("int_x_wrong", ctx));

        assertThrows(LookupException.class, () -> config.lookupNodeTypeMetaInfo("fooBar", ctx));
        assertThrows(LookupException.class, () -> config.lookupNodeTypeMetaInfo("int_x_wrong", ctx));

        assertThrows(LookupException.class, () -> config.lookupNodeTypeMetaInfoByNodeType("wrong"));

        assertThrows(IllegalArgumentException.class, () -> config.lookup(null));
        assertThrows(IllegalArgumentException.class, () -> config.lookupField(null, ctx));

        assertThrows(IllegalArgumentException.class, () -> config.lookup(""));
        assertThrows(IllegalArgumentException.class, () -> config.lookupField("", ctx));

    }

    // Warning below ignored (difficult to avoid with fluent API testing)
    @Test
    @SuppressWarnings("java:S5778")
    void testBadRemapping() {

        // @formatter:off
        assertThrows(ConfigException.class, () -> MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .dataField(FIELD_NAME_1, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                    .dataField(FIELD_NAME_2, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                    .get());
        // @formatter:on

    }

    @Test
    void testMapFieldTwice() {
        // @formatter:off
        MainDocumentConfig config = MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                      .dataField(FIELD_NAME_1, SOLR_STRING)
                                                          .mappedToArgName(ARG_NAME_1)
                                                      .dataField(FIELD_NAME_1, SOLR_STRING)
                                                          .mappedToArgName(ARG_NAME_2)
                                                      .get();
        // @formatter:on

        assertEquals(SolrDocumentNature.MAIN, config.documentNature());

        assertEquals(Collections.emptyList(), config.documentFilters());

        assertEquals(DefaultAutoMappingPolicy.NONE, config.autoMappingPolicy());

        ArgFieldAssignment assignment = config.lookupAssignment(ARG_NAME_1, ProcessContext.empty());
        assertEquals(ARG_NAME_1, assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals(FIELD_NAME_1, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

        assignment = config.lookupAssignment(ARG_NAME_2, ProcessContext.empty());
        assertEquals(ARG_NAME_2, assignment.arg().argName());
        assertEquals(STRING, assignment.arg().type());
        assertFalse(assignment.arg().isAlwaysKnown());
        assertFalse(assignment.arg().isCollection());
        assertEquals(FIELD_NAME_1, assignment.field().fieldName());
        assertEquals(SOLR_STRING, assignment.field().fieldType());
        assertFalse(assignment.field().isCollection());

    }

    // Warning below ignored (difficult to avoid with fluent API testing)
    @Test
    @SuppressWarnings("java:S5778")
    void testConflictingTypeMapping() {

        // @formatter:off
        assertThrows(ConfigException.class, () -> MainDocumentConfig.forNodeType(NODE_TYPE_1)
                                                    .dataField(FIELD_NAME_1, SOLR_STRING)
                                                        .mappedToArgName(ARG_NAME_1)
                                                    .dataField(FIELD_NAME_1, SOLR_INTEGER)
                                                        .mappedToArgName(ARG_NAME_2)
                                                    .get());
        // @formatter:on

    }

    // Warning below ignored (difficult to avoid with fluent API testing)
    @Test
    @SuppressWarnings("java:S5778")
    void testAutoMapperNull() {

        assertThrows(ConfigException.class, () -> MainDocumentConfig.forNodeType(NODE_TYPE_1).autoMapped((_, _) -> null).get());

    }

    // Warning below ignored (difficult to avoid with fluent API testing)
    @Test
    @SuppressWarnings("java:S5778")
    void testNameNull() {

        assertThrowsErrorCode(CommonErrors.ERR_3000_MAPPING_FAILED,
                () -> MainDocumentConfig.forNodeType(NODE_TYPE_1).dataField(null, SOLR_STRING).mappedToArgName(ARG_NAME_1).get());

        assertThrowsErrorCode(CommonErrors.ERR_3000_MAPPING_FAILED,
                () -> MainDocumentConfig.forNodeType(NODE_TYPE_1).dataField("some", SOLR_STRING).mappedToArgName(null).get());

    }

    // Warning below ignored (difficult to avoid with fluent API testing)
    @Test
    @SuppressWarnings("java:S5778")
    void testTypeUnknown() {

        assertThrowsErrorCode(CommonErrors.ERR_4002_CONFIG_ERROR,
                () -> MainDocumentConfig.forNodeType(NODE_TYPE_1).dataField("some").mappedToArgName(ARG_NAME_1).get());

    }

}
