//@formatter:off
/*
 * SolrConversionDirective
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

import de.calamanari.adl.Flag;
import de.calamanari.adl.cnv.tps.DefaultAdlType;
import de.calamanari.adl.solr.DefaultAdlSolrType;

/**
 * {@link SolrConversionDirective}s are {@link Flag}s that influence details of the Solr query generation process.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public enum SolrConversionDirective implements Flag {

    /**
     * By default the converter will align {@link DefaultAdlType#DATE} to higher resolution types {@link DefaultAdlSolrType#SOLR_LONG},
     * {@link DefaultAdlSolrType#SOLR_INTEGER} by turning the query into a range query.
     * <p>
     * This directive turns off this feature. <br>
     * Be aware that with this directive dates will be matched against timestamps assuming a 00:00:00 as the time portion.<br>
     * For example <code>2024-12-13</code> will no longer match (equal) the db-timestamp <code>2024-12-13 12:30:12</code>.
     */
    DISABLE_DATE_TIME_ALIGNMENT,

    /**
     * This disallows any CONTAINS to SOLR-translation, no matter how the mapping is configured.
     */
    DISABLE_CONTAINS,

    /**
     * This disallows any LESS THAN or GREATER THAN to be translated, no matter how the mapping is configured
     */
    DISABLE_LESS_THAN_GREATER_THAN,

    /**
     * This disallows any reference matching, no matter how the mapping is configured.
     */
    DISABLE_REFERENCE_MATCHING;
}
