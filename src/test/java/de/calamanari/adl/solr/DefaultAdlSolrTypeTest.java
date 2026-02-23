//@formatter:off
/*
 * DefaultAdlSolrTypeTest
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

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.calamanari.adl.cnv.tps.AdlType;
import de.calamanari.adl.cnv.tps.ArgValueFormatter;
import de.calamanari.adl.cnv.tps.ConfigException;
import de.calamanari.adl.cnv.tps.DefaultAdlType;
import de.calamanari.adl.cnv.tps.DefaultArgValueFormatter;
import de.calamanari.adl.cnv.tps.NativeTypeCaster;
import de.calamanari.adl.cnv.tps.PassThroughTypeCaster;
import de.calamanari.adl.irl.MatchOperator;

import static de.calamanari.adl.cnv.tps.DefaultAdlType.BOOL;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.DATE;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.DECIMAL;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.INTEGER;
import static de.calamanari.adl.cnv.tps.DefaultAdlType.STRING;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_BOOLEAN;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_DATE;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_DOUBLE;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_FLOAT;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_INTEGER;
import static de.calamanari.adl.solr.DefaultAdlSolrType.SOLR_STRING;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class DefaultAdlSolrTypeTest {

    static final Logger LOGGER = LoggerFactory.getLogger(DefaultAdlSolrTypeTest.class);

    @Test
    void testBasics() {
        AdlSolrType type1 = SOLR_BOOLEAN;

        AdlSolrType type2 = SOLR_BOOLEAN.withFormatter(DefaultArgValueFormatter::formatBool);

        assertNotEquals(type1, type2);
        assertNotEquals(type1.name(), type2.name());

        assertEquals(type1, type2.getBaseType());

        AdlSolrType type2a = type2.withFormatter(DefaultArgValueFormatter::formatBool);

        assertNotEquals(type2, type2a);
        assertNotEquals(type2.name(), type2a.name());

        assertEquals(type1, type2a.getBaseType());

        assertTrue(type2a.isCompatibleWith(BOOL));

        type2 = SOLR_BOOLEAN.withFormatter("SOLR_BOOLEAN", DefaultArgValueFormatter::formatBool);

        assertNotEquals(type1, type2);
        assertEquals(type1.name(), type2.name());

        type1 = SOLR_STRING;

        type2 = SOLR_STRING.withFormatter((_, argValue, _) -> argValue);

        assertNotEquals(type1, type2);
        assertNotEquals(type1.name(), type2.name());

        assertEquals(type1, type2.getBaseType());

        assertSame(type1, type1.withFormatter(null));
        assertSame(type1, type1.withNativeTypeCaster(null));

    }

    @Test
    void testStandardBehavior() {
        for (DefaultAdlSolrType type : DefaultAdlSolrType.values()) {
            assertEquals(expectedFormatter(type), type.getFormatter());
            assertEquals(expectedCoType(type).supportsContains(), type.supportsContains());
            assertEquals(expectedCoType(type).supportsLessThanGreaterThan(), type.supportsLessThanGreaterThan());
        }

    }

    private static ArgValueFormatter expectedFormatter(AdlSolrType adlSolrType) {
        switch (adlSolrType) {
        case DefaultAdlSolrType.SOLR_BOOLEAN:
            return DefaultSolrFormatter.SOLR_BOOLEAN;
        case DefaultAdlSolrType.SOLR_LONG:
            return DefaultSolrFormatter.SOLR_LONG;
        case DefaultAdlSolrType.SOLR_INTEGER:
            return DefaultSolrFormatter.SOLR_INTEGER;
        case DefaultAdlSolrType.SOLR_STRING:
            return DefaultSolrFormatter.SOLR_STRING;
        case DefaultAdlSolrType.SOLR_DATE:
            return DefaultSolrFormatter.SOLR_DATE;
        case DefaultAdlSolrType.SOLR_DOUBLE:
            return DefaultSolrFormatter.SOLR_DOUBLE;
        case DefaultAdlSolrType.SOLR_FLOAT:
            return DefaultSolrFormatter.SOLR_FLOAT;
        default:
            throw new IllegalStateException("Unknown new AdlSolrType: " + adlSolrType);
        }

    }

    private static AdlType expectedCoType(AdlSolrType adlSolrType) {
        switch (adlSolrType) {
        case DefaultAdlSolrType.SOLR_BOOLEAN:
            return DefaultAdlType.BOOL;
        case DefaultAdlSolrType.SOLR_INTEGER, DefaultAdlSolrType.SOLR_LONG:
            return DefaultAdlType.INTEGER;
        case DefaultAdlSolrType.SOLR_STRING:
            return DefaultAdlType.STRING;
        case DefaultAdlSolrType.SOLR_DATE:
            return DefaultAdlType.DATE;
        case DefaultAdlSolrType.SOLR_FLOAT, DefaultAdlSolrType.SOLR_DOUBLE:
            return DefaultAdlType.DECIMAL;
        default:
            throw new IllegalStateException("Unknown new AdlSolrType: " + adlSolrType);
        }
    }

    @Test
    void testCompatibility1() {

        assertTrue(SOLR_BOOLEAN.isCompatibleWith(BOOL));
        assertTrue(SOLR_BOOLEAN.isCompatibleWith(INTEGER));
        assertTrue(SOLR_BOOLEAN.isCompatibleWith(STRING));
        assertFalse(SOLR_BOOLEAN.isCompatibleWith(DATE));
        assertFalse(SOLR_BOOLEAN.isCompatibleWith(DECIMAL));
        assertFalse(SOLR_BOOLEAN.isCompatibleWith(SOLR_BOOLEAN));

        assertTrue(SOLR_STRING.isCompatibleWith(BOOL));
        assertTrue(SOLR_STRING.isCompatibleWith(INTEGER));
        assertTrue(SOLR_STRING.isCompatibleWith(STRING));
        assertTrue(SOLR_STRING.isCompatibleWith(DATE));
        assertTrue(SOLR_STRING.isCompatibleWith(DECIMAL));
        assertFalse(SOLR_STRING.isCompatibleWith(SOLR_STRING));
    }

    @Test
    void testCompatibility2() {

        assertFalse(SOLR_DATE.isCompatibleWith(BOOL));
        assertTrue(SOLR_DATE.isCompatibleWith(INTEGER));
        assertTrue(SOLR_DATE.isCompatibleWith(STRING));
        assertTrue(SOLR_DATE.isCompatibleWith(DATE));
        assertTrue(SOLR_DATE.isCompatibleWith(DECIMAL));
        assertFalse(SOLR_DATE.isCompatibleWith(SOLR_DATE));

        assertFalse(SOLR_DOUBLE.isCompatibleWith(BOOL));
        assertTrue(SOLR_DOUBLE.isCompatibleWith(INTEGER));
        assertTrue(SOLR_DOUBLE.isCompatibleWith(STRING));
        assertTrue(SOLR_DOUBLE.isCompatibleWith(DATE));
        assertTrue(SOLR_DOUBLE.isCompatibleWith(DECIMAL));
        assertFalse(SOLR_DOUBLE.isCompatibleWith(SOLR_DOUBLE));

        assertFalse(SOLR_FLOAT.isCompatibleWith(BOOL));
        assertTrue(SOLR_FLOAT.isCompatibleWith(INTEGER));
        assertTrue(SOLR_FLOAT.isCompatibleWith(STRING));
        assertFalse(SOLR_FLOAT.isCompatibleWith(DATE));
        assertTrue(SOLR_FLOAT.isCompatibleWith(DECIMAL));
        assertFalse(SOLR_FLOAT.isCompatibleWith(SOLR_FLOAT));
    }

    @Test
    void testCompatibility3() {

        assertTrue(SOLR_INTEGER.isCompatibleWith(BOOL));
        assertTrue(SOLR_INTEGER.isCompatibleWith(INTEGER));
        assertTrue(SOLR_INTEGER.isCompatibleWith(STRING));
        assertTrue(SOLR_INTEGER.isCompatibleWith(DATE));
        assertTrue(SOLR_INTEGER.isCompatibleWith(DECIMAL));
        assertFalse(SOLR_INTEGER.isCompatibleWith(SOLR_INTEGER));

    }

    @Test
    void testDecoration() {

        ArgValueFormatter formatter = new ArgValueFormatter() {

            private static final long serialVersionUID = 7769081572229587962L;

            @Override
            public String format(String argName, String argValue, MatchOperator operator) {
                return argValue;
            }
        };

        assertDerived(SOLR_INTEGER, SOLR_INTEGER.withFormatter(formatter));
        assertDerived(SOLR_INTEGER, SOLR_INTEGER.withFormatter("SOLR_INTEGER-new", formatter));
        assertEquals("SOLR_INTEGER-new2", SOLR_INTEGER.withFormatter("SOLR_INTEGER-new2", formatter).name());
        assertEquals(formatter, SOLR_INTEGER.withFormatter("SOLR_INTEGER-new2", formatter).getFormatter());

        NativeTypeCaster ntc = new NativeTypeCaster() {

            private static final long serialVersionUID = -2716335076800837311L;

            @Override
            public String formatNativeTypeCast(String argName, String nativeFieldName, AdlType argType, AdlType requestedArgType) {
                return "bla";
            }

        };

        assertThrows(ConfigException.class, () -> SOLR_INTEGER.withNativeTypeCaster(ntc));

        AdlSolrType customType = SOLR_INTEGER.withFormatter(formatter);

        assertEquals(formatter, customType.getFormatter());

        assertEquals(PassThroughTypeCaster.getInstance(), customType.getNativeTypeCaster());

        assertEquals(SOLR_INTEGER.getFormatter(), customType.getBaseType().getFormatter());

    }

    private static void assertDerived(AdlSolrType baseType, AdlSolrType customType) {

        assertNotEquals(baseType, customType);
        assertNotEquals(baseType.toString(), customType.toString());
        assertEquals(baseType, customType.getBaseType());
        assertTrue(customType.name().startsWith(baseType.name()));

        assertEquals(baseType.supportsContains(), customType.supportsContains());
        assertEquals(baseType.supportsLessThanGreaterThan(), customType.supportsLessThanGreaterThan());

    }

}
