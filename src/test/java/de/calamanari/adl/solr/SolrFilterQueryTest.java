//@formatter:off
/*
 * SolrFilterQueryTest
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.calamanari.adl.DeepCopyUtils;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class SolrFilterQueryTest {

    @Test
    void testBasics() {

        List<SolrQueryField> fieldsEmpty = Collections.emptyList();
        List<SolrConditionType> conditionTypesEmpty = Collections.emptyList();

        assertThrows(IllegalArgumentException.class, () -> new SolrFilterQuery(null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new SolrFilterQuery("", null, null));
        assertThrows(IllegalArgumentException.class, () -> new SolrFilterQuery("some query", null, null));
        assertThrows(IllegalArgumentException.class, () -> new SolrFilterQuery("some query", fieldsEmpty, null));
        assertThrows(IllegalArgumentException.class, () -> new SolrFilterQuery("some query", fieldsEmpty, conditionTypesEmpty));

        List<SolrConditionType> conditionTypes = new ArrayList<>(Arrays.asList(SolrConditionType.CMP_VALUE));

        List<SolrQueryField> fields = new ArrayList<>();

        fields.add(new SolrQueryField("node1", "field1"));

        assertCanConstruct("some query", fields, conditionTypes);

        conditionTypes.add(SolrConditionType.CMP_VALUE);

        assertCanConstruct("some query", fields, conditionTypes);

        conditionTypes.clear();

        conditionTypes.add(SolrConditionType.CMP_VALUE);

        fields.add(new SolrQueryField("node1", "field1"));

        SolrFilterQuery query = new SolrFilterQuery("some query", fields, conditionTypes);
        assertEquals(1, query.fields().size());
        assertEquals(new SolrQueryField("node1", "field1"), query.fields().get(0));

        fields.clear();

        fields.add(new SolrQueryField("node1", "field1"));
        fields.add(null);

        assertThrows(IllegalArgumentException.class, () -> new SolrFilterQuery("some query", fields, conditionTypes));

        fields.clear();
        fields.add(new SolrQueryField("node1", "field1"));
        fields.add(new SolrQueryField("node1", "field2"));

        assertCanConstruct("some query", fields, conditionTypes);

        fields.clear();
        fields.add(new SolrQueryField("node2", "field2"));
        fields.add(new SolrQueryField("node2", "field1"));
        fields.add(new SolrQueryField("node1", "field1"));

        assertCanConstruct("some query", fields, conditionTypes);

        assertEquals(2, new SolrFilterQuery("some query", fields, conditionTypes).nodeTypesInvolved().size());

        assertEquals("some query", new SolrFilterQuery("  some query  ", fields, conditionTypes).queryString());

    }

    @Test
    void testEqualsAndCompare() {

        List<SolrConditionType> conditionTypes = new ArrayList<>(Arrays.asList(SolrConditionType.CMP_VALUE));
        List<SolrConditionType> conditionTypes2 = new ArrayList<>(Arrays.asList(SolrConditionType.CMP_VALUE, SolrConditionType.CMP_RANGE));

        List<SolrQueryField> fields = new ArrayList<>();
        fields.add(new SolrQueryField("node2", "field2"));
        fields.add(new SolrQueryField("node2", "field1"));
        fields.add(new SolrQueryField("node1", "field1"));

        List<SolrQueryField> fields2 = new ArrayList<>();
        fields2.add(new SolrQueryField("node3", "field3"));

        SolrFilterQuery sfq1 = new SolrFilterQuery("some query", fields, conditionTypes);
        SolrFilterQuery sfq2 = new SolrFilterQuery("some query", fields2, conditionTypes);

        assertEquals(sfq1, sfq2);
        assertEquals(sfq2, sfq1);

        assertEquals(0, sfq2.compareTo(sfq1));
        assertEquals(0, sfq1.compareTo(sfq2));

        fields.clear();
        fields.add(new SolrQueryField("node1", "field1"));
        fields.add(new SolrQueryField("node2", "field1"));
        fields.add(new SolrQueryField("node2", "field2"));

        SolrFilterQuery sfq3 = new SolrFilterQuery("some query", fields, conditionTypes2);

        assertEquals(sfq2, sfq3);
        assertEquals(sfq3, sfq1);

        assertEquals(0, sfq3.compareTo(sfq2));
        assertEquals(0, sfq1.compareTo(sfq3));

    }

    @Test
    void testSerialization() {

        List<SolrConditionType> conditionTypes = new ArrayList<>(Arrays.asList(SolrConditionType.CMP_VALUE));

        List<SolrQueryField> fields = new ArrayList<>();
        fields.add(new SolrQueryField("node3", "field3"));

        SolrFilterQuery sfq1 = new SolrFilterQuery("some query", fields, conditionTypes);

        SolrFilterQuery sfq1s = DeepCopyUtils.deepCopy(sfq1);

        assertEquals(sfq1, sfq1s);

        assertEquals(sfq1.fields(), sfq1s.fields());

        assertEquals(sfq1.conditionTypes(), sfq1s.conditionTypes());

        fields.clear();
        fields.add(new SolrQueryField("node2", "field2"));
        fields.add(new SolrQueryField("node2", "field1"));
        fields.add(new SolrQueryField("node1", "field1"));

        SolrFilterQuery sfq2 = new SolrFilterQuery("some query", fields, conditionTypes);

        SolrFilterQuery sfq2s = DeepCopyUtils.deepCopy(sfq2);

        assertEquals(sfq2, sfq2s);

        assertEquals(sfq2.fields(), sfq2s.fields());

        assertEquals(sfq2.conditionTypes(), sfq2s.conditionTypes());

    }

    private static void assertCanConstruct(String queryString, List<SolrQueryField> fields, List<SolrConditionType> conditionTypes) {

        SolrFilterQuery sfq = new SolrFilterQuery(queryString, fields, conditionTypes);

        assertEquals(queryString.trim(), sfq.queryString());

        for (SolrQueryField field : fields) {
            assertTrue(sfq.fields().contains(field));
            assertTrue(sfq.nodeTypesInvolved().contains(field.nodeType()));
        }

        String lastNodeFieldCombi = null;
        for (SolrQueryField field : sfq.fields()) {
            assertTrue(sfq.nodeTypesInvolved().contains(field.nodeType()));
            String nodeFieldCombi = field.nodeType() + "." + field.fieldName();
            if (lastNodeFieldCombi != null) {
                assertTrue(nodeFieldCombi.compareTo(lastNodeFieldCombi) > 0);
            }
            lastNodeFieldCombi = nodeFieldCombi;
        }
        assertEquals(sfq.nodeTypesInvolved().size(), sfq.nodeTypesInvolved().stream().distinct().count());

        Set<SolrConditionType> conditionTypesTemp = new HashSet<>(conditionTypes);
        List<SolrConditionType> expectedConditionTypes = new ArrayList<>(conditionTypesTemp);
        Collections.sort(expectedConditionTypes);

        assertEquals(expectedConditionTypes, sfq.conditionTypes());
    }

}
