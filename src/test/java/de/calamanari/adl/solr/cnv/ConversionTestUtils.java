//@formatter:off
/*
 * ConversionTestUtils
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

import static de.calamanari.adl.cnv.StandardConversions.parseCoreExpression;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.DATE;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.INTEGER;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.STRING;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_DATE;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_INTEGER;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_STRING;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrDocumentList;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.calamanari.adl.cnv.StandardConversions;
import de.calamanari.adl.cnv.tps.ArgMetaInfo;
import de.calamanari.adl.cnv.tps.DefaultAdlType;
import de.calamanari.adl.irl.CombinedExpression;
import de.calamanari.adl.irl.CoreExpression;
import de.calamanari.adl.irl.MatchExpression;
import de.calamanari.adl.irl.MatchOperator;
import de.calamanari.adl.irl.NegationExpression;
import de.calamanari.adl.irl.SimpleExpression;
import de.calamanari.adl.solr.AdlSolrType;
import de.calamanari.adl.solr.DefaultAdlSolrType;
import de.calamanari.adl.solr.SolrConditionType;
import de.calamanari.adl.solr.SolrTestBase;
import de.calamanari.adl.solr.config.ArgFieldAssignment;
import de.calamanari.adl.solr.config.DataField;
import de.calamanari.adl.solr.config.MainDocumentConfig;
import de.calamanari.adl.solr.config.SolrMappingConfig;
import de.calamanari.adl.solr.config.SubDocumentConfig;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class ConversionTestUtils {

    /**
     * Returns a context with a dry (not physically bound) mapping configuration with a couple of default fields and types for testing
     * 
     * @return dry context for testing common scenarios
     */
    public static ResettableScpContext createDryTestContext() {
        // @formatter:off
        SolrMappingConfig mappingConfig = MainDocumentConfig.forNodeType(SolrTestBase.NODE_TYPE_1)
                                                                  .dataField("color", SOLR_STRING)
                                                                      .mappedToArgName("color", STRING)
                                                                  .dataField("taste", SOLR_STRING)
                                                                      .mappedToArgName("taste", STRING)
                                                                  .dataField("age", SOLR_INTEGER)
                                                                      .mappedToArgName("age", INTEGER)
                                                                  .dataField("date_of_birth", SOLR_DATE)
                                                                      .mappedToArgName("date_of_birth")
                                                                  .dataField("date_of_marriage", SOLR_DATE)
                                                                      .mappedToArgName("date_of_marriage")
                                                                  .dataField("numbers_is")
                                                                      .mappedToArgName("numbers")
                                                                  .defaultAutoMapped()
                                                                  .subConfig(SubDocumentConfig.forNodeType(SolrTestBase.NODE_TYPE_2)
                                                                      .nested()
                                                                      .dataField("fav_color_s")
                                                                          .mappedToArgName("favColor")
                                                                      .dataField("car_owner_b")
                                                                          .mappedToArgName("carOwner").multiDoc()
                                                                      .autoMapped(s -> s.endsWith("_md_i") ? s : null)
                                                                          .multiDoc()
                                                                      .autoMapped(s -> s.endsWith("_i") ? s : null)
                                                                      .autoMapped(s -> s.endsWith("_md_l") ? s : null)
                                                                          .multiDoc()
                                                                      .autoMapped(s -> s.endsWith("_l") ? s : null)
                                                                  .get())
                                                                  .subConfig(SubDocumentConfig.forNodeType(SolrTestBase.NODE_TYPE_3)
                                                                      .dependent()
                                                                      .dataField("article_s")
                                                                          .mappedToArgName("article")
                                                                      .dataField("price_d")
                                                                          .mappedToArgName("price")
                                                                      .dataField("purchase_date_dt")
                                                                          .mappedToArgName("purchaseDate")
                                                                      .dataField("purchase_date_dt")
                                                                          .mappedToArgName("anyPurchaseDate").multiDoc()
                                                                      .dataField("contact_date_l")
                                                                          .mappedToArgName("contact_date", DATE)
                                                                      .dataField("contact_date_s")
                                                                          .mappedToArgName("contact_date2", DATE)
                                                                      .dataField("contact_date_dt")
                                                                          .mappedToArgName("contact_date3", STRING)
                                                                      .autoMapped(s -> s.endsWith("_md_b") ? s : null)
                                                                          .multiDoc()
                                                                      .autoMapped(s -> s.endsWith("_b") ? s : null)
                                                                      .autoMapped(s -> s.endsWith("_md_f") ? s : null)
                                                                          .multiDoc()
                                                                      .autoMapped(s -> s.endsWith("_f") ? s : null)
                                                                  .get())
                                                              .get();
        // @formatter:on

        return new ResettableScpContext(mappingConfig, null, null);

    }

    /**
     * Creates an object mapper with the following settings:
     * <ul>
     * <li>Ignore any unknown properties on de-serialization</li>
     * <li>Do not serialize properties with null-value.</li>
     * <li>Pretty-print (if flag true) with 4-space indentation and line-break preceding each array element.</li>
     * </ul>
     * 
     * @param prettyPrint if true create json with indentation, otherwise inline
     * @return object mapper
     */
    public static ObjectMapper createObjectMapper(boolean prettyPrint) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        if (prettyPrint) {
            DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
            DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
            prettyPrinter.indentObjectsWith(indenter);
            prettyPrinter.indentArraysWith(indenter);
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.setDefaultPrettyPrinter(prettyPrinter);
        }
        return objectMapper;
    }

    /**
     * Produces a readable debug string (json) of the documents returned by solr
     * 
     * @param results
     * @return json list or null for null
     */
    public static String toDebugString(SolrDocumentList results) {
        if (results == null) {
            return "null";
        }
        else if (results.isEmpty()) {
            return "[]";
        }
        else {
            return prettyPrintJson(results.stream().map(d -> d.jsonStr()).collect(Collectors.joining(",\n", "\n[\n", "\n]")));
        }
    }

    /**
     * Parses the given json and returns it pretty-printed
     * 
     * @param mapper
     * @param json
     * @return formatted version
     */
    public static String prettyPrintJson(String json) {
        if (json != null && !json.isBlank()) {
            ObjectMapper mapper = createObjectMapper(true);
            try {
                JsonNode root = mapper.readTree(json);
                json = mapper.writeValueAsString(root);
            }
            catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return json;
    }

    /**
     * Assigns the field to the argName with type string
     * 
     * @param nodeType
     * @param argName
     * @param fieldName
     * @return assignment
     */
    public static ArgFieldAssignment simpleAssignment(String nodeType, String argName, String fieldName) {

        ArgMetaInfo argMetaInfo = new ArgMetaInfo(argName, STRING, false, false);

        DataField field = new DataField(nodeType, fieldName, SOLR_STRING, false);

        return new ArgFieldAssignment(argMetaInfo, field, false);

    }

    /**
     * Parses the given expression and returns it
     * 
     * @param <T> expected type
     * @param expression
     * @return expression
     */
    public static <T extends CoreExpression> T expr(String expression) {
        CoreExpression temp = parseCoreExpression(expression);

        @SuppressWarnings("unchecked")
        T res = (T) temp;
        return res;
    }

    /**
     * Parses the given expressions and returns a list
     * 
     * @param <T> expected type
     * @param expressions
     * @return list of expressions or single list
     */
    public static <T extends CoreExpression> List<T> exprList(String... expressions) {

        List<CoreExpression> temp = Arrays.stream(expressions).map(StandardConversions::parseCoreExpression).toList();
        if (temp.stream().allMatch(MatchExpression.class::isInstance)) {
            List<MatchExpression> temp2 = temp.stream().map(MatchExpression.class::cast).toList();

            @SuppressWarnings("unchecked")
            List<T> res = (List<T>) temp2;
            return res;
        }
        if (temp.stream().allMatch(SimpleExpression.class::isInstance)) {
            List<SimpleExpression> temp2 = temp.stream().map(SimpleExpression.class::cast).toList();

            @SuppressWarnings("unchecked")
            List<T> res = (List<T>) temp2;
            return res;
        }
        @SuppressWarnings("unchecked")
        List<T> res = (List<T>) temp;
        return res;

    }

    /**
     * @param expression
     * @param ctx
     * @return basic match-tree (all simple expressions wrapped), negations all with {@link MatchInstruction#NEGATE}
     */
    public static MatchTreeElement matchTreeOf(String expression, SolrConversionProcessContext ctx) {
        return matchTreeOf(expr(expression), ctx);
    }

    /**
     * @param expression
     * @param ctx
     * @return basic match-tree (all simple expressions wrapped), negations all with {@link MatchInstruction#NEGATE}
     */
    public static MatchTreeElement matchTreeOf(CoreExpression expression, SolrConversionProcessContext ctx) {
        switch (expression) {
        case CombinedExpression cmb:
            List<MatchTreeElement> members = cmb.childExpressions().stream().map(e -> matchTreeOf(e, ctx)).toList();
            return new CombinedMatchTreeElement(cmb.combiType(), members);
        case SimpleExpression simple:
            return wrap(simple, ctx);
        default:
            throw new IllegalArgumentException("Unsupported expression: " + expression);
        }
    }

    /**
     * @param expression
     * @param ctx
     * @return match wrapper for a single match or negation
     */
    public static SingleMatchWrapper wrap(String expression, SolrConversionProcessContext ctx) {
        CoreExpression e = expr(expression);
        if (e instanceof SimpleExpression simple) {
            return wrap(simple, ctx);
        }
        throw new IllegalArgumentException("Attempt to wrap unsupported expression type (expected SimpleExpression): " + expression);
    }

    /**
     * @param expression
     * @param ctx
     * @return match wrapper for a single match or negation
     */
    public static SingleMatchWrapper wrap(SimpleExpression expression, SolrConversionProcessContext ctx) {

        MatchExpression candidate = null;
        MatchInstruction instruction = expression.operator() == MatchOperator.IS_UNKNOWN ? MatchInstruction.NEGATE : MatchInstruction.DEFAULT;
        if (expression instanceof NegationExpression neg) {
            candidate = neg.delegate();
            instruction = expression.operator() == MatchOperator.IS_UNKNOWN ? MatchInstruction.DEFAULT : MatchInstruction.NEGATE;
        }
        else {
            candidate = (MatchExpression) expression;
        }

        return new SingleMatchWrapper(nodeTypeOf(candidate, ctx), candidate, instruction, false);
    }

    /**
     * @param expression (reference matches must have a common node type)
     * @param ctx
     * @return node type of the given expression
     */
    public static String nodeTypeOf(SimpleExpression expression, SolrConversionProcessContext ctx) {
        String nodeTypeLeft = ctx.getMappingConfig().lookupNodeTypeMetaInfo(expression.argName(), ctx).nodeType();
        String nodeTypeRight = expression.referencedArgName() == null ? null
                : ctx.getMappingConfig().lookupNodeTypeMetaInfo(expression.referencedArgName(), ctx).nodeType();

        if (nodeTypeRight != null && !nodeTypeRight.equals(nodeTypeLeft)) {
            throw new RuntimeException(String.format("Different node types %s, %s detected for expression=%s", nodeTypeLeft, nodeTypeRight, expression));
        }
        return nodeTypeLeft;
    }

    /**
     * @param candidates
     * @param ctx
     * @return common node type of the given match expressions
     */
    public static String nodeTypeOf(List<MatchExpression> candidates, SolrConversionProcessContext ctx) {
        String nodeType = nodeTypeOf(candidates.get(0), ctx);
        for (int i = 1; i < candidates.size(); i++) {
            String otherNodeType = nodeTypeOf(candidates.get(i), ctx);
            if (!nodeType.equals(otherNodeType)) {
                throw new RuntimeException(String.format("Different node types %s, %s detected for expression=%s", nodeType, otherNodeType, candidates));
            }
        }
        return nodeType;
    }

    /**
     * @param expression
     * @param ctx
     * @return name of the field the left argument is mapped to
     */
    public static String fieldNameLeft(SimpleExpression expression, SolrConversionProcessContext ctx) {
        return ctx.getMappingConfig().lookupAssignment(expression.argName(), ctx).field().fieldName();
    }

    /**
     * @param expression
     * @param ctx
     * @return name of the field the right argument is mapped to or null if this is not a reference match
     */
    public static String fieldNameRight(SimpleExpression expression, SolrConversionProcessContext ctx) {
        return expression.referencedArgName() == null ? null : ctx.getMappingConfig().lookupAssignment(expression.referencedArgName(), ctx).field().fieldName();
    }

    /**
     * @param expression
     * @param ctx
     * @return data field the left argument is mapped to
     */
    public static DataField dataFieldLeft(SimpleExpression expression, SolrConversionProcessContext ctx) {
        return ctx.getMappingConfig().lookupField(expression.argName(), ctx);
    }

    /**
     * @param expression
     * @param ctx
     * @return data field the right argument is mapped to or null if this is not a reference match
     */
    public static DataField dataFieldRight(SimpleExpression expression, SolrConversionProcessContext ctx) {
        if (expression.referencedArgName() == null) {
            return null;
        }
        return ctx.getMappingConfig().lookupField(expression.referencedArgName(), ctx);
    }

    /**
     * @param expression
     * @param ctx
     * @return expected condition type
     */
    public static SolrConditionType conditionTypeOf(SimpleExpression expression, SolrConversionProcessContext ctx) {

        MatchOperator operator = expression.operator();

        if (expression.referencedArgName() == null) {

            switch (operator) {
            case EQUALS:
                return isDateComplication(expression, ctx) ? SolrConditionType.CMP_RANGE : SolrConditionType.CMP_VALUE;
            case LESS_THAN, GREATER_THAN:
                return SolrConditionType.CMP_RANGE;
            case CONTAINS:
                return SolrConditionType.CMP_TXT_CONTAINS;
            case IS_UNKNOWN:
                return SolrConditionType.CMP_ANY;
            default:
                throw new IllegalArgumentException("Unsupported operator: " + expression);
            }
        }
        else {
            return SolrConditionType.FRANGE;
        }
    }

    /**
     * @param expression
     * @param ctx
     * @return true if date alignment is involved
     */
    public static boolean isDateComplication(SimpleExpression expression, SolrConversionProcessContext ctx) {
        ArgFieldAssignment assignment = ctx.getMappingConfig().lookupAssignment(expression.argName(), ctx);
        if (assignment.arg().type().getBaseType().equals(DefaultAdlType.DATE)
                && !SolrConversionDirective.DISABLE_DATE_TIME_ALIGNMENT.check(ctx.getGlobalFlags())) {

            AdlSolrType fieldType = assignment.field().fieldType().getBaseType();
            if (fieldType.equals(DefaultAdlSolrType.SOLR_INTEGER) || fieldType.equals(DefaultAdlSolrType.SOLR_LONG)
                    || fieldType.equals(DefaultAdlSolrType.SOLR_DATE)) {
                return true;
            }

        }
        return false;
    }

    public static void assertEqualsIgnoreElementOrder(MatchElement left, MatchElement right) {

        assertTrue(String.format("%s was not effectively equal to: %s", left, right), equalsIgnoreElementOrder(left, right));

    }

    private static boolean equalsIgnoreElementOrder(MatchElement left, MatchElement right) {

        if (Objects.equals(left, right)) {
            return true;
        }

        if (left instanceof CombinedMatchTreeElement cmbLeft && right instanceof CombinedMatchTreeElement cmbRight) {
            if (cmbLeft.combiType() != cmbRight.combiType()) {
                return false;
            }
            for (MatchTreeElement memberLeft : cmbLeft.childElements()) {
                if (!cmbRight.childElements().stream().anyMatch(memberRight -> equalsIgnoreElementOrder(memberLeft, memberRight))) {
                    return false;
                }
            }
            return true;

        }

        return false;

    }

    public static void assertEqualElements(Collection<?> expected, Collection<?> given) {

        assertEquals(expected.size(), given.size(), String.format("Expected: %s, found: %s of different size", expected, given));

        for (Object expectedElement : expected) {
            assertTrue(String.format("Expected: %s, found: %s, missing: %s", expected, given, expectedElement), given.contains(expectedElement));
        }

    }

    private ConversionTestUtils() {
        // utilities
    }

}
