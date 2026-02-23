//@formatter:off
/*
 * ConfigUtilsTest
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class ConfigUtilsTest {

    @Test
    void testAssertValidArgName() {

        assertTrue(ConfigUtils.isValidArgName("a"));
        assertTrue(ConfigUtils.isValidArgName("a.b"));
        assertTrue(ConfigUtils.isValidArgName("*"));

        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidArgName(null));
        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidArgName(""));
        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidArgName("  "));
        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidArgName(" \n "));

    }

    @Test
    void testAssertValidSolrName() {

        assertTrue(ConfigUtils.isValidSolrName("a"));
        assertTrue(ConfigUtils.isValidSolrName("a_b"));
        assertTrue(ConfigUtils.isValidSolrName("A_B"));
        assertTrue(ConfigUtils.isValidSolrName("a5"));
        assertTrue(ConfigUtils.isValidSolrName("a_5"));

        assertFalse(ConfigUtils.isValidSolrName(null));
        assertFalse(ConfigUtils.isValidSolrName(""));
        assertFalse(ConfigUtils.isValidSolrName("  "));
        assertFalse(ConfigUtils.isValidSolrName(" n "));
        assertFalse(ConfigUtils.isValidSolrName("a name"));
        assertFalse(ConfigUtils.isValidSolrName("a.a"));
        assertFalse(ConfigUtils.isValidSolrName("_name"));
        assertFalse(ConfigUtils.isValidSolrName("name_"));
        assertFalse(ConfigUtils.isValidSolrName("_name_"));
        assertFalse(ConfigUtils.isValidSolrName("germanSZ_ß"));

        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidSolrName(null));
        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidSolrName(""));
        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidSolrName("  "));
        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidSolrName("a name"));
        assertThrows(IllegalArgumentException.class, () -> ConfigUtils.assertValidSolrName("_name_"));

    }

    @Test
    void testIsSolrLetterOrDigit() {

        String validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; i++) {
            char ch = (char) i;
            if (validChars.indexOf(ch) > -1) {
                assertTrue(ConfigUtils.isSolrLetterOrDigit(ch));
            }
            else {
                assertFalse(ConfigUtils.isSolrLetterOrDigit(ch));
            }
        }

    }

}
