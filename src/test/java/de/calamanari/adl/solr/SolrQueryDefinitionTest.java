//@formatter:off
/*
 * SolrQueryDefinitionTest
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import de.calamanari.adl.DeepCopyUtils;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class SolrQueryDefinitionTest {

    @Test
    void testBasics() {

        List<SolrConditionType> conditionTypes = new ArrayList<>(Arrays.asList(SolrConditionType.CMP_VALUE));

        assertCanConstruct(null, null, null);
        assertCanConstruct(null, Collections.emptyList(), null);

        assertThrows(IllegalArgumentException.class, () -> new SolrQueryDefinition("", null, null));
        assertThrows(IllegalArgumentException.class, () -> new SolrQueryDefinition(null, null, ""));

        List<SolrFilterQuery> filterQueries = new ArrayList<>();

        List<SolrQueryField> fields = new ArrayList<>();

        fields.add(new SolrQueryField("node1", "field1"));

        SolrFilterQuery fq1 = new SolrFilterQuery("some query", fields, conditionTypes);

        filterQueries.add(fq1);
        assertCanConstruct(null, filterQueries, null);

        filterQueries.clear();
        filterQueries.add(fq1);
        filterQueries.add(fq1);

        assertThrows(IllegalArgumentException.class, () -> new SolrQueryDefinition(null, filterQueries, null));

        fields.clear();
        fields.add(new SolrQueryField("node1", "field1"));
        fields.add(new SolrQueryField("node2", "field1"));

        SolrFilterQuery fq2 = new SolrFilterQuery("some query", fields, conditionTypes);

        filterQueries.clear();
        filterQueries.add(fq1);
        filterQueries.add(fq2);

        assertThrows(IllegalArgumentException.class, () -> new SolrQueryDefinition(null, filterQueries, null));

        SolrFilterQuery fq3 = new SolrFilterQuery("some other query", fields, conditionTypes);

        filterQueries.clear();
        filterQueries.add(fq1);
        filterQueries.add(fq3);

        assertCanConstruct(null, filterQueries, null);

        assertEquals(new SolrQueryDefinition("foo", filterQueries, null), new SolrQueryDefinition(" foo ", Arrays.asList(fq3, fq1), null));

    }

    @Test
    void testSerialization() {

        List<SolrConditionType> conditionTypes = new ArrayList<>(Arrays.asList(SolrConditionType.CMP_VALUE));

        List<SolrFilterQuery> filterQueries = new ArrayList<>();

        List<SolrQueryField> fields = new ArrayList<>();

        fields.add(new SolrQueryField("node1", "field1"));

        SolrFilterQuery fq1 = new SolrFilterQuery("some query", fields, conditionTypes);

        fields.clear();
        fields.add(new SolrQueryField("node1", "field1"));
        fields.add(new SolrQueryField("node2", "field1"));

        SolrFilterQuery fq2 = new SolrFilterQuery("some other query", fields, conditionTypes);

        filterQueries.clear();
        filterQueries.add(fq1);
        filterQueries.add(fq2);

        SolrQueryDefinition sqd1 = new SolrQueryDefinition(null, filterQueries, null);

        SolrQueryDefinition sqd1s = DeepCopyUtils.deepCopy(sqd1);

        assertEquals(sqd1, sqd1s);

        SolrQueryDefinition sqd2 = new SolrQueryDefinition("mainQ", filterQueries, "myId");

        SolrQueryDefinition sqd2s = DeepCopyUtils.deepCopy(sqd2);

        assertEquals(sqd2, sqd2s);

        assertEquals("mainQ", sqd2s.mainQueryString());

        assertEquals("myId", sqd2s.uniqueKeyFieldName());

    }

    private static void assertCanConstruct(String queryString, List<SolrFilterQuery> filterQueries, String uniqueKey) {

        SolrQueryDefinition sqd = new SolrQueryDefinition(queryString, filterQueries, uniqueKey);

        assertEquals(queryString == null ? SolrFormatConstants.QUERY_ALL_DOCUMENTS : queryString.trim(), sqd.mainQueryString());

        assertEquals(uniqueKey == null ? SolrFormatConstants.DEFAULT_UNIQUE_KEY_FIELD_NAME : uniqueKey, sqd.uniqueKeyFieldName());

        if (filterQueries != null && !filterQueries.isEmpty()) {
            List<SolrFilterQuery> temp = new ArrayList<>(filterQueries);
            Collections.sort(temp);
            assertEquals(temp, sqd.filterQueries());
            Set<String> nodeTypesInDef = new TreeSet<>();
            for (SolrFilterQuery fq : filterQueries) {
                for (SolrQueryField field : fq.fields()) {
                    nodeTypesInDef.add(field.nodeType());
                }
            }
            assertEquals(new ArrayList<>(nodeTypesInDef), sqd.nodeTypesInvolved());
        }
        else {
            assertTrue(sqd.filterQueries().isEmpty());
            assertEquals(Collections.emptyList(), sqd.nodeTypesInvolved());
        }

    }
}
