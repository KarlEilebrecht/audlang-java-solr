//@formatter:off
/*
 * SolrExpressionConverter
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.calamanari.adl.AudlangMessage;
import de.calamanari.adl.CombinedExpressionType;
import de.calamanari.adl.CommonErrors;
import de.calamanari.adl.ConversionException;
import de.calamanari.adl.Flag;
import de.calamanari.adl.FormatStyle;
import de.calamanari.adl.SpecialSetType;
import de.calamanari.adl.cnv.AbstractCoreExpressionConverter;
import de.calamanari.adl.irl.CombinedExpression;
import de.calamanari.adl.irl.CoreExpression;
import de.calamanari.adl.irl.MatchExpression;
import de.calamanari.adl.irl.MatchOperator;
import de.calamanari.adl.irl.NegationExpression;
import de.calamanari.adl.irl.Operand;
import de.calamanari.adl.irl.SimpleExpression;
import de.calamanari.adl.irl.SpecialSetExpression;
import de.calamanari.adl.solr.DefaultAdlSolrType;
import de.calamanari.adl.solr.SolrFilterQuery;
import de.calamanari.adl.solr.SolrFormatConstants;
import de.calamanari.adl.solr.SolrQueryDefinition;
import de.calamanari.adl.solr.config.ArgFieldAssignment;
import de.calamanari.adl.solr.config.DataField;
import de.calamanari.adl.solr.config.NodeTypeMetaInfo;
import de.calamanari.adl.solr.config.SolrDocumentNature;
import de.calamanari.adl.solr.config.SolrMappingConfig;

/**
 * The {@link SolrExpressionConverter} translates {@link CoreExpression}s into {@link SolrQueryDefinition}s creating joins of documents where required.
 * <p>
 * <b>Important:</b> The set of global variables and flags you specify to the constructor act as a <i>template</i> for each subsequent run. A conversion run
 * will never modify the given map instance with the variables or the given set with the flags provided at construction time because it will run on independent
 * copies. Accordingly, any external modification to the given map and set <i>during</i> a conversion won't have any impact on that particular run but take
 * effect when the next conversion gets executed. In other words: each conversion will be initialized <i>at that time</i> with the variables and flags from the
 * specified global map and set to run on a copy afterwards.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class SolrExpressionConverter extends AbstractCoreExpressionConverter<SolrQueryDefinition, SolrConversionContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrExpressionConverter.class);

    /**
     * Process context of this converter (across levels), each call to {@link #convert(Object)} resets it to its initial state to avoid artifacts leaking into
     * the next conversion.
     */
    private final ResettableScpContext processContext;

    /**
     * Ensure every newly supplied local level context shares the process context with the converter
     * 
     * @param converter
     * @param processContext
     */
    private static void registerProcessContextProvider(SolrExpressionConverter converter, ResettableScpContext processContext) {

        // this is by intention in a static method, so that the lambda below has no unintended backward reference to the converter instance
        // references shall all go away from the converter to avoid circles
        converter.setContextPreparator(localContext -> {
            localContext.setProcessContext(processContext);
            return localContext;
        });
    }

    /**
     * Creates a new instance of the converter, fully prepared to call {@link #convert(Object)}.
     * 
     * @see #getProcessContext()
     * @param contextSupplier to create a context for each level of the expression we visit
     * @param mappingConfig
     * @param globalVariables optional global variables (null means empty)
     * @param flags directives and dynamic flags (null means empty)
     */
    protected SolrExpressionConverter(Supplier<? extends SolrConversionContext> contextSupplier, SolrMappingConfig mappingConfig,
            Map<String, Serializable> globalVariables, Set<Flag> flags) {
        super(contextSupplier);
        this.processContext = new ResettableScpContext(mappingConfig, globalVariables, flags);
        registerProcessContextProvider(this, processContext);
    }

    /**
     * @param mappingConfig
     * @param globalVariables initial global variables
     * @param flags initial flags
     */
    public SolrExpressionConverter(SolrMappingConfig mappingConfig, Map<String, Serializable> globalVariables, Set<Flag> flags) {
        this(SolrConversionContext::new, mappingConfig, globalVariables, flags);
    }

    /**
     * @param mappingConfig
     * @param flags initial flags
     */
    public SolrExpressionConverter(SolrMappingConfig mappingConfig, Flag... flags) {
        this(SolrConversionContext::new, mappingConfig, null, flags != null ? new HashSet<>(Arrays.asList(flags)) : null);
    }

    /**
     * Returns the converter's <i>process context</i> across the levels of a conversion.
     * <p>
     * While each level of an expression's DAG gets its own level context {@link #getContext()}, {@link #getParentContext()}, {@link #getRootContext()}), the
     * process context (global variables and general converter state) is common to all of them and allows data exchange.
     * <p>
     * By sharing the process context callers have access to the full data and feature set of the converter without sharing the converter instance itself.
     * 
     * @return the current global process context of the converter (variables, flags, etc.), instance of {@link ResettableScpContext}
     */
    public final SolrConversionProcessContext getProcessContext() {
        return this.processContext;
    }

    @Override
    public void init() {
        super.init();
        ((ResettableScpContext) getProcessContext()).reset();
    }

    /**
     * Returns the variable configuration passed to the constructor
     * <p>
     * This configuration acts <i>as a template</i> for all subsequent calls to {@link #convert(Object)}.
     * <p>
     * You can use this method if you did not specify any variables at construction time but instead want to define them now (before calling
     * {@link #convert(Object)}).
     * 
     * @return mutable map
     */
    public final Map<String, Serializable> getInitialVariables() {
        return ((ResettableScpContext) getProcessContext()).getGlobalVariablesTemplate();
    }

    /**
     * Returns the flag configuration passed to the constructor
     * <p>
     * This configuration acts <i>as a template</i> for all subsequent calls to {@link #convert(Object)}
     * <p>
     * You can use this method if you did not specify any flags at construction time but instead want to define them now (before calling
     * {@link #convert(Object)}).
     * 
     * @return mutable set
     */
    public final Set<Flag> getInitialFlags() {
        return ((ResettableScpContext) getProcessContext()).getGlobalFlagsTemplate();
    }

    /**
     * @return configured formatting style (inline or multi-line)
     */
    public final FormatStyle getStyle() {
        return getProcessContext().getStyle();
    }

    /**
     * @param style formatting style (inline or multi-line)
     */
    public final void setStyle(FormatStyle style) {
        ((ResettableScpContext) getProcessContext()).setStyle(style);
    }

    /**
     * @return nodeType of the field mapped to the given argName
     */
    protected final String nodeTypeOf(String argName) {
        return getProcessContext().getMappingConfig().lookupNodeTypeMetaInfo(argName, processContext).nodeType();
    }

    /**
     * @return Solr-mapping configuration from the context
     */
    protected final SolrMappingConfig mappingConfig() {
        return getProcessContext().getMappingConfig();
    }

    /**
     * @return current run's match filter factory
     * @see SolrConversionProcessContext#getMatchFilterFactory()
     */
    protected final MatchFilterFactory matchFilterFactory() {
        return getProcessContext().getMatchFilterFactory();
    }

    /**
     * @return current run's match tree helper
     * @see SolrConversionProcessContext#getMatchTreeHelper()
     */
    protected final MatchTreeHelper matchTreeHelper() {
        return getProcessContext().getMatchTreeHelper();
    }

    /**
     * @return the filter query builder for this conversion run
     */
    protected final SolrFilterQueryBuilder fqBuilder() {
        return getProcessContext().getFilterQueryBuilder();
    }

    @Override
    protected CoreExpression prepareRootExpression() {
        CoreExpression rootExpression = getRootExpression();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Preparing \n{}", rootExpression.format(FormatStyle.PRETTY_PRINT));
        }

        if (rootExpression instanceof SpecialSetExpression spc) {

            if (spc.setType() == SpecialSetType.ALL) {
                throw new ConversionException(String.format("Unable to convert the given expression because it implies <%s>.", SpecialSetExpression.all()),
                        AudlangMessage.msg(CommonErrors.ERR_1001_ALWAYS_TRUE));
            }
            else {
                throw new ConversionException(String.format("Unable to convert the given expression because it implies <%s>.", SpecialSetExpression.none()),
                        AudlangMessage.msg(CommonErrors.ERR_1002_ALWAYS_FALSE));
            }
        }

        rootExpression = IsNotUnknownPropagator.process(rootExpression);

        ((ResettableScpContext) getProcessContext()).setExpressionHelper(createCoreExpressionSolrHelper(rootExpression));
        ((ResettableScpContext) getProcessContext()).setMatchFilterFactory(createMatchFilterFactory(rootExpression));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Preparation complete: \n{} \nflags={}", rootExpression.format(FormatStyle.PRETTY_PRINT), getProcessContext().getGlobalFlags());
        }
        return rootExpression;
    }

    /**
     * This method allows sub-classes to replace the helper with a custom one
     * 
     * @param rootExpression the prepared root expression before start
     * @return new helper instance, a {@link DefaultMatchTreeHelper} by default
     */
    protected MatchTreeHelper createCoreExpressionSolrHelper(CoreExpression rootExpression) {
        return new DefaultMatchTreeHelper(getProcessContext());
    }

    /**
     * This method allows sub-classes to replace the match filter factory with a custom one
     * 
     * @param rootExpression the prepared root expression before start
     * @return new factory instance, a {@link DefaultMatchFilterFactory} by default
     */
    protected MatchFilterFactory createMatchFilterFactory(CoreExpression rootExpression) {
        return new DefaultMatchFilterFactory(getProcessContext());
    }

    /**
     * This method turns the (NOT) IS UNKNOWNS into (NOT) HAS ANY and determines if additional verification (co-matching of IS NOT UNKNOWN) is required in case
     * of a negation match.
     * 
     * @param expression
     * @return instruction derived from the expression
     */
    private MatchInstruction createMatchInstruction(MatchExpression expression) {
        if (getParentContext().isNegation()) {
            return createNegationMatchInstruction(expression);
        }
        else if (expression.operator() == MatchOperator.IS_UNKNOWN) {
            return MatchInstruction.NEGATE;
        }
        else {
            return MatchInstruction.DEFAULT;
        }
    }

    /**
     * Checks complex co-matching requirements in case of negations
     * 
     * @param expression
     * @return instruction derived from the expression
     */
    private MatchInstruction createNegationMatchInstruction(MatchExpression expression) {
        Set<String> pinnedIsUnknownArgNames = getContext().getPinnedIsUnknownArgNames();
        if (expression.referencedArgName() != null) {
            if (pinnedIsUnknownArgNames.contains(expression.argName()) && pinnedIsUnknownArgNames.contains(expression.referencedArgName())) {
                return MatchInstruction.NEGATE;
            }
            else if (pinnedIsUnknownArgNames.contains(expression.argName())) {
                // it might be that this is unreachable because of the pinning effect of surrounding IS-(NOT)-UNKNOWN constraints
                return MatchInstruction.NEGATE_VERIFY_RIGHT_ARG_HAS_ANY_VALUE;
            }
            else if (pinnedIsUnknownArgNames.contains(expression.referencedArgName())) {
                // it might be that this is unreachable because of the pinning effect of surrounding IS-(NOT)-UNKNOWN constraints
                return MatchInstruction.NEGATE_VERIFY_LEFT_ARG_HAS_ANY_VALUE;
            }
            else {
                return MatchInstruction.NEGATE_VERIFY_BOTH_ARGS_HAVE_ANY_VALUE;
            }
        }
        else {
            if (expression.operator() == MatchOperator.IS_UNKNOWN) {
                return MatchInstruction.DEFAULT;
            }
            else if (pinnedIsUnknownArgNames.contains(expression.argName())) {
                return MatchInstruction.NEGATE;
            }
            else {
                return MatchInstruction.NEGATE_VERIFY_LEFT_ARG_HAS_ANY_VALUE;
            }
        }
    }

    /**
     * This method corresponds to the preparation work of {@link IsNotUnknownPropagator}: Any argName in and AND that is either <i>pinned</i> to a value, to IS
     * UNKNOWN or NOT IS UNKNOWN does not require any <i>co-match</i> to check for IS NOT UNKNOWN in case of a negation on this level and below.
     * 
     * @param cmbAnd
     */
    private void registerDirectlyPinnedIsUnknownArgNamesFromAndCondition(CombinedExpression cmbAnd) {
        Set<String> pinnedIsUnknownArgNames = getContext().getPinnedIsUnknownArgNames();
        // @formatter:off
        cmbAnd.childExpressions().stream().filter(SimpleExpression.class::isInstance)
                                          .map(SimpleExpression.class::cast)
                                          .filter(e -> (e instanceof MatchExpression) || e.operator() == MatchOperator.IS_UNKNOWN)
                                          .forEach(simple -> {
                                              pinnedIsUnknownArgNames.add(simple.argName());
                                              if (simple.referencedArgName() != null) {
                                                  pinnedIsUnknownArgNames.add(simple.referencedArgName());
                                              }
                                          });
        // @formatter:on

    }

    /**
     * This method corresponds to the preparation work of {@link IsNotUnknownPropagator}: Any argName IS UNKNOWN in an OR implies that we don't need any
     * <i>co-match</i> to check for IS NOT UNKNOWN in case of a negation on this level and below.
     * 
     * @param cmbOr
     */
    private void registerDirectlyPinnedIsUnknownArgNamesFromOrCondition(CombinedExpression cmbOr) {
        Set<String> pinnedIsUnknownArgNames = getContext().getPinnedIsUnknownArgNames();
        // @formatter:off
        cmbOr.childExpressions().stream().filter(MatchExpression.class::isInstance)
                                          .map(MatchExpression.class::cast)
                                          .filter(e -> e.operator() == MatchOperator.IS_UNKNOWN)
                                          .map(MatchExpression::argName)
                                          .forEach(pinnedIsUnknownArgNames::add);
        // @formatter:on

    }

    /**
     * Copies the pinned is unknown argNames from the parent context to the current context
     */
    private void inheritParentPinnedIsUnknownArgNames() {
        Set<String> parentPinnedIsUnknownNames = getParentContext().getPinnedIsUnknownArgNames();
        Set<String> pinnedIsUnknownNames = getContext().getPinnedIsUnknownArgNames();
        pinnedIsUnknownNames.addAll(parentPinnedIsUnknownNames);
    }

    /**
     * Tries to resolve a boolean reference match into a combination of value matches:
     * <p>
     * <ul>
     * <li><code>argName1 = &#64;argName2</code> can be written as <code>(argName1 = 1 AND argName2 = 1) OR (argName2 = 0 AND argName2 = 0)</code></li>
     * <li><code>STRICT NOT argName1 = &#64;argName2</code> can be written as
     * <code>(argName1 = 1 AND argName2 = 0) OR (argName1 = 0 AND argName2 = 1)</code></li>
     * </ul>
     * This is not only more efficient, it also allows us to <i>compare boolean fields across documents</i>.
     * 
     * @param expression if this is a boolean reference match, the expression will be treated in a special way
     * @return true if the expression has been consumed (no further processing required)
     */
    protected boolean tryResolveBooleanReferenceMatch(MatchExpression expression) {

        if (expression.referencedArgName() != null) {

            DataField fieldLeft = mappingConfig().lookupField(expression.argName(), processContext);
            DataField fieldRight = mappingConfig().lookupField(expression.referencedArgName(), processContext);

            if (fieldLeft.fieldType().equals(DefaultAdlSolrType.SOLR_BOOLEAN) && fieldRight.fieldType().equals(DefaultAdlSolrType.SOLR_BOOLEAN)) {
                CombinedMatchTreeElement cmteReplacement = createBooleanReferenceMatchReplacement(expression);

                getParentContext().getChildResultElements().add(cmteReplacement);
                return true;

            }
        }
        return false;
    }

    /**
     * This method takes a boolean reference match as input to create a complex match tree element that replaces the the reference match with a combination of
     * boolean value matches.
     * <p>
     * Here we also deal with the collection-intersection semantics, see
     * <a href="https://github.com/KarlEilebrecht/audlang-spec/blob/main/doc/AudienceDefinitionLanguageSpecification.md#7-audlang-and-collection-attributes"> §7
     * Audlang Spec</a>.
     * 
     * @param booleanReferenceMatch
     * @return replacement
     */
    protected CombinedMatchTreeElement createBooleanReferenceMatchReplacement(MatchExpression booleanReferenceMatch) {
        String nodeTypeLeft = nodeTypeOf(booleanReferenceMatch.argName());
        String nodeTypeRight = nodeTypeOf(booleanReferenceMatch.referencedArgName());

        CoreExpression leftMatchTrue = MatchExpression.of(booleanReferenceMatch.argName(), MatchOperator.EQUALS, Operand.of("1", false));
        CoreExpression leftMatchFalse = MatchExpression.of(booleanReferenceMatch.argName(), MatchOperator.EQUALS, Operand.of("0", false));
        CoreExpression rightMatchTrue = MatchExpression.of(booleanReferenceMatch.referencedArgName(), MatchOperator.EQUALS, Operand.of("1", false));
        CoreExpression rightMatchFalse = MatchExpression.of(booleanReferenceMatch.referencedArgName(), MatchOperator.EQUALS, Operand.of("0", false));

        SingleMatchWrapper smwLeftTrue = new SingleMatchWrapper(nodeTypeLeft, (MatchExpression) leftMatchTrue, MatchInstruction.DEFAULT, false);
        SingleMatchWrapper smwLeftFalse = new SingleMatchWrapper(nodeTypeLeft, (MatchExpression) leftMatchFalse, MatchInstruction.DEFAULT, false);
        SingleMatchWrapper smwLeftNotTrue = new SingleMatchWrapper(nodeTypeLeft, (MatchExpression) leftMatchTrue, MatchInstruction.NEGATE, false);

        SingleMatchWrapper smwRightTrue = new SingleMatchWrapper(nodeTypeRight, (MatchExpression) rightMatchTrue, MatchInstruction.DEFAULT, false);
        SingleMatchWrapper smwRightFalse = new SingleMatchWrapper(nodeTypeRight, (MatchExpression) rightMatchFalse, MatchInstruction.DEFAULT, false);
        SingleMatchWrapper smwRightNotTrue = new SingleMatchWrapper(nodeTypeRight, (MatchExpression) rightMatchTrue, MatchInstruction.NEGATE, false);

        CombinedMatchTreeElement cmte1 = null;
        CombinedMatchTreeElement cmte2 = null;

        ArgFieldAssignment assignmentLeft = processContext.getMappingConfig().lookupAssignment(booleanReferenceMatch.argName(), processContext);
        ArgFieldAssignment assignmentRight = processContext.getMappingConfig().lookupAssignment(booleanReferenceMatch.referencedArgName(), processContext);
        BooleanReferenceMatchCase matchCase = BooleanReferenceMatchCase.from(isMulti(assignmentLeft), isMulti(assignmentRight),
                getParentContext().isNegation());

        switch (matchCase) {
        case LEFT_SINGLE_RIGHT_SINGLE:
            cmte1 = new CombinedMatchTreeElement(CombinedExpressionType.AND, Arrays.asList(smwLeftTrue, smwRightTrue));
            cmte2 = new CombinedMatchTreeElement(CombinedExpressionType.AND, Arrays.asList(smwLeftFalse, smwRightFalse));
            break;
        case LEFT_SINGLE_RIGHT_SINGLE_NEGATION:
            cmte1 = new CombinedMatchTreeElement(CombinedExpressionType.AND, Arrays.asList(smwLeftTrue, smwRightFalse));
            cmte2 = new CombinedMatchTreeElement(CombinedExpressionType.AND, Arrays.asList(smwLeftFalse, smwRightTrue));
            break;
        case LEFT_SINGLE_RIGHT_MULTI:
            cmte1 = new CombinedMatchTreeElement(CombinedExpressionType.AND, Arrays.asList(smwLeftTrue, smwRightTrue));
            cmte2 = new CombinedMatchTreeElement(CombinedExpressionType.AND, Arrays.asList(smwLeftFalse, smwRightFalse, smwRightNotTrue));
            break;
        case LEFT_SINGLE_RIGHT_MULTI_NEGATION:
            cmte1 = new CombinedMatchTreeElement(CombinedExpressionType.AND, Arrays.asList(smwLeftTrue, smwRightFalse, smwRightNotTrue));
            cmte2 = new CombinedMatchTreeElement(CombinedExpressionType.AND, Arrays.asList(smwLeftFalse, smwRightTrue));
            break;
        case LEFT_MULTI_RIGHT_SINGLE:
            cmte1 = new CombinedMatchTreeElement(CombinedExpressionType.AND, Arrays.asList(smwLeftTrue, smwRightTrue));
            cmte2 = new CombinedMatchTreeElement(CombinedExpressionType.AND, Arrays.asList(smwLeftFalse, smwLeftNotTrue, smwRightFalse));
            break;
        case LEFT_MULTI_RIGHT_SINGLE_NEGATION:
            cmte1 = new CombinedMatchTreeElement(CombinedExpressionType.AND, Arrays.asList(smwLeftTrue, smwRightFalse));
            cmte2 = new CombinedMatchTreeElement(CombinedExpressionType.AND, Arrays.asList(smwLeftFalse, smwLeftNotTrue, smwRightTrue));
            break;
        case LEFT_MULTI_RIGHT_MULTI:
            cmte1 = new CombinedMatchTreeElement(CombinedExpressionType.AND, Arrays.asList(smwLeftTrue, smwRightTrue));
            cmte2 = new CombinedMatchTreeElement(CombinedExpressionType.AND, Arrays.asList(smwLeftFalse, smwLeftNotTrue, smwRightFalse, smwRightNotTrue));
            break;
        case LEFT_MULTI_RIGHT_MULTI_NEGATION:
            cmte1 = new CombinedMatchTreeElement(CombinedExpressionType.AND, Arrays.asList(smwLeftTrue, smwRightFalse, smwRightNotTrue));
            cmte2 = new CombinedMatchTreeElement(CombinedExpressionType.AND, Arrays.asList(smwLeftFalse, smwLeftNotTrue, smwRightTrue));
            break;
        default:
            throw new IllegalStateException("Unexpected match case (implementation error): " + matchCase);
        }

        return new CombinedMatchTreeElement(CombinedExpressionType.OR, Arrays.asList(cmte1, cmte2));

    }

    /**
     * 
     * @param assignment
     * @return true if either the assignment is marked as multi-doc or the the field is a collection
     */
    private boolean isMulti(ArgFieldAssignment assignment) {
        return assignment.isMultiDoc() || assignment.field().isCollection();
    }

    @Override
    public void handleMatchExpression(MatchExpression expression) {
        if (!tryResolveBooleanReferenceMatch(expression)) {
            inheritParentPinnedIsUnknownArgNames();
            getParentContext().getChildResultElements()
                    .add(new SingleMatchWrapper(nodeTypeOf(expression.argName()), expression, createMatchInstruction(expression), false));
        }
    }

    @Override
    public void enterCombinedExpression(CombinedExpression expression) {
        inheritParentPinnedIsUnknownArgNames();
        if (expression.combiType() == CombinedExpressionType.AND) {
            registerDirectlyPinnedIsUnknownArgNamesFromAndCondition(expression);
        }
        else {
            registerDirectlyPinnedIsUnknownArgNamesFromOrCondition(expression);
        }
    }

    @Override
    public void exitCombinedExpression(CombinedExpression expression) {
        CombinedMatchTreeElement cmte = new CombinedMatchTreeElement(expression.combiType(), getContext().getChildResultElements());
        getParentContext().getChildResultElements().add(cmte);
    }

    @Override
    public void enterNegationExpression(NegationExpression expression) {
        inheritParentPinnedIsUnknownArgNames();
        getContext().markNegation();
    }

    @Override
    public void exitNegationExpression(NegationExpression expression) {
        getParentContext().getChildResultElements().add(getContext().getChildResultElements().get(0));
    }

    @Override
    public void handleSpecialSetExpression(SpecialSetExpression expression) {
        // this code only exists to cover the case that the prepareRootExpression() method has been overwritten in a wrong way
        // otherwise this code is not reachable

        if (expression.setType() == SpecialSetType.ALL) {
            throw new ConversionException("Cannot convert <ALL> to Solr.", AudlangMessage.msg(CommonErrors.ERR_1001_ALWAYS_TRUE));
        }
        else if (expression.setType() == SpecialSetType.NONE) {
            throw new ConversionException("Cannot convert <NONE> to Solr.", AudlangMessage.msg(CommonErrors.ERR_1002_ALWAYS_FALSE));
        }
    }

    @Override
    protected SolrQueryDefinition finishResult() {

        MatchTreeElement rootElement = matchTreeHelper().consolidateMatchTree(getContext().getChildResultElements().get(0));

        List<SolrFilterQuery> filterQueries = new ArrayList<>();

        switch (rootElement) {
        case CombinedMatchTreeElement rootAnd when rootAnd.combiType() == CombinedExpressionType.AND:
            filterQueries.addAll(createMultipleMainFilterQueries(rootAnd));
            break;
        case CombinedMatchTreeElement rootOr when rootOr.combiType() == CombinedExpressionType.OR:
            filterQueries.add(createCombinedMainFilterQuery(rootOr));
            break;
        case MatchWrapper rootMatchWrapper:
            filterQueries.add(createSimpleMainFilterQuery(rootMatchWrapper));
            break;
        default:
            throw new IllegalArgumentException("Unexpected root match tree element: " + rootElement);
        }
        return new SolrQueryDefinition(SolrFormatConstants.QUERY_ALL_DOCUMENTS, filterQueries, getProcessContext().getUniqueKeyFieldName());
    }

    /**
     * Returns the child elements of the AND, preserving the semantics.
     * <p>
     * <b>Background:</b> Let <code><b>flagA=1 AND flagB=1</b></code> be two conditions on the same nested or dependent nodeType.<br>
     * In this case it can happen that there are multiple sub-documents involved where it was decided NOT to mark the field assignments as multi-doc (e.g. the
     * POS-data example). When we split now the AND above into two filter queries, magically the semantics change to anything like
     * <code><b><i>any(flagA=1)</i> AND <i>any(flagB=1)</i></b></code>. Virtually, we influenced the multi-document behavior and broke the expression. This
     * method prevents this by re-arranging the AND, so members like in the example above will be put into the same <i>sub-AND</i> to ensure they will be part
     * of the same Solr filter-query.
     * 
     * @param rootAnd
     * @return list with the members (potentially rearranged)
     */
    private List<MatchTreeElement> splitRootAnd(CombinedMatchTreeElement rootAnd) {

        if (isRootAndSplittingProhibited(rootAnd)) {
            return Collections.singletonList(rootAnd);
        }

        String mainNodeType = fqBuilder().getMainNodeType();
        String subNodeType = null;
        List<MatchTreeElement> subAndMembers = new ArrayList<>();
        List<MatchTreeElement> resultMembers = new ArrayList<>();
        for (MatchTreeElement member : rootAnd.childElements()) {

            String currentSubNodeType = member.commonNodeType();
            if (currentSubNodeType == null || !currentSubNodeType.equals(subNodeType) || !member.isGroupingEligible()) {
                addPendingResultMemberAndClear(subAndMembers, resultMembers);
                if (currentSubNodeType == null || currentSubNodeType.equals(mainNodeType)) {
                    subNodeType = null;
                    resultMembers.add(member);
                }
                else {
                    subNodeType = currentSubNodeType;
                    subAndMembers.add(member);
                }
            }
            else {
                subAndMembers.add(member);
            }
        }
        addPendingResultMemberAndClear(subAndMembers, resultMembers);

        return resultMembers;

    }

    /**
     * This is a test if the if the members of the root-and constraint each other to avoid that and AND accidentally turns into an AND ANY.
     * <p>
     * In general if there are multiple constraints they influence each other and thus cannot be split.<br>
     * If there is one constraint and at least one involving a multi-doc assignment then we cannot split.
     * 
     * @param rootAnd
     * @return true if we must not split this AND into two separate filter queries
     */
    private boolean isRootAndSplittingProhibited(CombinedMatchTreeElement rootAnd) {
        if (rootAnd.commonNodeType() != null && !rootAnd.commonNodeType().equals(fqBuilder().getMainNodeType())) {
            int constraints = 0;

            int potentialVictims = 0;

            for (MatchTreeElement member : rootAnd.childElements()) {
                if (member.isGroupingEligible()) {
                    // any (complex) condition that fixes a data set
                    constraints++;
                }
                else if (containsAnyMultiDocMatch(member)) {
                    potentialVictims++;
                }
            }
            return constraints > 1 || (constraints == 1 && potentialVictims > 0);
        }
        return false;
    }

    /**
     * @param member
     * @return true if this is a match against a multi-doc assignment or if it contains any (recursively)
     */
    private boolean containsAnyMultiDocMatch(MatchTreeElement member) {
        switch (member) {
        case MatchWrapper matchWrapper:
            ArgFieldAssignment assignment = mappingConfig().lookupAssignment(matchWrapper.argName(), processContext);
            return assignment.isMultiDoc();
        case CombinedMatchTreeElement cmte:
            return cmte.childElements().stream().anyMatch(this::containsAnyMultiDocMatch);
        default:
            return false;
        }

    }

    /**
     * @param subAndMembers
     * @param resultMembers
     */
    private void addPendingResultMemberAndClear(List<MatchTreeElement> subAndMembers, List<MatchTreeElement> resultMembers) {
        if (subAndMembers.size() == 1) {
            resultMembers.add(subAndMembers.get(0));
        }
        else if (!subAndMembers.isEmpty()) {
            resultMembers.add(new CombinedMatchTreeElement(CombinedExpressionType.AND, subAndMembers));
        }
        subAndMembers.clear();
    }

    /**
     * If the root element is an AND then we don't want to combine its elements. Instead, each member query will be passed to Solr as a separate filter query to
     * allow for optional caching.
     * 
     * @param rootAnd
     * @return list of filter queries
     */
    protected List<SolrFilterQuery> createMultipleMainFilterQueries(CombinedMatchTreeElement rootAnd) {

        List<SolrFilterQuery> res = new ArrayList<>();

        for (MatchTreeElement childElement : splitRootAnd(rootAnd)) {

            fqBuilder().reset();

            fqBuilder().appendFilterQuery(matchFilterFactory().createNodeTypeFilter(fqBuilder().getMainNodeType()));

            fqBuilder().appendAND();

            appendChildElement(CombinedExpressionType.AND, childElement);

            res.add(fqBuilder().getResult());

        }

        return res.stream().distinct().toList();
    }

    /**
     * The root of the expression was an OR. Thus we will create a single filter query for Solr combining all the conditions in one.
     * 
     * @param rootOr
     * @return main filter query
     */
    protected SolrFilterQuery createCombinedMainFilterQuery(CombinedMatchTreeElement rootOr) {

        fqBuilder().reset();

        fqBuilder().appendFilterQuery(matchFilterFactory().createNodeTypeFilter(fqBuilder().getMainNodeType()));

        fqBuilder().appendAND();

        appendChildElement(CombinedExpressionType.AND, rootOr);

        return fqBuilder().getResult();
    }

    private void appendChildElement(CombinedExpressionType parentCombiType, MatchTreeElement childElement) {
        List<MatchElement> groups = matchTreeHelper().createExecutionGroups(childElement);

        if (groups.size() == 1) {
            appendGroupedMatchElements(groups, parentCombiType);
        }
        else {
            CombinedExpressionType groupCombiType = startGroup(childElement, parentCombiType);
            appendGroupedMatchElements(groups, groupCombiType);
            endGroup(groupCombiType, parentCombiType);
        }
    }

    private CombinedExpressionType startGroup(MatchTreeElement mte, CombinedExpressionType parentCombiType) {

        CombinedExpressionType groupCombiType = parentCombiType;
        if (mte instanceof CombinedMatchTreeElement cmte) {
            groupCombiType = cmte.combiType();
        }

        if (groupCombiType != parentCombiType) {
            fqBuilder().openBrace();
        }
        return groupCombiType;
    }

    private void endGroup(CombinedExpressionType groupCombiType, CombinedExpressionType parentCombiType) {
        if (groupCombiType != parentCombiType) {
            fqBuilder().closeBrace();
        }
    }

    /**
     * The root expression was a simple condition, so the result is either a simple condition or a simple condition subtracted from <i>all</i>.
     * 
     * @param rootMatchWrapper
     * @return main filter query
     */
    protected SolrFilterQuery createSimpleMainFilterQuery(MatchWrapper rootMatchWrapper) {
        fqBuilder().reset();

        if (rootMatchWrapper.isNegation()) {
            appendNegativeMatch(rootMatchWrapper, true);
        }
        else {
            appendPositiveMatch(rootMatchWrapper);
        }

        return fqBuilder().getResult();
    }

    /**
     * Appends the correct combiner (AND/OR) if the index is &gt;0, means, we are after the first and before a successor element.
     * 
     * @param elementIdx
     * @param combiType
     */
    protected void appendCombinerIfRequired(int elementIdx, CombinedExpressionType combiType) {
        if (elementIdx > 0 && combiType == CombinedExpressionType.AND) {
            fqBuilder().appendAND();
        }
        else if (elementIdx > 0) {
            fqBuilder().appendOR();
        }
    }

    /**
     * Appends the instruction to start a join to the given node type, only if the given node type is not the main node type.
     * <p>
     * This operation always ends with <code><i>AND</i></code> on the join-builder of {@link SolrFilterQueryBuilder}, ready to append further conditions.
     * 
     * @param nodeType
     */
    protected void startJoinIfRequired(String nodeType) {
        if (!fqBuilder().getMainNodeType().equals(nodeType)) {
            NodeTypeMetaInfo nodeTypeMetaInfo = getProcessContext().getMappingConfig().lookupNodeTypeMetaInfoByNodeType(nodeType);
            if (nodeTypeMetaInfo.documentNature() == SolrDocumentNature.NESTED) {
                fqBuilder().startNestedJoin(nodeType);
            }
            else {
                fqBuilder().startDependentJoin(nodeType);
            }
            fqBuilder().appendFilterQuery(matchFilterFactory().createNodeTypeFilter(nodeType));
            fqBuilder().appendAND();
        }
    }

    /**
     * If a join was open, ends this join.
     * 
     */
    protected void endJoinIfOpen() {
        if (fqBuilder().isJoinOpen()) {
            fqBuilder().endJoin();
        }
    }

    /**
     * Each group (single element or or hybrid combination or chunk of elements for the same node type) will be appended to the expression
     * <p>
     * <i>Hint:</i> Here we are on the main level if the Solr-expression, never inside any join!
     * 
     * @param matchElements single elements and chunks of elements related to the same node type
     * @param groupCombiType how the group expressions should be combined
     */
    protected void appendGroupedMatchElements(List<MatchElement> matchElements, CombinedExpressionType groupCombiType) {

        for (int i = 0; i < matchElements.size(); i++) {
            appendCombinerIfRequired(i, groupCombiType);
            MatchElement matchElement = matchElements.get(i);

            switch (matchElement) {
            case MatchWrapper match when !match.isNegation():
                appendPositiveMatch(match);
                break;
            case MatchWrapper negation when negation.isNegation():
                appendNegativeMatch(negation, groupCombiType == CombinedExpressionType.OR);
                break;
            case CombinedMatchTreeElement cmb:
                appendChildElement(groupCombiType, cmb);
                break;
            case NodeTypeMatchTreeElementGroup group:
                appendGroup(group, groupCombiType);
                break;
            case MissingSubDocumentMatchElement msd:
                appendMissingSubDocumentMatch(msd, groupCombiType == CombinedExpressionType.OR);
                break;
            default:
                throw new IllegalArgumentException("Unexpected match element: " + matchElement);
            }
        }

    }

    /**
     * Appends the Solr-expression corresponding to the given wrapper <i>as-is</i> (no joining here)
     * 
     * @param matchWrapper
     */
    protected void appendPlainMatch(MatchWrapper matchWrapper) {
        fqBuilder().appendFilterQuery(matchFilterFactory().createMatchFilter(matchWrapper));
    }

    /**
     * If the given match wrapper is related to the main document, then this is equivalent to {@link #appendPlainMatch(MatchWrapper)}, otherwise the match
     * condition will be wrapped in a join to the related node type.
     * 
     * @param matchWrapper
     */
    protected void appendPositiveMatch(MatchWrapper matchWrapper) {
        startJoinIfRequired(matchWrapper.nodeType());
        appendPlainMatch(matchWrapper);
        endJoinIfOpen();
    }

    /**
     * Appends the given condition as a subtraction from <i>all</i>. If the related node type is not the main document, then the condition will be wrapped in a
     * join to that node type.
     * 
     * @param matchWrapper
     * @param braced if true creates a brace (AND) subtracting the NOT from <i>all</i>
     */
    protected void appendNegativeMatch(MatchWrapper matchWrapper, boolean braced) {
        if (braced) {
            fqBuilder().openBrace();
            fqBuilder().appendFilterQuery(matchFilterFactory().createNodeTypeFilter(fqBuilder().getMainNodeType()));
        }
        if (matchWrapper.matchInstruction() == MatchInstruction.NEGATE || !matchWrapper.isGroupingEligible()) {
            appendDefaultNegation(matchWrapper, braced);
        }
        else {
            appendStrictNegation(matchWrapper, braced);
        }
        if (braced) {
            fqBuilder().closeBrace();
        }

    }

    protected void appendStrictNegation(MatchWrapper matchWrapper, boolean braced) {

        if (braced) {
            fqBuilder().appendAND();
        }

        startJoinIfRequired(matchWrapper.nodeType());

        if (matchWrapper.matchInstruction().requiresIsNotUnknownVerification()) {
            appendIsNotUnknownFiltersPlain(matchWrapper);
            fqBuilder().appendAND();
        }

        fqBuilder().appendNOT();
        appendPlainMatch(matchWrapper);
        endJoinIfOpen();
    }

    protected void appendDefaultNegation(MatchWrapper matchWrapper, boolean braced) {
        boolean haveIsNotUnknownSection = appendIsNotUnknownFiltersIfRequired(matchWrapper, braced);
        if (braced || haveIsNotUnknownSection) {
            fqBuilder().appendAND();
        }
        fqBuilder().appendNOT();
        appendPositiveMatch(matchWrapper);
    }

    protected void appendMissingSubDocumentMatch(MissingSubDocumentMatchElement msd, boolean braced) {

        if (braced) {
            fqBuilder().openBrace();
            fqBuilder().appendFilterQuery(matchFilterFactory().createNodeTypeFilter(fqBuilder().getMainNodeType()));
        }
        if (braced) {
            fqBuilder().appendAND();
        }
        fqBuilder().appendNOT();

        String nodeType = msd.nodeType();
        NodeTypeMetaInfo nodeTypeMetaInfo = getProcessContext().getMappingConfig().lookupNodeTypeMetaInfoByNodeType(nodeType);
        if (nodeTypeMetaInfo.documentNature() == SolrDocumentNature.NESTED) {
            fqBuilder().startNestedJoin(nodeType);
        }
        else {
            fqBuilder().startDependentJoin(nodeType);
        }
        fqBuilder().appendFilterQuery(matchFilterFactory().createNodeTypeFilter(nodeType));
        fqBuilder().endJoin();
        if (braced) {
            fqBuilder().closeBrace();
        }

    }

    /**
     * Depending on the parent path of an expression a negative expression may require <i>co-matches</i> (NOT IS UNKNOWN resp. "has any value") for the
     * argName(s) of a comparison. This method appends these verification query parts.
     * 
     * @param matchWrapper
     * @param prependAnd true to prepend the optional filters with AND
     * @return true if there are filters
     * @see IsNotUnknownPropagator
     */
    protected boolean appendIsNotUnknownFiltersIfRequired(MatchWrapper matchWrapper, boolean prependAnd) {

        MatchInstruction matchInstruction = matchWrapper.matchInstruction();
        if (!matchInstruction.requiresIsNotUnknownVerification()) {
            return false;
        }

        if (prependAnd) {
            fqBuilder().appendAND();
        }

        String nodeType = matchWrapper.commonNodeType();

        startJoinIfRequired(nodeType);

        appendIsNotUnknownFiltersPlain(matchWrapper);

        endJoinIfOpen();
        return true;

    }

    /**
     * Appends the is-unknown filters without creating any joins (assumes we are on the correct document level)
     * 
     * @param matchWrapper
     * @see IsNotUnknownPropagator
     */
    private void appendIsNotUnknownFiltersPlain(MatchWrapper matchWrapper) {
        MatchInstruction matchInstruction = matchWrapper.matchInstruction();
        switch (matchInstruction) {
        case NEGATE_VERIFY_LEFT_ARG_HAS_ANY_VALUE:
            fqBuilder().appendFilterQuery(matchFilterFactory().createHasAnyValueFilter(matchWrapper.argName()));
            break;
        case NEGATE_VERIFY_RIGHT_ARG_HAS_ANY_VALUE:
            fqBuilder().appendFilterQuery(matchFilterFactory().createHasAnyValueFilter(matchWrapper.referencedArgName()));
            break;
        case NEGATE_VERIFY_BOTH_ARGS_HAVE_ANY_VALUE:
            fqBuilder().appendFilterQuery(matchFilterFactory().createHasAnyValueFilter(matchWrapper.argName()));
            fqBuilder().appendAND();
            fqBuilder().appendFilterQuery(matchFilterFactory().createHasAnyValueFilter(matchWrapper.referencedArgName()));
            break;
        // $CASES-OMITTED$
        default:
            throw new IllegalStateException("Unexpected match instruction: " + matchWrapper);
        }
    }

    /**
     * Appends an expression part for a {@link NodeTypeMatchTreeElementGroup} related to a particular node type.
     * <p>
     * If the node type is not the main document, the expression will be wrapped in a join to that node type.
     * <p>
     * <b>Hint:</b> This method is called when being on the main document level, never inside a join.
     * 
     * @param group
     * @param parentCombiType surrounding combination type to decide whether we need braces or not
     * @see NodeTypeMatchTreeElementGroup
     */
    protected void appendGroup(NodeTypeMatchTreeElementGroup group, CombinedExpressionType parentCombiType) {

        String nodeType = group.commonNodeType();

        boolean bracesRequired = fqBuilder().getMainNodeType().equals(nodeType) && parentCombiType != group.combiType();

        if (bracesRequired) {
            fqBuilder().openBrace();
        }

        startJoinIfRequired(nodeType);

        if (fqBuilder().isJoinOpen() && group.combiType() == CombinedExpressionType.OR) {
            fqBuilder().openBrace();
        }

        for (int i = 0; i < group.size(); i++) {
            MatchTreeElement mte = group.members().get(i);
            appendCombinerIfRequired(i, group.combiType());
            appendGroupElement(mte, group.combiType(), i == 0);
        }

        if (fqBuilder().isJoinOpen() && group.combiType() == CombinedExpressionType.OR) {
            fqBuilder().closeBrace();
        }
        endJoinIfOpen();

        if (bracesRequired) {
            fqBuilder().closeBrace();
        }

    }

    /**
     * Appends a single or combined group element (recursively)
     * <p>
     * <b>Hint:</b> This method is called when being inside a group (main document or already joined document), no further joins required (allowed).
     * 
     * @param matchTreeElement single or combined group element
     * @param parentCombiType surrounding combination of elements (AND vs. OR)
     * @param firstElementInGroup tells that this is the first group element (relevant in case of negation match wrappers)
     */
    protected void appendGroupElement(MatchTreeElement matchTreeElement, CombinedExpressionType parentCombiType, boolean firstElementInGroup) {
        switch (matchTreeElement) {
        case CombinedMatchTreeElement combinedMatchTreeElement:
            appendGroupElement(combinedMatchTreeElement);
            break;
        case MatchWrapper matchWrapper:
            appendGroupElement(matchWrapper, parentCombiType, firstElementInGroup);
            break;
        default:
            throw new IllegalArgumentException(
                    String.format("Unsupported match tree element, given: matchTreeElement=%s, groupCombiType=%s, firstElementInGroup=%s", matchTreeElement,
                            parentCombiType, firstElementInGroup));
        }
    }

    /**
     * Appends a combined group element (recursively)
     * <p>
     * <b>Hint:</b> This method is called when being inside a group (main document or already joined document), no further joins required (allowed).
     * 
     * @param combinedMatchTreeElement single or combined group element
     */
    protected void appendGroupElement(CombinedMatchTreeElement combinedMatchTreeElement) {

        fqBuilder().openBrace();

        for (int i = 0; i < combinedMatchTreeElement.childElements().size(); i++) {
            MatchTreeElement mte = combinedMatchTreeElement.childElements().get(i);
            appendCombinerIfRequired(i, combinedMatchTreeElement.combiType());
            appendGroupElement(mte, combinedMatchTreeElement.combiType(), i == 0);
        }

        fqBuilder().closeBrace();

    }

    /**
     * Appends a single group element
     * <p>
     * <b>Hint:</b> This method is called when being inside a group (main document or already joined document), no further joins required (allowed).
     * 
     * @param matchWrapper single group element
     * @param parentCombiType surrounding combination of elements (AND vs. OR)
     * @param firstElementInGroup tells that this is the first group element (relevant in case of negation match wrappers)
     */
    protected void appendGroupElement(MatchWrapper matchWrapper, CombinedExpressionType parentCombiType, boolean firstElementInGroup) {

        if (matchWrapper.isNegation()) {

            if (parentCombiType == CombinedExpressionType.OR) {
                fqBuilder().openBrace();
            }

            if (parentCombiType == CombinedExpressionType.OR || (parentCombiType == CombinedExpressionType.AND && firstElementInGroup)) {
                fqBuilder().appendFilterQuery(matchFilterFactory().createNodeTypeFilter(matchWrapper.nodeType()));
                fqBuilder().appendAND();
            }
            if (matchWrapper.matchInstruction().requiresIsNotUnknownVerification()) {
                appendIsNotUnknownFiltersPlain(matchWrapper);
                fqBuilder().appendAND();
            }

            fqBuilder().appendNOT();
            appendPlainMatch(matchWrapper);

            if (parentCombiType == CombinedExpressionType.OR) {
                fqBuilder().closeBrace();
            }

        }
        else {
            appendPlainMatch(matchWrapper);
        }

    }
}
