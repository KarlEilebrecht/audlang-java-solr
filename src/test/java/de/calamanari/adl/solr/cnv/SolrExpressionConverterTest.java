//@formatter:off
/*
 * SolrExpressionConverterTest
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.calamanari.adl.CommonErrors;
import de.calamanari.adl.solr.SolrTestBase;

import static de.calamanari.adl.solr.cnv.ConversionTestUtils.createDryTestContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class SolrExpressionConverterTest extends SolrTestBase {

    static final Logger LOGGER = LoggerFactory.getLogger(SolrExpressionConverterTest.class);

    @BeforeAll
    static void prepareEmbeddedSolr() throws IOException {
        initTestServerWithHybridMapping();
    }

    @Test
    void testBasics() {

        assertEquals(6, selectCount("provider=LOGMOTH"));

        assertEquals(1, selectCount("q.children.int = 7"));

        assertEquals(9, selectCount("provider!=ZOMBEE"));

        assertEquals(10, selectCount("q.children.int = 7 OR provider!=ZOMBEE"));

        assertEquals(5, selectCount("(q.children.int = 7 OR provider!=ZOMBEE) AND home-country=USA"));

        List<Integer> left = new ArrayList<>(selectIds("(q.children.int = 7 OR provider!=ZOMBEE)"));
        List<Integer> right = new ArrayList<>(selectIds("home-country=USA"));

        left.retainAll(right);

        assertEquals(left, selectIds("(q.children.int = 7 OR provider!=ZOMBEE) AND home-country=USA"));

        assertEquals(4, selectCount("q.children.int != 1 AND home-country=USA"));

        assertEquals(10, selectCount("q.children.int != 1 OR home-country=USA"));

        assertEquals(7, selectCount("fact.provider=CLCPRO AND q.children.int != 1"));

        assertEquals(10, selectCount("fact.provider=CLCPRO OR q.children.int != 1"));

        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016, 19019), "fact.provider=CLCPRO AND fact.homeCity.str!=Chicago");

        assertEquals(1, selectCount("fact.provider=CLCPRO AND STRICT fact.homeCity.str!=Chicago"));

        assertQueryResult(list(19019), "fact.homeCity.str STRICT NOT ANY OF (Chicago, London)");

        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19021), "fact.homeCity.str NOT ANY OF (Chicago, London)");

        assertQueryResult(list(19016, 19021), "home-city ANY OF (Chicago, London) AND sports=football");

        assertQueryResult(list(19013, 19014, 19017), "omScore < 1700");

        assertQueryResult(list(19012, 19013, 19014, 19015, 19016), "demCode any of (8,9)");

        assertQueryResult(list(19011, 19013, 19019, 19020, 19021), "home-country=USA");

        assertQueryResult(list(19011, 19014, 19016, 19021), "sports=football");

        assertQueryResult(list(19011, 19015), "pos.name=SANDWICH AND pos.unitPrice > 3.0 AND pos.unitPrice < 4.0");

        assertQueryResult(list(19011, 19015),
                "pos.name=SANDWICH AND pos.unitPrice > 3.0 AND pos.unitPrice < 4.0 AND (pos.date=2024-03-22 OR pos.anyDate=2024-03-17)");

    }

    @Test
    void testBoolean() {

        assertEquals(0, selectCount("true1=0"));
        assertEquals(1, selectCount("true1=1"));

        assertEquals(1, selectCount("false1=0"));
        assertEquals(0, selectCount("false1=1"));

        assertEquals(1, selectCount("true1=@trueTrue"));
        assertEquals(1, selectCount("STRICT false1!=@trueTrue"));

        assertEquals(1, selectCount("trueFalse1=@trueTrue"));
        assertEquals(0, selectCount("trueFalse1=@falseFalse"));
        assertEquals(1, selectCount("trueFalse1=@trueFalse2"));

        assertEquals(0, selectCount("false1=@fact.hasBird.flg"));
        assertEquals(1, selectCount("trueFalse1=@fact.hasBird.flg"));

        assertQueryResult(list(19014), "fact.hasPet.flg=1 AND fact.hasBird.flg=1");

        assertQueryResult(list(19011, 19012, 19014, 19016, 19019), "fact.hasPet.flg=1 OR fact.hasBird.flg=1");

        assertQueryResult(list(19015, 19020), "fact.hasPet.flg=0");

        assertQueryResult(list(19013, 19015, 19017, 19018, 19020, 19021), "fact.hasPet.flg!=1");

        assertQueryResult(list(19015, 19020), "STRICT fact.hasPet.flg!=1");

        assertQueryResult(list(19019), "fact.hasPet.flg=@fact.trueFalse.flgs");

        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016, 19017, 19018, 19020, 19021), "fact.hasPet.flg!=@fact.trueFalse.flgs");

        assertQueryResult(list(), "STRICT fact.hasPet.flg!=@fact.trueFalse.flgs");

        assertQueryResult(list(19019), "STRICT fact.hasPet.flg!=@fact.falseFalse.flgs");

        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "q.carOwner.flg!=@fact.hasBird.flg");

        assertQueryResult(list(19011), "STRICT q.carOwner.flg!=@fact.hasBird.flg");

    }

    @Test
    void testMultipleFacts() {

        assertQueryResult(list(19011), "bState != @fact.video.flg AND fact.hasBird.flg = 0");

        assertEquals(1, selectCount("fact.contactTime.ts=2023-12-24"));

        assertQueryResult(list(19011, 19012, 19013), "fact.contactTime.ts > 2024-12-20");

        assertQueryResult(list(19011), "fact.contactTime.ts=2023-12-24 AND fact.contactTime.ts=2024-12-22");

    }

    @Test
    void testWithGlobalVariable() {

        assertQueryResult(list(19011, 19013, 19018, 19021), "q.carOwner.flg=1");

        assertEquals(list(19011, 19013, 19018, 19021), selectIds("q.carOwner.flg=1", withTenant(17)));

        assertEquals(list(), selectIds("q.carOwner.flg=1", withTenant(89)));

    }

    @Test
    void testPosData() {
        assertQueryResult(list(19011, 19015),
                "provider=HMPF OR (pos.name=SANDWICH AND pos.unitPrice > 3.0 AND pos.unitPrice < 4.0 AND (pos.date=2024-03-22 OR pos.anyDate=2024-03-17))");

        assertQueryResult(list(19015),
                "provider=HMPF OR (pos.name=SANDWICH AND pos.unitPrice > 3.0 AND pos.unitPrice < 4.0 AND (pos.date=2024-03-22 OR pos.anyDate != 2024-03-17))");

        assertQueryResult(list(19015),
                "provider=HMPF OR (pos.name=SANDWICH AND pos.unitPrice > 3.0 AND pos.unitPrice < 4.0 AND (pos.date=2024-03-22 OR STRICT pos.anyDate != 2024-03-17))");

    }

    @Test
    void testBasicSingleAttributeSelect1() {

        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016), "provider = LOGMOTH");
        assertQueryResult(list(19017, 19018), "provider = ZOMBEE");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016, 19019, 19020, 19021), "provider != ZOMBEE");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016, 19019, 19020, 19021), "STRICT provider != ZOMBEE");

        assertThrowsErrorCode(CommonErrors.ERR_1002_ALWAYS_FALSE, () -> selectCount("provider is unknown AND provider is not unknown"));
        assertThrowsErrorCode(CommonErrors.ERR_1001_ALWAYS_TRUE, () -> selectCount("provider is not unknown OR provider is unknown"));

        assertQueryResult(list(19011, 19013, 19019, 19020, 19021), "home-country = USA");
        assertQueryResult(list(19015, 19016), "home-country = UK");
        assertQueryResult(list(19012, 19014, 19015, 19016, 19017, 19018), "home-country != USA");
        assertQueryResult(list(19014, 19015, 19016, 19017, 19018), "STRICT home-country != USA");
        assertQueryResult(list(19012), "home-country is unknown");

        assertQueryResult(list(19011, 19019, 19021), "demCode = 7");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19021), "demCode > 5");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19021), "demCode != 5");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19021), "STRICT demCode != 5");
        assertQueryResult(list(), "demCode is unknown");

        assertQueryResult(list(19012, 19015, 19018), "omScore > 8000");
        assertQueryResult(list(19011, 19013, 19014, 19016, 19017, 19019, 19020, 19021), "omScore < 8000");
        assertQueryResult(list(), "omScore is unknown");

    }

    @Test
    void testBasicSingleAttributeSelect2() {

        // mapped to timestamp, requires range-queries
        assertQueryResult(list(19011, 19013), "upd1 = 2024-09-24");
        assertQueryResult(list(19019, 19020, 19021), "upd1 < 2024-09-24");
        assertQueryResult(list(19011, 19013, 19019, 19020, 19021), "upd1 <= 2024-09-24");
        assertQueryResult(list(19012, 19014, 19015, 19016, 19017, 19018), "upd1 >= 2024-09-25");
        assertQueryResult(list(19012, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "upd1 != 2024-09-24");
        assertQueryResult(list(19012, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "STRICT upd1 != 2024-09-24");
        assertQueryResult(list(), "upd1 is unknown");

        // mapped to date, no range queries
        assertQueryResult(list(19011, 19013), "upd2 = 2024-09-24");
        assertQueryResult(list(19019, 19020, 19021), "upd2 < 2024-09-24");
        assertQueryResult(list(19011, 19013, 19019, 19020, 19021), "upd2 <= 2024-09-24");
        assertQueryResult(list(19012, 19014, 19015, 19016, 19017, 19018), "upd2 >= 2024-09-25");
        assertQueryResult(list(19012, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "upd2 != 2024-09-24");
        assertQueryResult(list(), "upd2 is unknown");

    }

    @Test
    void testBasicSingleAttributeSelect3() {

        assertQueryResult(list(19017, 19018), "tntCode = 6");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016, 19019, 19020, 19021), "tntCode != 6");
        assertQueryResult(list(19011, 19013, 19014, 19015, 19016, 19019, 19020, 19021), "STRICT tntCode != 6");
        assertQueryResult(list(19015, 19017, 19018), "tntCode > 3");
        assertQueryResult(list(19011, 19016, 19019, 19020, 19021), "tntCode < 3");

        assertQueryResult(list(19013, 19016), "bState = 0");
        assertQueryResult(list(19011, 19012, 19014, 19015, 19017, 19018, 19019, 19020, 19021), "bState != 0");
        assertQueryResult(list(19011, 19014, 19017, 19018), "STRICT bState != 0");
        assertQueryResult(list(19011, 19014, 19017, 19018), "bState = 1");

        assertQueryResult(list(19014, 19017), "sCode = 17");
        assertQueryResult(list(19012, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "sCode != 89");
        assertQueryResult(list(19013, 19014, 19016, 19017, 19018), "STRICT sCode != 89");
        assertQueryResult(list(19011, 19013, 19014, 19017), "sCode > 11");
        assertQueryResult(list(19016, 19018), "sCode < 17");

    }

    @Test
    void testBasicSingleAttributeSelect4() {

        assertQueryResult(list(19018), "biCode = 5512831");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016, 19017, 19019, 19020, 19021), "biCode != 5512831");
        assertQueryResult(list(19011, 19013, 19015, 19016, 19017), "STRICT biCode != 5512831");
        assertQueryResult(list(19011, 19013, 19015, 19016, 19017), "biCode > 5512831");
        assertQueryResult(list(19018), "biCode < 6612732");

        assertQueryResult(list(19015, 19016, 19018), "nCode > 19273213");
        assertQueryResult(list(19012, 19013, 19017), "nCode < 19273213");

        assertQueryResult(list(19012, 19015, 19016, 19017, 19018), "demCode BETWEEN (9, 11)");

        assertQueryResult(list(19011, 19013, 19014, 19019, 19020, 19021), "demCode NOT BETWEEN (9, 11)");

        assertQueryResult(list(19011, 19013, 19014, 19019, 19020, 19021), "demCode STRICT NOT BETWEEN (9, 11)");

    }

    @Test
    void testBasicSingleAttributeSelect5() {

        assertQueryResult(list(19011, 19012, 19014, 19016, 19019), "fact.hasPet.flg = 1");
        assertQueryResult(list(19011, 19016), "fact.hasDog.flg = 1");
        assertQueryResult(list(19012, 19013, 19014, 19015, 19017, 19018, 19019, 19020, 19021), "fact.hasDog.flg != 1");
        assertQueryResult(list(19012), "STRICT fact.hasDog.flg != 1");
        assertQueryResult(list(19012), "fact.hasDog.flg = 0");

        assertQueryResult(list(19011), "fact.dateOfBirth.dt = 2000-03-05");
        assertQueryResult(list(19014, 19019), "fact.dateOfBirth.dt > 2001-01-01");

        assertQueryResult(list(19011), "fact.contactTime.ts = 2023-12-24");
        assertQueryResult(list(19011), "fact.contactTime.ts < 2024-01-01");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19019, 19020), "fact.contactTime.ts > 2024-01-01");
        assertQueryResult(list(19012, 19013, 19014, 19019, 19020), "STRICT NOT fact.contactTime.ts < 2024-01-01");

        assertQueryResult(list(19011, 19013), "fact.contactCode.str = RX89");
        assertQueryResult(list(19012, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "fact.contactCode.str != RX89");
        assertQueryResult(list(19012, 19014, 19019, 19020), "STRICT fact.contactCode.str != RX89");
        assertQueryResult(list(19019, 19020), "fact.contactCode.str > RX89");
        assertQueryResult(list(19015, 19016, 19017, 19018, 19021), "fact.contactCode.str IS UNKNOWN");

    }

    @Test
    void testBasicSingleAttributeSelect6() {

        assertQueryResult(list(19011, 19012, 19015), "fact.yearOfBirth.int = 2000");
        assertQueryResult(list(19014, 19019), "fact.yearOfBirth.int > 2000");
        assertQueryResult(list(19011, 19012, 19013, 19015, 19016, 19017, 19018, 19020, 19021), "NOT fact.yearOfBirth.int > 2000");
        assertQueryResult(list(19011, 19012, 19015), "STRICT NOT fact.yearOfBirth.int > 2000");

        assertQueryResult(list(), "fact.xScore.dec > 1");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19016, 19018, 19019, 19020), "fact.xScore.dec < 1");

    }

    @Test
    void testBasicSingleAttributeSelect7() {

        assertQueryResult(list(19011), "q.monthlyIncome.int = 4600");
        assertQueryResult(list(19011, 19012, 19013, 19018), "q.monthlyIncome.int >= 4600");
        assertQueryResult(list(19011), "q.monthlyIncome.int < 5000");
        assertQueryResult(list(19014, 19015, 19016, 19017, 19019, 19020, 19021), "NOT q.monthlyIncome.int >= 4600");

        assertQueryResult(list(19011), "q.martialStatus.str = married");
        assertQueryResult(list(19012, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "q.martialStatus.str != married");
        assertQueryResult(list(19012, 19013, 19017, 19018, 19021), "STRICT q.martialStatus.str != married");

        assertQueryResult(list(19012, 19021), "q.children.int = 0");
        assertQueryResult(list(19013, 19017), "q.children.int > 1");
        assertQueryResult(list(19011, 19012, 19014, 19015, 19016, 19018, 19019, 19020, 19021), "NOT q.children.int > 1");

        assertQueryResult(list(19011, 19013, 19018, 19021), "q.carOwner.flg = 1");
        assertQueryResult(list(19012), "q.carOwner.flg = 0");

    }

    @Test
    void testBasicSingleAttributeSelect8() {

        assertQueryResult(list(19011), "pos.date = 2024-01-13");
        assertQueryResult(list(19012, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "pos.date != 2024-01-13");
        assertQueryResult(list(19012, 19013, 19014, 19015, 19017), "STRICT pos.date != 2024-01-13");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19017), "pos.date <= 2024-06-20");

        assertQueryResult(list(19012), "pos.name = POPCORN");
        assertQueryResult(list(19013, 19017), "pos.name contains WATER");

        assertQueryResult(list(19011, 19013, 19014, 19015, 19017), "pos.quantity > 1");
        assertQueryResult(list(19012, 19016, 19018, 19019, 19020, 19021), "NOT pos.quantity > 1");
        assertQueryResult(list(19012), "STRICT NOT pos.quantity > 1");

        assertQueryResult(list(19012), "pos.unitPrice > 500");

        assertQueryResult(list(19011, 19012, 19013), "pos.country = USA");

    }

    @Test
    void testBasicSingleAttributeSelect9() {

        assertQueryResult(list(19011, 19012, 19016), "sports = tennis");
        assertQueryResult(list(19013, 19014, 19015, 19017, 19018, 19019, 19020, 19021), "sports != tennis");
        assertQueryResult(list(19014, 19017, 19021), "STRICT sports != tennis");

        assertQueryResult(list(19011, 19013, 19014, 19016, 19021), "sizeCM > 175");
        assertQueryResult(list(19021), "sizeCM = 188");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19016, 19021), "sizeCM > 90");
        assertQueryResult(list(19015, 19017, 19018, 19019, 19020), "NOT sizeCM > 90");
        assertQueryResult(list(), "STRICT NOT sizeCM > 90");

        assertQueryResult(list(19011, 19012, 19013, 19016, 19021), "bodyTempCelsius > 4");

        assertQueryResult(list(19011, 19013, 19016, 19021), "anniverseryDate > 2024-01-01");
        assertQueryResult(list(19011), "anniverseryDate = 2024-09-20");
        assertQueryResult(list(19012, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "anniverseryDate != 2024-09-20");
        assertQueryResult(list(19012, 19013, 19016, 19021), "STRICT anniverseryDate != 2024-09-20");

        assertQueryResult(list(19011, 19013, 19015, 19017, 19021), "clubMember = 1");
        assertQueryResult(list(19014, 19016), "clubMember = 0");
        assertQueryResult(list(19012, 19014, 19016, 19018, 19019, 19020), "clubMember != 1");

    }

    @Test
    void testFlatOnlySingleAttributeSelect() {
        assertQueryResult(list(19014, 19017, 19021), "STRICT sports != tennis");
    }

    @Test
    void testFactsOnlySingleAttributeSelect() {
        assertQueryResult(list(19011, 19012, 19015), "fact.yearOfBirth.int = 2000");
        assertQueryResult(list(19014, 19019), "fact.yearOfBirth.int > 2000");
        assertQueryResult(list(19011, 19012, 19015), "STRICT NOT fact.yearOfBirth.int > 2000");
    }

    @Test
    void testSurveyOnlySingleAttributeSelect() {
        assertQueryResult(list(19011), "q.monthlyIncome.int = 4600");
        assertQueryResult(list(19011, 19012, 19013, 19018), "q.monthlyIncome.int >= 4600");
        assertQueryResult(list(), "STRICT NOT q.monthlyIncome.int >= 4600");
    }

    @Test
    void testPosDataOnlySingleAttributeSelect() {
        assertQueryResult(list(19011), "pos.date = 2024-01-13");
        assertQueryResult(list(19012, 19013, 19014, 19015, 19017), "STRICT pos.date != 2024-01-13");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19017), "pos.date <= 2024-06-20");
    }

    @Test
    @Disabled("These tests could be unreliable due to precision problems (for me they work ;-)")
    void testBasicSingleAttributeDecimalCount() {
        assertEquals(4, selectCount("omScore <= 1723.9"));
        assertEquals(8, selectCount("omScore >= 1723.9"));

        assertEquals(2, selectCount("nCode = 19273213.21"));
        assertEquals(9, selectCount("nCode != 19273213.21"));
        assertEquals(4, selectCount("STRICT nCode != 19273213.21"));
        assertEquals(1, selectCount("nCode > 19273213.21"));
        assertEquals(3, selectCount("nCode < 19273213.21"));

        assertEquals(2, selectCount("fact.xScore.dec = 0.9871621"));
        assertEquals(0, selectCount("fact.xScore.dec > 0.9871621"));
        assertEquals(6, selectCount("fact.xScore.dec < 0.9871621"));
        assertEquals(9, selectCount("fact.xScore.dec != 0.9871621"));
        assertEquals(6, selectCount("STRICT fact.xScore.dec != 0.9871621"));

        assertEquals(2, selectCount("pos.unitPrice = 499.99"));

    }

    @Test
    void testLtGtOnCollection() {
        assertQueryResult(list(19014, 19016, 19021), "sports < football");
        assertQueryResult(list(19011, 19012, 19016, 19017), "sports > football");

    }

    @Test
    void testCoverConstructors() {

        ResettableScpContext ctx = createDryTestContext();

        SolrExpressionConverter converter = new SolrExpressionConverter(ctx.getMappingConfig());
        assertTrue(converter.getInitialFlags().isEmpty());
        assertTrue(converter.getInitialVariables().isEmpty());

        converter = new SolrExpressionConverter(ctx.getMappingConfig(), SolrConversionDirective.DISABLE_CONTAINS);

        assertTrue(converter.getInitialFlags().contains(SolrConversionDirective.DISABLE_CONTAINS));

        converter = new SolrExpressionConverter(ctx.getMappingConfig(), vars().put("foo", "bar").get(), flags(SolrConversionDirective.DISABLE_CONTAINS));

        assertTrue(converter.getInitialFlags().contains(SolrConversionDirective.DISABLE_CONTAINS));
        assertEquals("bar", converter.getInitialVariables().get("foo"));

    }

}
