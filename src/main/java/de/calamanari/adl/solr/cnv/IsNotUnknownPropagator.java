//@formatter:off
/*
 * IsNotUnknownBubbler
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import de.calamanari.adl.CombinedExpressionType;
import de.calamanari.adl.irl.CombinedExpression;
import de.calamanari.adl.irl.CoreExpression;
import de.calamanari.adl.irl.MatchExpression;
import de.calamanari.adl.irl.MatchOperator;
import de.calamanari.adl.irl.NegationExpression;
import de.calamanari.adl.irl.SimpleExpression;

/**
 * The {@link IsNotUnknownPropagator} addresses the problem that any "NOT argName <i>&lt;op&gt;</i> value" expression is STRICT and thus requires a "co-match"
 * to verify that the referenced argName has <i>any</i> value. The propagator lets these NOT-IS-UNKNOWN checks <i>bubble up</i> to the most outer position in
 * the expression's DAG to avoid redundant parts in the same expression (given there are multiple NOT-conditions related to the same argument).
 * <p>
 * The method {@link #process(CoreExpression)} should be called during the preparation step and creates an alternative root expression if required.
 * <p>
 * <b>Background:</b> Solr relies on <i>subtracting sets</i> to realize a <b><code>NOT</code></b>. This creates a problem in conjunction with Solr's dynamic
 * fields. E.g., if you take all records and subtract the ones with <code>color_s:blue</code> then you implicitly <i>include</i> all records which don't have
 * any value for the field <code>color_s</code>. Audlang defines this as a default (non-STRICT) negation (see
 * <a href="https://github.com/KarlEilebrecht/audlang-spec/blob/main/doc/AudienceDefinitionLanguageSpecification.md#5-negation">§5 Audlang Spec</a>).<br>
 * As a consequence, whenever we want to express a STRICT negation we must additionally verify that the involved argName(s) have any value.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 * @see MatchInstruction
 */
public class IsNotUnknownPropagator {

    /**
     * For each expression we track if it implies any argName or referenced argName not to be unknown.
     */
    private final Map<CoreExpression, Set<String>> requiredIsNotUnknownsArgNames = new HashMap<>();

    /**
     * For each expression we track recursively all argNames involved in negations.
     */
    private final Map<CoreExpression, Set<String>> argNamesInNegations = new HashMap<>();

    /**
     * Re-builds the given root expression if required to bring IS-NOT-UNKNOWNS to the most outer position in the expression's DAG.
     * 
     * @param rootExpression
     * @return rootExpression or updated root expression if any IS NOT UNKNOWN has been propagated from an inner to an outer position
     */
    public static CoreExpression process(CoreExpression rootExpression) {
        if (rootExpression instanceof CombinedExpression cmb) {
            IsNotUnknownPropagator propagator = new IsNotUnknownPropagator();
            propagator.findRequiredIsNotUnknownsBottomUp(rootExpression);
            CoreExpression upd = propagator.rebuildExpression(cmb, Collections.emptySet(), Collections.emptySet());
            // this is just nice-to-have: should the operation cause a change which
            // is not a change (because of redundancy), then we return the original root expression
            if (!upd.equals(rootExpression)) {
                return upd;
            }
        }
        return rootExpression;
    }

    /**
     * Step 1: we recursively track the implications bottom-up.
     * <p>
     * Finally, we will know for each part of the expression (including the root expression) what arguments must not be unknown.
     * 
     * @param expression
     */
    private void findRequiredIsNotUnknownsBottomUp(CoreExpression expression) {

        if (requiredIsNotUnknownsArgNames.containsKey(expression)) {
            // already seen
            return;
        }
        Set<String> req = new HashSet<>();

        switch (expression) {
        case SimpleExpression simple when (simple instanceof NegationExpression || simple.operator() != MatchOperator.IS_UNKNOWN):
            req.add(simple.argName());
            if (simple.referencedArgName() != null) {
                req.add(simple.referencedArgName());
            }
            if (simple instanceof NegationExpression neg) {
                findRequiredIsNotUnknownsBottomUp(neg.delegate());
                registerArgNamesInNegation(neg);
            }
            requiredIsNotUnknownsArgNames.put(expression, req);
            break;
        case CombinedExpression cmbAnd when cmbAnd.combiType() == CombinedExpressionType.AND:
            cmbAnd.childExpressions().stream().forEach(this::findRequiredIsNotUnknownsBottomUp);
            cmbAnd.childExpressions().stream().map(this::lookupRequiredIsNotUnknowns).flatMap(Set::stream).forEach(req::add);
            requiredIsNotUnknownsArgNames.put(expression, req);
            argNamesInNegations.put(expression,
                    cmbAnd.childExpressions().stream().map(this::lookupArgNamesInNegation).flatMap(Set::stream).collect(Collectors.toCollection(HashSet::new)));
            break;
        case CombinedExpression cmbOr when cmbOr.combiType() == CombinedExpressionType.OR:
            req.addAll(findIsNotUnknownsRequiredByAllOrMembers(cmbOr));
            requiredIsNotUnknownsArgNames.put(expression, req);
            argNamesInNegations.put(expression,
                    cmbOr.childExpressions().stream().map(this::lookupArgNamesInNegation).flatMap(Set::stream).collect(Collectors.toCollection(HashSet::new)));
            break;
        default:
            requiredIsNotUnknownsArgNames.put(expression, req);
        }
    }

