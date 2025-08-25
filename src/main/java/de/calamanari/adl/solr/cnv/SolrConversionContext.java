//@formatter:off
/*
 * SolrConversionContext
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.calamanari.adl.cnv.ConversionContext;
import de.calamanari.adl.irl.CoreExpression;

/**
 * The {@link SolrConversionContext} is a container holding the information for a particular <i>level</i> of the expression to be converted while visiting the
 * {@link CoreExpression}'s DAG.
 * <p>
 * <b>Important:</b> Each {@link SolrConversionContext} instance has a local (level) state and a <b>global state</b> (process state of the converter). The
 * {@link #clear()}-command does not affect the instance returned by {@link #getProcessContext()}.<br>
 * The global state gets assigned by the converter (precisely the {@link SolrConversionProcessContext} <i>injects itself</i> upon creation of the level context
 * object. This ensures that the global process is common for all level context objects created during the same conversion run.
 * <p>
 * The concept allows parts of the conversion logic not only to access configuration data and the isolated data of a level and the parent level but also to
 * <i>exchange</i> information.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class SolrConversionContext implements ConversionContext {

    /**
     * Global process context, not covered by {@link #clear()}
     */
    private SolrConversionProcessContext processContext;

    /**
     * If true, we are processing a negated match
     */
    private boolean negation = false;

    /**
     * child elements, members of AND/OR filled bottom-up during conversion
     */
    private List<MatchTreeElement> childResultElements = new ArrayList<>();

    /**
     * argNames which don't require additional IS NOT UNKNOWN checks because the their IS-UNKNOWN-state is pinned earlier (upward) in the expression
     */
    private Set<String> pinnedIsUnknownArgNames = new HashSet<>();

    /**
     * @return true if we are currently processing a negated match
     */
    public boolean isNegation() {
        return negation;
    }

    /**
     * Marks this level to be a negated match
     */
    public void markNegation() {
        this.negation = true;
    }

    /**
     * Returns the list of arguments collected on the path from the root expression to the current expression that logically don't require any IS NOT UNKNOWN
     * checks because higher level conditions "pin" their state (either IS UNKNOWN or IS NOT UNKNOWN).
     * 
     * @return argNames which don't require additional IS NOT UNKNOWN checks because the their IS-UNKNOWN-state is pinned earlier (upward) in the expression
     */
    public Set<String> getPinnedIsUnknownArgNames() {
        return pinnedIsUnknownArgNames;
    }

    /**
     * @return reference to the list of child elements (filled bottom-up)
     */
    public List<MatchTreeElement> getChildResultElements() {
        return childResultElements;
    }

    /**
     * Resets this context to its defaults
     */
    @Override
    public void clear() {
        negation = false;
        childResultElements.clear();
        pinnedIsUnknownArgNames.clear();
    }

    /**
     * Method to be called initially by the converter to ensure all level contexts created during a conversion share the same global process context.
     * 
     * @param processContext the converters global state
     */
    public void setProcessContext(SolrConversionProcessContext processContext) {
        this.processContext = processContext;
    }

    /**
     * @return the global part of the context (related to the conversion process independent from the levels)
     */
    public SolrConversionProcessContext getProcessContext() {
        return processContext;
    }

}
