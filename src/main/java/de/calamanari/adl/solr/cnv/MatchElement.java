//@formatter:off
/*
 * MatchElement
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

/**
 * A {@link MatchElement} is the most generic form of a simple or complex condition on Solr-field(s) from one or multiple documents.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public interface MatchElement {

    /**
     * @return true if this element is a negation or any of its children is or contains a negation
     */
    boolean containsAnyNegation();

    /**
     * If this element or all its leaf child elements have the same node type then this node type will be returned.
     * 
     * @return common node type or null if there is no common node type (hybrid complex condition)
     */
    String commonNodeType();

    /**
     * Tells whether we should include the case that the corresponding nested or dependent document {@link #commonNodeType()} is missing should be included in
     * the match result.
     * <p>
     * This flag has no relevance for match elements on the main document (cannot be absent).
     * 
     * @return if this match has a common node type and the missing document case should be included (match not present)
     */
    boolean isMissingDocumentIncluded();

    /**
     * appends a pretty-formatted representation ({@link Object#toString()} by default) after adding indentation
     * 
     * @param sb to add the debug info
     * @param indentation number of indents
     */
    default void appendDebugString(StringBuilder sb, int indentation) {
        for (int i = 0; i < indentation; i++) {
            sb.append("    ");
        }
        sb.append(this.toString());
    }

    /**
     * @return pretty formatted debug information about this element and its siblings
     */
    default String toDebugString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        this.appendDebugString(sb, 0);
        sb.append("\n");
        return sb.toString();
    }

}