    /**
     * Registers the argName(s) from the given negation
     * 
     * @param neg
     */
    private void registerArgNamesInNegation(NegationExpression neg) {
        if (neg.operator() != MatchOperator.IS_UNKNOWN) {
            Set<String> argNames = new HashSet<>();
            argNames.add(neg.argName());
            if (neg.referencedArgName() != null) {
                argNames.add(neg.referencedArgName());
            }
            argNamesInNegations.put(neg, argNames);
        }
    }

    /**
     * @param cmbOr
     * @return intersection of the is-not-unknowns of all or-members
     */
    private Set<String> findIsNotUnknownsRequiredByAllOrMembers(CombinedExpression cmbOr) {
        cmbOr.childExpressions().stream().forEach(this::findRequiredIsNotUnknownsBottomUp);
        Set<String> reqByAllOrMembers = new HashSet<>();
        for (CoreExpression orMember : cmbOr.childExpressions()) {
            Set<String> reqByCurrentOrMember = lookupRequiredIsNotUnknowns(orMember);
            if (reqByAllOrMembers.isEmpty()) {
                reqByAllOrMembers.addAll(reqByCurrentOrMember);
            }
            else {
                reqByAllOrMembers.retainAll(reqByCurrentOrMember);
            }
            if (reqByAllOrMembers.isEmpty()) {
                break;
            }
        }
        return reqByAllOrMembers;
    }

    /**
     * Rebuilds the combined expression after processing all the members
     * 
     * @param cmb to be rebuilt if required
     * @param parentIsNotUnknowns IS-NOT-UNKNOWNS upwards in the expression
     * @param parentOptionalIsUnknowns optional IS UNKNOWNS (or'd upwards in the expression)
     * @return
     */
    private CoreExpression rebuildCombinedExpressionRecursively(CombinedExpression cmb, Set<String> parentIsNotUnknowns, Set<String> parentOptionalIsUnknowns) {
        boolean haveMemberChange = false;
        List<CoreExpression> cmbMembers = new ArrayList<>();
        Set<String> currentIsNotUnknowns = new HashSet<>(parentIsNotUnknowns);
        currentIsNotUnknowns.addAll(requiredIsNotUnknownsArgNames.get(cmb));

        Set<String> currentOptionalIsUnknowns = new HashSet<>(parentOptionalIsUnknowns);
        if (cmb.combiType() == CombinedExpressionType.OR) {
            cmb.childExpressions().stream().filter(MatchExpression.class::isInstance).map(MatchExpression.class::cast)
                    .filter(e -> e.operator() == MatchOperator.IS_UNKNOWN).map(MatchExpression::argName).forEach(currentOptionalIsUnknowns::add);
        }

        for (CoreExpression member : cmb.childExpressions()) {
            CoreExpression memberUpd = rebuildExpression(member, currentIsNotUnknowns, currentOptionalIsUnknowns);
            cmbMembers.add(memberUpd);
            if (memberUpd != member) {
                haveMemberChange = true;
            }
        }

        if (haveMemberChange) {
            return CombinedExpression.of(cmbMembers, cmb.combiType());
        }
        return cmb;
    }

