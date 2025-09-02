//@formatter:off
/*
 * MissingSubDocumentMatchElement
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
 * {@link MissingSubDocumentMatchElement} covers the case that a main document should be matched if the nested or dependent sub-document of the given node type
 * is <b>not</b> present.
 * <p>
 * This addresses a problem with negations on joined documents.
 * <p>
 * <b>Example:</b> Let <b><code>sub</code></b> be a nested document and <b><code>profile</code></b> be the main document. <br>
 * Now there is a query <b><code>sub.color=res OR sub.shape!=circle</code></b>
 * <p>
 * The query correctly returns all profiles having any sub-document with red color or any shape other than circle (including no shape at all). But the query
 * misses the case that the sub document <b><code>sub</code></b> is not present. In this case we need a <i>companion condition</i> to include potential matches
 * in the result.
 * 
 * @param nodeType the type of sub-document that might be missing
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public record MissingSubDocumentMatchElement(String nodeType) implements MatchTreeElement {

    /**
     * @param nodeType the type of sub-document that might be missing, not null
     */
    public MissingSubDocumentMatchElement {
        if (nodeType == null) {
            throw new IllegalArgumentException("Argument nodeType must not be null.");
        }
    }

    @Override
    public boolean containsAnyNegation() {
        return true;
    }

    @Override
    public String commonNodeType() {
        return nodeType;
    }

    @Override
    public boolean isMissingDocumentIncluded() {
        return true;
    }

    @Override
    public boolean isGroupingEligible() {
        return false;
    }

}
