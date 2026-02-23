//@formatter:off
/*
 * ResettableScpContextTest
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

package de.calamanari.adl.solr.cnv;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.calamanari.adl.Flag;
import de.calamanari.adl.solr.SolrFormatConstants;
import de.calamanari.adl.solr.SolrTestBase;
import de.calamanari.adl.solr.config.MainDocumentConfig;
import de.calamanari.adl.solr.config.SolrMappingConfig;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class ResettableScpContextTest extends SolrTestBase {

    private static final MainDocumentConfig CONFIG = MainDocumentConfig.forNodeType(NODE_TYPE_1).defaultAutoMapped().get();

    @Test
    void testBasics() {

        assertThrows(IllegalArgumentException.class, () -> new ResettableScpContext(null, null, null));

        assertCanConstruct(CONFIG, null, null);

        assertCanConstruct(CONFIG, Collections.emptyMap(), Collections.emptySet());

        Map<String, Serializable> globalVariablesTemplate = new HashMap<>();
        globalVariablesTemplate.put(SolrConversionOverrides.OVERRIDE_DEPENDENT_MAIN_KEY_FIELD_NAME.name(), "alt_dep_main_key");
        globalVariablesTemplate.put(SolrConversionOverrides.OVERRIDE_NODE_TYPE_FIELD_NAME.name(), "alt_node_type");
        globalVariablesTemplate.put(SolrConversionOverrides.OVERRIDE_UNIQUE_KEY_FIELD_NAME.name(), "alt_id");

        Set<Flag> flagsTemplate = new HashSet<>();

        Flag myFlag = new Flag() {

            private static final long serialVersionUID = -7803370238932549869L;
        };

        flagsTemplate.add(myFlag);

        assertCanConstruct(CONFIG, globalVariablesTemplate, flagsTemplate);

        ResettableScpContext ctx = new ResettableScpContext(CONFIG, globalVariablesTemplate, null);

        assertFalse(myFlag.check(ctx.getGlobalFlags()));

        ctx = new ResettableScpContext(CONFIG, globalVariablesTemplate, flagsTemplate);

        assertTrue(myFlag.check(ctx.getGlobalFlags()));
        assertEquals("alt_dep_main_key", ctx.getDependentMainKeyFieldName());
        assertEquals("alt_node_type", ctx.getNodeTypeFieldName());
        assertEquals("alt_id", ctx.getUniqueKeyFieldName());

        ctx.getGlobalFlags().remove(myFlag);
        ctx.getGlobalVariables().put(SolrConversionOverrides.OVERRIDE_NODE_TYPE_FIELD_NAME.name(), "alt_node_type_changed");

        assertFalse(myFlag.check(ctx.getGlobalFlags()));
        assertEquals("alt_node_type_changed", ctx.getNodeTypeFieldName());

        ctx.getGlobalVariablesTemplate().put(SolrConversionOverrides.OVERRIDE_NODE_TYPE_FIELD_NAME.name(), "alt_node_type_changed2");
        assertEquals("alt_node_type_changed", ctx.getNodeTypeFieldName());

        ctx.reset();
        assertEquals("alt_node_type_changed2", ctx.getNodeTypeFieldName());
        assertTrue(myFlag.check(ctx.getGlobalFlags()));
        ctx.getGlobalFlagsTemplate().remove(myFlag);

        ctx.reset();
        assertFalse(myFlag.check(ctx.getGlobalFlags()));

    }

    private static void assertCanConstruct(SolrMappingConfig mappingConfig, Map<String, Serializable> globalVariablesTemplate, Set<Flag> flagsTemplate) {

        ResettableScpContext ctx = new ResettableScpContext(mappingConfig, globalVariablesTemplate, flagsTemplate);

        globalVariablesTemplate = globalVariablesTemplate != null ? globalVariablesTemplate : Collections.emptyMap();
        flagsTemplate = flagsTemplate != null ? flagsTemplate : Collections.emptySet();

        assertEquals(mappingConfig, ctx.getMappingConfig());

        assertEquals(globalVariablesTemplate, ctx.getGlobalVariablesTemplate());
        assertEquals(globalVariablesTemplate, ctx.getGlobalVariables());
        assertEquals(flagsTemplate, ctx.getGlobalFlagsTemplate());
        assertEquals(flagsTemplate, ctx.getGlobalFlags());

        String expectedDependentMainKeyFieldName = (String) globalVariablesTemplate.get(SolrConversionOverrides.OVERRIDE_DEPENDENT_MAIN_KEY_FIELD_NAME.name());
        String expectedNodeTypeFieldName = (String) globalVariablesTemplate.get(SolrConversionOverrides.OVERRIDE_NODE_TYPE_FIELD_NAME.name());
        String expectedUniqueKeyFieldName = (String) globalVariablesTemplate.get(SolrConversionOverrides.OVERRIDE_UNIQUE_KEY_FIELD_NAME.name());

        expectedDependentMainKeyFieldName = expectedDependentMainKeyFieldName != null ? expectedDependentMainKeyFieldName
                : SolrFormatConstants.DEFAULT_DEPENDENT_MAIN_KEY_FIELD_NAME;
        expectedNodeTypeFieldName = expectedNodeTypeFieldName != null ? expectedNodeTypeFieldName : SolrFormatConstants.DEFAULT_NODE_TYPE_FIELD_NAME;
        expectedUniqueKeyFieldName = expectedUniqueKeyFieldName != null ? expectedUniqueKeyFieldName : SolrFormatConstants.DEFAULT_UNIQUE_KEY_FIELD_NAME;

        assertEquals(expectedDependentMainKeyFieldName, ctx.getDependentMainKeyFieldName());
        assertEquals(expectedNodeTypeFieldName, ctx.getNodeTypeFieldName());
        assertEquals(expectedUniqueKeyFieldName, ctx.getUniqueKeyFieldName());

        assertNotNull(ctx.getFilterQueryBuilder());
        assertNull(ctx.getMatchFilterFactory());
        assertNull(ctx.getMatchTreeHelper());

    }

}
