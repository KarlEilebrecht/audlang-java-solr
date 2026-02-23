//@formatter:off
/*
 * DefaultMatchTreeHelper
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.calamanari.adl.CombinedExpressionType;
import de.calamanari.adl.irl.MatchExpression;
import de.calamanari.adl.irl.MatchOperator;
import de.calamanari.adl.solr.SolrFormatUtils;
import de.calamanari.adl.solr.config.ArgFieldAssignment;

import static de.calamanari.adl.solr.cnv.MatchWrapperType.VALUE_MATCH;
import static de.calamanari.adl.solr.cnv.MatchWrapperType.VALUE_OR_EQ_MATCH;

/**
 * Default implementation of a {@link MatchTreeHelper}.
 * <p>
 * A few comments regarding the general logic in this class which is a bit tricky:
 * <ul>
 * <li>The implementation heavily relies on sorting. It is crucial to understand how the elements are ordered before processing.</li>
 * <li>There are two phases: <b>tree consolidation</b> and <b>grouping</b>.
 * <ul>
 * <li>Tree consolidation happens only once for a tree of match elements parsed from an expression. This rebuilt of the match tree combines elements (e.g.,
 * <i>greater than or equals</i>) and prepares the order of the elements on each tree level.</li>
 * <li>Grouping happens while converting the expression. It processes the elements of a level of the tree. Whenever possible we try to collapse an entire
 * subtree into a single join.</li>
 * </ul>
 * <li>The <b>grouping eligibility</b> addresses the problem of creating as few as possible joins to nested or dependent documents (node types). It is a
 * <i>static</i> concept. Tree elements will be marked as eligible or not during the consolidation phase. Once consolidated the eligibility does not change
 * anymore. Eligibility is <i>inherited bottom-up</i>, hence you must not group any element that contains any that is not eligible.</li>
 * <li><b>Dealing with negations:</b>
 * <p>
 * This implementation does not group any negations with other conditions because of various implications this would have, mainly an <i>anomaly</i> caused be
 * the strictness rules in conjunction with with mapping <i>related fields</i> in the same sub document.
 * <p>
 * <b>Example:</b> Let there be two main documents <b><code>shop4711</code></b> and <b><code>shop4712</code></b>, with nested document(s) of node type
 * <b><code>item</code></b>
 * <p>
 * 
 * <pre>
 * shop4711
 *    {"article"="toaster", "price"=50.0}
 *    {"article"="bread", "price"=1.49}
 *    
 * shop4712
 *    {"article"="lemon", "price"=0.45}
 *    {"article"="apple", "price"=0.15}
 * </pre>
 * <p>
 * In this scenario, the mappings of the fields <code>article</code> and <code>price</code> are <b>not marked multi-doc</b> because they are <i>related</i>.
 * <p>
 * Query <i>Q1</i> <code>article=toaster AND price = 50.0</code> returns <b><code>shop4711</code></b> (as expected).
 * <p>
 * The problematic query is:<br>
 * <i>Q2:</i> <code>STRICT NOT (article=toaster AND price = 50.0)</code> which is technically
 * <code>(STRICT NOT article=toaster OR STRICT NOT price = 50.0)</code>.
 * <p>
 * When we test the negative conditions <i>inside the same join</i> from the main document (shop) to the sub document item, <i>Q2</i> will return
 * <b><code>shop4712</code></b> (as expected) but also <b><code>shop4711</code></b>.
 * <p>
 * <i><b>What's going on here?!</b></i>
 * <p>
 * The problem is that we do the subtraction (NOT) <i>in the context of a particular item</i> (the instance is <i>pinned</i> by the join).<br>
 * In other words, <i>Q2</i> asks <i>"Give me all shops where <b>any item</b> is not toaster or has not the price 50.0"</i>. So, <b><code>shop4711</code></b>
 * will be included in the result because <code>"bread" <b>!=</b> "toaster"</code>.
 * <p>
 * I decided to address this problem in a similar way I have done it in my SQL-implementation. I force any negation onto its own join. This way it becomes a
 * root-level existence check (shop).
 * <p>
 * The query now translates to <i>"Give me all shops which do not have any toasters <b>plus</b> all the shops which do not have any items with the price
 * 50.0"</i>.
 * <p>
 * The problem originates from the interference of the boolean logic with the mapping, and I believe this is the best way to tackle it.</li>
 * </ul>
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class DefaultMatchTreeHelper implements MatchTreeHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMatchTreeHelper.class);

    /**
     * sorts wrappers by members
     */
    protected static final Comparator<MatchWrapper> MW_MEMBERS_COMPARATOR = (mw1, mw2) -> {

        List<MatchExpression> members1 = mw1.members();
        List<MatchExpression> members2 = mw2.members();

        int res = 0;

        int len = Math.min(members1.size(), members2.size());
        for (int i = 0; res == 0 && i < len; i++) {
            res = members1.get(i).compareTo(members2.get(i));
        }

        if (res == 0) {
            res = Integer.compare(members1.size(), members2.size());
        }
        return res;

    };

    /**
     * This sorts the {@link SingleMatchWrapper}s by operand (null to the end) and <i>then</i> by operator.
     */
    protected static final Comparator<SingleMatchWrapper> SMW_OPERAND_THEN_OPERATOR_COMPARATOR = (smw1, smw2) -> {
        // @formatter:off
            int res = 0;
            if (smw1.operator() != MatchOperator.IS_UNKNOWN && smw2.operator() != MatchOperator.IS_UNKNOWN) {
                // we want to keep value matches with the same argName and operand together
                // if any there is an is-unknown, we skip the operand-comparison and rely on the subsequent operator-comparison
                res = smw1.matchExpression().operand().value().compareTo(smw2.matchExpression().operand().value());
            }
            if (res == 0) {
                res = smw1.operator().compareTo(smw2.operator());
            }
            return res;
        // @formatter:on
    };

    /**
     * Orders the {@link MatchWrapper}s so that the {@link SingleMatchWrapper}s come last ordered by {@link #SMW_OPERAND_THEN_OPERATOR_COMPARATOR}
     */
    protected static final Comparator<MatchWrapper> MW_SMW_LAST_COMPARATOR = (mw1, mw2) -> {
        // @formatter:off
            int res = 0;
            if (mw1 instanceof SingleMatchWrapper && !(mw2 instanceof SingleMatchWrapper)) {
                res = 1;
            }
            else if (!(mw1 instanceof SingleMatchWrapper) && mw2 instanceof SingleMatchWrapper) {
                res = -1;
            }
            else if (mw1 instanceof SingleMatchWrapper smw1 && mw2 instanceof SingleMatchWrapper smw2) {
                res = SMW_OPERAND_THEN_OPERATOR_COMPARATOR.compare(smw1, smw2);
            }
            return res;
        // @formatter:on
    };

    /**
     * This comparator brings a list of {@link SingleMatchWrapper}s in the correct order for grouping:
     * <ul>
     * <li>Move all wrappers <i>not</i> being grouping-eligible to the end.</li>
     * <li>Group all wrappers related to the same node type.</li>
     * <li>Move negations after positive conditions (order by match instruction).</li>
     * <li>Keep expressions with the same argName together.</li>
     * <li>Move reference matches after value-matches.</li>
     * <li>Sort by {@link #SMW_OPERAND_THEN_OPERATOR_COMPARATOR} if applicable.</li>
     * <li>Finally order by the contained match expression for deterministic behavior.</li>
     * </ul>
     */
    protected static final Comparator<SingleMatchWrapper> SMW_PREPARATION_ORDER_COMPARATOR = Comparator
    // @formatter:off
                            .comparing(SingleMatchWrapper::isGroupingEligible).reversed()
                            .thenComparing(SingleMatchWrapper::nodeType)
                            .thenComparing(SingleMatchWrapper::matchInstruction)
                            .thenComparing(SingleMatchWrapper::argName)
                            .thenComparing(SingleMatchWrapper::isReferenceMatch)
                            .thenComparing(SMW_OPERAND_THEN_OPERATOR_COMPARATOR)
                            .thenComparing(SingleMatchWrapper::matchExpression);
    // @formatter:on

    /**
     * Orders by node type, moving the node type of the <b>main document to the begin</b>
     */
    protected final Comparator<String> mainFirstNodeTypeComparator;

    /**
     * This comparator brings a list of {@link MatchWrapper}s processing order:
     * <ul>
     * <li>Group all wrappers related to the same node type, {@link #mainFirstNodeTypeComparator} .</li>
     * <li>Move negations after positive conditions.</li>
     * <li>Keep expressions with the same argName together.</li>
     * <li>Move reference matches after value-matches.</li>
     * <li>Move the {@link SingleMatchWrapper}s to the end: {@link #MW_SMW_LAST_COMPARATOR}.</li>
     * <li>Finally order by members: {@link #MW_MEMBERS_COMPARATOR}.</li>
     * </ul>
     */
    protected final Comparator<MatchWrapper> mwFinalOrderComparator;

    /**
     * Sorts leaves after combined elements, leaved ordered by {@link #mwFinalOrderComparator}, combined elements ordered by their members
     */
    protected final Comparator<MatchTreeElement> mteDefaultOrderComparator;

    /**
     * Sorts by common node type (if applicable, hybrids go last) and otherwise by {@link #mteDefaultOrderComparator}
     */
    protected final Comparator<MatchTreeElement> mteCommonNodeTypeOrderComparator;

    /**
     * Context of the current conversion.
     */
    protected final SolrConversionProcessContext ctx;

    /**
     * the node type of the main document
     */
    protected final String mainNodeType;

    /**
     * Creates a new helper instance (per run)
     * 
     * @param ctx
     */
    public DefaultMatchTreeHelper(SolrConversionProcessContext ctx) {
        this.ctx = ctx;
        this.mainFirstNodeTypeComparator = createMainFirstNodeTypeComparator(ctx.getMappingConfig().mainNodeTypeMetaInfo().nodeType());
        this.mwFinalOrderComparator = createMwFinalOrderComparator(mainFirstNodeTypeComparator);
        this.mteDefaultOrderComparator = (mte1, mte2) -> compareMte(mwFinalOrderComparator, mte1, mte2);
        this.mteCommonNodeTypeOrderComparator = createMteCommonNodeTypeOrderComparator(mainFirstNodeTypeComparator, mteDefaultOrderComparator);
        this.mainNodeType = ctx.getMappingConfig().mainNodeTypeMetaInfo().nodeType();
    }

    /**
     * main node type to the begin, otherwise alpha-numeric order
     * 
     * @param ctx
     * @return comparator result
     */
    private static Comparator<String> createMainFirstNodeTypeComparator(String mainNodeType) {
        return (nodeType1, nodeType2) -> {
            if (nodeType1.equals(mainNodeType) && !nodeType2.equals(mainNodeType)) {
                return -1;
            }
            else if (!nodeType1.equals(mainNodeType) && nodeType2.equals(mainNodeType)) {
                return 1;
            }
            else {
                return nodeType1.compareTo(nodeType2);
            }
        };

    }

    /**
     * <ul>
     * <li>Group all wrappers related to the same node type (given comparator).</li>
     * <li>Move elements not eligible for grouping to the end.</li>
     * <li>Move negations after positive conditions.</li>
     * <li>Move negations with existence verification after negations without any restrictions.</li>
     * <li>Keep expressions with the same argName together.</li>
     * <li>Move reference matches after value-matches.</li>
     * <li>Move the {@link SingleMatchWrapper}s to the end: {@link #MW_SMW_LAST_COMPARATOR}.</li>
     * <li>Finally order by members: {@link #MW_MEMBERS_COMPARATOR}.</li>
     * </ul>
     * 
     * @param nodeTypeComparator
     * @return comparator
     */
    private static Comparator<MatchWrapper> createMwFinalOrderComparator(Comparator<String> nodeTypeComparator) {
        Comparator<MatchWrapper> leadComparator = (mw1, mw2) -> nodeTypeComparator.compare(mw1.nodeType(), mw2.nodeType());

        // @formatter:off
        return leadComparator
                    .thenComparing(Comparator.comparing(MatchWrapper::isGroupingEligible).reversed())
                    .thenComparing(MatchWrapper::matchInstruction)
                    .thenComparing(MatchWrapper::argName)
                    .thenComparing(MatchWrapper::isReferenceMatch)
                    .thenComparing(MW_SMW_LAST_COMPARATOR)
                    .thenComparing(MW_MEMBERS_COMPARATOR);
        // @formatter:on

    }

    /**
     * Sorts by common node type (if applicable, hybrids go last) and otherwise by given default comparator
     * 
     * @param nodeTypeComparator
     * @param mteDefaultOrderComparator
     * @return comparator
     */
    private static Comparator<MatchTreeElement> createMteCommonNodeTypeOrderComparator(Comparator<String> nodeTypeComparator,
            Comparator<MatchTreeElement> mteDefaultOrderComparator) {
        return (mte1, mte2) -> {
            int res = 0;
            if (mte1.commonNodeType() == null && mte2.commonNodeType() != null) {
                res = 1;
            }
            else if (mte1.commonNodeType() != null && mte2.commonNodeType() == null) {
                res = -1;
            }
            else if (mte1.commonNodeType() != null) {
                res = nodeTypeComparator.compare(mte1.commonNodeType(), mte2.commonNodeType());
            }
            if (res == 0) {
                res = mteDefaultOrderComparator.compare(mte1, mte2);
            }
            return res;
        };
    }

    /**
     * Compares sorting the leaves to the end, otherwise:
     * <ul>
     * <li>Elements with <code>{@link MatchTreeElement#containsAnyNegation()}=true</code> to the end.</li>
     * <li>Then move leaf elements to the end.</li>
     * <li>Then
     * <ul>
     * <li>Order match wrappers using the given comparator.</li>
     * <li>Order combined elements by their members.</li>
     * </ul>
     * </li>
     * </ul>
     * 
     * @param mwFinalOrderComparator
     * @param mte1
     * @param mte2
     * @return compare result
     */
    private static int compareMte(Comparator<MatchWrapper> mwFinalOrderComparator, MatchTreeElement mte1, MatchTreeElement mte2) {
        int res = 0;
        if (mte1.containsAnyNegation() && !mte2.containsAnyNegation()) {
            res = 1;
        }
        else if (!mte1.containsAnyNegation() && mte2.containsAnyNegation()) {
            res = -1;
        }
        if (res == 0 && mte1.isLeafElement() && !mte2.isLeafElement()) {
            res = 1;
        }
        else if (res == 0 && !mte1.isLeafElement() && mte2.isLeafElement()) {
            res = -1;
        }
        if (res == 0 && mte1 instanceof MatchWrapper mw1 && mte2 instanceof MatchWrapper mw2) {
            res = mwFinalOrderComparator.compare(mw1, mw2);
        }
        else if (res == 0 && mte1 instanceof CombinedMatchTreeElement cmte1 && mte2 instanceof CombinedMatchTreeElement cmte2) {
            res = compareMteMembers(mwFinalOrderComparator, cmte1, cmte2);
        }
        return res;
    }

    /**
     * Recursive member comparison
     * 
     * @param cmte1
     * @param cmte2
     * @return compare result of the first difference
     */
    private static final int compareMteMembers(Comparator<MatchWrapper> mwFinalOrderComparator, CombinedMatchTreeElement cmte1,
            CombinedMatchTreeElement cmte2) {
        List<MatchTreeElement> childElements1 = cmte1.childElements();
        List<MatchTreeElement> childElements2 = cmte2.childElements();
        int res = 0;

        int len = Math.min(childElements1.size(), childElements2.size());
        for (int i = 0; res == 0 && i < len; i++) {
            res = compareMte(mwFinalOrderComparator, childElements1.get(i), childElements2.get(i));
        }

        if (res == 0) {
            res = Integer.compare(childElements1.size(), childElements2.size());
        }

        return res;

    }

    @Override
    public MatchTreeElement consolidateMatchTree(MatchTreeElement root) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("\n\nBEFORE consolidateMatchTree: {}", root.toDebugString());
        }

        MatchTreeElement res = consolidateMatchTreeElementsRecursively(root, CombinedExpressionType.AND, Collections.emptyList());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("\n\nAFTER consolidateMatchTree: {}", res.toDebugString());
        }

        return res;
    }

    @Override
    public List<MatchElement> createExecutionGroups(MatchTreeElement treeElement) {
        if (treeElement instanceof CombinedMatchTreeElement cmte) {
            return findExecutionGroups(cmte);
        }
        else {
            return Collections.singletonList(treeElement);
        }
    }

    /**
     * Detects groups of expressions related to the same node type.
     * <p>
     * Each element in the returned list is either a match condition on the main document or it will cause a join to a nested or dependent document.
     * 
     * @param cmb combined element
     * @return chunks of elements to be executed separately
     */
    protected List<MatchElement> findExecutionGroups(CombinedMatchTreeElement cmb) {

        List<MatchElement> res = new ArrayList<>();

        String groupNodeType = null;

        List<MatchTreeElement> groupMembers = new ArrayList<>();

        for (MatchTreeElement currentElement : cmb.childElements()) {
            if (!currentElement.isGroupingEligible()) {
                addPendingMatchElementAndClearGroup(cmb.combiType(), groupMembers, res);
                res.add(currentElement);
                groupNodeType = null;
            }
            else if (canExpandGroup(groupNodeType, currentElement)) {
                groupMembers.add(currentElement);
            }
            else {
                addPendingMatchElementAndClearGroup(cmb.combiType(), groupMembers, res);
                groupNodeType = currentElement.commonNodeType();
                if (groupNodeType == null) {
                    res.add(currentElement);
                }
                else {
                    groupMembers.add(currentElement);
                }
            }
        }
        addPendingMatchElementAndClearGroup(cmb.combiType(), groupMembers, res);
        return res;
    }

    /**
     * @param groupNodeType
     * @param currentElement
     * @return true if we can add the element to current group
     */
    private boolean canExpandGroup(String groupNodeType, MatchTreeElement currentElement) {
        return groupNodeType != null && groupNodeType.equals(currentElement.commonNodeType());
    }

    /**
     * Checks for pending group members, singletons go as-is into the matchElements list, multiple members will be combined to a
     * {@link NodeTypeMatchTreeElementGroup}
     * <p>
     * Finally, the pending group members list will be cleared
     * 
     * @param groupMembers pending group members (all related to the same node type)
     * @param matchElements destination
     */
    private void addPendingMatchElementAndClearGroup(CombinedExpressionType combiType, List<MatchTreeElement> groupMembers, List<MatchElement> matchElements) {
        if (groupMembers.size() == 1) {
            matchElements.add(groupMembers.get(0));
        }
        else if (!groupMembers.isEmpty()) {
            matchElements.add(new NodeTypeMatchTreeElementGroup(combiType, groupMembers));
        }
        groupMembers.clear();
    }

    /**
     * Sets the eligibility to the requested state
     * 
     * @param <T> input and output type
     * @param matchWrapper
     * @param isGroupingEligible
     * @return the given matchWrapper (no change) or replacement with eligibility updated
     */
    private <T extends MatchWrapper> T setGroupingEligibilityInternal(T matchWrapper, boolean isGroupingEligible) {

        if (matchWrapper.isGroupingEligible() == isGroupingEligible) {
            return matchWrapper;
        }

        switch (matchWrapper) {
        case SingleMatchWrapper smw:
            @SuppressWarnings("unchecked")
            T smwReplaced = (T) new SingleMatchWrapper(smw.nodeType(), smw.matchExpression(), smw.matchInstruction(), isGroupingEligible);
            return smwReplaced;
        case MultiMatchWrapper mmw:
            @SuppressWarnings("unchecked")
            T mmwReplaced = (T) new MultiMatchWrapper(mmw.nodeType(), mmw.members(), mmw.matchInstruction(), isGroupingEligible);
            return mmwReplaced;
        default:
            throw new IllegalStateException("Unexpected element: " + matchWrapper);
        }
    }

    /**
     * Applies a couple of rules to eventually set the grouping eligibility.
     * 
     * @param <T> input and output type
     * @param matchWrapper
     * @param isPinnedSubDocument if true we must apply special handling to multi-doc fields
     * @return the given matchWrapper (no change) or replacement with eligibility updated
     */
    private <T extends MatchWrapper> T adjustGroupingEligibility(T matchWrapper, boolean isPinnedSubDocument) {

        // case I: It is a negation, only eligible if the surrounding conditions do not pin the document
        if (!matchWrapper.commonNodeType().equals(mainNodeType) && matchWrapper.isNegation()) {
            return setGroupingEligibilityInternal(matchWrapper, false);
        }

        // case II: sub-document is not pinned, so we can safely group any conditions
        if (!isPinnedSubDocument) {
            return setGroupingEligibilityInternal(matchWrapper, true);
        }

        // case III: a match involving any field assignment marked multi-doc is NOT eligible for grouping
        ArgFieldAssignment assignmentLeft = ctx.getMappingConfig().lookupAssignment(matchWrapper.argName(), ctx);
        if (assignmentLeft.isMultiDoc()) {
            return setGroupingEligibilityInternal(matchWrapper, false);
        }
        else {
            String argNameRight = matchWrapper.referencedArgName();
            if (argNameRight != null) {
                ArgFieldAssignment assignmentRight = ctx.getMappingConfig().lookupAssignment(argNameRight, ctx);
                return setGroupingEligibilityInternal(matchWrapper, !assignmentRight.isMultiDoc());
            }
            else {
                return setGroupingEligibilityInternal(matchWrapper, true);
            }
        }

    }

    /**
     * For Solr we must create separate joins to nested or dependent documents of a particular sub-node-type.
     * <p>
     * Whenever we start a join it depends on the combination type of conditions whether we are "pinning" a (set of) document(s).<br>
     * E.g.: Be <code><b>C1, C2, C3</b></code> conditions on the same nested or dependent document <b>X</b> and <code><b>C1 AND (C2 OR C3)</b></code> a combined
     * condition. In this case <code><b>C1</b></code> "pins" the set documents from the perspective of <code><b>(C2 OR C3)</b></code> resp.
     * <code><b>(C2 OR C3)</b></code> "pins" the set documents from the perspective of <code><b>C1</b></code>.<br>
     * If any of these conditions contains a field mapped <i>multi-doc</i> this becomes important, because suddenly <code><b>C1 AND (C2 OR C3)</b></code> can
     * become true with <code><b>C1</b></code> and <code><b>(C2 OR C3)</b></code> applied to <i>different documents</i>. In a single join that would fail.
     * <p>
     * The purpose of this method is determining whether we are inside such a "pinning path" or not.
     * <p>
     * <b>Clarification:</b> The result list contains node types, so this tells us which type (or types in case of a hybrid AND) are logically pinned by this
     * element, see also {@link #detectPinnedSubDocumentsAndCase(CombinedMatchTreeElement)}.
     * 
     * @param cmte
     * @param parentPinnedDocuments
     * @return new pinned documents or empty
     */
    private List<String> detectPinnedSubDocuments(CombinedMatchTreeElement cmte, List<String> parentPinnedDocuments) {

        String commonNodeType = cmte.commonNodeType();

        if (mainNodeType.equals(commonNodeType)) {
            // it is only the main node type (not inside a document join)
            return Collections.emptyList();
        }

        if (commonNodeType != null && (cmte.combiType() == CombinedExpressionType.AND || parentPinnedDocuments.contains(commonNodeType))) {
            // the common node type is pinned, so it remains pinned
            return Collections.singletonList(commonNodeType);
        }

        if (cmte.combiType() == CombinedExpressionType.OR) {
            // no common node type or a node type change: means we are at main document level to start new join(s)
            return Collections.emptyList();
        }
        else {
            // AND-case, hybrid (no common node type):
            // documents pinned by the parent are irrelevant here, because the hybrid case means we will anyway start a fresh join
            // to each involved nested or dependent document
            return detectPinnedSubDocumentsAndCase(cmte);
        }
    }

    /**
     * Two members of an AND influence each other if both of them require the same node type.
     * <p>
     * One member would "pin" the document instance potentially preventing the other member from matching<br>
     * e.g. <b><code>fact.color=blue AND fact.color=red AND main.country=USA</code></b> with <i>fact</i> being a dependent document with multi-doc enabled.<br>
     * In this case the expression would yield false if we tried to evaluate it inside a single join but can become true if we use separate joins
     * 
     * @param cmteAnd to be analyzed
     * @return list of pinned documents (node types)
     */
    private List<String> detectPinnedSubDocumentsAndCase(CombinedMatchTreeElement cmteAnd) {

        Set<String> nodeTypesPinnedEarlier = new HashSet<>();
        List<String> res = new ArrayList<>();

        for (MatchTreeElement member : cmteAnd.childElements()) {
            String memberCommonNodeType = member.commonNodeType();

            // a hybrid combined member (no common node type) cannot pin any document
            // main document never pins
            if (memberCommonNodeType != null && !memberCommonNodeType.equals(mainNodeType)) {
                if (nodeTypesPinnedEarlier.contains(memberCommonNodeType) && !res.contains(memberCommonNodeType)) {
                    res.add(memberCommonNodeType);
                }
                nodeTypesPinnedEarlier.add(memberCommonNodeType);
            }
        }
        return res;
    }

    /**
     * Rebuild the tree recursively
     * 
     * @param matchTreeElement
     * @param parentCombiType element is located inside an AND vs. OR
     * @param pinnedSubDocuments nested or dependent documents "pinned" in the current branch
     * @return consolidated element
     */
    private MatchTreeElement consolidateMatchTreeElementsRecursively(MatchTreeElement matchTreeElement, CombinedExpressionType parentCombiType,
            List<String> pinnedSubDocuments) {

        if (matchTreeElement instanceof MatchWrapper matchWrapper) {
            return adjustGroupingEligibility(matchWrapper, pinnedSubDocuments.contains(matchWrapper.commonNodeType()));
        }

        CombinedMatchTreeElement cmte = (CombinedMatchTreeElement) matchTreeElement;

        List<String> currentPinnedSubDocuments = detectPinnedSubDocuments(cmte, pinnedSubDocuments);

        List<SingleMatchWrapper> singleMatchWrappers = new ArrayList<>();
        List<MatchTreeElement> otherElements = new ArrayList<>();
        splitMatchWrappers(matchTreeElement.childElements(), singleMatchWrappers, otherElements);

        singleMatchWrappers = prepareSingleMatchWrappers(singleMatchWrappers, currentPinnedSubDocuments);

        // for all the combined elements we recursively repeat the exercise
        otherElements = otherElements.stream().map(e -> this.consolidateMatchTreeElementsRecursively(e, cmte.combiType(), currentPinnedSubDocuments))
                .collect(Collectors.toCollection(ArrayList::new));

        List<List<SingleMatchWrapper>> smwByNodeType = groupByNodeType(singleMatchWrappers);

        List<MatchWrapper> combinedMatchWrappers = new ArrayList<>();
        List<SingleMatchWrapper> remainingMatchWrappers = new ArrayList<>();

        for (List<SingleMatchWrapper> orderedNodeTypeMatchWrappers : smwByNodeType) {
            extractMultiMatches(cmte.combiType(), parentCombiType, orderedNodeTypeMatchWrappers, combinedMatchWrappers, remainingMatchWrappers);
        }

        tryCreateBetweenMatchWrappers(cmte.combiType(), combinedMatchWrappers, remainingMatchWrappers, otherElements);

        List<MatchTreeElement> updated = mergeAndSort(combinedMatchWrappers, remainingMatchWrappers, otherElements);

        if (updated.size() > 1 && cmte.combiType() == CombinedExpressionType.OR) {
            removeRedundantIsUnknownChecksFromOr(updated);
        }

        if (updated.size() == 1) {
            return updated.get(0);
        }
        else {
            MatchTreeElement res = new CombinedMatchTreeElement(cmte.combiType(), updated);
            if (res.equals(matchTreeElement)) {
                res = matchTreeElement;
            }
            return res;
        }
    }

    /**
     * This method addresses <b>ISSUE #2</b> <i>Default negation causes redundant query part</i>
     * <p>
     * When we recombine an OR we test if there is a NOT ANY_VALUE_MATCH (semantically "is unknown") for any argument where we have another negative condition
     * for. If so, we remove the redundant condition.
     * <p>
     * <b>Example:</b> The set <b><code>NOT color:*</code></b> (no color at all) is <i>fully contained</i> in the set <b><code>NOT color:red</code></b>, so we
     * can eliminate <b><code>NOT color:*</code></b> from the OR without affecting the result.
     * 
     * @param orMembers members of the OR to be created
     */
    private void removeRedundantIsUnknownChecksFromOr(List<MatchTreeElement> orMembers) {

        List<SingleMatchWrapper> candidates = new ArrayList<>();
        List<String> coveredIsUnknownArgNames = new ArrayList<>();

        // @formatter:off
        orMembers.stream().filter(MatchWrapper.class::isInstance)
                          .map(MatchWrapper.class::cast)
                          .filter(MatchWrapper::isNegation)
                          .forEach(matchWrapper -> {
        // @formatter:on
                    if (matchWrapper.type() == MatchWrapperType.ANY_VALUE_MATCH) {
                        candidates.add((SingleMatchWrapper) matchWrapper);
                    }
                    else {
                        String argNameLeft = matchWrapper.argName();
                        String argNameRight = matchWrapper.referencedArgName();
                        if (!coveredIsUnknownArgNames.contains(argNameLeft)) {
                            coveredIsUnknownArgNames.add(argNameLeft);
                        }
                        if (!coveredIsUnknownArgNames.contains(argNameRight)) {
                            coveredIsUnknownArgNames.add(argNameRight);
                        }
                    }
                });

        removeRedundantIsUnknownChecksFromOr(orMembers, candidates, coveredIsUnknownArgNames);

    }

    /**
     * Removes any candidate from the orMembers if the argName is contained in the list of covered arg names
     * 
     * @param orMembers
     * @param candidates
     * @param coveredIsUnknownArgNames
     */
    private void removeRedundantIsUnknownChecksFromOr(List<MatchTreeElement> orMembers, List<SingleMatchWrapper> candidates,
            List<String> coveredIsUnknownArgNames) {
        if (!candidates.isEmpty() && !coveredIsUnknownArgNames.isEmpty()) {
            for (SingleMatchWrapper candidate : candidates) {
                if (coveredIsUnknownArgNames.contains(candidate.argName())) {
                    orMembers.remove(candidate);
                }
            }
        }
    }

    /**
     * Prepares the list of single match wrappers on the current hierarchy level (and AND or OR combination).
     * <p>
     * Therefore we update (detect) the eligibility for grouping and bring the elements in preparation order.
     * 
     * @param singleMatchWrappers
     * @param pinnedSubDocuments nested or dependent documents "pinned" in the current branch
     * @return sorted list of wrappers
     * @see DefaultMatchTreeHelper#SMW_PREPARATION_ORDER_COMPARATOR
     */
    private List<SingleMatchWrapper> prepareSingleMatchWrappers(List<SingleMatchWrapper> singleMatchWrappers, List<String> pinnedSubDocuments) {
        singleMatchWrappers = singleMatchWrappers.stream().map(smw -> adjustGroupingEligibility(smw, pinnedSubDocuments.contains(smw.commonNodeType())))
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.sort(singleMatchWrappers, SMW_PREPARATION_ORDER_COMPARATOR);
        return singleMatchWrappers;
    }

    /**
     * Fills only the single match wrappers from the input in the given target list and moves all others into the other list.
     * 
     * @param input (won't be changed)
     * @param singleMatchWrappers empty mutable list to be filled
     * @param otherElements empty mutable list to be filled
     */
    private void splitMatchWrappers(List<MatchTreeElement> input, List<SingleMatchWrapper> singleMatchWrappers, List<MatchTreeElement> otherElements) {
        for (MatchTreeElement inputElement : input) {
            if (inputElement instanceof SingleMatchWrapper smw) {
                singleMatchWrappers.add(smw);
            }
            else {
                otherElements.add(inputElement);
            }
        }
    }

    /**
     * This step looks for a chance to combined gt/lt resp. gte/lte to a {@link BetweenMatchWrapper}
     * <p>
     * <b>Important:</b> This method <i>intentionally</i> avoids creating BETWEENs in certain situations to enforce semantic consistency.<br>
     * Please refer to the {@link BetweenMatchWrapper} documentation.
     * 
     * @param combiType the combination type of the given elements to distinguish IN-clause from NOT IN
     * @param combinedMatchWrappers
     * @param remainingMatchWrappers
     * @see BetweenMatchWrapper
     */
    protected void tryCreateBetweenMatchWrappers(CombinedExpressionType combiType, List<MatchWrapper> combinedMatchWrappers,
            List<SingleMatchWrapper> remainingMatchWrappers, List<MatchTreeElement> otherElements) {

        List<MatchWrapper> lowerBoundCandidates = new ArrayList<>();
        List<MatchWrapper> upperBoundCandidates = new ArrayList<>();

        collectBetweenBoundCandidates(combiType, combinedMatchWrappers, lowerBoundCandidates, upperBoundCandidates);
        collectBetweenBoundCandidates(combiType, otherElements, lowerBoundCandidates, upperBoundCandidates);
        collectBetweenBoundCandidates(combiType, remainingMatchWrappers, lowerBoundCandidates, upperBoundCandidates);

        List<MatchWrapper> consumed = new ArrayList<>();

        for (MatchWrapper lowerBound : lowerBoundCandidates) {
            MatchWrapper upperBound = findUpperBound(lowerBound, upperBoundCandidates);
            if (upperBound != null) {
                combinedMatchWrappers.add(new BetweenMatchWrapper(lowerBound, upperBound));
                consumed.add(lowerBound);
                consumed.add(upperBound);
                upperBoundCandidates.remove(upperBound);
            }
        }

        for (MatchWrapper matchWrapper : consumed) {
            if (matchWrapper instanceof SingleMatchWrapper smw) {
                remainingMatchWrappers.remove(smw);
            }
            else {
                combinedMatchWrappers.remove(matchWrapper);
                otherElements.remove(matchWrapper);
            }
        }
    }

    /**
     * @param lowerBound
     * @param upperBoundCandidates
     * @return an upper bound from the list matching the given lower bound, or null if not found
     */
    private MatchWrapper findUpperBound(MatchWrapper lowerBound, List<MatchWrapper> upperBoundCandidates) {
        for (MatchWrapper upperBoundCandidate : upperBoundCandidates) {
            if (lowerBound.argName().equals(upperBoundCandidate.argName())
                    && !lowerBound.firstMember().operand().value().equals(upperBoundCandidate.firstMember().operand().value())) {
                return upperBoundCandidate;
            }
        }
        return null;
    }

    /**
     * Collects bound-candidates from the given elements list
     * 
     * @param combiType decides whether this will become a (NOT) IN clause
     * @param matchTreeElements input
     * @param lowerBoundCandidates output
     * @param upperBoundCandidates output
     */
    private void collectBetweenBoundCandidates(CombinedExpressionType combiType, List<? extends MatchTreeElement> matchTreeElements,
            List<MatchWrapper> lowerBoundCandidates, List<MatchWrapper> upperBoundCandidates) {

        boolean expectNegation = (combiType == CombinedExpressionType.OR);

        for (MatchTreeElement mte : matchTreeElements) {
            if (mte instanceof MatchWrapper matchWrapper && matchWrapper.isNegation() == expectNegation) {
                if (isBetweenBoundCandidate(matchWrapper, MatchOperator.GREATER_THAN)) {
                    lowerBoundCandidates.add(matchWrapper);
                }
                else if (isBetweenBoundCandidate(matchWrapper, MatchOperator.LESS_THAN)) {
                    upperBoundCandidates.add(matchWrapper);
                }
            }
        }

    }

    /**
     * @param matchWrapper
     * @return true if the given match element is related to a field that is either a collection or its assignment is marked multi-doc
     */
    private boolean isCollectionFieldOrMultiDoc(MatchWrapper matchWrapper) {
        ArgFieldAssignment afa = ctx.getMappingConfig().lookupAssignment(matchWrapper.argName(), ctx);
        return afa.isMultiDoc() || afa.field().isCollection();
    }

    /**
     * Determines if the given match wrapper could be a lower or upper bound in a BETWEEN.
     * <p>
     * This depends on the kind of operation but also on way of the mapping, see {@link BetweenMatchWrapper}
     * 
     * @param matchWrapper
     * @param operator
     * @return true if this could be a bound
     */
    private boolean isBetweenBoundCandidate(MatchWrapper matchWrapper, MatchOperator operator) {

        // @formatter:off
        return (matchWrapper != null && !matchWrapper.isReferenceMatch() 
                && (matchWrapper.type() == VALUE_MATCH || matchWrapper.type() == VALUE_OR_EQ_MATCH)
                && (matchWrapper.firstMember().operator() == operator)
                && !isCollectionFieldOrMultiDoc(matchWrapper));
        // @formatter:on
    }

    /**
     * Creates the list of match tree elements in the given order and finally sorts the list to ensure all elements for the same node types appear in continuous
     * chunks.
     * 
     * @param combinedMatchWrappers (come first)
     * @param remainingMatchWrappers
     * @param otherElements
     * @return element list for execution (mutable)
     */
    protected List<MatchTreeElement> mergeAndSort(List<? extends MatchWrapper> combinedMatchWrappers, List<SingleMatchWrapper> remainingMatchWrappers,
            List<MatchTreeElement> otherElements) {
        List<MatchTreeElement> res = new ArrayList<>();
        res.addAll(combinedMatchWrappers);
        res.addAll(remainingMatchWrappers);
        res.addAll(otherElements);
        Collections.sort(res, mteCommonNodeTypeOrderComparator);
        return res;
    }

    /**
     * Given a list of wrappers ordered by node type this method creates a list of sub-lists, each containing the elements related to one node type.
     * <b>Important:</b> The order of the given elements is crucial for achieving the maximum group size to avoid unnecessary joins.
     * 
     * @param typeOrderdMatchWrappers input, a pre-ordered list so that all wrappers related to the same node type appear in continuous chunks
     * @return list of lists, one list per node type
     */
    protected List<List<SingleMatchWrapper>> groupByNodeType(List<SingleMatchWrapper> typeOrderdMatchWrappers) {

        List<List<SingleMatchWrapper>> res = new ArrayList<>();

        List<SingleMatchWrapper> currentList = null;

        String currentNodeType = null;

        for (SingleMatchWrapper matchWrapper : typeOrderdMatchWrappers) {
            if (!matchWrapper.nodeType().equals(currentNodeType)) {
                currentList = new ArrayList<>();
                res.add(currentList);
                currentNodeType = matchWrapper.nodeType();
            }
            currentList.add(matchWrapper);
        }
        return res;
    }

    /**
     * This method "re-creates" (NOT)IN-clauses as well as the combinations LESS/GREATER-THAN-OR-EQUALS as {@link MultiMatchWrapper}.
     * 
     * @param combiType the current combination type to group either negative or positive wrappers but never mixed
     * @param parentCombiType the combi type one level above, just in case the element collapses into a single multi-match-wrapper
     * @param orderedNodeTypeMatchWrappers input (won't be changed)
     * @param combinedMatchWrappers to add the detected multi-matches
     * @param remainingMatchWrappers to return all the remaining wrappers which were not subject to any grouping
     */
    protected void extractMultiMatches(CombinedExpressionType combiType, CombinedExpressionType parentCombiType,
            List<SingleMatchWrapper> orderedNodeTypeMatchWrappers, List<MatchWrapper> combinedMatchWrappers, List<SingleMatchWrapper> remainingMatchWrappers) {
        List<SingleMatchWrapper> remainingMatchWrappersTemp = new ArrayList<>();
        extractLtGtMultiMatches(combiType, orderedNodeTypeMatchWrappers, combinedMatchWrappers, remainingMatchWrappersTemp);
        extractStandardMultiMatches(combiType, remainingMatchWrappersTemp, combinedMatchWrappers, remainingMatchWrappers);
    }

    /**
     * Finds <code>&lt;=</code> resp. <code>&gt;=</code>
     * 
     * @param combiType indicates whether we can group or not (in an OR we group positive, in an AND only negative, but never mixed)
     * @param orderedNodeTypeMatchWrappers input, sorted
     * @param combinedMatchWrappers to store elements
     * @param remainingMatchWrappers to store not consumed elements
     */
    private void extractLtGtMultiMatches(CombinedExpressionType combiType, List<SingleMatchWrapper> orderedNodeTypeMatchWrappers,
            List<MatchWrapper> combinedMatchWrappers, List<SingleMatchWrapper> remainingMatchWrappers) {
        int idxLeft = 0;
        for (; idxLeft < orderedNodeTypeMatchWrappers.size(); idxLeft++) {
            SingleMatchWrapper left = orderedNodeTypeMatchWrappers.get(idxLeft);
            if (idxLeft < orderedNodeTypeMatchWrappers.size() - 1
                    && (left.operator() == MatchOperator.LESS_THAN || left.operator() == MatchOperator.GREATER_THAN)) {
                SingleMatchWrapper right = orderedNodeTypeMatchWrappers.get(idxLeft + 1);
                if (canCombineInLtGtMultiMatch(combiType, left, right)) {
                    combinedMatchWrappers.add(new MultiMatchWrapper(left.nodeType(), Arrays.asList(left.matchExpression(), right.matchExpression()),
                            left.matchInstruction(), left.isGroupingEligible()));
                    assertSameMatchInstructionInMultiMatch(left, right, combiType, orderedNodeTypeMatchWrappers);
                    idxLeft++;
                }
                else {
                    remainingMatchWrappers.add(left);
                }
            }
            else {
                remainingMatchWrappers.add(left);
            }
        }
    }

    /**
     * Finds logical IN-clauses
     * 
     * @param combiType indicates whether we can group or not (in an OR we group positive, in an AND only negative, but never mixed)
     * @param orderedNodeTypeMatchWrappers input, sorted
     * @param combinedMatchWrappers to store elements
     * @param remainingMatchWrappers to store not consumed elements
     */
    private void extractStandardMultiMatches(CombinedExpressionType combiType, List<SingleMatchWrapper> orderedNodeTypeMatchWrappers,
            List<MatchWrapper> combinedMatchWrappers, List<SingleMatchWrapper> remainingMatchWrappers) {
        int idxLeft = 0;
        for (; idxLeft < orderedNodeTypeMatchWrappers.size(); idxLeft++) {
            SingleMatchWrapper left = orderedNodeTypeMatchWrappers.get(idxLeft);
            if (idxLeft < orderedNodeTypeMatchWrappers.size() - 1 && left.operator() == MatchOperator.EQUALS && !isDateAlignmentRequired(left)
                    && !left.isReferenceMatch()) {
                int idxUpd = extractStandardMultiMatches(combiType, orderedNodeTypeMatchWrappers, left, idxLeft, combinedMatchWrappers);
                if (idxUpd > idxLeft) {
                    idxLeft = idxUpd;
                }
                else {
                    remainingMatchWrappers.add(left);
                }
            }
            else {
                remainingMatchWrappers.add(left);
            }
        }
    }

    /**
     * @param matchWrapper
     * @return true if date alignment is required for the given match condition
     * @see SolrFormatUtils#shouldAlignDate(de.calamanari.adl.cnv.tps.AdlType, de.calamanari.adl.solr.AdlSolrType)
     */
    protected boolean isDateAlignmentRequired(SingleMatchWrapper matchWrapper) {
        if (matchWrapper.isReferenceMatch() || SolrConversionDirective.DISABLE_DATE_TIME_ALIGNMENT.check(ctx.getGlobalFlags())) {
            return false;
        }
        ArgFieldAssignment assignment = ctx.getMappingConfig().lookupAssignment(matchWrapper.argName(), ctx);
        return SolrFormatUtils.shouldAlignDate(assignment.arg().type(), assignment.field().fieldType());
    }

    /**
     * Tries to find neighbors for a multi-match
     * 
     * @param combiType
     * @param orderedNodeTypeMatchWrappers
     * @param left
     * @param idxLeft
     * @param combinedMatchWrappers
     * @return the new position in the members list (index of the last consumed element, idxLeft by default)
     */
    private int extractStandardMultiMatches(CombinedExpressionType combiType, List<SingleMatchWrapper> orderedNodeTypeMatchWrappers, SingleMatchWrapper left,
            int idxLeft, List<MatchWrapper> combinedMatchWrappers) {
        List<MatchExpression> candidates = new ArrayList<>();
        for (int idxRight = idxLeft + 1; idxRight < orderedNodeTypeMatchWrappers.size(); idxRight++) {
            SingleMatchWrapper right = orderedNodeTypeMatchWrappers.get(idxRight);
            if (canCombineInStandardMultiMatch(combiType, left, right)) {
                candidates.add(right.matchExpression());
                assertSameMatchInstructionInMultiMatch(left, right, combiType, orderedNodeTypeMatchWrappers);
            }
            else {
                break;
            }
        }
        if (!candidates.isEmpty()) {
            idxLeft = idxLeft + candidates.size();
            candidates.add(0, left.matchExpression());
            combinedMatchWrappers.add(new MultiMatchWrapper(left.nodeType(), candidates, left.matchInstruction(), left.isGroupingEligible()));
        }
        return idxLeft;
    }

    /**
     * Tells whether we can group two elements (less/greater than or equal) or not, which depends on the characteristics plus the combiType
     * 
     * @param combiType
     * @param left
     * @param right
     * @return true if the two can be combined
     */
    private boolean canCombineInLtGtMultiMatch(CombinedExpressionType combiType, SingleMatchWrapper left, SingleMatchWrapper right) {
        boolean expectNegate = (combiType == CombinedExpressionType.AND);
        return (left.argName().equals(right.argName())
                && (left.matchExpression().operator() == MatchOperator.LESS_THAN || left.matchExpression().operator() == MatchOperator.GREATER_THAN)
                && right.matchExpression().operator() == MatchOperator.EQUALS && left.matchExpression().operand().equals(right.matchExpression().operand())
                && left.isNegation() == expectNegate && right.isNegation() == expectNegate);
    }

    /**
     * Tells whether we can group two elements (IN-clause) or not, which depends on the characteristics plus the combiType
     * 
     * @param combiType
     * @param left
     * @param right
     * @return true if the two can be combined
     */
    private boolean canCombineInStandardMultiMatch(CombinedExpressionType combiType, SingleMatchWrapper left, SingleMatchWrapper right) {

        boolean expectNegate = (combiType == CombinedExpressionType.AND);
        return (left.argName().equals(right.argName()) && left.matchExpression().operator() == MatchOperator.EQUALS
                && right.matchExpression().operator() == MatchOperator.EQUALS && !left.isReferenceMatch() && !right.isReferenceMatch()
                && left.isNegation() == expectNegate && right.isNegation() == expectNegate);
    }

    /**
     * Safety: According to the current logic we expect the two instructions to be the same, if not, throw an exception (it's a bug)
     * 
     * @param left
     * @param right
     */
    protected static void assertSameMatchInstructionInMultiMatch(SingleMatchWrapper left, SingleMatchWrapper right, CombinedExpressionType combiType,
            List<SingleMatchWrapper> orderedNodeTypeMatchWrappers) {
        if (left.matchInstruction() != right.matchInstruction()) {
            // either we are trying to combine wrappers for a multi-match that cannot be combined (bug in this class)
            // or a previous step decided to create different instructions for the same argName on the same level of the expression (bug in the converter)
            throw new IllegalStateException(String.format(
                    "Implementation error: two wrappers were expected to have identical instructions for creating a multi-match, "
                            + "but they did not, given: left=%s, right=%s, combiType=%s, orderedNodeTypeMatchWrappers=%s",
                    left, right, combiType, orderedNodeTypeMatchWrappers));
        }
    }

}
