//@formatter:off
/*
 * MatchFilterFactory
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

import de.calamanari.adl.solr.SolrFilterQuery;

/**
 * A {@link MatchFilterFactory} abstracts the creation of a single Solr match condition (field against value, field against multiple values, field against
 * field).
 * <p>
 * A complex Solr-query works on "data sets". Starting with a basic super-set (often <code>*:*</code>) every <i>positive</i> condition added using
 * <code><b>AND</b></code> reduces this set.<br>
 * On the other hand if you concatenate conditions using <code><b>OR</b></code> can increase the size of the result set.
 * <p>
 * Negations are more complicated. A negation means an exclusion (this is equivalent to Audlang's default negation, see also
 * <a href="https://github.com/KarlEilebrecht/audlang-spec/blob/main/doc/AudienceDefinitionLanguageSpecification.md#51-default-negation">§5.1 Audlang Spec</a>).
 * This is the reason why Solr-queries of the form <code>(color:blue AND shape:square) OR NOT color:blue</code> don't work. The set
 * <code>OR NOT color:blue</code> means subtracting <code>color:blue</code> from <i>nothing</i>. Consequently, it is empty and won't contribute to the result
 * set. To fix this, we must ensure that the <code>OR'd</code> set is not empty by starting with a positive base set:
 * <code>(color:blue AND shape:square) OR (*:* AND NOT color:blue)</code>.<br>
 * Nested or dependent documents create another challenge. The aforementioned sets contain main documents (the contributions to the result). So, every condition
 * must be created in a way that it results in a set of main documents, even if the attributes sit on a nested or dependent document. This also means that a
 * negative condition may be fulfilled because (1) the attribute is present and has a different value <i><b>or</b></i> (2) the attribute is not present
 * <i><b>or</b></i> (3) the sub-document is not present. The last two options are <i>not</i> STRICT!
 * <p>
 * Not to mingle all these concerns, a <b>{@link MatchFilterFactory} only creates positive conditions</b> (restricting filter expressions) and leaves dealing
 * with negations entirely to the calling component.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public interface MatchFilterFactory {

    /**
     * Creates a filter expression to ensure the Solr-field mapped to the given argName has <i>any</i> value, e.g. <code>arg_field:*</code>.
     * 
     * @param argName
     * @return Solr filter expression
     */
    SolrFilterQuery createHasAnyValueFilter(String argName);

    /**
     * Creates a filter expression for the Solr-documents of the given node type including optional document filters.
     * <p>
     * The idea of the node type filter addresses the problem that you can't have conditions on <i>different</i> Solr-documents (node types) side-by-side.<br>
     * It will always be a mix of expressions (joins resp. block-joins). Each of these should start with the corresponding node type filter (at least the node
     * type itself). It is also important to turn every expression like <code><b>OR NOT</b> <i>&lt;condition&gt;</i></code> into
     * <code><b>OR</b> (<i>&lt;nodeTypeFilter&gt;</i> <b>AND NOT</b> <i>&lt;condition&gt;</i>)</code>.
     * 
     * @param nodeType
     * @return Solr filter expression
     */
    SolrFilterQuery createNodeTypeFilter(String nodeType);

    /**
     * Creates a filter expression from the given {@link MatchWrapper}
     * <p>
     * <b>Note:</b> All filter expressions are <i>positive restrictions</i>, so the {@link MatchWrapper#isNegation()} flag has <b>no influence</b> on the
     * created Solr-expression.
     * 
     * @param matchWrapper
     * @return condition
     */
    SolrFilterQuery createMatchFilter(MatchWrapper matchWrapper);

}
