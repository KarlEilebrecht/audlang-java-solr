//@formatter:off
/*
 * DummyDocumentConfig
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

import static de.calamanari.adl.solr.config.ConfigUtils.assertContextNotNull;
import static de.calamanari.adl.solr.config.ConfigUtils.assertValidArgName;

import java.util.Collections;
import java.util.List;

import de.calamanari.adl.FormatUtils;
import de.calamanari.adl.ProcessContext;
import de.calamanari.adl.cnv.tps.ArgMetaInfo;
import de.calamanari.adl.cnv.tps.DefaultAdlType;
import de.calamanari.adl.solr.DefaultAdlSolrType;

/**
 * This implementation is for testing and debugging, it returns a dummy main document for any valid argName
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
@SuppressWarnings("java:S6548")
public class DummyDocumentConfig implements SolrMappingConfig, NodeTypeMetaInfo {

    private static final long serialVersionUID = 6398015314341134852L;

    public static final String DUMMY_NODE_TYPE = "dummy_doc";

    private static final DummyDocumentConfig INSTANCE = new DummyDocumentConfig();

    /**
     * @return the only instance (JVM-singleton)
     */
    public static DummyDocumentConfig getInstance() {
        return INSTANCE;
    }

    private DummyDocumentConfig() {
        // singleton
    }

    @Override
    public boolean contains(String argName) {
        assertValidArgName(argName);
        return true;
    }

    @Override
    public String nodeType() {
        return DUMMY_NODE_TYPE;
    }

    @Override
    public SolrDocumentNature documentNature() {
        return SolrDocumentNature.MAIN;
    }

    @Override
    public List<FilterField> documentFilters() {
        return Collections.emptyList();
    }

    @Override
    public ArgFieldAssignment lookupAssignment(String argName, ProcessContext ctx) {
        assertContextNotNull(ctx);
        assertValidArgName(argName);
        return new ArgFieldAssignment(new ArgMetaInfo(argName, DefaultAdlType.STRING, false, false),
                new DataField(DUMMY_NODE_TYPE, cleanLowerCase(argName), DefaultAdlSolrType.SOLR_STRING, false), false);
    }

    @Override
    public int numberOfNodeTypes() {
        return 1;
    }

    @Override
    public NodeTypeMetaInfo lookupNodeTypeMetaInfo(String argName, ProcessContext ctx) {
        assertContextNotNull(ctx);
        assertValidArgName(argName);
        return this;
    }

    @Override
    public List<NodeTypeMetaInfo> allNodeTypeMetaInfos() {
        return Collections.singletonList(this);
    }

    @Override
    public NodeTypeMetaInfo mainNodeTypeMetaInfo() {
        return this;
    }

    /**
     * @return singleton instance in JVM
     */
    Object readResolve() {
        return INSTANCE;
    }

    /**
     * Cosmetics, converts the argName into a dummy field name, upper case, no whitespace for better distinguishing input from output.
     * <p>
     * In any case the returned field name will be compliant to Solr dynamic field conventions.
     * 
     * @param argName NOT NULL, not empty
     * @return pseudo dynamic Solr field name of type string
     */
    static String cleanLowerCase(String argName) {
        if (argName == null || argName.isEmpty()) {
            throw new IllegalArgumentException("The argument argName must not be null or empty, given: " + argName);
        }
        boolean lastWasUpperCaseOrDigit = false;
        StringBuilder sb = new StringBuilder(argName.length() + 2);
        for (int i = 0; i < argName.length(); i++) {
            char ch = argName.charAt(i);
            lastWasUpperCaseOrDigit = cleanLowerCaseAppend(sb, ch, lastWasUpperCaseOrDigit);
        }
        sb.append("_s");
        return sb.toString();
    }

    /**
     * Appends a single digit from the source
     * 
     * @param sb
     * @param ch
     * @param lastWasUpperCaseOrDigit
     * @return lastWasUpperCaseOrDigit (updated)
     */
    private static boolean cleanLowerCaseAppend(StringBuilder sb, char ch, boolean lastWasUpperCaseOrDigit) {
        if (sb.isEmpty() && Character.isDigit(ch)) {
            sb.append("v_");
            sb.append(ch);
            lastWasUpperCaseOrDigit = true;
        }
        else if (!ConfigUtils.isSolrLetterOrDigit(ch)) {
            if (sb.isEmpty()) {
                sb.append("v");
            }
            sb.append("_");
            lastWasUpperCaseOrDigit = Character.isDigit(ch);
        }
        else if (Character.isUpperCase(ch)) {
            if (!sb.isEmpty() && !lastWasUpperCaseOrDigit && !FormatUtils.endsWith(sb, "_")) {
                sb.append("_");
            }
            sb.append(Character.toLowerCase(ch));
            lastWasUpperCaseOrDigit = true;
        }
        else {
            sb.append(ch);
            lastWasUpperCaseOrDigit = Character.isDigit(ch);
        }
        return lastWasUpperCaseOrDigit;
    }

}
