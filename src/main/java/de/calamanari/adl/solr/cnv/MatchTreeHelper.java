//@formatter:off
/*
 * MatchTreeHelper
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

/**
 * The {@link MatchTreeHelper}'s purpose is consolidating and sorting the levels of the match tree, so that the converter can produce a Solr-expression with a
 * minimum of switches between main document and joined documents.
 * <p>
 * <b>Background:</b> A Solr query always selects main documents, but fields can sit on the main document or any joined document (nested or dependent). Because
 * Solr does not allow us to work with fields from different documents <i>side-by-side</i>, every combination of two documents requires a new root level
 * condition. Lets assume there are conditions <b><code>A1, A2</code></b> related to the <b>main document <code>A</code></b> and conditions
 * <b><code>B1, B2, B3</code></b> from document <b><code>B</code></b>. The expression might be <b><code>(A1 AND B1 AND A2 AND (B2 OR B3))</code></b>. Without
 * sorting, this would result in costly switching between each condition and its successor. If we sort beforehand by node type, the semantically equivalent
 * expression <b><code>(A1 AND A2 AND B1 AND (B2 OR B3))</code></b> can be executed as <b><code>A(1 AND 2) AND B(1 AND (2 OR 3))</code></b>. This creates a
 * single join rather than three to document <b><code>B</code></b>.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public interface MatchTreeHelper {

    /**
     * Post-processes the Solr-match-tree by recursively (bottom-up) detecting and replacing groups of {@link MatchTreeElement}s that should be part of a
     * combined expression (e.g., <i>less-than-or-equals</i>).
     * <p>
     * All tree-levels will be sorted by node type so that we can reduce the need for switches when composing the final expression.
     * 
     * @param root
     * @return consolidated root
     */
    MatchTreeElement consolidateMatchTree(MatchTreeElement root);

    /**
     * Creates groups of elements to be executed together from a level of a previously consolidated tree (see {@link #consolidateMatchTree(MatchTreeElement)}).
     * <p>
     * This method processes a single level, so in case of complex expressions the result may contain {@link NodeTypeMatchTreeElementGroup}s side-by-side with
     * {@link MatchWrapper}s and {@link CombinedMatchTreeElement}s. The latter must be passed to this method again (recursively).
     * <p>
     * Grouping of negations is tricky as it depends on multiple factors.
     * <ul>
     * <li>Conditions on the main documents are by definition a group, and it does not matter whether these conditions are positive or negative.</li>
     * <li>Conditions on a dependent or nested document are more complicated. Here are two Solr-expressions:
     * <ul>
     * <li><code>... AND NOT {!parent which="node_type:node1" v="node_type:node2 AND color\:red"}</code></li>
     * <li><code>... AND {!parent which="node_type:node1" v="node_type:node2 AND NOT color\:red"}</code>.</li>
     * </ul>
     * There is a semantical difference between these expressions. In the second case we require the nested document (<code>node2</code>) to exist, which is
     * potentially wrong. Let <b><code>C1, C2</code></b> be positive conditions on a nested document and <b><code>CN1, CN2</code></b> negative conditions on
     * that same document.
     * <ul>
     * <li><b>Case 1: <code>C1 AND CN2</code></b>. Here we can allow grouping because <b><code>C1</code></b> implies the existence of the nested document.</li>
     * <li><b>Case 2: <code>C1 OR CN2</code></b>. Grouping would be wrong. The problem is that putting both conditions in one group would create a common join
     * that requires the nested document to exist. In other words we would not find any main documents where the nested document does not exist.</li>
     * <li><b>Case 3: <code>CN1 AND CN2</code></b>. Again, grouping would be wrong. A common join would be created that requires the nested document to
     * exist.</li>
     * <li><b>Case 4: <code>C1 OR (C2 AND (CN1 OR CN2))</code></b>. Grouping is ok <i>for all levels</i> because <b><code>C1</code></b> implies the existence of
     * the nested document.</li>
     * </ul>
     * <li><b>Conclusion:</b> Grouping can be allowed if any AND-parent-condition pins the existence of the related dependent or nested document. Otherwise
     * grouping is not allowed.</li>
     * </ul>
     * 
     * @param treeElement current level node of the match tree
     * @return chunks of elements to be executed separately
     */
    List<MatchElement> createExecutionGroups(MatchTreeElement treeElement);

}
