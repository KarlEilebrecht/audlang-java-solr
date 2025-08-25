//@formatter:off
/*
 * SolrFilterQuery
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

package de.calamanari.adl.solr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * A {@link SolrFilterQuery} is a filter query string along with meta information which might help with execution.
 * <p>
 * The meta-data (node-types and fields) may be used to estimate the complexity and decide about result caching.
 * <p>
 * <b>Important:</b> {@link #hashCode()}, {@link #equals(Object)} and {@link #compareTo(SolrFilterQuery)} are solely defined on the {@link #queryString}.
 * <p>
 * Instances are <i>deeply immutable</i>.
 * 
 * @param queryString solr filter query instruction NOT NULL, not blank
 * @param fields all Solr-fields involved in this query, not empty
 * @param conditionTypes kinds of conditions in this filter query, not empty
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public record SolrFilterQuery(String queryString, List<SolrQueryField> fields, List<SolrConditionType> conditionTypes)
        implements Comparable<SolrFilterQuery>, Serializable {

    /**
     * @param queryString solr filter query instruction NOT NULL, not blank
     * @param fields all Solr-fields involved in this query for info, not empty
     * @param conditionTypes kinds of conditions in this filter query, not empty
     */
    public SolrFilterQuery(String queryString, List<SolrQueryField> fields, List<SolrConditionType> conditionTypes) {

        if (queryString == null || queryString.isBlank() || fields == null || fields.isEmpty() || fields.stream().anyMatch(Objects::isNull)
                || conditionTypes == null || conditionTypes.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(String.format(
                    "The parameter queryString must not be null or blank, fields and conditionTypes must not be null or empty nor contain any nulls, given: queryString=%s, fields=%s, conditionTypes=%s",
                    queryString, fields, conditionTypes));
        }

        EnumSet<SolrConditionType> tempConditions = EnumSet.noneOf(SolrConditionType.class);
        tempConditions.addAll(conditionTypes);

        List<SolrQueryField> tempFields = new ArrayList<>(fields);
        Collections.sort(tempFields, Comparator.comparing(SolrQueryField::nodeType).thenComparing(SolrQueryField::fieldName));

        // remove any surrounding whitespace
        this.queryString = queryString.trim();
        this.fields = Collections.unmodifiableList(tempFields.stream().distinct().toList());
        this.conditionTypes = Collections.unmodifiableList(new ArrayList<>(tempConditions));
    }

    /**
     * @return all node types involved in this query or empty list of this information was not provided
     */
    public List<String> nodeTypesInvolved() {
        if (fields.isEmpty()) {
            return Collections.emptyList();
        }
        else {
            return fields.stream().map(SolrQueryField::nodeType).distinct().toList();
        }
    }

    @Override
    public int hashCode() {
        return queryString.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SolrFilterQuery other = (SolrFilterQuery) obj;
        return Objects.equals(queryString, other.queryString);
    }

    @Override
    public int compareTo(SolrFilterQuery o) {
        return this.queryString.compareTo(o.queryString);
    }

}
