//@formatter:off
/*
 * SolrQueryDefinition
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.calamanari.adl.solr.config.ConfigUtils;

/**
 * Instances of this class contain the query definitions (to eventually setup native queries be executed on Solr).
 * <p>
 * Considering the workflow of Solr and its caching capabilities plus the fact that Audlang only uses boolean features, the main focus lies on the filter
 * queries. By default the main query (q) should be "*:*" (all documents) to avoid unnecessary scoring.<br>
 * <b>Remark:</b> Solr <i>first</i> executes the filter query/queries (considering cached results), intersects these and <i>then</i> applies the main query.
 * <p>
 * Multiple filter queries cause an intersection of the related data sets. Based on the meta-data of the {@link SolrFilterQuery} you may decide to apply caching
 * or not to cache a particular filter query but to prepend estimated cost to control the execution order. For example, a very simple strategy might be to cache
 * only filter queries on the root document but no filter queries involving any nested documents.
 * <p>
 * You may rely on the {@link #equals(Object)} method of this implementation because the queryString will be trimmed and the filterQueries list will be ordered
 * (alpha-numeric) during construction. In other words: two identical definitions (except for the order of the filter queries) will result in <i>equal</i>
 * definition records. This also means that the order of the given filter queries does <i>not</i> indicate/recommend any specific execution order.
 * <p>
 * Instances are <i>deeply immutable</i>.
 * 
 * @param mainQueryString the main Solr-query (<code><i><b>q=...</b></i></code>), defaults to {@link SolrFormatConstants#QUERY_ALL_DOCUMENTS}
 * @param filterQueries the filter queries (<code><i><b>fq=...</b></i></code>), to be joined using <i>AND</i>, null means empty
 * @param uniqueKeyFieldName name of the unique key field in the Solr schema, defaults to {@link SolrFormatConstants#DEFAULT_UNIQUE_KEY_FIELD_NAME}
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 * @see DefaultQueryType
 */
public record SolrQueryDefinition(String mainQueryString, List<SolrFilterQuery> filterQueries, String uniqueKeyFieldName) implements Serializable {

    /**
     * @param mainQueryString the main Solr-query (<code><i><b>q=...</b></i></code>), null defaults to {@link SolrFormatConstants#QUERY_ALL_DOCUMENTS}
     * @param filterQueries the filter queries (<code><i><b>fq=...</b></i></code>), to be joined using <i>AND</i>, null means empty
     * @param uniqueKeyFieldName name of the unique key field in the Solr schema, null defaults to {@link SolrFormatConstants#DEFAULT_UNIQUE_KEY_FIELD_NAME}
     */
    public SolrQueryDefinition(String mainQueryString, List<SolrFilterQuery> filterQueries, String uniqueKeyFieldName) {

        if ((mainQueryString != null && mainQueryString.isBlank()) || (filterQueries != null && filterQueries.stream().anyMatch(Objects::isNull))
                || (uniqueKeyFieldName != null && !ConfigUtils.isValidSolrName(uniqueKeyFieldName))) {
            throw new IllegalArgumentException(String.format(
                    "The parameter mainQueryString must not be blank, filterQueries must not contain any nulls, uniqueKey must be a valid solr field name, given: mainQueryString=%s, filterQueries=%s, uniqueKeyFieldName=%s%n%s",
                    mainQueryString, filterQueries, uniqueKeyFieldName, SolrFormatConstants.SOLR_NAMING_DEBUG_INFO));

        }

        if (filterQueries != null && filterQueries.stream().distinct().count() < filterQueries.size()) {
            throw new IllegalArgumentException(String.format(
                    "The list of filterQueries must not contain any duplicates, given: mainQueryString=%s, filterQueries=%s, uniqueKeyFieldName=%s",
                    mainQueryString, filterQueries, uniqueKeyFieldName));
        }

        this.mainQueryString = mainQueryString == null ? SolrFormatConstants.QUERY_ALL_DOCUMENTS : mainQueryString.trim();

        if (filterQueries == null) {
            this.filterQueries = Collections.emptyList();
        }
        else {
            List<SolrFilterQuery> temp = new ArrayList<>(filterQueries);
            Collections.sort(temp);
            this.filterQueries = Collections.unmodifiableList(temp);
        }
        this.uniqueKeyFieldName = uniqueKeyFieldName == null ? SolrFormatConstants.DEFAULT_UNIQUE_KEY_FIELD_NAME : uniqueKeyFieldName;

    }

    /**
     * @return all node types involved in this query definition or empty list of this information was not provided
     */
    public List<String> nodeTypesInvolved() {
        if (filterQueries.isEmpty()) {
            return Collections.emptyList();
        }
        else {
            return filterQueries.stream().map(SolrFilterQuery::nodeTypesInvolved).flatMap(Collection::stream).distinct().sorted().toList();
        }
    }

    /**
     * @return only the filter queries in a better readable for diagnosis purposes
     */
    public String toExpressionDebugString() {

        StringBuilder sb = new StringBuilder();
        sb.append(filterQueries.stream().map(SolrFilterQuery::queryString).collect(Collectors.joining("\n\n", "\n<<<[\n\n", "\n\n]>>>")));

        return sb.toString();

    }

}
