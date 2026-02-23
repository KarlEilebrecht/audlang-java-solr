//@formatter:off
/*
 * DefaultSolrFormatter
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import de.calamanari.adl.cnv.tps.AdlDateUtils;
import de.calamanari.adl.cnv.tps.AdlFormattingException;
import de.calamanari.adl.cnv.tps.ArgValueFormatter;
import de.calamanari.adl.cnv.tps.DefaultArgValueFormatter;
import de.calamanari.adl.irl.MatchOperator;
import de.calamanari.adl.solr.config.FilterField;
import de.calamanari.adl.util.TriFunction;

import static de.calamanari.adl.solr.SolrFormatConstants.FALSE;
import static de.calamanari.adl.solr.SolrFormatConstants.TRUE;

/**
 * The formatters in this enumeration define the default behavior when formatting values for Apache Solr.
 * <p>
 * All formatters return values <i>properly escaped</i>, so they can be included directly in a query string (affects plain strings and timestamps).
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 * @see SolrFormatUtils#escape(String)
 */
public enum DefaultSolrFormatter implements ArgValueFormatter {

    // @formatter:off
    SOLR_STRING(DefaultSolrFormatter::formatSolrString), 
    SOLR_INTEGER(DefaultSolrFormatter::formatSolrInteger), 
    SOLR_LONG(DefaultArgValueFormatter.INTEGER::format), 
    SOLR_FLOAT(DefaultSolrFormatter::formatFloat), 
    SOLR_DOUBLE(DefaultSolrFormatter::formatDouble), 
    SOLR_DATE(DefaultSolrFormatter::formatSolrTimestamp), 
    SOLR_BOOLEAN(DefaultSolrFormatter::formatSolrBoolean);
    // @formatter:on

    private final TriFunction<String, String, MatchOperator, String> formatterFunction;

    private DefaultSolrFormatter(TriFunction<String, String, MatchOperator, String> formatterFunction) {
        this.formatterFunction = formatterFunction;
    }

    @Override
    public String format(String argName, String argValue, MatchOperator operator) {
        return formatterFunction.apply(argName, argValue, operator);
    }

    /**
     * Formats the given string as Solr String value by escaping any special characters.
     * 
     * @param argName
     * @param argValue
     * @param operator
     * @return FALSE or TRUE
     * @throws AdlFormattingException if the input does not match
     */
    public static String formatSolrString(String argName, String argValue, MatchOperator operator) {
        if (argValue == null || argValue.isEmpty()) {
            throw new AdlFormattingException(
                    String.format("Unable to format argName=%s, argValue=%s, operator=%s (null-values and empty strings are not supported by Solr).", argName,
                            argValue, operator));
        }
        return SolrFormatUtils.escape(String.valueOf(argValue));
    }

    /**
     * Formats the given string as a boolean value (TRUE/FALSE) if it is '0', '1', 'TRUE' or 'FALSE'
     * 
     * @param argName
     * @param argValue
     * @param operator
     * @return FALSE or TRUE
     * @throws AdlFormattingException if the input does not match
     */
    public static String formatSolrBoolean(String argName, String argValue, MatchOperator operator) {

        // the tolerant behavior (1/0) is a compromise to avoid confusion

        if (TRUE.equals(argValue) || "1".equals(argValue)) {
            return TRUE;
        }
        else if (FALSE.equals(argValue) || "0".equals(argValue)) {
            return FALSE;
        }
        throw new AdlFormattingException(
                String.format("Unable to format argName=%s, argValue=%s, operator=%s (TRUE, 1, FALSE, 0 input expected).", argName, argValue, operator));
    }

    /**
     * Formats the given string as integer if it is in 32-bit signed integer range
     * 
     * @param argName
     * @param argValue
     * @param operator
     * @return int value as string
     * @throws AdlFormattingException if the input does not match
     */
    public static String formatSolrInteger(String argName, String argValue, MatchOperator operator) {
        String formatted = DefaultArgValueFormatter.INTEGER.format(argName, argValue, operator);
        long temp = Long.parseLong(formatted);

        if (temp < Integer.MIN_VALUE || temp > Integer.MAX_VALUE) {
            throw new AdlFormattingException(String.format("Unable to format argName=%s, argValue=%s, operator=%s (value out of integer range [%d, %d]).",
                    argName, argValue, operator, Integer.MIN_VALUE, Integer.MAX_VALUE));

        }
        return formatted;
    }

    /**
     * Formats the given string as float if it is in signed float range
     * 
     * @param argName
     * @param argValue
     * @param operator
     * @return float value as string
     * @throws AdlFormattingException if the input does not match
     */
    public static String formatFloat(String argName, String argValue, MatchOperator operator) {
        String formatted = DefaultArgValueFormatter.DECIMAL.format(argName, argValue, operator);

        float temp = Float.parseFloat(formatted);

        if (temp < (Float.MAX_VALUE * -1.0d) || temp > Float.MAX_VALUE) {
            throw new AdlFormattingException(String.format("Unable to format argName=%s, argValue=%s, operator=%s (value out of float range [%f, %f]).",
                    argName, argValue, operator, Float.MIN_VALUE, Float.MAX_VALUE));

        }
        return formatted;
    }

    /**
     * Formats the given string as double if it is in signed double range
     * 
     * @param argName
     * @param argValue
     * @param operator
     * @return double value as string
     * @throws AdlFormattingException if the input does not match
     */
    public static String formatDouble(String argName, String argValue, MatchOperator operator) {
        return DefaultArgValueFormatter.DECIMAL.format(argName, argValue, operator);
    }

    /**
     * Formats the given string if it either matches the format <code>yyyy-MM-dd</code> or <code>yyyy-MM-dd HH:mm:ss</code> and represents a valid Solr
     * UTC-time.
     * <p>
     * <b>Notes</b>The compromise to support full time stamps was introduced to avoid confusion when specifying {@link FilterField}s, it should be irrelevant
     * for regular fields (by default Audlang deals with date only).
     * 
     * @param argName
     * @param argValue
     * @param operator
     * @return stamp of the form <code>yyyy\-MM\-dd<b>T</b>HH\:mm\:ss<b>Z</b></code>
     */
    public static String formatSolrTimestamp(String argName, String argValue, MatchOperator operator) {
        // this lenient behavior was required to fully support Timestamp as FilterColumn condition
        // without this tolerance we would always lose the time portion
        StringBuilder sb = new StringBuilder(24);
        if (isFullTimestamp(argValue)) {
            sb.append(argValue, 0, 4).append('\\').append(argValue, 4, 7).append('\\').append(argValue, 7, 10).append('T').append(argValue, 11, 13).append('\\')
                    .append(argValue, 13, 16).append('\\').append(argValue, 16, 19).append('Z');
        }
        else {
            String temp = DefaultArgValueFormatter.DATE.format(argName, argValue, operator);
            sb.append(temp, 0, 4).append('\\').append(temp, 4, 7).append('\\').append(temp, 7, 10).append("T00\\:00\\:00Z");
        }
        return sb.toString();
    }

    /**
     * @param argValue
     * @return true if the given string is a valid timestamp following the pattern <code>yyyy-MM-dd HH:mm:ss</code>
     */
    private static boolean isFullTimestamp(String argValue) {
        if (argValue != null && argValue.length() > 10) {
            SimpleDateFormat sdf = new SimpleDateFormat(AdlDateUtils.AUDLANG_DATE_FORMAT + " HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                return argValue.equals(sdf.format(sdf.parse(argValue)));
            }
            catch (ParseException _) {
                return false;
            }
        }
        return false;
    }

}