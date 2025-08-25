//@formatter:off
/*
 * AdlSolrTypeDecorator
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

import java.util.concurrent.atomic.AtomicInteger;

import de.calamanari.adl.cnv.tps.ArgValueFormatter;
import de.calamanari.adl.cnv.tps.NativeTypeCaster;

/**
 * Often an {@link AdlSolrType}s behavior will be common resp. applicable in many scenarios but we want to change the formatter. To avoid creating boiler-plate
 * code the {@link AdlSolrTypeDecorator} provides an easy solution by composition. A given type gets wrapped to adapt its behavior.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class AdlSolrTypeDecorator implements AdlSolrType {

    private static final long serialVersionUID = 2612345610457026315L;

    /**
     * Static counter to ensure we get unique names for the decorators
     */
    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();

    /**
     * the decorated type
     */
    private final AdlSolrType delegate;

    /**
     * decorated formatter or null (use the one of the delegate)
     */
    private final ArgValueFormatter formatter;

    /**
     * We append a number to the original name, this keeps the identifiers short and still informative regarding the base type
     */
    private final String decoratorName;

    @Override
    public AdlSolrType getBaseType() {
        return delegate.getBaseType();
    }

    /**
     * @param name if null, the wrapper gets a unique id assigned as its name
     * @param delegate NOT NULL
     * @param formatter
     */
    AdlSolrTypeDecorator(String name, AdlSolrType delegate, ArgValueFormatter formatter) {
        this.delegate = delegate;
        this.formatter = formatter;
        if (name != null) {
            this.decoratorName = name;
        }
        else if (delegate instanceof AdlSolrTypeDecorator dec) {
            this.decoratorName = dec.getBaseName() + "-" + INSTANCE_COUNTER.incrementAndGet();
        }
        else {
            this.decoratorName = delegate.name() + "-" + INSTANCE_COUNTER.incrementAndGet();
        }
    }

    /**
     * @return the name of the inner type (center of the "type onion")
     */
    private String getBaseName() {
        if (delegate instanceof AdlSolrTypeDecorator dec) {
            return dec.getBaseName();
        }
        return delegate.name();
    }

    @Override
    public String name() {
        return this.decoratorName;
    }

    @Override
    public ArgValueFormatter getFormatter() {
        return this.formatter == null ? delegate.getFormatter() : this.formatter;
    }

    @Override
    public NativeTypeCaster getNativeTypeCaster() {
        return delegate.getNativeTypeCaster();
    }

    @Override
    public boolean supportsContains() {
        return delegate.supportsContains();
    }

    @Override
    public boolean supportsLessThanGreaterThan() {
        return delegate.supportsLessThanGreaterThan();
    }

    @Override
    public String toString() {
        return this.decoratorName;
    }

}
