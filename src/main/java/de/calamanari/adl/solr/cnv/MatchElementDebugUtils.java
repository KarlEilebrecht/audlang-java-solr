//@formatter:off
/*
 * MatchElementUtils
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

import java.util.List;

import de.calamanari.adl.CombinedExpressionType;

/**
 * Utilities for quickly visualizing the details of a match element tree.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class MatchElementDebugUtils {

    private MatchElementDebugUtils() {
        // static utilities
    }

    /**
     * Appends a readable (tree-like) formatted string to the builder
     * 
     * @param sb for appending
     * @param indentation number of current indentation steps
     * @param groupInfo summary about the parent
     * @param combiType the combiner of the parent
     * @param members to be visualized (from a combined element)
     */
    public static void appendTreeLevelMembersDebugString(StringBuilder sb, int indentation, String groupInfo, CombinedExpressionType combiType,
            List<? extends MatchElement> members) {
        for (int k = 0; k < indentation; k++) {
            sb.append("    ");
        }
        sb.append(groupInfo).append(" (");
        if (members == null) {
            sb.append("null");
        }
        else if (!members.isEmpty()) {
            for (int i = 0; i < members.size(); i++) {
                sb.append("\n");
                if (i > 0) {
                    for (int k = 0; k < indentation + 1; k++) {
                        sb.append("    ");
                    }
                    sb.append(combiType.name());
                    sb.append("\n");
                }
                members.get(i).appendDebugString(sb, indentation + 2);
            }
            sb.append("\n");
            for (int k = 0; k < indentation; k++) {
                sb.append("    ");
            }
        }
        sb.append(")");

    }

    /**
     * Creates a list debug string with line breaks between the elements for better readability.
     * 
     * @param elements list of match elements
     * @return readable debug string
     */
    public static String createListDebugString(List<MatchElement> elements) {

        if (elements == null) {
            return "\nnull\n";
        }

        if (elements.isEmpty()) {
            return "\n[]\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n[");

        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                sb.append(",\n");
            }
            sb.append("\n");
            elements.get(i).appendDebugString(sb, 1);
        }
        sb.append("\n]\n");
        return sb.toString();
    }

}
