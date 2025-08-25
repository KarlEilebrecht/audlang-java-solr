//@formatter:off
/*
 * ResettableScpContext
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.calamanari.adl.Flag;
import de.calamanari.adl.FormatStyle;
import de.calamanari.adl.solr.config.MainDocumentConfig;
import de.calamanari.adl.solr.config.SolrMappingConfig;

/**
 * Implementation of a resettable {@link SolrConversionProcessContext}.
 * <p>
 * This class covers the state of an {@link SolrExpressionConverter} instance throughout its lifetime. <br>
 * It allows to reset this state to an initial configuration before each conversion run, so multiple runs won't interfere.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class ResettableScpContext implements SolrConversionProcessContext {

    /**
     * Global variables to be set initially for each conversion run (construction time of converter)
     */
    private final Map<String, Serializable> globalVariablesTemplate;

    /**
     * Global variables of the current conversion run
     */
    private final Map<String, Serializable> globalVariables = new HashMap<>();

    /**
     * Flags to be set initially for each conversion run (construction time of converter)
     */
    private final Set<Flag> globalFlagsTemplate;

    /**
     * Flags for the current run
     */
    private final Set<Flag> globalFlags = new HashSet<>();

    /**
     * Solr-mapping information
     */
    private final SolrMappingConfig mappingConfig;

    /**
     * Match tree helper, set by the converter during preparation
     */
    private MatchTreeHelper matchTreeHelper = null;

    /**
     * Match filter factory, set by the converter during preparation
     */
    private MatchFilterFactory matchFilterFactory = null;

    /**
     * filter query builder instance used to build a single filter query
     */
    private SolrFilterQueryBuilder filterQueryBuilder = null;

    /**
     * Decides whether the converter adds line breaks and indentation or not, by default we use pretty-printing for better readability
     */
    private FormatStyle style = FormatStyle.PRETTY_PRINT;

    /**
     * @param mappingConfig to be set initially for each conversion run (usually the {@link MainDocumentConfig})
     * @param globalVariablesTemplate initially for each conversion run
     * @param flagsTemplate to be set initially for each conversion run
     */
    public ResettableScpContext(SolrMappingConfig mappingConfig, Map<String, Serializable> globalVariablesTemplate, Set<Flag> flagsTemplate) {
        if (mappingConfig == null) {
            throw new IllegalArgumentException(
                    String.format("The argument mappingConfig must not be null, given: mappingConfig=%s, globalVariablesTemplate=%s, flagsTemplate=%s",
                            mappingConfig, globalVariablesTemplate, flagsTemplate));
        }
        this.globalVariablesTemplate = globalVariablesTemplate == null ? new HashMap<>() : globalVariablesTemplate;
        this.globalFlagsTemplate = flagsTemplate = flagsTemplate == null ? new HashSet<>() : flagsTemplate;
        this.mappingConfig = mappingConfig;
        this.reset();
    }

    /**
     * Initializes this context, so its state has the initially configured flags and variables again
     * <p>
     * Because we assume them to be rather stable once configured, the following properties won't be reset by this method:
     * <ul>
     * <li>{@link #setStyle(FormatStyle)}</li>
     * </ul>
     */
    public void reset() {
        this.globalVariables.clear();
        this.globalVariables.putAll(globalVariablesTemplate);
        this.globalFlags.clear();
        this.globalFlags.addAll(globalFlagsTemplate);
        this.matchTreeHelper = null;
        this.matchFilterFactory = null;
        this.filterQueryBuilder = new SolrFilterQueryBuilder(this);
    }

    @Override
    public Map<String, Serializable> getGlobalVariables() {
        return this.globalVariables;
    }

    @Override
    public Set<Flag> getGlobalFlags() {
        return this.globalFlags;
    }

    /**
     * @return the variables specified at construction time (this config acts as a template during {@link #reset()})
     */
    public Map<String, Serializable> getGlobalVariablesTemplate() {
        return globalVariablesTemplate;
    }

    /**
     * @return the flags specified at construction time (this config acts as a template during {@link #reset()})
     */
    public Set<Flag> getGlobalFlagsTemplate() {
        return globalFlagsTemplate;
    }

    @Override
    public SolrMappingConfig getMappingConfig() {
        return this.mappingConfig;
    }

    /**
     * @return the converter's match tree helper
     */
    @Override
    public MatchTreeHelper getMatchTreeHelper() {
        return this.matchTreeHelper;
    }

    /**
     * @param expressionHelper the converter's expression helper to work with expressions and sub-expressions
     */
    public void setExpressionHelper(MatchTreeHelper expressionHelper) {
        this.matchTreeHelper = expressionHelper;
    }

    /**
     * @return the converter's match filter factory for translating basic expressions into Solr-expressions
     */
    @Override
    public MatchFilterFactory getMatchFilterFactory() {
        return this.matchFilterFactory;
    }

    /**
     * @param matchFilterFactory the converter's match filter factory for translating basic expressions into Solr-expressions
     */
    public void setMatchFilterFactory(MatchFilterFactory matchFilterFactory) {
        this.matchFilterFactory = matchFilterFactory;
    }

    @Override
    public FormatStyle getStyle() {
        return style;
    }

    /**
     * @param style formatting style (inline or multi-line), default is {@link FormatStyle#PRETTY_PRINT}
     */
    public void setStyle(FormatStyle style) {
        this.style = style;
    }

    @Override
    public SolrFilterQueryBuilder getFilterQueryBuilder() {
        return this.filterQueryBuilder;
    }

}
