//@formatter:off
/*
 * CombinedMatchTreeElement
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
import java.util.Objects;
import java.util.function.Predicate;

import de.calamanari.adl.CombinedExpressionType;

/**
 * A {@link CombinedMatchTreeElement} is the combination of {@link MatchTreeElement}s (nested).
 * 
 * @param combiType identifies an AND vs. an OR
 * @param childElements not null, at least two elements
 * @param commonNodeType nodeType if all children have the same nodeType, or null if this a hybrid combination relating to fields from different Solr documents
 * @param isGroupingEligible true if all members are grouping-eligible
 * @param containsAnyNegation true if any of the members or members' members is a negation
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public record CombinedMatchTreeElement(CombinedExpressionType combiType, List<MatchTreeElement> childElements, String commonNodeType,
        boolean isGroupingEligible, boolean containsAnyNegation) implements MatchTreeElement {

    /**
     * @param combiType not null, identifies an AND vs. an OR
     * @param childElements not null, at least two elements
     * @param commonNodeType typically null, auto-resolved from the given childElements documents
     * @param isGroupingEligible true if all members are grouping-eligible
     * @param containsAnyNegation true if any of the members or members' members is a negation
     */
    public CombinedMatchTreeElement(CombinedExpressionType combiType, List<MatchTreeElement> childElements, String commonNodeType, boolean isGroupingEligible,
            boolean containsAnyNegation) {
        if (combiType == null || childElements == null) {
            throw new IllegalArgumentException(String.format(
                    "Arguments combiType and childElements must not be null, given: combiType=%s, childElements=%s, commonNodeType=%s, "
                            + "isGroupingEligible=%s, containsAnyNegation=%s",
                    combiType, childElements, commonNodeType, isGroupingEligible, containsAnyNegation));
        }
        if (childElements.size() < 2) {
            throw new IllegalArgumentException(String.format(
                    "Argument childElements must have at least two members, given: combiType=%s, "
                            + "childElements=%s, commonNodeType=%s, isGroupingEligible=%s, containsAnyNegation=%s",
                    combiType, childElements, commonNodeType, isGroupingEligible, containsAnyNegation));
        }
        String detectedCommonNodeType = detectCommonNodeType(childElements);

        if (commonNodeType != null && !Objects.equals(commonNodeType, detectedCommonNodeType)) {
            throw new IllegalArgumentException(String.format(
                    "The specified commonNodeType must be either null or identical to the effective commonNodeType=%s, "
                            + "given: combiType=%s, childElements=%s, commonNodeType=%s, isGroupingEligible=%s, containsAnyNegation=%s",
                    detectedCommonNodeType, combiType, childElements, commonNodeType, isGroupingEligible, containsAnyNegation));
        }

        if ((isGroupingEligible && (detectedCommonNodeType == null || childElements.stream().anyMatch(Predicate.not(MatchTreeElement::isGroupingEligible))))
                || (!isGroupingEligible && commonNodeType != null && childElements.stream().allMatch(MatchTreeElement::isGroupingEligible))) {
            throw new IllegalArgumentException(String.format("""
                    The specified value for isGroupingEligible is inconsistent to the child elements.
                    All elements must be grouping eligible and share the same commonNodeType.
                    given: combiType=%s, childElements=%s, commonNodeType=%s, isGroupingEligible=%s, containsAnyNegation=%s
                    """, combiType, childElements, detectedCommonNodeType, isGroupingEligible, containsAnyNegation));
        }
        if ((containsAnyNegation && childElements.stream().noneMatch(MatchTreeElement::containsAnyNegation))
                || (!containsAnyNegation && childElements.stream().anyMatch(MatchTreeElement::containsAnyNegation))) {
            throw new IllegalArgumentException(String.format(
                    "The specified value for containsAnyNegation is inconsistent to the child elements, given: combiType=%s, "
                            + "childElements=%s, commonNodeType=%s, isGroupingEligible=%s, containsAnyNegation=%s",
                    combiType, childElements, commonNodeType, isGroupingEligible, containsAnyNegation));
        }

        this.combiType = combiType;
        this.childElements = Collections.unmodifiableList(new ArrayList<>(childElements));
        this.commonNodeType = detectedCommonNodeType;
        this.isGroupingEligible = isGroupingEligible;
        this.containsAnyNegation = containsAnyNegation;
    }

    /**
     * @param childElements
     * @return
     */
    private static String detectCommonNodeType(List<MatchTreeElement> childElements) {
        String nodeType = childElements.get(0).commonNodeType();
        if (nodeType != null && childElements.stream().allMatch(childElement -> nodeType.equals(childElement.commonNodeType()))) {
            return nodeType;
        }
        return null;
    }

    /**
     * Auto-detects the common node type (or null), {@link #isGroupingEligible} and {@link #containsAnyNegation}
     * <p>
     * This is the preferred constructor.
     * 
     * @param combiType not null, identifies an AND vs. an OR
     * @param childElements not null, at least two elements
     */
    public CombinedMatchTreeElement(CombinedExpressionType combiType, List<MatchTreeElement> childElements) {
        this(combiType, childElements, null, deriveGroupingEligible(childElements),
                childElements != null && childElements.stream().anyMatch(MatchTreeElement::containsAnyNegation));
    }

    private static boolean deriveGroupingEligible(List<MatchTreeElement> childElements) {
        if (childElements == null || childElements.isEmpty()) {
            return false;
        }
        return childElements.stream().allMatch(MatchTreeElement::isGroupingEligible) && detectCommonNodeType(childElements) != null;
    }

    private String createDebugNodeTypeInfo() {
        if (commonNodeType() == null) {
            return "(<HYBRID>)";
        }
        else {
            return "(" + commonNodeType() + ")";
        }
    }

    @Override
    public String toString() {
        return "CombinedMatchTreeElement " + createDebugIndicator() + "[combiType=" + combiType + " " + createDebugNodeTypeInfo() + ", childElements="
                + childElements + "]";
    }

    @Override
    public void appendDebugString(StringBuilder sb, int indentation) {
        MatchElementDebugUtils.appendTreeLevelMembersDebugString(sb, indentation,
                "CombinedMatchTreeElement " + createDebugIndicator() + "[combiType=" + combiType + " " + createDebugNodeTypeInfo() + "]", combiType,
                childElements);
    }

}
