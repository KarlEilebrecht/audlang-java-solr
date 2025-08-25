//@formatter:off
/*
 * ConfigUtilsTest
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

import static de.calamanari.adl.cnv.tps.DefaultAdlType.INTEGER;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.STRING;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_INTEGER;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_STRING;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.calamanari.adl.cnv.tps.ArgMetaInfo;
import de.calamanari.adl.cnv.tps.ArgMetaInfoLookup;
import de.calamanari.adl.cnv.tps.ConfigException;
import de.calamanari.adl.cnv.tps.DefaultAdlType;
import de.calamanari.adl.cnv.tps.DefaultArgMetaInfoLookup;
import de.calamanari.adl.solr.DefaultAdlSolrType;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class ConfigUtilsTest {

    @Test
    void testAssertValidArgName() {

        assertTrue(ConfigUtils.isValidArgName("a"));
        assertTrue(ConfigUtils.isValidArgName("a.b"));
        assertTrue(ConfigUtils.isValidArgName("*"));

        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidArgName(null));
        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidArgName(""));
        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidArgName("  "));
        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidArgName(" \n "));

    }

    @Test
    void testAssertValidSolrName() {

        assertTrue(ConfigUtils.isValidSolrName("a"));
        assertTrue(ConfigUtils.isValidSolrName("a_b"));
        assertTrue(ConfigUtils.isValidSolrName("A_B"));
        assertTrue(ConfigUtils.isValidSolrName("a5"));
        assertTrue(ConfigUtils.isValidSolrName("a_5"));

        assertFalse(ConfigUtils.isValidSolrName(null));
        assertFalse(ConfigUtils.isValidSolrName(""));
        assertFalse(ConfigUtils.isValidSolrName("  "));
        assertFalse(ConfigUtils.isValidSolrName(" n "));
        assertFalse(ConfigUtils.isValidSolrName("a name"));
        assertFalse(ConfigUtils.isValidSolrName("a.a"));
        assertFalse(ConfigUtils.isValidSolrName("_name"));
        assertFalse(ConfigUtils.isValidSolrName("name_"));
        assertFalse(ConfigUtils.isValidSolrName("_name_"));
        assertFalse(ConfigUtils.isValidSolrName("germanSZ_ß"));

        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidSolrName(null));
        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidSolrName(""));
        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidSolrName("  "));
        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidSolrName("a name"));
        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidSolrName("_name_"));

    }

    @Test
    void testValidateRequiredDocumentConfigFields() {

        ConfigUtils.validateRequiredDocumentConfigFields("node1", SolrDocumentNature.MAIN, null, null);
        assertThrows(ConfigException.class, () -> ConfigUtils.validateRequiredDocumentConfigFields(null, SolrDocumentNature.MAIN, null, null));
        assertThrows(ConfigException.class, () -> ConfigUtils.validateRequiredDocumentConfigFields("", SolrDocumentNature.MAIN, null, null));
        assertThrows(ConfigException.class, () -> ConfigUtils.validateRequiredDocumentConfigFields("_node", SolrDocumentNature.MAIN, null, null));
        assertThrows(ConfigException.class, () -> ConfigUtils.validateRequiredDocumentConfigFields("node1", null, null, null));

    }

    @Test
    void testValidateArgFieldMap() {

        Map<String, ArgFieldAssignment> argFieldMapEmpty = Collections.emptyMap();

        ConfigUtils.validateArgFieldMap("node1", SolrDocumentNature.MAIN, null, argFieldMapEmpty);

        Map<String, ArgFieldAssignment> argFieldMapGood = new HashMap<>();

        DataField field1 = new DataField("node1", "field1", DefaultAdlSolrType.SOLR_STRING, false);

        ArgMetaInfo metaInfo1 = new ArgMetaInfo("arg", DefaultAdlType.STRING, false, false);

        ArgFieldAssignment afa1 = new ArgFieldAssignment(metaInfo1, field1, false);

        argFieldMapGood.put("arg", afa1);

        ConfigUtils.validateArgFieldMap("node1", SolrDocumentNature.MAIN, null, argFieldMapEmpty);
        ConfigUtils.validateArgFieldMap("node1", SolrDocumentNature.MAIN, null, argFieldMapGood);

        Map<String, ArgFieldAssignment> argFieldMapBad = new HashMap<>();
        argFieldMapBad.put("argBad", afa1);

        assertThrows(ConfigException.class, () -> ConfigUtils.validateArgFieldMap("node1", SolrDocumentNature.MAIN, null, argFieldMapBad));

        assertThrows(ConfigException.class, () -> ConfigUtils.validateArgFieldMap("node2", SolrDocumentNature.MAIN, null, argFieldMapGood));

        assertThrows(ConfigException.class, () -> ConfigUtils.validateArgFieldMap("node1", SolrDocumentNature.MAIN, null, null));

        Map<String, ArgFieldAssignment> argFieldMap = new HashMap<>();

        argFieldMap.put(null, afa1);

        assertThrows(ConfigException.class, () -> ConfigUtils.validateArgFieldMap("node1", SolrDocumentNature.MAIN, null, argFieldMap));

        argFieldMap.clear();

        argFieldMap.put("arg", null);

        assertThrows(ConfigException.class, () -> ConfigUtils.validateArgFieldMap("node1", SolrDocumentNature.MAIN, null, argFieldMap));

        argFieldMap.clear();

        argFieldMap.put("", afa1);

        assertThrows(ConfigException.class, () -> ConfigUtils.validateArgFieldMap("node1", SolrDocumentNature.MAIN, null, argFieldMap));

    }

    @Test
    void testValidateDocumentFilters() {

        Map<String, ArgFieldAssignment> argFieldMapEmpty = Collections.emptyMap();

        List<FilterField> documentFilters = new ArrayList<>();

        ConfigUtils.validateDocumentFilters("node1", SolrDocumentNature.MAIN, null, argFieldMapEmpty);

        ConfigUtils.validateDocumentFilters("node1", null, documentFilters, argFieldMapEmpty);

        documentFilters.add(new FilterField("node1", "fieldName", DefaultAdlSolrType.SOLR_STRING, "value"));

        ConfigUtils.validateDocumentFilters("node1", null, documentFilters, argFieldMapEmpty);

        documentFilters.add(new FilterField("node1", "fieldName", DefaultAdlSolrType.SOLR_STRING, "value"));

        assertThrows(ConfigException.class, () -> ConfigUtils.validateDocumentFilters("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMapEmpty));

        documentFilters.remove(1);

        documentFilters.add(new FilterField("node1", "anotherfieldName", DefaultAdlSolrType.SOLR_STRING, "value"));

        ConfigUtils.validateDocumentFilters("node1", null, documentFilters, argFieldMapEmpty);

        documentFilters.add(new FilterField("node2", "thirdFieldName", DefaultAdlSolrType.SOLR_STRING, "value"));

        assertThrows(ConfigException.class, () -> ConfigUtils.validateDocumentFilters("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMapEmpty));

        documentFilters.remove(2);

        documentFilters.add(null);

        assertThrows(ConfigException.class, () -> ConfigUtils.validateDocumentFilters("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMapEmpty));

        documentFilters.remove(2);

        Map<String, ArgFieldAssignment> argFieldMapGood = new HashMap<>();

        DataField field1 = new DataField("node1", "fieldName", DefaultAdlSolrType.SOLR_STRING, false);

        ArgMetaInfo metaInfo1 = new ArgMetaInfo("arg", DefaultAdlType.STRING, false, false);

        ArgFieldAssignment afa1 = new ArgFieldAssignment(metaInfo1, field1, false);

        argFieldMapGood.put("arg", afa1);

        ConfigUtils.validateDocumentFilters("node1", null, documentFilters, argFieldMapEmpty);

        assertThrows(ConfigException.class, () -> ConfigUtils.validateDocumentFilters("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMapGood));

        documentFilters.clear();

        documentFilters.add(new FilterField("node1", "fieldName4", DefaultAdlSolrType.SOLR_STRING, "${argName}"));

        assertThrows(ConfigException.class, () -> ConfigUtils.validateDocumentFilters("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMapGood));

        documentFilters.clear();

        documentFilters.add(new FilterField("node1", "fieldName4", DefaultAdlSolrType.SOLR_STRING, "${tenant}"));

        ConfigUtils.validateDocumentFilters("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMapGood);

    }

    @Test
    void testValidateSubDocumentConfigs() {

        List<FilterField> documentFiltersEmpty = Collections.emptyList();
        Map<String, ArgFieldAssignment> argFieldMapEmpty = Collections.emptyMap();
        List<SubDocumentConfig> subDocumentConfigsEmpty = Collections.emptyList();

        Map<String, ArgFieldAssignment> argFieldMap = new HashMap<>();
        List<SubDocumentConfig> subDocumentConfigs = new ArrayList<>();

        DataField field1 = new DataField("node1", "fieldName", DefaultAdlSolrType.SOLR_STRING, false);

        ArgMetaInfo metaInfo1 = new ArgMetaInfo("arg", DefaultAdlType.STRING, false, false);

        ArgFieldAssignment afa1 = new ArgFieldAssignment(metaInfo1, field1, false);

        argFieldMap.put("arg", afa1);

        ConfigUtils.validateSubDocumentConfigs(null, null, null, null);
        ConfigUtils.validateSubDocumentConfigs("node1", documentFiltersEmpty, null, null);
        ConfigUtils.validateSubDocumentConfigs("node1", documentFiltersEmpty, argFieldMapEmpty, null);
        ConfigUtils.validateSubDocumentConfigs("node1", documentFiltersEmpty, argFieldMapEmpty, subDocumentConfigsEmpty);

        subDocumentConfigs.add(null);

        assertThrows(ConfigException.class, () -> ConfigUtils.validateSubDocumentConfigs("node1", documentFiltersEmpty, argFieldMapEmpty, subDocumentConfigs));

        subDocumentConfigs.clear();

        // @formatter:off
        SubDocumentConfig sdc1 = SubDocumentConfig.forNodeType("node2")
                                                     .nested()
                                                     .filteredBy("filter1", DefaultAdlSolrType.SOLR_STRING, "val")
                                                     .dataField("fieldName", DefaultAdlSolrType.SOLR_STRING)
                                                         .mappedToArgName("arg", DefaultAdlType.STRING)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.add(sdc1);

        ConfigUtils.validateSubDocumentConfigs("node1", documentFiltersEmpty, argFieldMapEmpty, subDocumentConfigs);

        // @formatter:off
        SubDocumentConfig sdcWrongNode = SubDocumentConfig.forNodeType("node1")
                                                              .nested()
                                                              .filteredBy("filter1", DefaultAdlSolrType.SOLR_STRING, "val")
                                                              .dataField("fieldName", DefaultAdlSolrType.SOLR_STRING)
                                                              .mappedToArgName("arg", DefaultAdlType.STRING)
                                                          .get();
        // @formatter:on

        subDocumentConfigs.add(sdcWrongNode);
        assertThrows(ConfigException.class, () -> ConfigUtils.validateSubDocumentConfigs("node1", documentFiltersEmpty, argFieldMapEmpty, subDocumentConfigs));

        subDocumentConfigs.clear();

        subDocumentConfigs.add(sdc1);
        subDocumentConfigs.add(sdc1);

        assertThrows(ConfigException.class, () -> ConfigUtils.validateSubDocumentConfigs("node1", documentFiltersEmpty, argFieldMapEmpty, subDocumentConfigs));

        subDocumentConfigs.clear();

        subDocumentConfigs.add(sdc1);

        assertThrows(ConfigException.class, () -> ConfigUtils.validateSubDocumentConfigs("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs));

    }

    @Test
    void testValidateUniqueFieldTypes() {

        List<FilterField> documentFiltersEmpty = Collections.emptyList();
        Map<String, ArgFieldAssignment> argFieldMapEmpty = Collections.emptyMap();
        List<SubDocumentConfig> subDocumentConfigsEmpty = Collections.emptyList();

        List<FilterField> documentFilters = new ArrayList<>();
        Map<String, ArgFieldAssignment> argFieldMap = new HashMap<>();
        List<SubDocumentConfig> subDocumentConfigs = new ArrayList<>();

        DataField field1 = new DataField("node1", "someFieldName", DefaultAdlSolrType.SOLR_STRING, false);

        ArgMetaInfo metaInfo1 = new ArgMetaInfo("arg", DefaultAdlType.STRING, false, false);

        ArgFieldAssignment afa1 = new ArgFieldAssignment(metaInfo1, field1, false);

        argFieldMap.put("arg", afa1);

        ConfigUtils.validateUniqueFieldTypes(null, null, null, null, null);
        ConfigUtils.validateUniqueFieldTypes("node1", null, null, null, null);
        ConfigUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, null, null, null);
        ConfigUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFiltersEmpty, null, null);
        ConfigUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFiltersEmpty, argFieldMapEmpty, null);
        ConfigUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFiltersEmpty, argFieldMapEmpty, subDocumentConfigsEmpty);

        documentFilters.add(new FilterField("node1", "filter1", DefaultAdlSolrType.SOLR_STRING, "mainValue"));

        ConfigUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigsEmpty);

        // @formatter:off
        SubDocumentConfig sdc1 = SubDocumentConfig.forNodeType("node2")
                                                     .nested()
                                                     .filteredBy("filter1", DefaultAdlSolrType.SOLR_STRING, "val")
                                                     .filteredBy("filter2", DefaultAdlSolrType.SOLR_STRING, "val2")
                                                     .dataField("someFieldName", DefaultAdlSolrType.SOLR_STRING)
                                                         .mappedToArgName("arg2", DefaultAdlType.STRING)
                                                     .dataField("otherFieldName", DefaultAdlSolrType.SOLR_STRING)
                                                         .mappedToArgName("arg3", DefaultAdlType.STRING)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.add(sdc1);

        ConfigUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigs);

        // @formatter:off
        SubDocumentConfig sdc2 = SubDocumentConfig.forNodeType("node2")
                                                     .nested()
                                                     .filteredBy("filter1", DefaultAdlSolrType.SOLR_BOOLEAN, "1")
                                                     .filteredBy("filter2", DefaultAdlSolrType.SOLR_STRING, "val2")
                                                     .dataField("someFieldName", DefaultAdlSolrType.SOLR_STRING)
                                                         .mappedToArgName("arg2", DefaultAdlType.STRING)
                                                     .dataField("otherFieldName", DefaultAdlSolrType.SOLR_STRING)
                                                         .mappedToArgName("arg3", DefaultAdlType.STRING)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.clear();
        subDocumentConfigs.add(sdc2);

        assertThrows(ConfigException.class,
                () -> ConfigUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigs));

        // @formatter:off
        SubDocumentConfig sdc3 = SubDocumentConfig.forNodeType("node2")
                                                     .nested()
                                                     .filteredBy("someFieldName", DefaultAdlSolrType.SOLR_BOOLEAN, "1")
                                                     .filteredBy("filter2", DefaultAdlSolrType.SOLR_STRING, "val2")
                                                     .dataField("otherFieldName", DefaultAdlSolrType.SOLR_STRING)
                                                         .mappedToArgName("arg3", DefaultAdlType.STRING)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.clear();
        subDocumentConfigs.add(sdc3);

        assertThrows(ConfigException.class,
                () -> ConfigUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigs));

        // @formatter:off
        SubDocumentConfig sdc4 = SubDocumentConfig.forNodeType("node2")
                                                     .nested()
                                                     .filteredBy("filter1", DefaultAdlSolrType.SOLR_STRING, "val")
                                                     .filteredBy("filter2", DefaultAdlSolrType.SOLR_STRING, "val2")
                                                     .dataField("someFieldName", DefaultAdlSolrType.SOLR_BOOLEAN)
                                                         .mappedToArgName("arg2", DefaultAdlType.BOOL)
                                                     .dataField("otherFieldName", DefaultAdlSolrType.SOLR_STRING)
                                                         .mappedToArgName("arg3", DefaultAdlType.STRING)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.clear();
        subDocumentConfigs.add(sdc4);

        assertThrows(ConfigException.class,
                () -> ConfigUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigs));

        // @formatter:off
        SubDocumentConfig sdc5 = SubDocumentConfig.forNodeType("node2")
                                                     .nested()
                                                     .filteredBy("filter2", DefaultAdlSolrType.SOLR_STRING, "val2")
                                                     .dataField("filter1", DefaultAdlSolrType.SOLR_BOOLEAN)
                                                         .mappedToArgName("arg2", DefaultAdlType.BOOL)
                                                     .dataField("otherFieldName", DefaultAdlSolrType.SOLR_STRING)
                                                         .mappedToArgName("arg3", DefaultAdlType.STRING)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.clear();
        subDocumentConfigs.add(sdc5);

        assertThrows(ConfigException.class,
                () -> ConfigUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigs));

        // @formatter:off
        SubDocumentConfig sdc6 = SubDocumentConfig.forNodeType("node2")
                                                     .nested()
                                                     .filteredBy("filter1", DefaultAdlSolrType.SOLR_STRING, "val")
                                                     .filteredBy("filter2", DefaultAdlSolrType.SOLR_STRING, "val2")
                                                     .dataField("someFieldName", DefaultAdlSolrType.SOLR_STRING)
                                                         .mappedToArgName("arg2", DefaultAdlType.STRING)
                                                     .dataField("otherFieldName", DefaultAdlSolrType.SOLR_STRING)
                                                         .mappedToArgName("arg3", DefaultAdlType.STRING)
                                                  .get();
        // @formatter:on

        // @formatter:off
        SubDocumentConfig sdc7 = SubDocumentConfig.forNodeType("node3")
                                                     .nested()
                                                     .filteredBy("filter1", DefaultAdlSolrType.SOLR_STRING, "val")
                                                     .filteredBy("filter2", DefaultAdlSolrType.SOLR_BOOLEAN, "1")
                                                     .dataField("someFieldName", DefaultAdlSolrType.SOLR_STRING)
                                                         .mappedToArgName("arg2", DefaultAdlType.STRING)
                                                     .dataField("otherFieldName", DefaultAdlSolrType.SOLR_STRING)
                                                         .mappedToArgName("arg3", DefaultAdlType.STRING)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.clear();
        subDocumentConfigs.add(sdc6);
        subDocumentConfigs.add(sdc7);

        assertThrows(ConfigException.class,
                () -> ConfigUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigs));

        // @formatter:off
        SubDocumentConfig sdc8 = SubDocumentConfig.forNodeType("node2")
                                                     .nested()
                                                     .filteredBy("filter1", DefaultAdlSolrType.SOLR_STRING, "val")
                                                     .filteredBy("filter2", DefaultAdlSolrType.SOLR_STRING, "val2")
                                                     .dataField("someFieldName", DefaultAdlSolrType.SOLR_STRING)
                                                         .mappedToArgName("arg2", DefaultAdlType.STRING)
                                                     .dataField("otherFieldName", DefaultAdlSolrType.SOLR_BOOLEAN)
                                                         .mappedToArgName("arg3", DefaultAdlType.BOOL)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.clear();
        subDocumentConfigs.add(sdc6);
        subDocumentConfigs.add(sdc8);

        assertThrows(ConfigException.class,
                () -> ConfigUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigs));

    }

    @Test
    void testValidateNoStaticFieldStealing() {

        List<FilterField> documentFiltersEmpty = Collections.emptyList();
        Map<String, ArgFieldAssignment> argFieldMapEmpty = Collections.emptyMap();
        List<SubDocumentConfig> subDocumentConfigsEmpty = Collections.emptyList();

        Map<String, ArgFieldAssignment> argFieldMap = new HashMap<>();
        List<SubDocumentConfig> subDocumentConfigs = new ArrayList<>();

        DataField field1 = new DataField("node1", "fooField_s", DefaultAdlSolrType.SOLR_STRING, false);

        ArgMetaInfo metaInfo1 = new ArgMetaInfo("foo_s", DefaultAdlType.STRING, false, false);

        ArgFieldAssignment afa1 = new ArgFieldAssignment(metaInfo1, field1, false);

        argFieldMap.put("foo_s", afa1);

        ConfigUtils.validateNoStaticFieldStealing(null, null, null, null);
        ConfigUtils.validateNoStaticFieldStealing("node1", null, null, null);
        ConfigUtils.validateNoStaticFieldStealing("node1", null, null, null);
        ConfigUtils.validateNoStaticFieldStealing("node1", documentFiltersEmpty, null, null);
        ConfigUtils.validateNoStaticFieldStealing("node1", documentFiltersEmpty, argFieldMapEmpty, null);
        ConfigUtils.validateNoStaticFieldStealing("node1", documentFiltersEmpty, argFieldMapEmpty, subDocumentConfigsEmpty);

        // @formatter:off
        SubDocumentConfig sdc1 = SubDocumentConfig.forNodeType("node2")
                                                     .nested()
                                                     .dataField("someFieldName", DefaultAdlSolrType.SOLR_STRING)
                                                         .mappedToArgName("arg2", DefaultAdlType.STRING)
                                                     .dataField("otherFieldName", DefaultAdlSolrType.SOLR_STRING)
                                                         .mappedToArgName("arg3", DefaultAdlType.STRING)
                                                     .autoMapped(argName -> argName.startsWith("var.") ? argName.substring(4) : null)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.add(sdc1);

        ConfigUtils.validateNoStaticFieldStealing("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs);

        // @formatter:off
        SubDocumentConfig sdc2 = SubDocumentConfig.forNodeType("node3")
                                                     .nested()
                                                     .autoMapped(argName -> argName.startsWith("var.") ? argName.substring(4) : null)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.clear();
        subDocumentConfigs.add(sdc1);
        subDocumentConfigs.add(sdc2);

        ConfigUtils.validateNoStaticFieldStealing("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs);

        // @formatter:off
        SubDocumentConfig sdc3 = SubDocumentConfig.forNodeType("node4")
                                                     .nested()
                                                     .autoMapped(argName -> argName.startsWith("f") ? argName : null)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.clear();
        subDocumentConfigs.add(sdc1);
        subDocumentConfigs.add(sdc2);
        subDocumentConfigs.add(sdc3);

        assertThrows(ConfigException.class, () -> ConfigUtils.validateNoStaticFieldStealing("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs));

        // @formatter:off
        SubDocumentConfig sdc4 = SubDocumentConfig.forNodeType("node5")
                                                     .nested()
                                                     .dataField("anotherField", DefaultAdlSolrType.SOLR_STRING)
                                                         .mappedToArgName("var.x_s", DefaultAdlType.STRING)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.clear();
        subDocumentConfigs.add(sdc1);
        subDocumentConfigs.add(sdc2);
        subDocumentConfigs.add(sdc4);

        assertThrows(ConfigException.class, () -> ConfigUtils.validateNoStaticFieldStealing("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs));

    }

    @Test
    void testValidateGuardianLookups() {

        // @formatter:off
        ArgMetaInfoLookup guardianLookup = DefaultArgMetaInfoLookup
            .withArg("foo").ofType(STRING)
                .thatIsAlwaysKnown()
            .withArg("int.count").ofType(INTEGER)
            .get();
                
        // @formatter:on

        // @formatter:off
        ArgMetaInfoLookup guardianLookup2 = DefaultArgMetaInfoLookup
            .withArg("bar").ofType(STRING)
                .thatIsAlwaysKnown()
            .withArg("int.count").ofType(INTEGER)
            .get();
                
        // @formatter:on

        List<FilterField> documentFiltersEmpty = Collections.emptyList();
        Map<String, ArgFieldAssignment> argFieldMapEmpty = Collections.emptyMap();
        List<SubDocumentConfig> subDocumentConfigsEmpty = Collections.emptyList();

        Map<String, ArgFieldAssignment> argFieldMap = new HashMap<>();
        List<SubDocumentConfig> subDocumentConfigs = new ArrayList<>();

        DataField field1 = new DataField("node1", "fooField_s", SOLR_STRING, false);

        ArgMetaInfo metaInfo1 = new ArgMetaInfo("foo", STRING, false, false);

        ArgFieldAssignment afa1 = new ArgFieldAssignment(metaInfo1, field1, false);

        argFieldMap.put("foo_s", afa1);

        ConfigUtils.validateGuardianLookups("node1", documentFiltersEmpty, argFieldMapEmpty, subDocumentConfigsEmpty, guardianLookup);

        // @formatter:off
        SubDocumentConfig sdc1 = SubDocumentConfig.forNodeType("node2", guardianLookup)
                                                     .nested()
                                                     .dataField("int_count", SOLR_INTEGER)
                                                         .mappedToArgName("int.count", INTEGER)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.add(sdc1);

        ConfigUtils.validateGuardianLookups("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs, guardianLookup);

        assertThrows(ConfigException.class, () -> ConfigUtils.validateGuardianLookups("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs, null));

        // @formatter:off
        SubDocumentConfig sdc2 = SubDocumentConfig.forNodeType("node2", guardianLookup2)
                                                     .nested()
                                                     .dataField("int_count", SOLR_INTEGER)
                                                         .mappedToArgName("int.count", INTEGER)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.clear();
        subDocumentConfigs.add(sdc2);

        assertThrows(ConfigException.class,
                () -> ConfigUtils.validateGuardianLookups("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs, guardianLookup));

        // @formatter:off
        SubDocumentConfig sdc3 = SubDocumentConfig.forNodeType("node2")
                                                     .nested()
                                                     .dataField("int_count", SOLR_INTEGER)
                                                         .mappedToArgName("int.count", INTEGER)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.clear();
        subDocumentConfigs.add(sdc3);

        // no guardian lookup at all
        ConfigUtils.validateGuardianLookups("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs, null);

        // this will probably be the most common: having a guardian lookup at the main config and none at the sub config

        assertThrows(ConfigException.class,
                () -> ConfigUtils.validateGuardianLookups("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs, guardianLookup));

    }

    @Test
    void testIsSolrLetterOrDigit() {

        String validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; i++) {
            char ch = (char) i;
            if (validChars.indexOf(ch) > -1) {
                assertTrue(ConfigUtils.isSolrLetterOrDigit(ch));
            }
            else {
                assertFalse(ConfigUtils.isSolrLetterOrDigit(ch));
            }
        }

    }

}
