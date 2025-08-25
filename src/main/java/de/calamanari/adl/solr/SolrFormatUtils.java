//@formatter:off
/*
 * SolrFormatUtils
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

import static de.calamanari.adl.solr.SolrFormatConstants.CLOSE_BRACE;
import static de.calamanari.adl.solr.SolrFormatConstants.COMMA;
import static de.calamanari.adl.solr.SolrFormatConstants.DAY_IN_MILLISECONDS;
import static de.calamanari.adl.solr.SolrFormatConstants.FUNC_AND;
import static de.calamanari.adl.solr.SolrFormatConstants.FUNC_DIV;
import static de.calamanari.adl.solr.SolrFormatConstants.FUNC_EXISTS;
import static de.calamanari.adl.solr.SolrFormatConstants.FUNC_FLOOR;
import static de.calamanari.adl.solr.SolrFormatConstants.FUNC_MS;
import static de.calamanari.adl.solr.SolrFormatConstants.FUNC_MUL;
import static de.calamanari.adl.solr.SolrFormatConstants.FUNC_SUB;
import static de.calamanari.adl.solr.SolrFormatConstants.OPEN_BRACE;

import de.calamanari.adl.cnv.tps.AdlType;
import de.calamanari.adl.cnv.tps.DefaultAdlType;

/**
 * Some additional utilities for formatting Solr-queries
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public class SolrFormatUtils {

    /**
     * Creates the builder instance on demand, initialized with the source string until the current position.
     * <p>
     * This <i>late initialization</i> produces the exact same builder state as if we had copied all previous characters sequentially to this builder.
     * 
     * @param sb to be created if null (otherwise no-op)
     * @param srcValue the string we are currently processing character by character
     * @param srcValueIdx the current position in the source value
     * @return sb or new builder instance
     */
    private static StringBuilder initBuilderOnDemand(StringBuilder sb, String srcValue, int srcValueIdx) {
        if (sb == null) {
            // we assume that escaping is not required for most of the characters, so small capacity increase only
            sb = new StringBuilder(srcValue.length() + 2);
            sb.append(srcValue, 0, srcValueIdx);
        }
        return sb;
    }

    /**
     * Escape all Solr special characters by prepending a backslash.
     * 
     * @param value
     * @return value or escaped string
     */
    public static String escape(String value) {
        StringBuilder sb = null;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isWhitespace(ch) || ch == '\\' || SolrFormatConstants.SOLR_SPECIAL_CHARACTERS.indexOf(ch) > -1) {
                sb = initBuilderOnDemand(sb, value, i);
                sb.append('\\');
            }
            if (sb != null) {
                sb.append(ch);
            }
        }
        return sb != null ? sb.toString() : value;
    }

    /**
     * Appends the given String to the builder after prepending each special character with a backslash.
     * <p>
     * This method is <i>not</i> idempotent.
     * 
     * @param sb to append the value
     * @param value to be appended after escaping, NOT NULL
     * @return the builder
     * @see SolrFormatConstants#SOLR_SPECIAL_CHARACTERS
     */
    public static StringBuilder appendEscaped(StringBuilder sb, String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isWhitespace(ch) || ch == '\\' || SolrFormatConstants.SOLR_SPECIAL_CHARACTERS.indexOf(ch) > -1) {
                sb.append('\\');
            }
            sb.append(ch);
        }
        return sb;
    }

    /**
     * Shorthand for appending the two strings separated by a colon.
     * <p>
     * Escaping must be performed beforehand.
     * 
     * @param sb
     * @param beforeColon
     * @param afterColon
     * @return sb
     */
    public static StringBuilder appendCondition(StringBuilder sb, String beforeColon, String afterColon) {
        return sb.append(beforeColon).append(':').append(afterColon);
    }

    /**
     * Appends an f-range header (the part in curly braces with lower and upper bound)
     * 
     * @param sb
     * @param lowerBound
     * @param upperBound
     * @return sb
     */
    public static StringBuilder appendFrangeHeader(StringBuilder sb, int lowerBound, int upperBound) {
        sb.append("{!frange l=").append(lowerBound).append(" u=").append(upperBound).append("}");
        return sb;
    }

    /**
     * Appends the existence checks for the two arguments of a frange.
     * <p>
     * The requirement for the existence check is related to the way a frange works. A missing side can lead to an unexpected match.
     * <p>
     * After this method there is a pending closing brace (an open AND combination of functions)
     * 
     * @param sb
     * @param fieldNameLeft
     * @param fieldNameRight
     * @return part to be included in the frange
     */
    public static StringBuilder appendFrangeStartWithExistenceChecks(StringBuilder sb, String fieldNameLeft, String fieldNameRight) {
        // @formatter:off
        return sb.append(FUNC_AND)
                        .append(OPEN_BRACE)
                            .append(FUNC_EXISTS).append(OPEN_BRACE).append(fieldNameLeft).append(CLOSE_BRACE)
                            .append(COMMA)
                            .append(FUNC_EXISTS).append(OPEN_BRACE).append(fieldNameRight).append(CLOSE_BRACE)
                            .append(COMMA);
        // @formatter:on
    }

    /**
     * This method applies function-surrounded field of type Solr-date to reflect the start of that same day inside a frange.
     * <p>
     * <b>Example:</b> Instead of the plain fieldName <b><code>updtime</code></b> this method will append
     * <b><code>sub(ms(updtime),sub(ms(updtime),mul(floor(div(ms(updtime),86400000)),86400000)))</code></b><br>
     * As a consequence the time portion that might be included like <code>2024-03-12T<b>17:52:00Z</b></code> will be erased:
     * <code>2024-03-12T<b>00:00:00Z</b></code> to allow a comparison against an other date field with <i>date precision</i>.
     * <p>
     * The current <b>inefficient implementation</b> is a workaround for a problem probably related to
     * <a href="https://issues.apache.org/jira/browse/SOLR-16361">SOLR-16361</a>.<br>
     * At the time of writing (Solr 9.8) I was still unable to get the expected results with Solr's modulus operator <b><code>mod</code></b>.<br>
     * Thus the created script <i>emulates</i> the modulus (performance must be lousy).
     * 
     * @param sb
     * @param fieldName solr field name to be wrapped
     */
    public static void appendDateFieldAtMidnightToFrange(StringBuilder sb, String fieldName) {
        // @formatter:off
        sb.append(FUNC_SUB).append(OPEN_BRACE)
            .append(FUNC_MS).append(OPEN_BRACE).append(fieldName).append(CLOSE_BRACE)
            .append(COMMA)
            .append(FUNC_SUB).append(OPEN_BRACE)
                .append(FUNC_MS).append(OPEN_BRACE).append(fieldName).append(CLOSE_BRACE)
                .append(COMMA)
                .append(FUNC_MUL).append(OPEN_BRACE)
                    .append(FUNC_FLOOR).append(OPEN_BRACE)
                        .append(FUNC_DIV).append(OPEN_BRACE)
                            .append(FUNC_MS).append(OPEN_BRACE).append(fieldName).append(CLOSE_BRACE)
                            .append(COMMA)
                            .append(DAY_IN_MILLISECONDS)
                        .append(CLOSE_BRACE)
                    .append(CLOSE_BRACE)
                    .append(COMMA)
                    .append(DAY_IN_MILLISECONDS)
                .append(CLOSE_BRACE)
            .append(CLOSE_BRACE)
        .append(CLOSE_BRACE);
        // @formatter:on
    }

    /**
     * Tells whether we should perform an alignment of the given data to a higher resolution stamp before creating the query, to for example turn an equals into
     * a range query.
     * 
     * @param type source type
     * @param fieldType destination type
     * @return true if the date needs alignment
     */
    public static boolean shouldAlignDate(AdlType type, AdlSolrType fieldType) {
        return (type.getBaseType() == DefaultAdlType.DATE && (fieldType.getBaseType() == DefaultAdlSolrType.SOLR_INTEGER
                || fieldType.getBaseType() == DefaultAdlSolrType.SOLR_LONG || fieldType.getBaseType() == DefaultAdlSolrType.SOLR_DATE));
    }

    private SolrFormatUtils() {
        // utilities
    }

}
