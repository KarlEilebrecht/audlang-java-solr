//@formatter:off
/*
 * SolrConversionProcessContext
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

import de.calamanari.adl.FormatStyle;
import de.calamanari.adl.ProcessContext;
import de.calamanari.adl.solr.SolrFormatConstants;
import de.calamanari.adl.solr.config.MainDocumentConfig;
import de.calamanari.adl.solr.config.SolrMappingConfig;

/**
 * Process context with variables, flags and conversion-related utilities.
 * <p>
 * The {@link SolrConversionProcessContext} is unique for a particular conversion run.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public interface SolrConversionProcessContext extends ProcessContext {

    /**
     * This method checks if {@link SolrConversionOverrides#OVERRIDE_UNIQUE_KEY_FIELD_NAME} is present in the {@link #getGlobalVariables()} or returns the
     * default if not.
     * 
     * @return name of the Solr-field that contains the document's unique key, <b><code>{@value SolrFormatConstants#DEFAULT_UNIQUE_KEY_FIELD_NAME}</code></b> by
     *         default
     */
    default String getUniqueKeyFieldName() {
        return (String) this.getGlobalVariables().getOrDefault(SolrConversionOverrides.OVERRIDE_UNIQUE_KEY_FIELD_NAME.name(),
                SolrFormatConstants.DEFAULT_UNIQUE_KEY_FIELD_NAME);
    }

    /**
     * This method checks if {@link SolrConversionOverrides#OVERRIDE_DEPENDENT_MAIN_KEY_FIELD_NAME} is present in the {@link #getGlobalVariables()} or returns
     * the default if not.
     * 
     * @return name of the Solr-field in each dependent document that contains the main document's unique key,
     *         <b><code>{@value SolrFormatConstants#DEFAULT_DEPENDENT_MAIN_KEY_FIELD_NAME}</code></b> by default
     */
    default String getDependentMainKeyFieldName() {
        return (String) this.getGlobalVariables().getOrDefault(SolrConversionOverrides.OVERRIDE_DEPENDENT_MAIN_KEY_FIELD_NAME.name(),
                SolrFormatConstants.DEFAULT_DEPENDENT_MAIN_KEY_FIELD_NAME);
    }

    /**
     * This method checks if {@link SolrConversionOverrides#OVERRIDE_NODE_TYPE_FIELD_NAME} is present in the {@link #getGlobalVariables()} or returns the
     * default if not.
     * 
     * @return name of the Solr-field in each document that contains the node-type,
     *         <b><code>{@value SolrFormatConstants#DEFAULT_NODE_TYPE_FIELD_NAME}</code></b> by default
     */
    default String getNodeTypeFieldName() {
        return (String) this.getGlobalVariables().getOrDefault(SolrConversionOverrides.OVERRIDE_NODE_TYPE_FIELD_NAME.name(),
                SolrFormatConstants.DEFAULT_NODE_TYPE_FIELD_NAME);
    }

    /**
     * @return Solr-mapping information (usually the {@link MainDocumentConfig})
     */
    SolrMappingConfig getMappingConfig();

    /**
     * @return the converter's match tree helper
     */
    MatchTreeHelper getMatchTreeHelper();

    /**
     * @return the converter's match filter factory for translating basic expressions into Solr-expressions
     */
    MatchFilterFactory getMatchFilterFactory();

    /**
     * filter query builder instance used to build a single filter query
     */
    SolrFilterQueryBuilder getFilterQueryBuilder();

    /**
     * @return configured formatting style (inline or multi-line)
     */
    FormatStyle getStyle();

}
