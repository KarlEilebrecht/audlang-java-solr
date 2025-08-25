//@formatter:off
/*
 * SolrFormatUtilsTest
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

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class SolrFormatUtilsTest {

    @Test
    void testEscape() {

        assertThrows(NullPointerException.class, () -> SolrFormatUtils.escape(null));
        assertNotEscaped("");
        assertNotEscaped("a");
        assertNotEscaped("A");
        assertNotEscaped("Aa");
        assertNotEscaped("TheQuickBrownFoxJumpedOverTheLazyDog");

        for (int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; i++) {
            char ch = (char) i;
            String input = "" + ch;
            if (ch == '\\' || Character.isWhitespace(ch) || SolrFormatConstants.SOLR_SPECIAL_CHARACTERS.indexOf(ch) >= 0) {
                assertEquals("\\" + ch, SolrFormatUtils.escape(input));
            }
            else {
                assertNotEscaped(input);
            }
        }

        assertEscaped("\\\\", "\\");
        assertEscaped("\\+a", "+a");
        assertEscaped("\\+abba", "+abba");
        assertEscaped("ab\\+ba", "ab+ba");
        assertEscaped("abba\\+", "abba+");
        assertEscaped("\\+ab\\+b\\+a\\+", "+ab+b+a+");
        assertEscaped("\\+ab\\+\\ b\\+a\\+", "+ab+ b+a+");
        assertEscaped("\\+ab\\+\\ \\\tb\\+a\\+", "+ab+ \tb+a+");
        assertEscaped("\\\\\\+a", "\\+a");
        assertEscaped("\\\\\\+abba", "\\+abba");
        assertEscaped("ab\\\\\\+ba", "ab\\+ba");
        assertEscaped("abba\\\\\\+", "abba\\+");
        assertEscaped("\\\\\\+ab\\\\\\+b\\\\\\+a\\\\\\+", "\\+ab\\+b\\+a\\+");
        assertEscaped("\\\\\\+ab\\\\\\+\\\\\\ b\\\\\\+a\\\\\\+", "\\+ab\\+\\ b\\+a\\+");
        assertEscaped("\\\\\\+ab\\\\\\+\\\\\\ \\\\\\\tb\\\\\\+a\\\\\\+", "\\+ab\\+\\ \\\tb\\+a\\+");

    }

    @Test
    void testAppendCondition() {
        StringBuilder sb = new StringBuilder();

        SolrFormatUtils.appendCondition(sb, "name", "value");

        assertEquals("name:value", sb.toString());

        sb.setLength(0);

        SolrFormatUtils.appendCondition(sb, "", "");

        assertEquals(":", sb.toString());

    }

    @Test
    void testAppendFrangeHeader() {
        StringBuilder sb = new StringBuilder();

        SolrFormatUtils.appendFrangeHeader(sb, 1, 2);

        assertEquals("{!frange l=1 u=2}", sb.toString());

        sb.setLength(0);

        SolrFormatUtils.appendFrangeHeader(sb, 2, 5);

        assertEquals("{!frange l=2 u=5}", sb.toString());

    }

    @Test
    void testAppendFrangeStartWithExistenceChecks() {

        StringBuilder sb = new StringBuilder();

        SolrFormatUtils.appendFrangeStartWithExistenceChecks(sb, "left", "right");

        assertEquals("and(exists(left),exists(right),", sb.toString());

    }

    @Test
    void testAppendDateFieldAtMidnightToFrange() {
        StringBuilder sb = new StringBuilder();

        SolrFormatUtils.appendDateFieldAtMidnightToFrange(sb, "fieldName");

        assertEquals("sub(ms(fieldName),sub(ms(fieldName),mul(floor(div(ms(fieldName),86400000)),86400000)))", sb.toString());

    }

    private static void assertNotEscaped(String input) {
        String output = SolrFormatUtils.escape(input);
        assertSame(input, output);
    }

    private static void assertEscaped(String expected, String input) {
        assertEquals(expected, SolrFormatUtils.escape(input));
        StringBuilder sb = new StringBuilder();
        SolrFormatUtils.appendEscaped(sb, input);
        assertEquals(expected, sb.toString());

        String prefix = "some prefix!";
        sb = new StringBuilder(prefix);
        SolrFormatUtils.appendEscaped(sb, input);
        assertEquals(prefix + expected, sb.toString());

    }

}
