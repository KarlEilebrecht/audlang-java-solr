//@formatter:off
/*
 * DefaultAdlSolrType
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

import java.sql.Types;

import de.calamanari.adl.cnv.tps.AdlType;
import de.calamanari.adl.cnv.tps.ArgValueFormatter;
import de.calamanari.adl.cnv.tps.DefaultAdlType;

/**
 * The {@link DefaultAdlSolrType} maps a selection of the Solr-types defined in {@link Types} and decorate them with the ability to act as {@link AdlType}.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public enum DefaultAdlSolrType implements AdlSolrType {

    SOLR_STRING(DefaultSolrFormatter.SOLR_STRING),
    SOLR_INTEGER(DefaultSolrFormatter.SOLR_INTEGER),
    SOLR_LONG(DefaultSolrFormatter.SOLR_LONG),
    SOLR_FLOAT(DefaultSolrFormatter.SOLR_FLOAT),
    SOLR_DOUBLE(DefaultSolrFormatter.SOLR_DOUBLE),
    SOLR_DATE(DefaultSolrFormatter.SOLR_DATE),
    SOLR_BOOLEAN(DefaultSolrFormatter.SOLR_BOOLEAN);

    private final ArgValueFormatter formatter;

    private DefaultAdlSolrType(ArgValueFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public ArgValueFormatter getFormatter() {
        return formatter;
    }

    @Override
    public boolean supportsContains() {
        return this == SOLR_STRING;
    }

    @Override
    public boolean supportsLessThanGreaterThan() {
        return this != SOLR_BOOLEAN;
    }

    @Override
    public boolean isCompatibleWith(AdlType type) {
        if (!(type instanceof AdlSolrType) && type.getBaseType() instanceof DefaultAdlType inputType) {
            switch (inputType) {
            case DefaultAdlType.STRING:
                return true;
            case DefaultAdlType.INTEGER:
                return true;
            case DefaultAdlType.DECIMAL:
                return (this != SOLR_BOOLEAN);
            case DefaultAdlType.BOOL:
                return (this == DefaultAdlSolrType.SOLR_INTEGER || this == DefaultAdlSolrType.SOLR_LONG || this == DefaultAdlSolrType.SOLR_BOOLEAN
                        || this == DefaultAdlSolrType.SOLR_STRING);
            case DefaultAdlType.DATE:
                return (this == DefaultAdlSolrType.SOLR_DATE || this == DefaultAdlSolrType.SOLR_INTEGER || this == DefaultAdlSolrType.SOLR_LONG
                        || this == DefaultAdlSolrType.SOLR_DOUBLE || this == DefaultAdlSolrType.SOLR_STRING);
            }
        }
        return false;

    }
}
