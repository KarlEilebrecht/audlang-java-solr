//@formatter:off
/*
 * NodeTypeMatchTreeElementGroup
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.calamanari.adl.CombinedExpressionType;

/**
 * A {@link NodeTypeMatchTreeElementGroup} combines (AND/OR) conditions related to the <b>same node type</b> that can be executed together. Groups can avoid
 * unnecessary joins in case of fields from nested or dependent documents.
 * <p>
 * The grouping occurs <i>after</i> building the match tree on one particular level of the tree. Hence, a {@link NodeTypeMatchTreeElementGroup} is <i>not</i> a
 * {@link MatchTreeElement}. It is a potentially complex {@link MatchElement} to be applied to a single node type.
 * <p>
 * Instances are deeply immutable.
 * 
 * @param combiType combination type of the group members
 * @param members at least two elements
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public record NodeTypeMatchTreeElementGroup(CombinedExpressionType combiType, List<MatchTreeElement> members) implements MatchElement {

    /**
     * @param combiType combination type of the group members
     * @param members at least two elements
     */
    public NodeTypeMatchTreeElementGroup(CombinedExpressionType combiType, List<MatchTreeElement> members) {
        assertGroupMembersCompliant(combiType, members);
        this.members = Collections.unmodifiableList(new ArrayList<>(members));
        this.combiType = combiType;
    }

    /**
     * This verification is only to report subtle errors as early as possible (potentially escape from complexity hell)
     * 
     * @param members
     */
    private static void assertGroupMembersCompliant(CombinedExpressionType combiType, List<MatchTreeElement> members) {

        if (combiType == null) {
            throw new IllegalArgumentException(String.format("The argument combiType must not be null, given: combiType=%s, members=%s", combiType, members));
        }

        if (members == null || members.size() < 2) {
            throw new IllegalArgumentException(String.format("Expected at least 2 group members, given: combiType=%s, members=%s", combiType, members));
        }

        MatchTreeElement firstElement = members.get(0);

        String commonNodeType = firstElement.commonNodeType();

        if (commonNodeType == null) {
            throw new IllegalArgumentException("A group requires a common node type, given: " + members);
        }

        for (int i = 1; i < members.size(); i++) {

            MatchTreeElement currentElement = members.get(i);

            if (!commonNodeType.equals(currentElement.commonNodeType())) {
                throw new IllegalArgumentException("A group requires a common node type, given: " + members);
            }

        }

    }

    @Override
    public boolean containsAnyNegation() {
        return members.stream().anyMatch(MatchTreeElement::containsAnyNegation);
    }

    @Override
    public String commonNodeType() {
        return members.get(0).commonNodeType();
    }

    /**
     * @return number of members in this group, &gt;=2
     */
    public int size() {
        return members.size();
    }

    @Override
    public String toString() {
        return "NodeTypeMatchTreeElementGroup [combiType=" + combiType + " (" + commonNodeType() + "), members=" + members + "]";
    }

    @Override
    public void appendDebugString(StringBuilder sb, int indentation) {
        MatchElementDebugUtils.appendTreeLevelMembersDebugString(sb, indentation,
                "NodeTypeMatchTreeElementGroup " + "[combiType=" + combiType + " " + commonNodeType() + "]", combiType, members);
    }

}
