//@formatter:off
/*
 * DefaultMatchFilterFactoryTest
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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.calamanari.adl.CommonErrors;
import de.calamanari.adl.ConversionException;
import de.calamanari.adl.cnv.tps.ContainsNotSupportedException;
import de.calamanari.adl.cnv.tps.LessThanGreaterThanNotSupportedException;
import de.calamanari.adl.cnv.tps.LookupException;
import de.calamanari.adl.irl.CombinedExpression;
import de.calamanari.adl.irl.MatchExpression;
import de.calamanari.adl.irl.MatchOperator;
import de.calamanari.adl.irl.NegationExpression;
import de.calamanari.adl.irl.SimpleExpression;
import de.calamanari.adl.solr.SolrConditionType;
import de.calamanari.adl.solr.SolrFilterQuery;
import de.calamanari.adl.solr.SolrQueryField;
import de.calamanari.adl.solr.SolrTestBase;
import de.calamanari.adl.solr.config.DataField;

import static de.calamanari.adl.solr.cnv.ConversionTestUtils.createDryTestContext;
import static de.calamanari.adl.solr.cnv.ConversionTestUtils.expr;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class DefaultMatchFilterFactoryTest extends SolrTestBase {

    static final Logger LOGGER = LoggerFactory.getLogger(DefaultMatchFilterFactoryTest.class);

    private ResettableScpContext ctx = createDryTestContext();

    private SolrConversionProcessContext resetCtx() {
        ctx.reset();
        return ctx;
    }

    private void newCtx() {
        ctx = createDryTestContext();
    }

    @Test
    void testCreateNodeTypeFilter() {

        DefaultMatchFilterFactory factory = new DefaultMatchFilterFactory(resetCtx());

        SolrFilterQuery fq = factory.createNodeTypeFilter(NODE_TYPE_1);

        assertEquals(Arrays.asList(SolrConditionType.ALL_DOCS), fq.conditionTypes());
        assertEquals(Arrays.asList(NODE_TYPE_1), fq.nodeTypesInvolved());
        assertEquals("node_type", fq.fields().get(0).fieldName());
        assertEquals(NODE_TYPE_1, fq.fields().get(0).nodeType());
        assertEquals("node_type:node1", fq.queryString());

        fq = factory.createNodeTypeFilter(NODE_TYPE_2);
        assertEquals(Arrays.asList(SolrConditionType.ALL_SUB_DOCS), fq.conditionTypes());
        assertEquals(Arrays.asList(NODE_TYPE_2), fq.nodeTypesInvolved());
        assertEquals("node_type", fq.fields().get(0).fieldName());
        assertEquals(NODE_TYPE_2, fq.fields().get(0).nodeType());
        assertEquals("node_type:node2", fq.queryString());

        assertThrows(LookupException.class, () -> factory.createNodeTypeFilter(null));
        assertThrows(LookupException.class, () -> factory.createNodeTypeFilter("foo"));

    }

    @Test
    void testCreateHasAnyValueFilter() {

        DefaultMatchFilterFactory factory = new DefaultMatchFilterFactory(resetCtx());

        SolrFilterQuery fq = factory.createHasAnyValueFilter("color");

        assertEquals(Arrays.asList(SolrConditionType.CMP_ANY), fq.conditionTypes());
        assertEquals(Arrays.asList(NODE_TYPE_1), fq.nodeTypesInvolved());
        assertEquals("color", fq.fields().get(0).fieldName());
        assertEquals(NODE_TYPE_1, fq.fields().get(0).nodeType());
        assertEquals("color:*", fq.queryString());

        fq = factory.createHasAnyValueFilter("clicks_l");
        assertEquals(Arrays.asList(SolrConditionType.CMP_ANY), fq.conditionTypes());
        assertEquals(Arrays.asList(NODE_TYPE_2), fq.nodeTypesInvolved());
        assertEquals("clicks_l", fq.fields().get(0).fieldName());
        assertEquals(NODE_TYPE_2, fq.fields().get(0).nodeType());
        assertEquals("clicks_l:*", fq.queryString());

        fq = factory.createHasAnyValueFilter("lat_f");
        assertEquals(Arrays.asList(SolrConditionType.CMP_ANY), fq.conditionTypes());
        assertEquals(Arrays.asList(NODE_TYPE_3), fq.nodeTypesInvolved());
        assertEquals("lat_f", fq.fields().get(0).fieldName());
        assertEquals(NODE_TYPE_3, fq.fields().get(0).nodeType());
        assertEquals("lat_f:*", fq.queryString());

    }

    @Test
    void testCreateMatchFilter() {

        assertSingleMatch("color:blue", "color=blue");
        assertSingleMatch("clicks_l:91837362674", "clicks_l=91837362674");
        assertSingleMatch("lat_f:9.1837363", "lat_f=9.1837362674");
        assertSingleMatch("story_s:The\\ quick\\ brown\\ fox\\ jumped\\ over\\ the\\ lazy\\ dog.", "story_s=\"The quick brown fox jumped over the lazy dog.\"");

        assertSingleMatch("story_s:*the\\ lazy\\ dog*", "story_s CONTAINS \"the lazy dog\"");

        assertSingleMatch("clicks_l:[* TO 91837362674}", "clicks_l < 91837362674");
        assertSingleMatch("lat_f:{9.1837363 TO *]", "lat_f > 9.1837362674");

        assertSingleMatch("flag1_b:TRUE", "flag1_b=1");
        assertSingleMatch("flag1_b:FALSE", "flag1_b=0");

        assertSingleMatch("_query_:\"\\{\\!frange\\ l=1\\ u=1\\}if\\(and\\(exists\\(color\\),exists\\(taste\\),eq\\(color,taste\\)\\),1,0\\)\"",
                "color = @taste");
        assertSingleMatch("_query_:\"\\{\\!frange\\ l=1\\ u=1\\}if\\(and\\(exists\\(color\\),exists\\(taste\\),gt\\(color,taste\\)\\),1,0\\)\"",
                "color > @taste");
        assertSingleMatch("_query_:\"\\{\\!frange\\ l=1\\ u=1\\}if\\(and\\(exists\\(color\\),exists\\(taste\\),lt\\(color,taste\\)\\),1,0\\)\"",
                "color < @taste");

        // all filters are positive
        assertSingleMatch("story_s:*", "story_s IS UNKNOWN");

    }

    @Test
    void testCreateMatchFilterDate() {

        assertSingleMatch("date_of_birth:[2025\\-03\\-01T00\\:00\\:00Z TO 2025\\-03\\-02T00\\:00\\:00Z}", "date_of_birth = 2025-03-01");
        assertSingleMatch("date_of_birth:[* TO 2025\\-03\\-01T00\\:00\\:00Z}", "date_of_birth < 2025-03-01");
        assertSingleMatch("date_of_birth:[2025\\-03\\-02T00\\:00\\:00Z TO *]", "date_of_birth > 2025-03-01");
        assertMultiValueMatch("date_of_birth:[* TO 2025\\-03\\-02T00\\:00\\:00Z}", "date_of_birth <= 2025-03-01");
        assertMultiValueMatch("date_of_birth:[2025\\-03\\-01T00\\:00\\:00Z TO *]", "date_of_birth >= 2025-03-01");

        assertSingleMatch("contact_date_l:[1740787200000 TO 1740873600000}", "contact_date = 2025-03-01");

        assertSingleMatch("contact_date_s:2025\\-03\\-01", "contact_date2 = 2025-03-01");
        assertSingleMatch("contact_date_s:[* TO 2025\\-03\\-01}", "contact_date2 < 2025-03-01");
        assertSingleMatch("contact_date_s:{2025\\-03\\-01 TO *]", "contact_date2 > 2025-03-01");

        // both sides are strings, no date involved
        assertSingleMatch("contact_date_s:2025\\-03\\-01\\ 00\\:55\\:11", "contact_date2 = \"2025-03-01 00:55:11\"");

        assertSingleMatch(
                "_query_:\"\\{\\!frange\\ l=1\\ u=1\\}if\\(and\\(exists\\(date_of_birth\\),exists\\(date_of_marriage\\),"
                        + "eq\\(sub\\(ms\\(date_of_birth\\),sub\\(ms\\(date_of_birth\\),"
                        + "mul\\(floor\\(div\\(ms\\(date_of_birth\\),86400000\\)\\),86400000\\)\\)\\),sub\\(ms\\(date_of_marriage\\),"
                        + "sub\\(ms\\(date_of_marriage\\),mul\\(floor\\(div\\(ms\\(date_of_marriage\\),86400000\\)\\),86400000\\)\\)\\)\\)\\),1,0\\)\"",
                "date_of_marriage = @date_of_birth");

        ctx.getGlobalFlagsTemplate().add(SolrConversionDirective.DISABLE_DATE_TIME_ALIGNMENT);

        assertSingleMatch("date_of_birth:2025\\-03\\-01T00\\:00\\:00Z", "date_of_birth = 2025-03-01");
        assertSingleMatch("date_of_birth:[* TO 2025\\-03\\-01T00\\:00\\:00Z}", "date_of_birth < 2025-03-01");
        assertSingleMatch("date_of_birth:{2025\\-03\\-01T00\\:00\\:00Z TO *]", "date_of_birth > 2025-03-01");

        assertSingleMatch("contact_date_l:1740787200000", "contact_date = 2025-03-01");

        assertSingleMatch("_query_:\"\\{\\!frange\\ l=1\\ u=1\\}if\\(and\\(exists\\(date_of_birth\\),exists\\(date_of_marriage\\),"
                + "eq\\(date_of_birth,date_of_marriage\\)\\),1,0\\)\"", "date_of_marriage = @date_of_birth");

        newCtx();

        // no date alignment because the the contact_date3 is of type STRING, graceful handling of time portion
        assertSingleMatch("contact_date_dt:2025\\-03\\-01T00\\:55\\:11Z", "contact_date3 = \"2025-03-01 00:55:11\"");

        // the argument date_of_birth is of type Audlang-DATE, so the given time portion is not not allowed (strict yyyy-MM-dd)
        assertThrowsErrorCode(CommonErrors.ERR_2006_VALUE_FORMAT_DATE,
                () -> assertSingleMatch("date_of_birth:[2025\\-03\\-01T00\\:00\\:00Z TO 2025\\-03\\-02T00\\:00\\:00Z}",
                        "date_of_birth = \"2025-03-01 00:55:11\""));

    }

    @Test
    void testCreateMultiValueMatchFilter() {

        assertMultiValueMatch("color:(blue OR red)", "color=blue OR color=red");
        assertMultiValueMatch("color:(blue OR green\\ stripes OR red)", "color ANY OF (blue, red, \"green stripes\")");

        assertMultiValueMatch("age:[18 TO *]", "age >= 18");
        assertMultiValueMatch("age:[* TO 17]", "age <= 17");

        // NOT happens outside!
        assertMultiValueMatch("color:(blue OR red)", "STRICT color!=blue AND STRICT color!=red");

        assertMultiValueMatch("_query_:\"\\{\\!frange\\ l=1\\ u=1\\}if\\(and\\(exists\\(color\\),exists\\(taste\\),lte\\(color,taste\\)\\),1,0\\)\"",
                "color <= @taste");
        assertMultiValueMatch("_query_:\"\\{\\!frange\\ l=1\\ u=1\\}if\\(and\\(exists\\(color\\),exists\\(taste\\),gte\\(color,taste\\)\\),1,0\\)\"",
                "color >= @taste");

        assertMultiValueMatch("_query_:\"\\{\\!frange\\ l=1\\ u=1\\}if\\(and\\(exists\\(color\\),exists\\(taste\\),lte\\(color,taste\\)\\),1,0\\)\"",
                "color <= @taste");
        assertMultiValueMatch("_query_:\"\\{\\!frange\\ l=1\\ u=1\\}if\\(and\\(exists\\(color\\),exists\\(taste\\),gte\\(color,taste\\)\\),1,0\\)\"",
                "color >= @taste");

        assertMultiValueMatch(
                "_query_:\"\\{\\!frange\\ l=1\\ u=1\\}if\\(and\\(exists\\(date_of_birth\\),exists\\(date_of_marriage\\),"
                        + "lte\\(sub\\(ms\\(date_of_birth\\),sub\\(ms\\(date_of_birth\\),"
                        + "mul\\(floor\\(div\\(ms\\(date_of_birth\\),86400000\\)\\),86400000\\)\\)\\),sub\\(ms\\(date_of_marriage\\),"
                        + "sub\\(ms\\(date_of_marriage\\),mul\\(floor\\(div\\(ms\\(date_of_marriage\\),86400000\\)\\),86400000\\)\\)\\)\\)\\),1,0\\)\"",
                "date_of_marriage >= @date_of_birth");

        assertMultiValueMatch("_query_:\"\\{\\!frange\\ l=1\\ u=1\\}if\\(and\\(exists\\(date_of_birth\\),exists\\(date_of_marriage\\),"
                + "gte\\(sub\\(ms\\(date_of_birth\\),sub\\(ms\\(date_of_birth\\),"
                + "mul\\(floor\\(div\\(ms\\(date_of_birth\\),86400000\\)\\),86400000\\)\\)\\),"
                + "sub\\(ms\\(date_of_marriage\\),sub\\(ms\\(date_of_marriage\\),mul\\(floor\\(div\\(ms\\(date_of_marriage\\),86400000\\)\\),86400000\\)\\)\\)\\)\\),1,0\\)\"",
                "date_of_marriage <= @date_of_birth");

    }

    @Test
    void testCreateBetweenMatchFilterStandard() {

        DefaultMatchFilterFactory factory = new DefaultMatchFilterFactory(resetCtx());

        MatchWrapper mw1 = wrap(expr("count_i > 1"));
        MatchWrapper mw2 = wrap(expr("count_i < 5"));

        BetweenMatchWrapper bmw = new BetweenMatchWrapper(mw1, mw2);

        assertEquals("count_i:{1 TO 5}", factory.createMatchFilter(bmw).queryString());

        MatchWrapper mw1eq = wrap(expr("count_i = 1"));
        MatchWrapper mw2eq = wrap(expr("count_i = 5"));

        MultiMatchWrapper mmw1 = new MultiMatchWrapper(mw1.nodeType(), Arrays.asList(mw1.firstMember(), mw1eq.firstMember()), mw1.matchInstruction(), true);

        bmw = new BetweenMatchWrapper(mmw1, mw2);

        assertEquals("count_i:[1 TO 5}", factory.createMatchFilter(bmw).queryString());

        MultiMatchWrapper mmw2 = new MultiMatchWrapper(mw2.nodeType(), Arrays.asList(mw2.firstMember(), mw2eq.firstMember()), mw2.matchInstruction(), true);

        bmw = new BetweenMatchWrapper(mmw1, mmw2);

        assertEquals("count_i:[1 TO 5]", factory.createMatchFilter(bmw).queryString());

        bmw = new BetweenMatchWrapper(mw1, mmw2);

        assertEquals("count_i:{1 TO 5]", factory.createMatchFilter(bmw).queryString());

    }

    @Test
    void testCreateBetweenMatchFilterDate() {

        DefaultMatchFilterFactory factory = new DefaultMatchFilterFactory(resetCtx());

        MatchWrapper mw1 = wrap(expr("date_of_birth > 2010-03-04"));
        MatchWrapper mw2 = wrap(expr("date_of_birth < 2010-04-04"));

        BetweenMatchWrapper bmw = new BetweenMatchWrapper(mw1, mw2);

        assertEquals("date_of_birth:[2010\\-03\\-05T00\\:00\\:00Z TO 2010\\-04\\-04T00\\:00\\:00Z}", factory.createMatchFilter(bmw).queryString());

        MatchWrapper mw1eq = wrap(expr("date_of_birth = 2010-03-04"));
        MatchWrapper mw2eq = wrap(expr("date_of_birth = 2010-04-04"));

        MultiMatchWrapper mmw1 = new MultiMatchWrapper(mw1.nodeType(), Arrays.asList(mw1.firstMember(), mw1eq.firstMember()), mw1.matchInstruction(), true);

        bmw = new BetweenMatchWrapper(mmw1, mw2);

        assertEquals("date_of_birth:[2010\\-03\\-04T00\\:00\\:00Z TO 2010\\-04\\-04T00\\:00\\:00Z}", factory.createMatchFilter(bmw).queryString());

        MultiMatchWrapper mmw2 = new MultiMatchWrapper(mw2.nodeType(), Arrays.asList(mw2.firstMember(), mw2eq.firstMember()), mw2.matchInstruction(), true);

        bmw = new BetweenMatchWrapper(mmw1, mmw2);

        assertEquals("date_of_birth:[2010\\-03\\-04T00\\:00\\:00Z TO 2010\\-04\\-05T00\\:00\\:00Z}", factory.createMatchFilter(bmw).queryString());

        bmw = new BetweenMatchWrapper(mw1, mmw2);

        assertEquals("date_of_birth:[2010\\-03\\-05T00\\:00\\:00Z TO 2010\\-04\\-05T00\\:00\\:00Z}", factory.createMatchFilter(bmw).queryString());

    }

    @Test
    void testCreateFieldValueConditionSpecial() {

        MatchExpression expression = expr("color IS UNKNOWN");

        DataField field = ctx.getMappingConfig().lookupField("color", ctx);

        DefaultMatchFilterFactory factory = new DefaultMatchFilterFactory(resetCtx());

        assertThrows(IllegalArgumentException.class, () -> factory.createFieldValueCondition(expression, field, false));
        assertThrows(IllegalArgumentException.class, () -> factory.createFieldValueCondition(expression, field, true));

        MatchExpression expression2 = expr("color CONTAINS red");

        factory.createFieldValueCondition(expression2, field, false);
        assertThrows(IllegalArgumentException.class, () -> factory.createFieldValueCondition(expression2, field, true));

        MatchExpression expression3 = expr("color = @taste");
        assertThrows(IllegalArgumentException.class, () -> factory.createFieldValueCondition(expression3, field, false));

    }

    @Test
    void testCreateFieldFieldConditionSpecial() {

        MatchExpression expression = expr("color = @taste");

        DataField fieldLeft = ctx.getMappingConfig().lookupField("color", ctx);
        DataField fieldRight = ctx.getMappingConfig().lookupField("taste", ctx);

        DefaultMatchFilterFactory factory = new DefaultMatchFilterFactory(resetCtx());

        String argNameLeft = expression.argName();
        String argNameRight = expression.referencedArgName();

        factory.createFieldFieldCondition(expression, argNameLeft, fieldLeft, argNameRight, fieldRight, false);

        assertCannotCreateFieldFieldCondition(factory, expression, argNameLeft, fieldLeft, argNameRight, fieldRight, true);

        expression = expr("color = red");

        assertCannotCreateFieldFieldCondition(factory, expression, argNameLeft, fieldLeft, argNameRight, fieldRight, false);

        expression = expr("age = @color");

        factory.createFieldFieldCondition(expression, expression.argName(), dataFieldLeft(expression), expression.referencedArgName(),
                dataFieldRight(expression), false);

        expression = expr("age = @field_in_different_document_i");

        assertCannotCreateFieldFieldCondition(ConversionException.class, factory, expression, expression.argName(), dataFieldLeft(expression),
                expression.referencedArgName(), dataFieldRight(expression), false);

        expression = expr("budget_md_i = @value_i");

        factory.createFieldFieldCondition(expression, expression.argName(), dataFieldLeft(expression), expression.referencedArgName(),
                dataFieldRight(expression), false);

        expression = expr("budget_md_i = @value_md_i");

        assertCannotCreateFieldFieldCondition(ConversionException.class, factory, expression, expression.argName(), dataFieldLeft(expression),
                expression.referencedArgName(), dataFieldRight(expression), false);

    }

    @Test
    void testProhibitiveDirectives() {
        ctx.getGlobalFlagsTemplate().add(SolrConversionDirective.DISABLE_REFERENCE_MATCHING);
        ctx.getGlobalFlagsTemplate().add(SolrConversionDirective.DISABLE_LESS_THAN_GREATER_THAN);
        ctx.getGlobalFlagsTemplate().add(SolrConversionDirective.DISABLE_CONTAINS);

        DefaultMatchFilterFactory factory = new DefaultMatchFilterFactory(resetCtx());

        MatchExpression expression = expr("color = @taste");

        assertCannotCreateFieldFieldCondition(ConversionException.class, factory, expression, expression.argName(), dataFieldLeft(expression),
                expression.referencedArgName(), dataFieldRight(expression), false);

        expression = expr("color CONTAINS red");

        assertCannotCreateFieldValueCondition(ContainsNotSupportedException.class, factory, expression, dataFieldLeft(expression), false);

        expression = expr("age > 17");

        assertCannotCreateFieldValueCondition(LessThanGreaterThanNotSupportedException.class, factory, expression, dataFieldLeft(expression), false);

        newCtx();

    }

    @Test
    void testOperationUnsupportedByType() {

        DefaultMatchFilterFactory factory = new DefaultMatchFilterFactory(resetCtx());

        MatchExpression expression = expr("age CONTAINS 6");
        assertCannotCreateFieldValueCondition(ContainsNotSupportedException.class, factory, expression, dataFieldLeft(expression), false);

        expression = expr("flag_b CONTAINS 6");
        assertCannotCreateFieldValueCondition(ContainsNotSupportedException.class, factory, expression, dataFieldLeft(expression), false);

        expression = expr("flag_b > 0");
        assertCannotCreateFieldValueCondition(LessThanGreaterThanNotSupportedException.class, factory, expression, dataFieldLeft(expression), false);

        expression = expr("flag1_b > @flag2_b");

        assertCannotCreateFieldFieldCondition(LessThanGreaterThanNotSupportedException.class, factory, expression, expression.argName(),
                dataFieldLeft(expression), expression.referencedArgName(), dataFieldRight(expression), false);

        expression = expr("hobbies_ss = @hobbies2_ss");

        assertCannotCreateFieldFieldCondition(ConversionException.class, factory, expression, expression.argName(), dataFieldLeft(expression),
                expression.referencedArgName(), dataFieldRight(expression), false);

    }

    private void assertCannotCreateFieldValueCondition(Class<? extends RuntimeException> expectedError, DefaultMatchFilterFactory factory,
            MatchExpression expression, DataField field, boolean orEquals) {
        assertThrows(expectedError, () -> factory.createFieldValueCondition(expression, field, orEquals));
    }

    private void assertCannotCreateFieldFieldCondition(DefaultMatchFilterFactory factory, MatchExpression expression, String argNameLeft, DataField fieldLeft,
            String argNameRight, DataField fieldRight, boolean orEquals) {
        assertThrows(IllegalArgumentException.class,
                () -> factory.createFieldFieldCondition(expression, argNameLeft, fieldLeft, argNameRight, fieldRight, orEquals));

    }

    private void assertCannotCreateFieldFieldCondition(Class<? extends RuntimeException> expectedError, DefaultMatchFilterFactory factory,
            MatchExpression expression, String argNameLeft, DataField fieldLeft, String argNameRight, DataField fieldRight, boolean orEquals) {
        assertThrows(expectedError, () -> factory.createFieldFieldCondition(expression, argNameLeft, fieldLeft, argNameRight, fieldRight, orEquals));

    }

    private void assertMultiValueMatch(String expected, String expression) {
        DefaultMatchFilterFactory factory = new DefaultMatchFilterFactory(resetCtx());

        CombinedExpression exprBase = expr(expression);

        List<SimpleExpression> simpleExprList = exprBase.childExpressions().stream().map(SimpleExpression.class::cast).toList();

        SimpleExpression firstExpression = simpleExprList.get(0);

        boolean notIn = firstExpression instanceof NegationExpression;
        List<MatchExpression> members = null;
        MultiMatchWrapper mmw = null;
        if (notIn) {
            members = simpleExprList.stream().map(NegationExpression.class::cast).map(NegationExpression::delegate).toList();
            mmw = new MultiMatchWrapper(nodeTypeOf(members), members, MatchInstruction.NEGATE, false);
        }
        else {
            members = simpleExprList.stream().map(MatchExpression.class::cast).toList();
            mmw = new MultiMatchWrapper(nodeTypeOf(members), members, MatchInstruction.DEFAULT, false);
        }
        assertEquals(nodeTypeOf(firstExpression), mmw.commonNodeType());
        assertEquals(notIn ? MatchInstruction.NEGATE : MatchInstruction.DEFAULT, mmw.matchInstruction());
        assertEquals(members.size(), mmw.members().size());
        assertTrue(mmw.isLeafElement());
        assertTrue(mmw.childElements().isEmpty());
        assertEquals(firstExpression.argName(), mmw.argName());
        assertEquals(notIn, mmw.containsAnyNegation());
        assertEquals(notIn, mmw.isNegation());
        assertEquals(firstExpression.referencedArgName() != null, mmw.isReferenceMatch());
        assertEquals(firstExpression.operator(), mmw.operator());

        SolrFilterQuery fq = factory.createMatchFilter(mmw);

        if (firstExpression.operator() == MatchOperator.LESS_THAN || firstExpression.operator() == MatchOperator.GREATER_THAN) {
            if (mmw.isReferenceMatch()) {
                assertEquals(Arrays.asList(SolrConditionType.FRANGE), fq.conditionTypes());
            }
            else {
                assertEquals(Arrays.asList(SolrConditionType.CMP_RANGE), fq.conditionTypes());
            }
        }
        else {
            assertEquals(Arrays.asList(SolrConditionType.CMP_VALUE), fq.conditionTypes());
        }
        assertEquals(Arrays.asList(nodeTypeOf(firstExpression)), fq.nodeTypesInvolved());
        assertTrue(fq.fields().stream().map(SolrQueryField::fieldName).anyMatch(fieldNameLeft(firstExpression)::equals));
        assertEquals(nodeTypeOf(firstExpression), fq.fields().get(0).nodeType());
        assertEquals(expected, fq.queryString());

    }

    private void assertSingleMatch(String expected, String expression) {
        DefaultMatchFilterFactory factory = new DefaultMatchFilterFactory(resetCtx());

        MatchExpression exprPositive = expr(expression);
        SolrFilterQuery fq = factory.createMatchFilter(wrap(exprPositive));

        assertSingleMatchInternal(expected, fq, exprPositive);

        NegationExpression exprNegative = (NegationExpression) exprPositive.negate(true);
        fq = factory.createMatchFilter(wrap(exprNegative));

        assertSingleMatchInternal(expected, fq, exprNegative);

    }

    private void assertSingleMatchInternal(String expected, SolrFilterQuery fq, SimpleExpression expr) {
        assertEquals(Arrays.asList(conditionTypeOf(expr)), fq.conditionTypes());
        assertEquals(Arrays.asList(nodeTypeOf(expr)), fq.nodeTypesInvolved());
        assertTrue(fq.fields().stream().map(SolrQueryField::fieldName).anyMatch(fieldNameLeft(expr)::equals));
        assertTrue(expr.referencedArgName() == null || fq.fields().stream().map(SolrQueryField::fieldName).anyMatch(fieldNameRight(expr)::equals));
        assertEquals(nodeTypeOf(expr), fq.fields().get(0).nodeType());
        assertEquals(expected, fq.queryString());

    }

    private SingleMatchWrapper wrap(SimpleExpression expression) {

        MatchExpression candidate = null;
        MatchInstruction instruction = MatchInstruction.DEFAULT;
        if (expression instanceof NegationExpression neg) {
            candidate = neg.delegate();
            instruction = MatchInstruction.NEGATE;
        }
        else {
            candidate = (MatchExpression) expression;
        }

        return new SingleMatchWrapper(nodeTypeOf(candidate), candidate, instruction, false);
    }

    private String nodeTypeOf(SimpleExpression expression) {
        return ConversionTestUtils.nodeTypeOf(expression, ctx);
    }

    private String nodeTypeOf(List<MatchExpression> candidates) {
        return ConversionTestUtils.nodeTypeOf(candidates, ctx);
    }

    private String fieldNameLeft(SimpleExpression expression) {
        return ConversionTestUtils.fieldNameLeft(expression, ctx);
    }

    private String fieldNameRight(SimpleExpression expression) {
        return ConversionTestUtils.fieldNameRight(expression, ctx);
    }

    private DataField dataFieldLeft(SimpleExpression expression) {
        return ConversionTestUtils.dataFieldLeft(expression, ctx);
    }

    private DataField dataFieldRight(SimpleExpression expression) {
        return ConversionTestUtils.dataFieldRight(expression, ctx);
    }

    private SolrConditionType conditionTypeOf(SimpleExpression expression) {
        return ConversionTestUtils.conditionTypeOf(expression, ctx);
    }

}
