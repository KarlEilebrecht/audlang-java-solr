//@formatter:off
/*
 * SolrDocumentationTest
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

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.calamanari.adl.CommonErrors;
import de.calamanari.adl.solr.EmbeddedSolrServerUtils;
import de.calamanari.adl.solr.SolrTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class SolrDocumentationTest extends SolrTestBase {

    static final Logger LOGGER = LoggerFactory.getLogger(SolrDocumentationTest.class);

    // Hint: You can always use the methods showDefinition(...) and showExecution(...) to check what's going on

    @Test
    void testExample1() throws IOException {

        initTestServerWithHybridMapping(EmbeddedSolrServerUtils.createHybridMappingConfigProfileOnly());

        assertQueryResult(list(19011, 19013), "provider = LOGMOTH AND home-country = USA");

        assertQueryResult(list(19011, 19012, 19013, 19015, 19018, 19019, 19020, 19021), "(provider = LOGMOTH AND home-country = USA) OR omScore > 5000");

    }

    @Test
    void testExample2() throws IOException {

        initTestServerWithHybridMapping(EmbeddedSolrServerUtils.createHybridMappingConfigProfileOnly());

        assertQueryResult(list(19011, 19013), "provider = LOGMOTH AND upd1 = 2024-09-24 AND upd2 = 2024-09-24");

        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016), "provider = LOGMOTH AND upd1 = @upd2");

    }

    @Test
    void testExample3() throws IOException {

        initTestServerWithHybridMapping(EmbeddedSolrServerUtils.createHybridMappingConfigProfileAndFactsNotMultiDoc());

        assertQueryResult(list(19014), "fact.provider = CLCPRO AND (fact.hasCat.flg=1 OR fact.hasBird.flg=1)");

        assertQueryResult(list(), "fact.provider = CLCPRO AND fact.hasCat.flg=1 AND fact.hasBird.flg=1");

        initTestServerWithHybridMapping(EmbeddedSolrServerUtils.createHybridMappingConfigProfileAndFacts());

        assertQueryResult(list(19014), "fact.provider = CLCPRO AND fact.hasCat.flg=1 AND fact.hasBird.flg=1");

    }

    @Test
    void testExample4() throws IOException {
        initTestServerWithHybridMapping(EmbeddedSolrServerUtils.createHybridMappingConfigProfileAndFacts());

        assertThrowsErrorCode(CommonErrors.ERR_2101_REFERENCE_MATCH_NOT_SUPPORTED,
                () -> assertQueryResult(list(19020), "fact.provider = @provider AND fact.hasPet.flg=0"));

        assertQueryResult(list(19014), "home-country=Germany AND fact.hasPet.flg=1 AND fact.hasBird.flg=1");

        assertQueryResult(list(19014), "fact.hasPet.flg=1 AND fact.hasBird.flg=1");

        assertQueryResult(list(19014), "fact.hasPet.flg=1 AND fact.hasBird.flg=1");

        assertQueryResult(list(19011, 19012, 19014, 19016, 19019), "fact.hasPet.flg=1 OR fact.hasBird.flg=1");

        assertQueryResult(list(19011, 19012, 19016, 19019), "fact.hasPet.flg=1 AND fact.hasBird.flg!=1");

    }

    @Test
    void testExample5() throws IOException {
        initTestServerWithHybridMapping(EmbeddedSolrServerUtils.createHybridMappingConfigProfileAndFacts());

        assertQueryResult(list(19011, 19012), "fact.contactTime.ts < 2024-09-01 AND fact.hasPet.flg=1");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19016, 19019), "fact.contactTime.ts > 2024-11-13 OR fact.hasPet.flg=1");

    }

    @Test
    void testExample6() throws IOException {

        initTestServerWithHybridMapping(EmbeddedSolrServerUtils.createHybridMappingConfigProfileAndPos());

        assertThrowsErrorCode(CommonErrors.ERR_2101_REFERENCE_MATCH_NOT_SUPPORTED,
                () -> assertQueryResult(list(19020), "pos.country=@home-country and pos.date > 2024-04-01"));

        assertQueryResult(list(19014), "home-country=Germany AND pos.date > 2024-04-01");

        assertQueryResult(list(19011, 19012, 19013, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "NOT (home-country=Germany AND pos.date > 2024-04-01)");

        assertQueryResult(list(19011, 19012, 19014, 19015), "pos.date > 2024-04-01");

        assertQueryResult(list(19017), "pos.name = CORNFLAKES AND pos.date=2024-03-15 AND pos.anyDate=2024-03-14");

        assertQueryResult(list(19011, 19013, 19015, 19017), "pos.quantity > @pos.unitPrice");

        assertQueryResult(list(19011, 19015), "pos.quantity > @pos.unitPrice AND pos.date > 2024-04-01");

        assertQueryResult(list(19012, 19013, 19014, 19016, 19017, 19018, 19019, 19020, 19021), "NOT (pos.quantity > @pos.unitPrice AND pos.date > 2024-04-01)");

        // 19016, 19018, 19019, 19020, 19021 don't have POS-data
        assertQueryResult(list(19012, 19013, 19014, 19017), "STRICT NOT (pos.quantity > @pos.unitPrice AND pos.date > 2024-04-01)");

        assertQueryResult(list(19015, 19017),
                "(pos.quantity > @pos.unitPrice AND pos.date=2024-03-14 AND pos.anyDate=2024-03-15) OR pos.name=\"MINTY MEATBALLS 1KG\"");

    }

    @Test
    void testExample7() throws IOException {

        initTestServerWithHybridMapping();

        assertEquals(list(19012, 19013, 19018), selectIds("q.monthlyIncome.int > 5000", withTenant(17)));

        assertEquals(list(), selectIds("q.monthlyIncome.int > 5000", withTenant(77)));

    }

    @Test
    void testExample8() throws IOException {
        initTestServerWithHybridMapping();

        assertQueryResult(list(19014, 19021), "sizeCM between (185, 195)");
        assertQueryResult(list(19011, 19013, 19021), "bodyTempCelsius between (38, 39.1)");
        assertQueryResult(list(19011, 19013, 19015, 19017, 19021), "clubMember=1");

        assertQueryResult(list(19011, 19013), "anniverseryDate between (2024-09-20, 2024-09-25)");

        assertQueryResult(list(19011, 19012, 19014), "fact.contactTime.ts between (2024-10-21, 2024-12-19)");

        showExecution("fact.contactTime.ts between (2024-10-21, 2024-12-19)");

    }

}
