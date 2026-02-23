//@formatter:off
/*
 * DefaultAutoMappingPolicyTest
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
import de.calamanari.adl.ProcessContext;
import de.calamanari.adl.cnv.tps.AdlType;
import de.calamanari.adl.cnv.tps.ArgMetaInfoLookup;
import de.calamanari.adl.cnv.tps.ConfigException;
import de.calamanari.adl.cnv.tps.DefaultArgMetaInfoLookup;
import de.calamanari.adl.solr.AdlSolrType;
import de.calamanari.adl.solr.SolrFormatConstants;
import de.calamanari.adl.solr.SolrTestBase;

import static de.calamanari.adl.cnv.tps.DefaultAdlType.BOOL;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.DATE;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.INTEGER;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.STRING;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_INTEGER;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_STRING;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class DefaultAutoMappingPolicyTest extends SolrTestBase {

    @Test
    void testBasics() {

        AutoMappingPolicy mapAllUsingConventionsPolicy = new DefaultAutoMappingPolicy(NODE_TYPE_1, null, null, null, null, false, null);

        for (String suffix : SolrFormatConstants.SOLR_STANDARD_SUFFIXES) {
            String argName = "arg" + suffix;

            assertTrue(mapAllUsingConventionsPolicy.isApplicable(argName));
            assertEquals(argName, mapAllUsingConventionsPolicy.map(argName, ProcessContext.empty()).arg().argName());
            assertEquals(ConventionUtils.determineGenericArgType(argName), mapAllUsingConventionsPolicy.map(argName, ProcessContext.empty()).arg().type());
            assertEquals(argName, mapAllUsingConventionsPolicy.map(argName, ProcessContext.empty()).field().fieldName());
            assertEquals(ConventionUtils.determineGenericSolrFieldType(argName),
                    mapAllUsingConventionsPolicy.map(argName, ProcessContext.empty()).field().fieldType());
            assertEquals(ConventionUtils.determineGenericIsCollection(argName),
                    mapAllUsingConventionsPolicy.map(argName, ProcessContext.empty()).field().isCollection());
            assertEquals(ConventionUtils.determineGenericIsCollection(argName),
                    mapAllUsingConventionsPolicy.map(argName, ProcessContext.empty()).arg().isCollection());
        }

        assertFalse(mapAllUsingConventionsPolicy.isApplicable(ARG_NAME_1));
        ProcessContext ctxEmpty = ProcessContext.empty();
        assertThrows(ConfigException.class, () -> mapAllUsingConventionsPolicy.map(ARG_NAME_1, ctxEmpty));

        for (String badName : BAD_SOLR_NAME_EXAMPLES) {
            assertThrows(IllegalArgumentException.class, () -> new DefaultAutoMappingPolicy(badName, null, null, null, null, false, null));
            assertThrows(IllegalArgumentException.class, () -> new DefaultAutoMappingPolicy(badName, null, null, null, null, true, null));
        }

    }

    @Test
    void testExtractApplyDefaults() {

        AutoMappingPolicy policy = new DefaultAutoMappingPolicy(NODE_TYPE_1, argName -> argName + "_s", null, null, null, false, null);

        assertFalse(policy.isApplicable(""));
        assertFalse(policy.isApplicable(null));

        assertMap("a", STRING, "a_s", SOLR_STRING, false, false, policy, "a");
        assertMap(ARG_NAME_1, STRING, ARG_NAME_1 + "_s", SOLR_STRING, false, false, policy, ARG_NAME_1);

        policy = new DefaultAutoMappingPolicy(NODE_TYPE_1, argName -> argName.startsWith("ints.") ? argName.substring(5) + "_is" : argName + "_s", null, null,
                null, false, null);

        assertMap("a", STRING, "a_s", SOLR_STRING, false, false, policy, "a");
        assertMap(ARG_NAME_1, STRING, ARG_NAME_1 + "_s", SOLR_STRING, false, false, policy, ARG_NAME_1);

        assertMap("ints.a", INTEGER, "a_is", SOLR_INTEGER, true, false, policy, "ints.a");
        assertMap("ints." + ARG_NAME_1, INTEGER, ARG_NAME_1 + "_is", SOLR_INTEGER, true, false, policy, "ints." + ARG_NAME_1);

    }

    @Test
    void testExtractBadSolrName() {

        AutoMappingPolicy policy = new DefaultAutoMappingPolicy(NODE_TYPE_1, argName -> argName + "_s", null, null, null, false, null);

        assertTrue(policy.isApplicable("number"));

        assertFalse(policy.isApplicable("my.number"));

    }

    @Test
    void testExtractApplyOverride() {

        // @formatter:off
        AutoMappingPolicy policy = new DefaultAutoMappingPolicy(NODE_TYPE_1, argName -> argName.startsWith("ints.") ? argName.substring(5) + "_is" : argName + "_s", 
                STRING, 
                null,
                null,
                false,
                null);
        // @formatter:on

        assertMap(ARG_NAME_1, STRING, ARG_NAME_1 + "_s", SOLR_STRING, false, false, policy, ARG_NAME_1);
        assertMap("ints." + ARG_NAME_1, STRING, ARG_NAME_1 + "_is", SOLR_INTEGER, true, false, policy, "ints." + ARG_NAME_1);

        // @formatter:off
        policy = new DefaultAutoMappingPolicy(NODE_TYPE_1, argName -> argName.startsWith("ints.") ? argName.substring(5) + "_is" : argName + "_s", 
                null, 
                SOLR_STRING,
                null,
                true,
                null);
        // @formatter:on

        assertMap(ARG_NAME_1, STRING, ARG_NAME_1 + "_s", SOLR_STRING, false, true, policy, ARG_NAME_1);
        assertMap("ints." + ARG_NAME_1, INTEGER, ARG_NAME_1 + "_is", SOLR_STRING, true, true, policy, "ints." + ARG_NAME_1);

        // @formatter:off
        policy = new DefaultAutoMappingPolicy(NODE_TYPE_1, argName -> argName.startsWith("ints.") ? argName.substring(5) + "_is" : argName + "_s", 
                null, 
                null,
                false,
                false,
                null);
        // @formatter:on

        assertMap(ARG_NAME_1, STRING, ARG_NAME_1 + "_s", SOLR_STRING, false, false, policy, ARG_NAME_1);
        assertMap("ints." + ARG_NAME_1, INTEGER, ARG_NAME_1 + "_is", SOLR_INTEGER, false, false, policy, "ints." + ARG_NAME_1);

        // @formatter:off
        ArgMetaInfoLookup logicalDataModel = DefaultArgMetaInfoLookup
            .withArg(ARG_NAME_1).ofType(DATE)
                .thatIsAlwaysKnown()
            .withArg(ARG_NAME_2).ofType(INTEGER)
            .withArg(ARG_NAME_3).ofType(BOOL)
            .get();
                
        // @formatter:on

        // @formatter:off
        assertThrows(ConfigException.class, () -> new DefaultAutoMappingPolicy(NODE_TYPE_1, argName -> argName.startsWith("ints.") ? argName.substring(5) + "_is" : argName + "_s", 
                STRING, 
                null,
                null,
                false,
                logicalDataModel));
        // @formatter:on

    }

    @Test
    void testNone() {

        AutoMappingPolicy policy1 = DefaultAutoMappingPolicy.NONE;

        assertFalse(policy1.isApplicable(null));
        assertFalse(policy1.isApplicable(""));
        assertFalse(policy1.isApplicable("good"));

        ProcessContext ctxEmpty = ProcessContext.empty();
        assertThrows(ConfigException.class, () -> policy1.map(null, ctxEmpty));
        assertThrows(ConfigException.class, () -> policy1.map("good", ctxEmpty));

        AutoMappingPolicy policy2 = DeepCopyUtils.deepCopy(policy1);

        assertSame(DefaultAutoMappingPolicy.NONE, policy2);
    }

    @Test
    void testBadSubClassing() {

        AutoMappingPolicy policy = new DefaultAutoMappingPolicy(NODE_TYPE_1, null, null, null, null, false, null) {

            private static final long serialVersionUID = 7555324488896184645L;

            @Override
            public boolean isApplicable(String argName) {
                return true;
            }

            @Override
            protected AdlType determineArgType(String argName, String argNameLocal) {
                return argName.startsWith("expectBadArgType_") ? null : STRING;
            }

            @Override
            protected AdlSolrType determineSolrFieldType(String argName, String argNameLocal) {
                return argName.startsWith("expectBadFieldType_") ? null : SOLR_STRING;
            }

        };

        assertMap(ARG_NAME_1 + "_s", STRING, ARG_NAME_1 + "_s", SOLR_STRING, false, false, policy, ARG_NAME_1 + "_s");
        assertMap(ARG_NAME_1 + "_ss", STRING, ARG_NAME_1 + "_ss", SOLR_STRING, true, false, policy, ARG_NAME_1 + "_ss");

        ProcessContext ctxEmpty = ProcessContext.empty();
        assertThrows(ConfigException.class, () -> policy.map("expectBadArgType_name", ctxEmpty));
        assertThrows(ConfigException.class, () -> policy.map("expectBadFieldType_name", ctxEmpty));

    }

    @Test
    void testSerialization() {

        // @formatter:off
        ArgMetaInfoLookup logicalDataModel = DefaultArgMetaInfoLookup
            .withArg("ints." + ARG_NAME_1).ofType(DATE)
                .thatIsAlwaysKnown()
            .withArg(ARG_NAME_2).ofType(INTEGER)
            .withArg(ARG_NAME_3).ofType(BOOL)
            .get();
                
        // @formatter:on

        // @formatter:off
        AutoMappingPolicy policy = new DefaultAutoMappingPolicy(NODE_TYPE_1, 
                argName -> argName.startsWith("ints.") ? argName.substring(5) + "_is" : argName + "_s", 
                null, 
                SOLR_STRING,
                false,
                true,
                logicalDataModel);
        // @formatter:on

        assertMap("ints." + ARG_NAME_1, DATE, ARG_NAME_1 + "_is", SOLR_STRING, false, true, policy, "ints." + ARG_NAME_1);

        AutoMappingPolicy policy2 = DeepCopyUtils.deepCopy(policy);

        assertMap("ints." + ARG_NAME_1, DATE, ARG_NAME_1 + "_is", SOLR_STRING, false, true, policy2, "ints." + ARG_NAME_1);

    }

    private static void assertMap(String expectedArgName, AdlType expectedArgType, String expectedFieldName, AdlSolrType expectedFieldType,
            boolean expectedIsCollection, boolean expectedIsMultiDoc, AutoMappingPolicy policy, String argName) {
        assertEquals(expectedArgName, policy.map(argName, ProcessContext.empty()).arg().argName());
        assertEquals(expectedArgType, policy.map(argName, ProcessContext.empty()).arg().type());
        assertEquals(expectedFieldName, policy.map(argName, ProcessContext.empty()).field().fieldName());
        assertEquals(expectedFieldType, policy.map(argName, ProcessContext.empty()).field().fieldType());
        assertEquals(expectedIsCollection, policy.map(argName, ProcessContext.empty()).field().isCollection());
        assertEquals(expectedIsCollection, policy.map(argName, ProcessContext.empty()).arg().isCollection());
        assertEquals(expectedIsMultiDoc, policy.map(argName, ProcessContext.empty()).isMultiDoc());

    }

}
