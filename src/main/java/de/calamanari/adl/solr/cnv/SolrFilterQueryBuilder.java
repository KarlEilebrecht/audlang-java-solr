//@formatter:off
/*
 * SolrFilterQueryBuilder
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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.calamanari.adl.FormatStyle;
import de.calamanari.adl.FormatUtils;
import de.calamanari.adl.solr.SolrConditionType;
import de.calamanari.adl.solr.SolrFilterQuery;
import de.calamanari.adl.solr.SolrFormatConstants;
import de.calamanari.adl.solr.SolrFormatUtils;
import de.calamanari.adl.solr.SolrQueryField;

import static de.calamanari.adl.solr.SolrFormatConstants.CLOSE_BRACE;
import static de.calamanari.adl.solr.SolrFormatConstants.OPEN_BRACE;

/**
 * The {@link SolrFilterQueryBuilder} combines atomic {@link SolrFilterQuery}s step by step to compose a single filter query string.
 * <p>
 * The result ({@link #getResult()}) is a complex filter query.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class SolrFilterQueryBuilder {

    /**
     * inline or pretty-print formatting of the current conversion
     */
    private final FormatStyle formatStyle;

    /**
     * Main document, required to return to main level when ending a join
     */
    private final String mainNodeType;

    /**
     * name of the Solr field that contains the node type
     */
    private final String nodeTypeFieldName;

    /**
     * name of the Solr field that contains the main document id in a non-nested dependent document
     */
    private final String dependentMainKeyFieldName;

    /**
     * Name of the id-field common to all Solr-document
     */
    private final String uniqueKeyFieldName;

    /**
     * The builder for the main-level of the Solr-expression, conditions are combined using AND/OR and braces.
     */
    private final StringBuilder mainBuilder = new StringBuilder();

    /**
     * The join-builder is a sub-builder while creating a join. We need it to apply escaping before appending the join-condition to the main builder.
     */
    private final StringBuilder joinBuilder = new StringBuilder();

    /**
     * collection of all the fields involved in the entire Solr-expression (statistics)
     */
    private final Set<SolrQueryField> fields = new HashSet<>();

    /**
     * collection of all conditions types involved in the entire Solr-expression (statistics)
     */
    private final Set<SolrConditionType> conditionTypes = EnumSet.noneOf(SolrConditionType.class);

    /**
     * Reference to the current output string builder
     */
    private StringBuilder output = mainBuilder;

    /**
     * type of the node we are currently appending conditions for
     */
    private String currentNodeType;

    /**
     * number of open braces on the main builder
     */
    private int openMainBraceCount = 0;

    /**
     * number of open braces on the join builder
     */
    private int openJoinBraceCount = 0;

    /**
     * @param mainNodeType node type of the main Solr document
     * @param nodeTypeFieldName name of the Solr-field carrying the node type in every document, usually
     *            {@value SolrFormatConstants#DEFAULT_NODE_TYPE_FIELD_NAME}
     * @param uniqueKeyFieldName name of the Solr-field carrying the unique id in every document, usually
     *            {@value SolrFormatConstants#DEFAULT_UNIQUE_KEY_FIELD_NAME}
     * @param dependentMainKeyFieldName name of the Solr-field carrying the id of the referenced main document in every dependent document to be joined, usually
     *            {@value SolrFormatConstants#DEFAULT_DEPENDENT_MAIN_KEY_FIELD_NAME}
     * @param formatStyle pretty-print or inline
     */
    protected SolrFilterQueryBuilder(String mainNodeType, String nodeTypeFieldName, String uniqueKeyFieldName, String dependentMainKeyFieldName,
            FormatStyle formatStyle) {
        this.mainNodeType = mainNodeType;
        this.currentNodeType = mainNodeType;
        this.formatStyle = formatStyle;
        this.nodeTypeFieldName = nodeTypeFieldName;
        this.uniqueKeyFieldName = uniqueKeyFieldName;
        this.dependentMainKeyFieldName = dependentMainKeyFieldName;
    }

    /**
     * Obtains all required information from the given context
     * 
     * @param ctx
     */
    public SolrFilterQueryBuilder(SolrConversionProcessContext ctx) {
        this(ctx.getMappingConfig().mainNodeTypeMetaInfo().nodeType(), ctx.getNodeTypeFieldName(), ctx.getUniqueKeyFieldName(),
                ctx.getDependentMainKeyFieldName(), ctx.getStyle());
    }

    /**
     * Starts a dependent document join
     * 
     * @param nodeType
     * @return this builder
     */
    public SolrFilterQueryBuilder startDependentJoin(String nodeType) {
        if (isJoinOpen()) {
            throw new IllegalStateException(String.format("Attempt to start dependent join with nodeType=%s inside join, given: %s", nodeType, this));
        }
        else if (mainNodeType.equals(nodeType)) {
            throw new IllegalStateException(String.format("Attempt to start dependent join with main node type, given: %s", this));
        }
        mainBuilder.append("{!join from=").append(dependentMainKeyFieldName);
        mainBuilder.append(" to=").append(uniqueKeyFieldName);
        currentNodeType = nodeType;
        output = joinBuilder;
        return this;
    }

    /**
     * Starts a nested document join (block join instruction)
     * 
     * @param nodeType
     * @return this builder
     */
    public SolrFilterQueryBuilder startNestedJoin(String nodeType) {
        if (isJoinOpen()) {
            throw new IllegalStateException(String.format("Attempt to start nested join with nodeType=%s inside join, given: %s", nodeType, this));
        }
        else if (mainNodeType.equals(nodeType)) {
            throw new IllegalStateException(String.format("Attempt to start nested join with main node type, given: %s", this));
        }
        mainBuilder.append("{!parent which=").append('"').append(nodeTypeFieldName).append(':').append(mainNodeType).append('"');
        currentNodeType = nodeType;
        output = joinBuilder;
        return this;
    }

    /**
     * Closes the dependent or nested join
     * 
     * @return this builder
     */
    public SolrFilterQueryBuilder endJoin() {
        if (!isJoinOpen()) {
            throw new IllegalStateException("Attempt to close non-existing join, given: " + this.toString());
        }
        if (openJoinBraceCount > 0) {
            throw new IllegalStateException("Attempt to close join with unclosed braces, given: " + this.toString());
        }

        String subQuery = SolrFormatUtils.escape(joinBuilder.toString());

        mainBuilder.append(" v=").append('"').append(subQuery).append('"').append('}');

        joinBuilder.setLength(0);
        currentNodeType = mainNodeType;
        output = mainBuilder;
        return this;
    }

    /**
     * Opens a new brace to group conditions
     * 
     * @return this builder
     */
    public SolrFilterQueryBuilder openBrace() {

        output.append(OPEN_BRACE);

        if (isJoinOpen()) {
            openJoinBraceCount++;
        }
        else {
            openMainBraceCount++;
        }

        appendLineBreak(false);
        return this;
    }

    /**
     * Closes the last opened brace
     * 
     * @return this builder
     */
    public SolrFilterQueryBuilder closeBrace() {
        if (isJoinOpen()) {
            if (openJoinBraceCount < 1) {
                throw new IllegalStateException("Attempt to close brace that was not open, given: " + this.toString());
            }
            openJoinBraceCount--;
        }
        else {
            if (openMainBraceCount < 1) {
                throw new IllegalStateException("Attempt to close brace that was not open, given: " + this.toString());
            }
            openMainBraceCount--;
        }

        appendLineBreak(false);
        output.append(CLOSE_BRACE);
        return this;
    }

    private void appendCombiner(String combiner) {
        appendLineBreak(true);
        output.append(combiner);
        output.append(' ');
    }

    /**
     * Appends the literal AND with surrounding spaces, in case of multi-line prepended by a line-break
     * 
     * @return this builder
     */
    public SolrFilterQueryBuilder appendAND() {
        appendCombiner(SolrFormatConstants.AND);
        return this;
    }

    /**
     * Appends the literal OR with surrounding spaces, in case of multi-line prepended by a line-break
     * 
     * @return this builder
     */
    public SolrFilterQueryBuilder appendOR() {
        appendCombiner(SolrFormatConstants.OR);
        return this;
    }

    /**
     * Appends the literal NOT with surrounding spaces
     * 
     * @return this builder
     */
    public SolrFilterQueryBuilder appendNOT() {
        if (!FormatUtils.endsWith(output, " ")) {
            output.append(' ');
        }
        output.append(SolrFormatConstants.NOT);
        output.append(' ');
        return this;
    }

    /**
     * Appends a line break if the format allows it.
     * 
     * @param spaceRequired insert a space if the line break is not allowed
     * @return this builder
     */
    public SolrFilterQueryBuilder appendLineBreak(boolean spaceRequired) {
        if (formatStyle.isMultiLine()) {
            output.append("\n");
            int indentCount = (openMainBraceCount + openJoinBraceCount);
            if (output == joinBuilder) {
                indentCount++;
            }
            for (int i = 0; i < indentCount; i++) {
                output.append(formatStyle.getIndent());
            }
        }
        else if (spaceRequired) {
            output.append(" ");
        }
        return this;
    }

    /**
     * Appends a <i>simple</i> filter query to this builder. Simple means that the given filter query must be solely related to the
     * {@link #getCurrentNodeType()}.
     * 
     * @param filterQuery
     * @return this builder
     */
    public SolrFilterQueryBuilder appendFilterQuery(SolrFilterQuery filterQuery) {

        List<String> nodeTypesInvolved = filterQuery.nodeTypesInvolved();

        if (nodeTypesInvolved.size() != 1 || (!nodeTypesInvolved.isEmpty() && !nodeTypesInvolved.get(0).equals(currentNodeType))) {
            throw new IllegalStateException(String
                    .format("Filter query to be appended must be solely related to the current node type, given: filterQuery=%s, this=%s", filterQuery, this));
        }
        output.append(filterQuery.queryString());
        fields.addAll(filterQuery.fields());
        conditionTypes.addAll(filterQuery.conditionTypes());
        return this;
    }

    /**
     * Appends the given text <i>as-is</i> to the current builder
     * 
     * @param s to be appended
     * @return this builder
     */
    public SolrFilterQueryBuilder append(String s) {
        output.append(s);
        return this;
    }

    /**
     * Closes all open joins and braces and returns the composed query.
     * 
     * @return result filter query
     */
    public SolrFilterQuery getResult() {
        if (isJoinOpen()) {
            throw new IllegalStateException("Attempt to obtain result with unfinished join, given: " + this.toString());
        }
        if (openMainBraceCount > 0) {
            throw new IllegalStateException("Attempt to obtain result with unclosed braces, given: " + this.toString());
        }
        return new SolrFilterQuery(mainBuilder.toString(), new ArrayList<>(fields), new ArrayList<>(conditionTypes));
    }

    /**
     * Resets the builder for the next filter query
     */
    public void reset() {
        this.conditionTypes.clear();
        this.currentNodeType = mainNodeType;
        this.fields.clear();
        this.joinBuilder.setLength(0);
        this.mainBuilder.setLength(0);
        this.openJoinBraceCount = 0;
        this.openMainBraceCount = 0;
    }

    /**
     * Returns the node type we are currently adding a condition for. Initially, this is the main node type. While building joins this will be the node type of
     * the dependent or nested document.
     * 
     * @return current node type
     */
    public String getCurrentNodeType() {
        return currentNodeType;
    }

    /**
     * Returns the node type of the main document
     * 
     * @return main node type
     */
    public String getMainNodeType() {
        return mainNodeType;
    }

    /**
     * @return true if this builder is inside a join
     */
    public boolean isJoinOpen() {
        return !currentNodeType.equals(mainNodeType);
    }

    @Override
    public String toString() {
        return String.format("""
                %s (
                    mainNodeType=%s,
                    currentNodeType=%s,
                    openMainBraceCount=%s,
                    openJoinBraceCount=%s,
                    mainBuilder=%s,
                    joinBuilder=%s,
                    joinOpen=%s,
                    fields=%s,
                    conditionTypes=%s,
                    formatStyle=%s
                )
                """, this.getClass().getSimpleName(), mainNodeType, currentNodeType, openMainBraceCount, openJoinBraceCount, mainBuilder, joinBuilder,
                isJoinOpen(), fields, conditionTypes, formatStyle);
    }

}
