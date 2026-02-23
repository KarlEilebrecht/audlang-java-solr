//@formatter:off
/*
 * ConfigValidationUtilsTest
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

import static de.calamanari.adl.cnv.tps.DefaultAdlType.INTEGER;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.STRING;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_INTEGER;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_STRING;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class ConfigValidationUtilsTest {

    @Test
    void testValidateRequiredDocumentConfigFields() {

        ConfigValidationUtils.validateRequiredDocumentConfigFields("node1", SolrDocumentNature.MAIN, null, null);
        assertThrows(ConfigException.class, () -> ConfigValidationUtils.validateRequiredDocumentConfigFields(null, SolrDocumentNature.MAIN, null, null));
        assertThrows(ConfigException.class, () -> ConfigValidationUtils.validateRequiredDocumentConfigFields("", SolrDocumentNature.MAIN, null, null));
        assertThrows(ConfigException.class, () -> ConfigValidationUtils.validateRequiredDocumentConfigFields("_node", SolrDocumentNature.MAIN, null, null));
        assertThrows(ConfigException.class, () -> ConfigValidationUtils.validateRequiredDocumentConfigFields("node1", null, null, null));

    }

    @Test
    void testValidateArgFieldMap() {

        Map<String, ArgFieldAssignment> argFieldMapEmpty = Collections.emptyMap();

        ConfigValidationUtils.validateArgFieldMap("node1", SolrDocumentNature.MAIN, null, argFieldMapEmpty);

        Map<String, ArgFieldAssignment> argFieldMapGood = new HashMap<>();

        DataField field1 = new DataField("node1", "field1", DefaultAdlSolrType.SOLR_STRING, false);

        ArgMetaInfo metaInfo1 = new ArgMetaInfo("arg", DefaultAdlType.STRING, false, false);

        ArgFieldAssignment afa1 = new ArgFieldAssignment(metaInfo1, field1, false);

        argFieldMapGood.put("arg", afa1);

        ConfigValidationUtils.validateArgFieldMap("node1", SolrDocumentNature.MAIN, null, argFieldMapEmpty);
        ConfigValidationUtils.validateArgFieldMap("node1", SolrDocumentNature.MAIN, null, argFieldMapGood);

        Map<String, ArgFieldAssignment> argFieldMapBad = new HashMap<>();
        argFieldMapBad.put("argBad", afa1);

        assertThrows(ConfigException.class, () -> ConfigValidationUtils.validateArgFieldMap("node1", SolrDocumentNature.MAIN, null, argFieldMapBad));

        assertThrows(ConfigException.class, () -> ConfigValidationUtils.validateArgFieldMap("node2", SolrDocumentNature.MAIN, null, argFieldMapGood));

        assertThrows(ConfigException.class, () -> ConfigValidationUtils.validateArgFieldMap("node1", SolrDocumentNature.MAIN, null, null));

        Map<String, ArgFieldAssignment> argFieldMap = new HashMap<>();

        argFieldMap.put(null, afa1);

        assertThrows(ConfigException.class, () -> ConfigValidationUtils.validateArgFieldMap("node1", SolrDocumentNature.MAIN, null, argFieldMap));

        argFieldMap.clear();

        argFieldMap.put("arg", null);

        assertThrows(ConfigException.class, () -> ConfigValidationUtils.validateArgFieldMap("node1", SolrDocumentNature.MAIN, null, argFieldMap));

        argFieldMap.clear();

        argFieldMap.put("", afa1);

        assertThrows(ConfigException.class, () -> ConfigValidationUtils.validateArgFieldMap("node1", SolrDocumentNature.MAIN, null, argFieldMap));

    }

    @Test
    void testValidateDocumentFilters() {

        Map<String, ArgFieldAssignment> argFieldMapEmpty = Collections.emptyMap();

        List<FilterField> documentFilters = new ArrayList<>();

        ConfigValidationUtils.validateDocumentFilters("node1", SolrDocumentNature.MAIN, null, argFieldMapEmpty);

        ConfigValidationUtils.validateDocumentFilters("node1", null, documentFilters, argFieldMapEmpty);

        documentFilters.add(new FilterField("node1", "fieldName", DefaultAdlSolrType.SOLR_STRING, "value"));

        ConfigValidationUtils.validateDocumentFilters("node1", null, documentFilters, argFieldMapEmpty);

        documentFilters.add(new FilterField("node1", "fieldName", DefaultAdlSolrType.SOLR_STRING, "value"));

        assertThrows(ConfigException.class,
                () -> ConfigValidationUtils.validateDocumentFilters("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMapEmpty));

        documentFilters.remove(1);

        documentFilters.add(new FilterField("node1", "anotherfieldName", DefaultAdlSolrType.SOLR_STRING, "value"));

        ConfigValidationUtils.validateDocumentFilters("node1", null, documentFilters, argFieldMapEmpty);

        documentFilters.add(new FilterField("node2", "thirdFieldName", DefaultAdlSolrType.SOLR_STRING, "value"));

        assertThrows(ConfigException.class,
                () -> ConfigValidationUtils.validateDocumentFilters("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMapEmpty));

        documentFilters.remove(2);

        documentFilters.add(null);

        assertThrows(ConfigException.class,
                () -> ConfigValidationUtils.validateDocumentFilters("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMapEmpty));

        documentFilters.remove(2);

        Map<String, ArgFieldAssignment> argFieldMapGood = new HashMap<>();

        DataField field1 = new DataField("node1", "fieldName", DefaultAdlSolrType.SOLR_STRING, false);

        ArgMetaInfo metaInfo1 = new ArgMetaInfo("arg", DefaultAdlType.STRING, false, false);

        ArgFieldAssignment afa1 = new ArgFieldAssignment(metaInfo1, field1, false);

        argFieldMapGood.put("arg", afa1);

        ConfigValidationUtils.validateDocumentFilters("node1", null, documentFilters, argFieldMapEmpty);

        assertThrows(ConfigException.class,
                () -> ConfigValidationUtils.validateDocumentFilters("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMapGood));

        documentFilters.clear();

        documentFilters.add(new FilterField("node1", "fieldName4", DefaultAdlSolrType.SOLR_STRING, "${argName}"));

        assertThrows(ConfigException.class,
                () -> ConfigValidationUtils.validateDocumentFilters("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMapGood));

        documentFilters.clear();

        documentFilters.add(new FilterField("node1", "fieldName4", DefaultAdlSolrType.SOLR_STRING, "${tenant}"));

        ConfigValidationUtils.validateDocumentFilters("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMapGood);

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

        ConfigValidationUtils.validateSubDocumentConfigs(null, null, null, null);
        ConfigValidationUtils.validateSubDocumentConfigs("node1", documentFiltersEmpty, null, null);
        ConfigValidationUtils.validateSubDocumentConfigs("node1", documentFiltersEmpty, argFieldMapEmpty, null);
        ConfigValidationUtils.validateSubDocumentConfigs("node1", documentFiltersEmpty, argFieldMapEmpty, subDocumentConfigsEmpty);

        subDocumentConfigs.add(null);

        assertThrows(ConfigException.class,
                () -> ConfigValidationUtils.validateSubDocumentConfigs("node1", documentFiltersEmpty, argFieldMapEmpty, subDocumentConfigs));

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

        ConfigValidationUtils.validateSubDocumentConfigs("node1", documentFiltersEmpty, argFieldMapEmpty, subDocumentConfigs);

        // @formatter:off
        SubDocumentConfig sdcWrongNode = SubDocumentConfig.forNodeType("node1")
                                                              .nested()
                                                              .filteredBy("filter1", DefaultAdlSolrType.SOLR_STRING, "val")
                                                              .dataField("fieldName", DefaultAdlSolrType.SOLR_STRING)
                                                              .mappedToArgName("arg", DefaultAdlType.STRING)
                                                          .get();
        // @formatter:on

        subDocumentConfigs.add(sdcWrongNode);
        assertThrows(ConfigException.class,
                () -> ConfigValidationUtils.validateSubDocumentConfigs("node1", documentFiltersEmpty, argFieldMapEmpty, subDocumentConfigs));

        subDocumentConfigs.clear();

        subDocumentConfigs.add(sdc1);
        subDocumentConfigs.add(sdc1);

        assertThrows(ConfigException.class,
                () -> ConfigValidationUtils.validateSubDocumentConfigs("node1", documentFiltersEmpty, argFieldMapEmpty, subDocumentConfigs));

        subDocumentConfigs.clear();

        subDocumentConfigs.add(sdc1);

        assertThrows(ConfigException.class,
                () -> ConfigValidationUtils.validateSubDocumentConfigs("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs));

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

        ConfigValidationUtils.validateUniqueFieldTypes(null, null, null, null, null);
        ConfigValidationUtils.validateUniqueFieldTypes("node1", null, null, null, null);
        ConfigValidationUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, null, null, null);
        ConfigValidationUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFiltersEmpty, null, null);
        ConfigValidationUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFiltersEmpty, argFieldMapEmpty, null);
        ConfigValidationUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFiltersEmpty, argFieldMapEmpty, subDocumentConfigsEmpty);

        documentFilters.add(new FilterField("node1", "filter1", DefaultAdlSolrType.SOLR_STRING, "mainValue"));

        ConfigValidationUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigsEmpty);

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

        ConfigValidationUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigs);

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
                () -> ConfigValidationUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigs));

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
                () -> ConfigValidationUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigs));

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
                () -> ConfigValidationUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigs));

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
                () -> ConfigValidationUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigs));

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
                () -> ConfigValidationUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigs));

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
                () -> ConfigValidationUtils.validateUniqueFieldTypes("node1", SolrDocumentNature.MAIN, documentFilters, argFieldMap, subDocumentConfigs));

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

        ConfigValidationUtils.validateNoStaticFieldStealing(null, null, null, null);
        ConfigValidationUtils.validateNoStaticFieldStealing("node1", null, null, null);
        ConfigValidationUtils.validateNoStaticFieldStealing("node1", null, null, null);
        ConfigValidationUtils.validateNoStaticFieldStealing("node1", documentFiltersEmpty, null, null);
        ConfigValidationUtils.validateNoStaticFieldStealing("node1", documentFiltersEmpty, argFieldMapEmpty, null);
        ConfigValidationUtils.validateNoStaticFieldStealing("node1", documentFiltersEmpty, argFieldMapEmpty, subDocumentConfigsEmpty);

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

        ConfigValidationUtils.validateNoStaticFieldStealing("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs);

        // @formatter:off
        SubDocumentConfig sdc2 = SubDocumentConfig.forNodeType("node3")
                                                     .nested()
                                                     .autoMapped(argName -> argName.startsWith("var.") ? argName.substring(4) : null)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.clear();
        subDocumentConfigs.add(sdc1);
        subDocumentConfigs.add(sdc2);

        ConfigValidationUtils.validateNoStaticFieldStealing("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs);

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

        assertThrows(ConfigException.class,
                () -> ConfigValidationUtils.validateNoStaticFieldStealing("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs));

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

        assertThrows(ConfigException.class,
                () -> ConfigValidationUtils.validateNoStaticFieldStealing("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs));

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

        ConfigValidationUtils.validateGuardianLookups("node1", documentFiltersEmpty, argFieldMapEmpty, subDocumentConfigsEmpty, guardianLookup);

        // @formatter:off
        SubDocumentConfig sdc1 = SubDocumentConfig.forNodeType("node2", guardianLookup)
                                                     .nested()
                                                     .dataField("int_count", SOLR_INTEGER)
                                                         .mappedToArgName("int.count", INTEGER)
                                                  .get();
        // @formatter:on

        subDocumentConfigs.add(sdc1);

        ConfigValidationUtils.validateGuardianLookups("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs, guardianLookup);

        assertThrows(ConfigException.class,
                () -> ConfigValidationUtils.validateGuardianLookups("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs, null));

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
                () -> ConfigValidationUtils.validateGuardianLookups("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs, guardianLookup));

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
        ConfigValidationUtils.validateGuardianLookups("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs, null);

        // this will probably be the most common: having a guardian lookup at the main config and none at the sub config

        assertThrows(ConfigException.class,
                () -> ConfigValidationUtils.validateGuardianLookups("node1", documentFiltersEmpty, argFieldMap, subDocumentConfigs, guardianLookup));

    }

}
