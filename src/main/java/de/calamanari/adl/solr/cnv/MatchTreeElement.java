//@formatter:off
/*
 * MatchTreeNode
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

/**
 * A {@link MatchTreeElement} is either a single comparison or a combination (AND/OR) of sub-{@link MatchTreeElement}s.
 * <p>
 * The {@link MatchTreeElement} (node of a tree of conditions) addresses a Solr-specific problem with nested expressions and different node types. Fields from
 * different documents are not available <i>side-by-side</i> as Solr has no method to qualify a field by its document. In other words: within any two braces of
 * the main expression you can only reference the fields of a single (sub-)document.
 * <p>
 * Thus, it is required to handle the node type somehow <i>orthogonal</i> to the query structure of the underlying Audlang expression, which makes it hard to
 * let the converter just append parts of an expression to the output while iterating through the source expression.
 * <p>
 * Instead the converter first creates a tree of wrappers, optimizes the arrangement of this tree before eventually creating the Solr-expression.
 * <p>
 * Instances are deeply immutable.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public interface MatchTreeElement extends MatchElement {

    /**
     * @return list of child elements if there are any
     */
    default List<MatchTreeElement> childElements() {
        return Collections.emptyList();
    }

    /**
     * @return true if this element has no siblings
     */
    default boolean isLeafElement() {
        return childElements().isEmpty();
    }

    /**
     * This method tells whether this element can be part of a complex condition in a Solr-expression or not.
     * <p>
     * Any element that is <i>not</i> eligible cannot be combined with other conditions within the same join.
     * 
     * @return true if this element resp. its match condition can be combined with other conditions within the same join
     */
    boolean isGroupingEligible();

    /**
     * Creates an indicator for better readability of debugging output, so we can quickly recognize certain information.
     * <ul>
     * <li>"<code>*!</code>" - {@link #isGroupingEligible()} AND {@link #containsAnyNegation()}</li>
     * <li>"<code>*_</code>" - {@link #isGroupingEligible()} AND NOT {@link #containsAnyNegation()}</li>
     * <li>"<code>_!</code>" - NOT {@link #isGroupingEligible()} AND {@link #containsAnyNegation()}</li>
     * <li>"<code>__</code>" - NOT {@link #isGroupingEligible()} AND NOT {@link #containsAnyNegation()}</li>
     * </ul>
     * 
     * @return returns an indicator for debugging purposes
     */
    default String createDebugIndicator() {
        if (isGroupingEligible() && containsAnyNegation()) {
            return "*!";
        }
        else if (isGroupingEligible()) {
            return "*_";
        }
        else if (containsAnyNegation()) {
            return "_!";
        }
        else {
            return "__";
        }
    }

}
