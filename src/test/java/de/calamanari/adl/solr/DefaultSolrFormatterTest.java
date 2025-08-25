//@formatter:off
/*
 * DefaultSolrFormatterTest
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

import static de.calamanari.adl.solr.DefaultSolrFormatter.SOLR_BOOLEAN;
import static de.calamanari.adl.solr.DefaultSolrFormatter.SOLR_DATE;
import static de.calamanari.adl.solr.DefaultSolrFormatter.SOLR_DOUBLE;
import static de.calamanari.adl.solr.DefaultSolrFormatter.SOLR_FLOAT;
import static de.calamanari.adl.solr.DefaultSolrFormatter.SOLR_INTEGER;
import static de.calamanari.adl.solr.DefaultSolrFormatter.SOLR_LONG;
import static de.calamanari.adl.solr.DefaultSolrFormatter.SOLR_STRING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.NumberFormat;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import de.calamanari.adl.cnv.tps.AdlFormattingException;
import de.calamanari.adl.cnv.tps.ArgValueFormatter;
import de.calamanari.adl.irl.MatchOperator;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class DefaultSolrFormatterTest {

    @Test
    void testFormatString() {

        assertThrows(AdlFormattingException.class, () -> SOLR_STRING.format("argName", null, MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_STRING.format("argName", "", MatchOperator.EQUALS));

        asserFormattedAsIs(SOLR_STRING, "a");
        asserFormattedAsIs(SOLR_STRING, "A");
        asserFormattedAsIs(SOLR_STRING, "Aa");
        asserFormattedAsIs(SOLR_STRING, "TheQuickBrownFoxJumpedOverTheLazyDog");

        asserFormatted("\\\\", SOLR_STRING, "\\");
        asserFormatted("\\+a", SOLR_STRING, "+a");
        asserFormatted("\\+abba", SOLR_STRING, "+abba");
        asserFormatted("ab\\+ba", SOLR_STRING, "ab+ba");
        asserFormatted("abba\\+", SOLR_STRING, "abba+");
        asserFormatted("\\+ab\\+b\\+a\\+", SOLR_STRING, "+ab+b+a+");
        asserFormatted("\\+ab\\+\\ b\\+a\\+", SOLR_STRING, "+ab+ b+a+");
        asserFormatted("\\+ab\\+\\ \\\tb\\+a\\+", SOLR_STRING, "+ab+ \tb+a+");

    }

    @Test
    void testFormatInteger() {

        assertThrows(AdlFormattingException.class, () -> SOLR_INTEGER.format("argName", null, MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_INTEGER.format("argName", "", MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_INTEGER.format("argName", "03", MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_INTEGER.format("argName", "" + Long.MAX_VALUE, MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_INTEGER.format("argName", "" + Long.MIN_VALUE, MatchOperator.EQUALS));

        asserFormattedAsIs(SOLR_INTEGER, "0");
        asserFormattedAsIs(SOLR_INTEGER, "123456");
        asserFormattedAsIs(SOLR_INTEGER, "-123456");
        asserFormattedAsIs(SOLR_INTEGER, "" + Integer.MAX_VALUE);
        asserFormattedAsIs(SOLR_INTEGER, "" + Integer.MIN_VALUE);

    }

    @Test
    void testFormatLong() {

        assertThrows(AdlFormattingException.class, () -> SOLR_LONG.format("argName", null, MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_LONG.format("argName", "", MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_LONG.format("argName", "03", MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_LONG.format("argName", "1" + Long.MAX_VALUE, MatchOperator.EQUALS));

        asserFormattedAsIs(SOLR_LONG, "0");
        asserFormattedAsIs(SOLR_LONG, "123456");
        asserFormattedAsIs(SOLR_LONG, "-123456");
        asserFormattedAsIs(SOLR_LONG, "" + Long.MAX_VALUE);
        asserFormattedAsIs(SOLR_LONG, "" + Long.MIN_VALUE);

    }

    @Test
    void testFormatFloat() {

        assertThrows(AdlFormattingException.class, () -> SOLR_FLOAT.format("argName", null, MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_FLOAT.format("argName", "", MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_FLOAT.format("argName", "03", MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_FLOAT.format("argName", "1" + Float.MAX_VALUE, MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_FLOAT.format("argName", "-1" + Float.MAX_VALUE, MatchOperator.EQUALS));

        asserFormatted("0.0", SOLR_FLOAT, "0");
        asserFormattedAsIs(SOLR_FLOAT, "0.0");
        asserFormatted("123456.0", SOLR_FLOAT, "123456");
        asserFormatted("-123456.0", SOLR_FLOAT, "-123456");
        asserFormatted("-1.123", SOLR_FLOAT, "-1.123");
        asserFormatted("1.123", SOLR_FLOAT, "1.123");
        asserFormatted("340282350000000000000000000000000000000.0", SOLR_FLOAT, "" + Float.MAX_VALUE);
        asserFormatted("-340282350000000000000000000000000000000.0", SOLR_FLOAT, "-" + Float.MAX_VALUE);

    }

    @Test
    void testFormatDouble() {

        assertThrows(AdlFormattingException.class, () -> SOLR_DOUBLE.format("argName", null, MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_DOUBLE.format("argName", "", MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_DOUBLE.format("argName", "03", MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_DOUBLE.format("argName", "1" + Double.MAX_VALUE, MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_DOUBLE.format("argName", "-1" + Double.MAX_VALUE, MatchOperator.EQUALS));

        asserFormatted("0.0", SOLR_DOUBLE, "0");
        asserFormattedAsIs(SOLR_DOUBLE, "0.0");
        asserFormatted("123456.0", SOLR_DOUBLE, "123456");
        asserFormatted("-123456.0", SOLR_DOUBLE, "-123456");
        asserFormatted("-1.123", SOLR_DOUBLE, "-1.123");
        asserFormatted("1.123", SOLR_DOUBLE, "1.123");
        asserFormatted("1340282350000000000000000000000000000000.0", SOLR_DOUBLE, "1" + Float.MAX_VALUE);
        asserFormatted("-1340282350000000000000000000000000000000.0", SOLR_DOUBLE, "-1" + Float.MAX_VALUE);

        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setGroupingUsed(false);
        nf.setMinimumFractionDigits(1);
        String maxDoubleAsString = nf.format(Double.MAX_VALUE);

        asserFormatted(maxDoubleAsString, SOLR_DOUBLE, "" + Double.MAX_VALUE);
        asserFormatted("-" + maxDoubleAsString, SOLR_DOUBLE, "-" + Double.MAX_VALUE);

    }

    @Test
    void testFormatBoolean() {
        assertThrows(AdlFormattingException.class, () -> SOLR_BOOLEAN.format("argName", null, MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_BOOLEAN.format("argName", "", MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_BOOLEAN.format("argName", "01", MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_BOOLEAN.format("argName", "true", MatchOperator.EQUALS));

        asserFormatted(SolrFormatConstants.TRUE, SOLR_BOOLEAN, "1");
        asserFormatted(SolrFormatConstants.FALSE, SOLR_BOOLEAN, "0");

        // tolerance
        asserFormattedAsIs(SOLR_BOOLEAN, SolrFormatConstants.TRUE);
        asserFormattedAsIs(SOLR_BOOLEAN, SolrFormatConstants.FALSE);

    }

    @Test
    void testFormatDate() {
        assertThrows(AdlFormattingException.class, () -> SOLR_DATE.format("argName", null, MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_DATE.format("argName", "", MatchOperator.EQUALS));
        assertThrows(AdlFormattingException.class, () -> SOLR_DATE.format("argName", "01", MatchOperator.EQUALS));

        asserFormatted("2025\\-07\\-03T00\\:00\\:00Z", SOLR_DATE, "2025-07-03");
        asserFormatted("2025\\-07\\-03T17\\:23\\:48Z", SOLR_DATE, "2025-07-03 17:23:48");

        assertThrows(AdlFormattingException.class, () -> SOLR_DATE.format("argName", "2025-07-03 17:23", MatchOperator.EQUALS));

    }

    private static void asserFormattedAsIs(ArgValueFormatter formatter, String input) {
        String output = formatter.format("argName", input, MatchOperator.EQUALS);
        assertEquals(input, output);
    }

    private static void asserFormatted(String expected, ArgValueFormatter formatter, String input) {
        String output = formatter.format("argName", input, MatchOperator.EQUALS);
        assertEquals(expected, output);
    }

}
