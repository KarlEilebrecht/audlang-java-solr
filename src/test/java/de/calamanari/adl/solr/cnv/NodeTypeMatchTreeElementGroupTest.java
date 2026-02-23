//@formatter:off
/*
 * NodeTypeMatchTreeElementGroupTest
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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.calamanari.adl.CombinedExpressionType;
import de.calamanari.adl.solr.SolrTestBase;

import static de.calamanari.adl.solr.cnv.ConversionTestUtils.createDryTestContext;
import static de.calamanari.adl.solr.cnv.ConversionTestUtils.matchTreeOf;
import static de.calamanari.adl.solr.cnv.ConversionTestUtils.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class NodeTypeMatchTreeElementGroupTest extends SolrTestBase {

    private SolrConversionProcessContext ctx = createDryTestContext();

    @Test
    void testBasics() {

        MatchTreeElement root = matchTreeOf("age=3 and color=red and taste=bad", ctx);

        NodeTypeMatchTreeElementGroup group = new NodeTypeMatchTreeElementGroup(CombinedExpressionType.AND, root.childElements());

        assertEquals(root.childElements().size(), group.size());

        assertFalse(group.containsAnyNegation());

        assertEquals(CombinedExpressionType.AND, group.combiType());

        assertEquals(NODE_TYPE_1, group.commonNodeType());

        root = matchTreeOf("age=3 and (color=red or taste=bad)", ctx);

        group = new NodeTypeMatchTreeElementGroup(CombinedExpressionType.AND, root.childElements());

        assertEquals(root.childElements().size(), group.size());

        assertFalse(group.containsAnyNegation());

        assertEquals(CombinedExpressionType.AND, group.combiType());

        assertEquals(NODE_TYPE_1, group.commonNodeType());

        root = matchTreeOf("STRICT age!=3 and STRICT color!=red and STRICT taste!=bad", ctx);

        group = new NodeTypeMatchTreeElementGroup(CombinedExpressionType.OR, root.childElements());

        assertEquals(root.childElements().size(), group.size());

        assertTrue(group.containsAnyNegation());

        assertEquals(CombinedExpressionType.OR, group.combiType());

        assertEquals(NODE_TYPE_1, group.commonNodeType());

        root = matchTreeOf("age=3 and (color = red OR (date_of_birth > 2020-01-01 AND taste = good))", ctx);

        group = new NodeTypeMatchTreeElementGroup(CombinedExpressionType.AND, root.childElements());

        assertEquals(root.childElements().size(), group.size());

        assertFalse(group.containsAnyNegation());

        root = matchTreeOf("age=3 and (color = red OR (date_of_birth > 2020-01-01 AND taste != good))", ctx);

        group = new NodeTypeMatchTreeElementGroup(CombinedExpressionType.AND, root.childElements());

        assertEquals(root.childElements().size(), group.size());

        assertTrue(group.containsAnyNegation());

    }

    @Test
    void testSpecial() {

        assertThrows(IllegalArgumentException.class, () -> new NodeTypeMatchTreeElementGroup(null, null));
        assertThrows(IllegalArgumentException.class, () -> new NodeTypeMatchTreeElementGroup(CombinedExpressionType.AND, null));
        assertThrows(IllegalArgumentException.class, () -> new NodeTypeMatchTreeElementGroup(CombinedExpressionType.OR, null));

        List<MatchTreeElement> empty = Collections.emptyList();

        assertThrows(IllegalArgumentException.class, () -> new NodeTypeMatchTreeElementGroup(CombinedExpressionType.AND, empty));

        List<MatchTreeElement> single = Collections.singletonList(wrap("color = red", ctx));

        assertThrows(IllegalArgumentException.class, () -> new NodeTypeMatchTreeElementGroup(CombinedExpressionType.AND, single));

        List<MatchTreeElement> nodeMix = matchTreeOf("age=3 and flag1_b=1", ctx).childElements();

        assertThrows(IllegalArgumentException.class, () -> new NodeTypeMatchTreeElementGroup(CombinedExpressionType.AND, nodeMix));

        List<MatchTreeElement> nodeMix2 = matchTreeOf("(age=3 OR clicks_l > 10000) and (flag1_b=1 or color = red)", ctx).childElements();

        assertThrows(IllegalArgumentException.class, () -> new NodeTypeMatchTreeElementGroup(CombinedExpressionType.AND, nodeMix2));

    }

}
