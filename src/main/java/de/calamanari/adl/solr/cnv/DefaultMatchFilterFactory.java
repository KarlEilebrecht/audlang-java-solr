//@formatter:off
/*
 * DefaultMatchFilterFactory
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.calamanari.adl.AudlangMessage;
import de.calamanari.adl.CommonErrors;
import de.calamanari.adl.ConversionException;
import de.calamanari.adl.cnv.TemplateParameterUtils;
import de.calamanari.adl.cnv.tps.AdlDateUtils;
import de.calamanari.adl.cnv.tps.ContainsNotSupportedException;
import de.calamanari.adl.cnv.tps.LessThanGreaterThanNotSupportedException;
import de.calamanari.adl.cnv.tps.LookupException;
import de.calamanari.adl.irl.MatchExpression;
import de.calamanari.adl.irl.MatchOperator;
import de.calamanari.adl.irl.Operand;
import de.calamanari.adl.irl.SimpleExpression;
import de.calamanari.adl.solr.AdlSolrType;
import de.calamanari.adl.solr.DefaultAdlSolrType;
import de.calamanari.adl.solr.SolrConditionType;
import de.calamanari.adl.solr.SolrFilterQuery;
import de.calamanari.adl.solr.SolrFormatUtils;
import de.calamanari.adl.solr.SolrQueryField;
import de.calamanari.adl.solr.config.DataField;
import de.calamanari.adl.solr.config.FilterField;
import de.calamanari.adl.solr.config.NodeTypeMetaInfo;
import de.calamanari.adl.solr.config.SolrMappingConfig;

import static de.calamanari.adl.FormatUtils.appendSpaced;
import static de.calamanari.adl.solr.SolrFormatConstants.AND;
import static de.calamanari.adl.solr.SolrFormatConstants.ARGNAME_DUMMY;
import static de.calamanari.adl.solr.SolrFormatConstants.ASTERISK;
import static de.calamanari.adl.solr.SolrFormatConstants.CLOSE_BRACE;
import static de.calamanari.adl.solr.SolrFormatConstants.COLON;
import static de.calamanari.adl.solr.SolrFormatConstants.COMMA;
import static de.calamanari.adl.solr.SolrFormatConstants.DOUBLE_QUOTES;
import static de.calamanari.adl.solr.SolrFormatConstants.FUNC_EQUALS;
import static de.calamanari.adl.solr.SolrFormatConstants.FUNC_GREATER_THAN;
import static de.calamanari.adl.solr.SolrFormatConstants.FUNC_GREATER_THAN_OR_EQUALS;
import static de.calamanari.adl.solr.SolrFormatConstants.FUNC_IF;
import static de.calamanari.adl.solr.SolrFormatConstants.FUNC_LESS_THAN;
import static de.calamanari.adl.solr.SolrFormatConstants.FUNC_LESS_THAN_OR_EQUALS;
import static de.calamanari.adl.solr.SolrFormatConstants.IF_VALUES_0_1;
import static de.calamanari.adl.solr.SolrFormatConstants.INLINE_QUERY;
import static de.calamanari.adl.solr.SolrFormatConstants.OPEN_BRACE;
import static de.calamanari.adl.solr.SolrFormatConstants.OR;
import static de.calamanari.adl.solr.SolrFormatUtils.appendCondition;
import static de.calamanari.adl.solr.SolrFormatUtils.appendDateFieldAtMidnightToFrange;
import static de.calamanari.adl.solr.SolrFormatUtils.appendFrangeHeader;
import static de.calamanari.adl.solr.SolrFormatUtils.appendFrangeStartWithExistenceChecks;
import static de.calamanari.adl.solr.SolrFormatUtils.escape;

/**
 * {@link DefaultMatchFilterFactory} provides a standard implementation for translating atomic match expressions into Solr-queries.
 * <p>
 * Sub-classes may adjust the behavior if required.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class DefaultMatchFilterFactory implements MatchFilterFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMatchFilterFactory.class);

    /**
     * physical data model mapping, variables and flags
     */
    protected final SolrConversionProcessContext ctx;

    /**
     * Contains an "all-documents-filter" for each node type of the mapping configuration
     */
    protected final Map<String, SolrFilterQuery> nodeTypeFilters;

    /**
     * Creates a new factory for the given context
     */
    public DefaultMatchFilterFactory(SolrConversionProcessContext ctx) {
        this.ctx = ctx;
        this.nodeTypeFilters = createNodeTypeFilters(ctx);
    }

    /**
     * Creates a filter for each configured node type (expected to be required frequently)
     * 
     * @param ctx
     * @return filter map
     */
    private static Map<String, SolrFilterQuery> createNodeTypeFilters(SolrConversionProcessContext ctx) {
        Map<String, SolrFilterQuery> map = new HashMap<>();
        for (NodeTypeMetaInfo nodeTypeMetaInfo : ctx.getMappingConfig().allNodeTypeMetaInfos()) {
            SolrFilterQuery nodeTypeFilter = createNodeTypeFilter(nodeTypeMetaInfo, ctx);
            map.put(nodeTypeMetaInfo.nodeType(), nodeTypeFilter);
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Creates a single node type filter considering the node type field itself and optional document filters
     * 
     * @param nodeTypeMetaInfo
     * @param ctx
     * @return filter query
     */
    private static SolrFilterQuery createNodeTypeFilter(NodeTypeMetaInfo nodeTypeMetaInfo, SolrConversionProcessContext ctx) {
        List<SolrQueryField> fields = new ArrayList<>();

        StringBuilder sb = new StringBuilder();

        if (!nodeTypeMetaInfo.documentFilters().isEmpty()) {
            sb.append(OPEN_BRACE);
        }

        appendCondition(sb, ctx.getNodeTypeFieldName(), nodeTypeMetaInfo.nodeType());
        fields.add(new SolrQueryField(nodeTypeMetaInfo.nodeType(), ctx.getNodeTypeFieldName()));

        for (FilterField filterField : nodeTypeMetaInfo.documentFilters()) {
            appendSpaced(sb, AND);
            appendCondition(sb, filterField.fieldName(), filterField.fieldType().getFormatter().format(ARGNAME_DUMMY,
                    TemplateParameterUtils.replaceVariables(filterField.filterValue(), ctx.getGlobalVariables()::get), MatchOperator.EQUALS));
            fields.add(new SolrQueryField(filterField.nodeType(), filterField.fieldName()));
        }

        if (!nodeTypeMetaInfo.documentFilters().isEmpty()) {
            sb.append(CLOSE_BRACE);
        }

        SolrConditionType conditionType = SolrConditionType.ALL_SUB_DOCS;
        if (nodeTypeMetaInfo.equals(ctx.getMappingConfig().mainNodeTypeMetaInfo())) {
            conditionType = SolrConditionType.ALL_DOCS;
        }
        return new SolrFilterQuery(sb.toString(), fields, Arrays.asList(conditionType));
    }

    @Override
    public SolrFilterQuery createHasAnyValueFilter(String argName) {
        SolrMappingConfig mappingConfig = ctx.getMappingConfig();
        DataField field = mappingConfig.lookupField(argName, ctx);
        return new SolrFilterQuery(field.fieldName() + COLON + ASTERISK, Arrays.asList(new SolrQueryField(field.nodeType(), field.fieldName())),
                Arrays.asList(SolrConditionType.CMP_ANY));
    }

    @Override
    public SolrFilterQuery createNodeTypeFilter(String nodeType) {
        SolrFilterQuery res = nodeTypeFilters.get(nodeType);
        if (res == null) {
            throw new LookupException("Unable to create node type filter, given: nodeType=" + nodeType);
        }
        return res;
    }

    @Override
    public SolrFilterQuery createMatchFilter(MatchWrapper matchWrapper) {
        MatchWrapperType wrapperType = matchWrapper.type();

        switch (wrapperType) {
        case ANY_VALUE_MATCH:
            return createHasAnyValueFilter(matchWrapper.argName());
        case VALUE_MATCH, REF_MATCH:
            return createSingleMatchFilter(matchWrapper.firstMember());
        case MULTI_VALUE_MATCH:
            return createMultiValueMatchFilter(matchWrapper.members());
        case VALUE_OR_EQ_MATCH, REF_OR_EQ_MATCH:
            return createLtGtOrEqualsMatchFilter(matchWrapper.firstMember());
        case VALUE_GT_AND_LT_MATCH, VALUE_GT_AND_LTE_MATCH, VALUE_GTE_AND_LT_MATCH, VALUE_GTE_AND_LTE_MATCH:
            return createBetweenMatchFilter((BetweenMatchWrapper) matchWrapper);
        default:
            throw new IllegalArgumentException("Unexpected type of match wrapper, given: " + matchWrapper);
        }
    }

    /**
     * Creates a single comparison of a field against a value or another field from the given match expression
     * 
     * @param expression
     * @return solr filter expression
     */
    protected SolrFilterQuery createSingleMatchFilter(MatchExpression expression) {
        SolrMappingConfig mappingConfig = ctx.getMappingConfig();
        String argNameLeft = expression.argName();
        DataField fieldLeft = mappingConfig.lookupField(argNameLeft, ctx);
        String argNameRight = expression.referencedArgName();
        DataField fieldRight = argNameRight != null ? mappingConfig.lookupField(argNameRight, ctx) : null;

        if (fieldRight == null) {
            return createFieldValueCondition(expression, fieldLeft, false);
        }
        else {
            return createFieldFieldCondition(expression, argNameLeft, fieldLeft, argNameRight, fieldRight, false);
        }

    }

    /**
     * This creates the equivalent of an IN-clause, a match of the same Solr-field against multiple values
     * 
     * @param expressions must be prepared correctly beforehand
     * @return solr filter expression
     */
    protected SolrFilterQuery createMultiValueMatchFilter(List<MatchExpression> expressions) {
        DataField field = ctx.getMappingConfig().lookupField(expressions.get(0).argName(), ctx);
        String argName = expressions.get(0).argName();
        AdlSolrType fieldType = field.fieldType();

        // @formatter:off
        String queryString = expressions.stream().map(MatchExpression::operand)
                                                 .map(Operand::value)
                                                 .map(value -> fieldType.getFormatter().format(argName, value, MatchOperator.EQUALS))
                                                 .sorted()
                                                 .collect(Collectors.joining(" " + OR + " ", field.fieldName() + COLON + OPEN_BRACE, "" + CLOSE_BRACE));
        // @formatter:on

        return new SolrFilterQuery(queryString, Arrays.asList(new SolrQueryField(field.nodeType(), field.fieldName())),
                Arrays.asList(SolrConditionType.CMP_VALUE));

    }

    /**
     * This creates a less-than-or-equals resp. a greater-than-or-equals expression
     * 
     * @param expression must be a less-than or greater-than expression
     * @return solr filter expression
     */
    protected SolrFilterQuery createLtGtOrEqualsMatchFilter(MatchExpression expression) {

        if (expression.referencedArgName() == null) {
            return createFieldValueCondition(expression, ctx.getMappingConfig().lookupField(expression.argName(), ctx), true);
        }
        else {
            String argNameLeft = expression.argName();
            String argNameRight = expression.referencedArgName();
            DataField fieldLeft = ctx.getMappingConfig().lookupField(argNameLeft, ctx);
            DataField fieldRight = ctx.getMappingConfig().lookupField(argNameRight, ctx);
            return createFieldFieldCondition(expression, argNameLeft, fieldLeft, argNameRight, fieldRight, true);
        }
    }

    /**
     * Creates a range-query to filter for values between two bounds
     * 
     * @param betweenWrapper
     * @return filter query
     */
    protected SolrFilterQuery createBetweenMatchFilter(BetweenMatchWrapper betweenWrapper) {
        SolrMappingConfig mappingConfig = ctx.getMappingConfig();
        DataField field = ctx.getMappingConfig().lookupField(betweenWrapper.argName(), ctx);

        assertLessThanGreaterThanSupported(betweenWrapper.firstMember(), betweenWrapper.argName(), field.fieldType(), ctx);

        boolean orEqualsLeft = betweenWrapper.type() == MatchWrapperType.VALUE_GTE_AND_LT_MATCH
                || betweenWrapper.type() == MatchWrapperType.VALUE_GTE_AND_LTE_MATCH;
        boolean orEqualsRight = betweenWrapper.type() == MatchWrapperType.VALUE_GT_AND_LTE_MATCH
                || betweenWrapper.type() == MatchWrapperType.VALUE_GTE_AND_LTE_MATCH;

        boolean shouldAlignDateFlag = !SolrConversionDirective.DISABLE_DATE_TIME_ALIGNMENT.check(ctx.getGlobalFlags())
                && SolrFormatUtils.shouldAlignDate(mappingConfig.typeOf(betweenWrapper.argName()), field.fieldType());

        StringBuilder sb = new StringBuilder();
        sb.append(field.fieldName());
        sb.append(COLON);

        if (shouldAlignDateFlag) {

            String refDayBegin = orEqualsLeft ? betweenWrapper.lowerBound()
                    : AdlDateUtils.computeDayAfter(ctx.getMappingConfig().typeOf(betweenWrapper.argName()).getFormatter().format(betweenWrapper.argName(),
                            betweenWrapper.lowerBound(), MatchOperator.GREATER_THAN));

            sb.append("[");
            sb.append(field.fieldType().getFormatter().format(betweenWrapper.argName(), refDayBegin, MatchOperator.GREATER_THAN));
            sb.append(" TO ");
            if (orEqualsRight) {
                String beginOfNextDay = AdlDateUtils.computeDayAfter(ctx.getMappingConfig().typeOf(betweenWrapper.argName()).getFormatter()
                        .format(betweenWrapper.argName(), betweenWrapper.upperBound(), MatchOperator.LESS_THAN));
                sb.append(field.fieldType().getFormatter().format(betweenWrapper.argName(), beginOfNextDay, MatchOperator.LESS_THAN));
            }
            else {
                String beginOfDay = ctx.getMappingConfig().typeOf(betweenWrapper.argName()).getFormatter().format(betweenWrapper.argName(),
                        betweenWrapper.upperBound(), MatchOperator.LESS_THAN);
                sb.append(field.fieldType().getFormatter().format(betweenWrapper.argName(), beginOfDay, MatchOperator.LESS_THAN));
            }
            sb.append("}");

        }
        else {
            sb.append(orEqualsLeft ? "[" : "{");
            sb.append(field.fieldType().getFormatter().format(betweenWrapper.argName(), betweenWrapper.lowerBound(), MatchOperator.GREATER_THAN));
            sb.append(" TO ");
            sb.append(field.fieldType().getFormatter().format(betweenWrapper.argName(), betweenWrapper.upperBound(), MatchOperator.LESS_THAN));
            sb.append(orEqualsRight ? "]" : "}");
        }

        return new SolrFilterQuery(sb.toString(), Arrays.asList(new SolrQueryField(betweenWrapper.nodeType(), field.fieldName())),
                Collections.singletonList(SolrConditionType.CMP_RANGE));

    }

    /**
     * Composes the filter query for direct value matches of the main field (fieldLeft)
     * <p>
     * Special handling of dates and preparation of contains snippets happens here.
     * <p>
     * Not applicable to reference matches.
     * 
     * @param expression
     * @param fieldLeft
     * @param orEquals if true the less-than/greater-than ranges will be adjusted to include the value itself
     * @return filter query
     */
    protected SolrFilterQuery createFieldValueCondition(MatchExpression expression, DataField fieldLeft, boolean orEquals) {
        AdlSolrType fieldType = fieldLeft.fieldType();
        SolrMappingConfig mappingConfig = ctx.getMappingConfig();
        MatchOperator operator = expression.operator();

        if (expression.referencedArgName() != null) {
            throw new IllegalArgumentException(String.format("Value match expected (implementation error), given: expression=%s, fieldLeft=%s, orEquals=%s",
                    expression, fieldLeft, orEquals));
        }

        if (orEquals && operator != MatchOperator.LESS_THAN && operator != MatchOperator.GREATER_THAN) {
            throw new IllegalArgumentException(String.format(
                    "Unexpected operator (implementation error, orEquals can only be specified "
                            + "in conjunction with less-than or greater-than), given: expression=%s, fieldLeft=%s, orEquals=%s",
                    expression, fieldLeft, orEquals));
        }

        String value = expression.operand() != null ? expression.operand().value() : null;

        StringBuilder sb = new StringBuilder();
        sb.append(fieldLeft.fieldName());
        sb.append(COLON);

        SolrConditionType conditionType = SolrConditionType.CMP_VALUE;
        String argNameLeft = expression.argName();
        boolean shouldAlignDateFlag = !SolrConversionDirective.DISABLE_DATE_TIME_ALIGNMENT.check(ctx.getGlobalFlags())
                && SolrFormatUtils.shouldAlignDate(mappingConfig.typeOf(argNameLeft), fieldType);

        switch (operator) {
        case LESS_THAN:
            if (shouldAlignDateFlag) {
                appendDateAlignedLessThanValueCondition(sb, expression, fieldLeft, orEquals);
            }
            else {
                appendStandardLessThanValueCondition(sb, expression, fieldLeft, orEquals);
            }
            conditionType = SolrConditionType.CMP_RANGE;
            break;
        case EQUALS:
            if (shouldAlignDateFlag) {
                appendDateAlignedEqualsValueCondition(sb, expression, fieldLeft);
                conditionType = SolrConditionType.CMP_RANGE;
            }
            else {
                sb.append(fieldLeft.fieldType().getFormatter().format(argNameLeft, value, operator));
            }
            break;
        case GREATER_THAN:
            if (shouldAlignDateFlag) {
                appendDateAlignedGreaterThanValueCondition(sb, expression, fieldLeft, orEquals);
            }
            else {
                appendStandardGreaterThanValueCondition(sb, expression, fieldLeft, orEquals);
            }
            conditionType = SolrConditionType.CMP_RANGE;
            break;
        case CONTAINS:
            assertContainsSupported(expression, argNameLeft, fieldType, ctx);
            sb.append(ASTERISK);
            sb.append(fieldLeft.fieldType().getFormatter().format(argNameLeft, value, operator));
            sb.append(ASTERISK);
            conditionType = SolrConditionType.CMP_TXT_CONTAINS;
            break;
        // $CASES-OMITTED$
        default:
            throw new IllegalArgumentException(String.format("Unexpected operator (implementation error), given: expression=%s, fieldLeft=%s, orEquals=%s",
                    expression, fieldLeft, orEquals));
        }

        return new SolrFilterQuery(sb.toString(), Arrays.asList(new SolrQueryField(fieldLeft.nodeType(), fieldLeft.fieldName())), Arrays.asList(conditionType));
    }

    /**
     * Uses the configured formatter to append the <b>value range</b> created from the given less than expression.
     * 
     * @param sb
     * @param expression
     * @param field
     * @param orEquals if true the less-than range will be adjusted to include the value itself
     */
    protected void appendStandardLessThanValueCondition(StringBuilder sb, MatchExpression expression, DataField field, boolean orEquals) {

        assertLessThanGreaterThanSupported(expression, expression.argName(), field.fieldType(), ctx);

        sb.append("[* TO ");
        sb.append(field.fieldType().getFormatter().format(expression.argName(), expression.operand().value(), expression.operator()));
        if (orEquals) {
            sb.append("]");
        }
        else {
            sb.append("}");
        }
    }

    /**
     * Uses the configured formatter to append the <b>value range</b> created from the given greater than expression.
     * 
     * @param sb
     * @param expression
     * @param field
     * @param orEquals if true the greater-than range will be adjusted to include the value itself
     */
    protected void appendStandardGreaterThanValueCondition(StringBuilder sb, MatchExpression expression, DataField field, boolean orEquals) {

        assertLessThanGreaterThanSupported(expression, expression.argName(), field.fieldType(), ctx);

        if (orEquals) {
            sb.append("[");
        }
        else {
            sb.append("{");
        }
        sb.append(field.fieldType().getFormatter().format(expression.argName(), expression.operand().value(), expression.operator()));
        sb.append(" TO *]");

    }

    /**
     * Appends a less-than condition after adjusting the bounds to consider the begin of the day as upper limit
     * 
     * @param sb
     * @param expression
     * @param field
     * @param orEquals if true the less-than range will be adjusted to include the value itself
     */
    protected void appendDateAlignedLessThanValueCondition(StringBuilder sb, MatchExpression expression, DataField field, boolean orEquals) {

        assertLessThanGreaterThanSupported(expression, expression.argName(), field.fieldType(), ctx);

        sb.append("[* TO ");
        if (orEquals) {
            String beginOfNextDay = AdlDateUtils.computeDayAfter(ctx.getMappingConfig().typeOf(expression.argName()).getFormatter().format(expression.argName(),
                    expression.operand().value(), expression.operator()));
            sb.append(field.fieldType().getFormatter().format(expression.argName(), beginOfNextDay, expression.operator()));
        }
        else {
            String beginOfDay = ctx.getMappingConfig().typeOf(expression.argName()).getFormatter().format(expression.argName(), expression.operand().value(),
                    expression.operator());
            sb.append(field.fieldType().getFormatter().format(expression.argName(), beginOfDay, expression.operator()));
        }
        sb.append("}");
    }

    /**
     * Appends an equals-condition as a range query after adjusting the bounds to consider the full day
     * 
     * @param sb
     * @param expression
     * @param field
     */
    protected void appendDateAlignedEqualsValueCondition(StringBuilder sb, MatchExpression expression, DataField field) {

        assertLessThanGreaterThanSupported(expression, expression.argName(), field.fieldType(), ctx);

        String beginOfDay = ctx.getMappingConfig().typeOf(expression.argName()).getFormatter().format(expression.argName(), expression.operand().value(),
                expression.operator());

        String beginOfNextDay = AdlDateUtils.computeDayAfter(ctx.getMappingConfig().typeOf(expression.argName()).getFormatter().format(expression.argName(),
                expression.operand().value(), expression.operator()));

        sb.append("[");
        sb.append(field.fieldType().getFormatter().format(expression.argName(), beginOfDay, expression.operator()));
        sb.append(" TO ");
        sb.append(field.fieldType().getFormatter().format(expression.argName(), beginOfNextDay, expression.operator()));
        sb.append("}");
    }

    /**
     * Appends a greater-than condition after adjusting the bounds to consider the begin of the day as upper limit
     * 
     * @param sb
     * @param expression
     * @param field
     * @param orEquals if true the greater-than range will be adjusted to include the value itself
     */
    protected void appendDateAlignedGreaterThanValueCondition(StringBuilder sb, MatchExpression expression, DataField field, boolean orEquals) {

        assertLessThanGreaterThanSupported(expression, expression.argName(), field.fieldType(), ctx);

        String refDayBegin = orEquals ? expression.operand().value()
                : AdlDateUtils.computeDayAfter(ctx.getMappingConfig().typeOf(expression.argName()).getFormatter().format(expression.argName(),
                        expression.operand().value(), expression.operator()));

        sb.append("[");
        sb.append(field.fieldType().getFormatter().format(expression.argName(), refDayBegin, expression.operator()));
        sb.append(" TO *]");
    }

    /**
     * Composes the filter query for a match of two fields (reference match)
     * 
     * @param expression
     * @param argNameLeft
     * @param fieldLeft
     * @param argNameRight
     * @param fieldRight
     * @param orEquals if true any greater/less-than range will be adjusted to include the value itself
     * @return expression
     */
    protected SolrFilterQuery createFieldFieldCondition(SimpleExpression expression, String argNameLeft, DataField fieldLeft, String argNameRight,
            DataField fieldRight, boolean orEquals) {

        if (expression.referencedArgName() == null) {
            throw new IllegalArgumentException(String.format(
                    "Reference match expected (implementation error), given: expression=%s, argNameLeft=%s, fieldLeft=%s, argNameRight=%s, fieldRight=%s, orEquals=%s",
                    expression, argNameLeft, fieldLeft, argNameRight, fieldRight, orEquals));
        }

        assertReferenceMatchSupported(expression, argNameLeft, fieldLeft, argNameRight, fieldRight, ctx);
        MatchOperator operator = expression.operator();

        if (orEquals && operator != MatchOperator.LESS_THAN && operator != MatchOperator.GREATER_THAN) {
            throw new IllegalArgumentException(String.format("Unexpected operator (implementation error, orEquals can only be specified "
                    + "in conjunction with less-than or greater-than), given: expression=%s, argNameLeft=%s, fieldLeft=%s, argNameRight=%s, fieldRight=%s, orEquals=%s",
                    expression, argNameLeft, fieldLeft, argNameRight, fieldRight, orEquals));
        }

        StringBuilder sb = new StringBuilder();
        appendFrangeHeader(sb, 1, 1).append(FUNC_IF).append(OPEN_BRACE);
        appendFrangeStartWithExistenceChecks(sb, fieldLeft.fieldName(), fieldRight.fieldName());

        switch (operator) {
        case LESS_THAN:
            assertLessThanGreaterThanSupported(expression, argNameLeft, fieldLeft.fieldType(), ctx);
            assertLessThanGreaterThanSupported(expression, argNameRight, fieldRight.fieldType(), ctx);
            sb.append(orEquals ? FUNC_LESS_THAN_OR_EQUALS : FUNC_LESS_THAN);
            break;
        case EQUALS:
            sb.append(FUNC_EQUALS);
            break;
        case GREATER_THAN:
            assertLessThanGreaterThanSupported(expression, argNameLeft, fieldLeft.fieldType(), ctx);
            assertLessThanGreaterThanSupported(expression, argNameRight, fieldRight.fieldType(), ctx);
            sb.append(orEquals ? FUNC_GREATER_THAN_OR_EQUALS : FUNC_GREATER_THAN);
            break;
        // $CASES-OMITTED$
        default:
            throw new ConversionException(
                    String.format("Reference match with unsupported operator (bug), given: expression=%s, argNameLeft=%s, argNameRight=%s", expression,
                            argNameLeft, argNameRight));
        }

        sb.append(OPEN_BRACE);
        appendFieldToFrange(sb, fieldLeft);
        sb.append(COMMA);
        appendFieldToFrange(sb, fieldRight);
        sb.append(CLOSE_BRACE);

        // this closes the part started above with the existence check, so the complete condition is inside the IF
        sb.append(CLOSE_BRACE);

        sb.append(IF_VALUES_0_1).append(CLOSE_BRACE);

        // Why can't we return the frange as is?
        // In Solr frange-queries cannot have "neighbors" (even enclosing them in braces leads to parser errors).
        // The only way to let them play well with other conditions is inlining them using the _query_ syntax.

        String frange = sb.toString();

        sb.setLength(0);
        sb.append(INLINE_QUERY);
        sb.append(COLON);
        sb.append(DOUBLE_QUOTES);
        sb.append(escape(frange));
        sb.append(DOUBLE_QUOTES);

        return new SolrFilterQuery(sb.toString(), Arrays.asList(new SolrQueryField(fieldLeft.nodeType(), fieldLeft.fieldName()),
                new SolrQueryField(fieldRight.nodeType(), fieldRight.fieldName())), Arrays.asList(SolrConditionType.FRANGE));
    }

    /**
     * Appends the field name or an adjustment function to the current function in the builder
     * 
     * @param sb
     * @param field solr field to be appended
     */
    protected void appendFieldToFrange(StringBuilder sb, DataField field) {
        boolean shouldAlignDateFlag = field.fieldType().getBaseType().equals(DefaultAdlSolrType.SOLR_DATE)
                && !SolrConversionDirective.DISABLE_DATE_TIME_ALIGNMENT.check(ctx.getGlobalFlags());

        if (shouldAlignDateFlag) {
            appendDateFieldAtMidnightToFrange(sb, field.fieldName());
        }
        else {
            sb.append(field.fieldName());
        }
    }

    /**
     * @param expression current expression
     * @param argName to be tested
     * @param fieldType to be tested
     * @param ctx context
     * @throws LessThanGreaterThanNotSupportedException if the translation is not possible or not permitted
     */
    protected static void assertLessThanGreaterThanSupported(SimpleExpression expression, String argName, AdlSolrType fieldType,
            SolrConversionProcessContext ctx) {
        SolrMappingConfig mappingConfig = ctx.getMappingConfig();
        if (!fieldType.supportsLessThanGreaterThan() || !mappingConfig.typeOf(argName).supportsLessThanGreaterThan()) {
            AudlangMessage userMessage = AudlangMessage.argMsg(CommonErrors.ERR_2201_LTGT_NOT_SUPPORTED, argName);
            throw new LessThanGreaterThanNotSupportedException(
                    String.format("LESS/GREATER THAN not supported by type=%s or columnType=%s, given: expression=%s, argName=%s",
                            mappingConfig.typeOf(argName), fieldType, expression, argName),
                    userMessage);
        }
        if (SolrConversionDirective.DISABLE_LESS_THAN_GREATER_THAN.check(ctx.getGlobalFlags())) {
            AudlangMessage userMessage = AudlangMessage.argMsg(CommonErrors.ERR_2201_LTGT_NOT_SUPPORTED, argName);
            throw new LessThanGreaterThanNotSupportedException(
                    String.format("LESS/GREATER THAN disabled by global directive, given: expression=%s, argName=%s", expression, argName), userMessage);
        }
    }

    /**
     * @param expression current expression
     * @param argName to be tested
     * @param fieldType to be tested
     * @param ctx context
     * @throws ContainsNotSupportedException if the translation is not possible or not permitted
     */
    protected static void assertContainsSupported(SimpleExpression expression, String argName, AdlSolrType fieldType, SolrConversionProcessContext ctx) {
        SolrMappingConfig mappingConfig = ctx.getMappingConfig();
        if (!fieldType.supportsContains() || !mappingConfig.typeOf(argName).supportsContains()) {
            AudlangMessage userMessage = AudlangMessage.argMsg(CommonErrors.ERR_2200_CONTAINS_NOT_SUPPORTED, argName);
            throw new ContainsNotSupportedException(String.format("CONTAINS not supported by type=%s or columnType=%s, given: expression=%s, argName=%s",
                    mappingConfig.typeOf(argName), fieldType, expression, argName), userMessage);
        }
        if (SolrConversionDirective.DISABLE_CONTAINS.check(ctx.getGlobalFlags())) {
            AudlangMessage userMessage = AudlangMessage.argMsg(CommonErrors.ERR_2200_CONTAINS_NOT_SUPPORTED, argName);
            throw new ContainsNotSupportedException(
                    String.format("CONTAINS disabled by global directive, given: expression=%s, argName=%s", expression, argName), userMessage);
        }
    }

    /**
     * @param expression current expression
     * @param argNameLeft to be tested
     * @param fieldLeft to be tested
     * @param argNameRight to be tested
     * @param fieldRight to be tested
     * @param ctx context
     */
    protected static void assertReferenceMatchSupported(SimpleExpression expression, String argNameLeft, DataField fieldLeft, String argNameRight,
            DataField fieldRight, SolrConversionProcessContext ctx) {
        if (SolrConversionDirective.DISABLE_REFERENCE_MATCHING.check(ctx.getGlobalFlags())) {
            AudlangMessage userMessage = AudlangMessage.argRefMsg(CommonErrors.ERR_2101_REFERENCE_MATCH_NOT_SUPPORTED, argNameLeft, argNameRight);
            throw new ConversionException(
                    String.format("Reference matching disabled by global directive, given: expression=%s, argNameLeft=%s, argNameRight=%s", expression,
                            argNameLeft, argNameRight),
                    userMessage);
        }

        if (!fieldLeft.nodeType().equals(fieldRight.nodeType())) {
            AudlangMessage userMessage = AudlangMessage.argRefMsg(CommonErrors.ERR_2101_REFERENCE_MATCH_NOT_SUPPORTED, argNameLeft, argNameRight);
            throw new ConversionException(String.format(
                    "Reference matching between fields from different documents (node types) is not supported, given: expression=%s, argNameLeft=%s (%s.%s), argNameRight=%s (%s.%s)",
                    expression, argNameLeft, fieldLeft.nodeType(), fieldLeft.fieldName(), argNameRight, fieldRight.nodeType(), fieldRight.fieldName()),
                    userMessage);
        }

        if (ctx.getMappingConfig().lookupAssignment(argNameLeft, ctx).isMultiDoc() && ctx.getMappingConfig().lookupAssignment(argNameRight, ctx).isMultiDoc()) {
            AudlangMessage userMessage = AudlangMessage.argRefMsg(CommonErrors.ERR_2101_REFERENCE_MATCH_NOT_SUPPORTED, argNameLeft, argNameRight);
            throw new ConversionException(
                    String.format("Reference matching between two fields both marked multi-doc is not supported "
                            + "because this would involve multiple document instances, given: expression=%s, argNameLeft=%s (%s.%s), argNameRight=%s (%s.%s)",
                            expression, argNameLeft, fieldLeft.nodeType(), fieldLeft.fieldName(), argNameRight, fieldRight.nodeType(), fieldRight.fieldName()),
                    userMessage);
        }

        if (LOGGER.isWarnEnabled() && !fieldLeft.fieldType().getBaseType().equals(fieldRight.fieldType().getBaseType())) {
            LOGGER.warn(String.format(
                    "Reference matching of different Solr-types may be unreliable, given: expression=%s, argNameLeft=%s (%s: %s), argNameRight=%s (%s: %s)",
                    expression, argNameLeft, fieldLeft.fieldName(), fieldLeft.fieldType().getBaseType(), argNameRight, fieldRight.fieldName(),
                    fieldRight.fieldType().getBaseType()));
        }

        if (fieldLeft.isCollection() || fieldRight.isCollection()) {
            AudlangMessage userMessage = AudlangMessage.argRefMsg(CommonErrors.ERR_2101_REFERENCE_MATCH_NOT_SUPPORTED, argNameLeft, argNameRight);
            throw new ConversionException(String.format(
                    "Reference matching involving multi-valued Solr-fields is not supported, given: expression=%s, argNameLeft=%s (%s%s: %s), argNameRight=%s (%s%s: %s)",
                    expression, argNameLeft, fieldLeft.fieldName(), fieldLeft.isCollection() ? "[]" : "", fieldLeft.fieldType().getBaseType(), argNameRight,
                    fieldRight.fieldName(), fieldRight.isCollection() ? "[]" : "", fieldRight.fieldType().getBaseType()), userMessage);
        }

    }

}
