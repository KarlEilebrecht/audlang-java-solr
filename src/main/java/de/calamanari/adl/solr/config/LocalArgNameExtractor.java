//@formatter:off
/*
 * LocalArgNameExtractor
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

package de.calamanari.adl.solr.config;

import java.io.Serializable;
import java.util.function.UnaryOperator;

/**
 * Functional interface to ensure this part of the configuration remains serializable.
 * <p>
 * The operator receives an Audlang argName as input to extract/map the local argName (the Solr field name)<br>
 * E.g., <code>s -> s.substring(0, 5)</code>
 * <p>
 * A concrete {@link LocalArgNameExtractor} returns <b><code>null</code></b> to indicate that this extractor does not feel responsible for the given input.
 * <p>
 * <b>Important:</b>
 * <ul>
 * <li>Extractor functions should not throw any exception as this indicates a hard error that stops the current expression conversion.</li>
 * <li>The returned <code>localArgName</code> must be a a valid Solr-name (see {@link ConfigUtils#isValidSolrName(String)}), otherwise the result will be
 * skipped (as if it was <b>null</b>).</li>
 * </ul>
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public interface LocalArgNameExtractor extends UnaryOperator<String>, Serializable {

    /**
     * This extractor function returns the given argName as localArgName, after checking that it is a valid Solr-name (see
     * {@link ConfigUtils#isValidSolrName(String)}).
     * 
     * @return extractor that returns the given argName as-is if it is a valid Solr-name
     */
    static LocalArgNameExtractor validateOnly() {
        return t -> ConfigUtils.isValidSolrName(t) ? t : null;
    }
}