    /**
     * Step 2: Rebuilds the expression from the given one (root) so that relevant IS NOT UNKNOWN checks bubble up to the most outer position.
     * 
     * @param expression
     * @param parentIsNotUnknowns IS-NOT-UNKNOWNS upwards in the expression
     * @param parentOptionalIsUnknowns optional IS UNKNOWNS (or'd upwards in the expression)
     * @return new (root) expression
     */
    private CoreExpression rebuildExpression(CoreExpression expression, Set<String> parentIsNotUnknowns, Set<String> parentOptionalIsUnknowns) {

        Set<String> currentArgNamesInNegations = lookupArgNamesInNegation(expression);

        if (currentArgNamesInNegations.isEmpty()) {
            return expression;
        }

        // @formatter:off
        Set<String> effectivelyRequiredIsNotUnknowns = requiredIsNotUnknownsArgNames.get(expression).stream()
                .filter(Predicate.not(parentIsNotUnknowns::contains))
                .filter(Predicate.not(parentOptionalIsUnknowns::contains))
                .filter(currentArgNamesInNegations::contains)
                .collect(Collectors.toCollection(HashSet::new));
        // @formatter:on

        if (expression instanceof CombinedExpression cmb) {
            if (cmb.combiType() == CombinedExpressionType.AND) {
                effectivelyRequiredIsNotUnknowns.removeAll(extractImplicitDirectIsNotUnknowns(cmb));
            }
            else {
                effectivelyRequiredIsNotUnknowns.removeAll(extractOptionalUnknowns(cmb));
            }
            expression = rebuildCombinedExpressionRecursively(cmb, parentIsNotUnknowns, parentOptionalIsUnknowns);
        }

        if (!effectivelyRequiredIsNotUnknowns.isEmpty()) {
            List<CoreExpression> replacementMembers = new ArrayList<>();
            replacementMembers.add(expression);
            effectivelyRequiredIsNotUnknowns.stream().map(MatchExpression::isNotUnknown).forEach(replacementMembers::add);
            expression = CombinedExpression.andOf(replacementMembers);
        }
        return expression;
    }

    /**
     * This method returns all the <i>immediate</i> implicit is-not-unknowns from this AND-expression, these are positive match expression argNames.
     * <p>
     * <b>Reason:</b> It does not make any sense to add a IS NOT UNKNOWN condition for an argName right besides a comparison of the same argName to a value or
     * another argName.
     * 
     * @param cmbAnd
     * @return direct implicit is-not-unknowns in an AND-condition
     */
    private List<String> extractImplicitDirectIsNotUnknowns(CombinedExpression cmbAnd) {

        List<String> res = new ArrayList<>();

        cmbAnd.childExpressions().stream().filter(MatchExpression.class::isInstance).map(MatchExpression.class::cast)
                .filter(e -> e.operator() != MatchOperator.IS_UNKNOWN).forEach(match -> {
                    res.add(match.argName());
                    if (match.referencedArgName() != null) {
                        res.add(match.referencedArgName());
                    }
                });

        return res;
    }

    /**
     * Returns the list of the argNames with IS UNKNOWN from the given OR-expression.
     * <p>
     * <b>Reason:</b> If there is an IS UNKNOWN part of an OR then we can remove the corresponding argName from the required IS NOT UNKNOWN list.
     * 
     * @param cmbOr
     * @return list of IS UNKNOWN argNames
     */
    private List<String> extractOptionalUnknowns(CombinedExpression cmbOr) {
        return cmbOr.childExpressions().stream().filter(MatchExpression.class::isInstance).map(MatchExpression.class::cast)
                .filter(e -> e.operator() == MatchOperator.IS_UNKNOWN).map(MatchExpression::argName).toList();
    }

    /**
     * @param expression
     * @return set of the required is-not-unknowns of the given expression or empty set if this expression is not in the cache
     */
    private Set<String> lookupRequiredIsNotUnknowns(CoreExpression expression) {
        Set<String> res = requiredIsNotUnknownsArgNames.get(expression);
        return res != null ? res : Collections.emptySet();
    }

    /**
     * @param expression
     * @return set of the argNames in negation of the given expression or empty set if this expression is not in the cache
     */
    private Set<String> lookupArgNamesInNegation(CoreExpression expression) {
        Set<String> res = argNamesInNegations.get(expression);
        return res != null ? res : Collections.emptySet();
    }

    private IsNotUnknownPropagator() {
        // static utility
    }

}
