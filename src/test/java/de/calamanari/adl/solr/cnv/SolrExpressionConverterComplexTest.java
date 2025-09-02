//@formatter:off
/*
 * SolrExpressionConverterComplexTest
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.calamanari.adl.CommonErrors;
import de.calamanari.adl.solr.EmbeddedSolrServerUtils;
import de.calamanari.adl.solr.SolrTestBase;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class SolrExpressionConverterComplexTest extends SolrTestBase {

    static final Logger LOGGER = LoggerFactory.getLogger(SolrExpressionConverterComplexTest.class);

    @BeforeAll
    static void prepareEmbeddedSolr() throws IOException {
        initTestServerWithHybridMapping();
    }

    @Test
    void testMultiConditionSingleDocument() {

        assertQueryResult(list(19011, 19013), "provider = LOGMOTH AND home-country = USA");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016, 19019, 19020, 19021), "provider = LOGMOTH OR home-country = USA");
        assertQueryResult(list(19012, 19015, 19020), "(provider = LOGMOTH OR home-country = USA) AND gender=male");
        assertQueryResult(list(19011, 19013, 19014, 19016), "(provider = LOGMOTH OR home-country = USA) AND (sCode any of (11, 17) OR bState IS NOT UNKNOWN)");

        assertQueryResult(list(19011), "fact.hasDog.flg=1 AND fact.hasCat.flg=0");
        assertQueryResult(list(19011, 19016), "fact.hasDog.flg=1 AND fact.hasCat.flg != 1");

        assertQueryResult(list(19012, 19013, 19018), "q.monthlyIncome.int > 4000 AND q.martialStatus.str != married");
        assertQueryResult(list(19012, 19013, 19017, 19018), "(q.monthlyIncome.int > 4000 AND q.martialStatus.str != married) OR q.children.int > 1");
        assertQueryResult(list(19011, 19012, 19013, 19018, 19020),
                "(q.monthlyIncome.int > 5000 AND q.martialStatus.str != married) OR (q.vegan.flg != 1 AND q.foodPref.str contains any of (fish, thai))");

        assertQueryResult(list(19012, 19013, 19017), "pos.name contains any of (MELON, PUMPKIN, CHEESE) AND pos.date > 2024-03-15");

        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19017), "STRICT NOT pos.quantity > 2");

        assertQueryResult(list(19012, 19013, 19017), "(pos.name contains any of (MELON, PUMPKIN, CHEESE) AND pos.date > 2024-03-15)");

        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19017),
                "(pos.name contains any of (MELON, PUMPKIN, CHEESE) AND pos.date > 2024-03-15) OR STRICT NOT pos.quantity > 2");

        assertQueryResult(list(19012, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021),
                "(pos.name contains any of (MELON, PUMPKIN, CHEESE) AND pos.date > 2024-03-15) OR NOT pos.quantity > 2");

        assertQueryResult(list(19013, 19015, 19017, 19021), "(clubMember = 1 OR hobbies=origami) AND sports!=tennis");
        assertQueryResult(list(19017, 19021), "(clubMember = 1 OR hobbies=origami) AND STRICT sports!=tennis");

    }

    @Test
    void testMultiConditionMultiDocuments() {

        assertQueryResult(list(19011), "provider = LOGMOTH AND home-country = USA AND fact.hasDog.flg=1");

        assertQueryResult(list(19011, 19014, 19016),
                "(provider = LOGMOTH AND home-country = USA AND fact.hasDog.flg=1 AND q.monthlySpending.int >= 5000) OR clubMember=0");

        assertQueryResult(list(19011, 19012, 19014, 19016, 19018, 19019, 19020),
                "(provider = LOGMOTH AND home-country = USA AND fact.hasDog.flg=1 AND q.monthlySpending.int >= 5000) OR NOT clubMember=1");

        assertQueryResult(list(19012), """
                        ((provider = LOGMOTH
                               AND home-country = USA
                               AND fact.hasDog.flg=1
                               AND q.monthlySpending.int >= 5000)
                            OR NOT clubMember=1
                            )
                        AND pos.name contains any of (MELON, PUMPKIN, CHEESE) AND pos.date > 2024-03-15
                """);

        assertQueryResult(list(19015, 19016, 19019, 19020),
                "(fact.hasPet.flg=1 AND home-city strict not any of (Paris, Berlin)) OR (fact.hasPet.flg=0 AND STRICT home-city != Karlsruhe)");

        assertQueryResult(list(19011),
                "q.monthlyIncome.int > 4500 AND pos.anyDate = 2024-03-21 AND (q.monthlySpending.int = 5000 OR (q.martialStatus.str=married AND (q.children.int=1 OR q.favColor.str=blue)))");

    }

    @Test
    void testReferenceMatchSingleDocument() {

        assertQueryResult(list(19013, 19017), "sCode > @tntCode AND nCode < @biCode");

        assertThrowsErrorCode(CommonErrors.ERR_2101_REFERENCE_MATCH_NOT_SUPPORTED,
                () -> assertQueryResult(list(19011), "fact.homeCity.str != @fact.contactCode.str AND STRICT fact.hasDog.flg != @fact.hasBird.flg"));

        assertThrowsErrorCode(CommonErrors.ERR_2101_REFERENCE_MATCH_NOT_SUPPORTED,
                () -> assertQueryResult(list(19011), "NOT q.monthlySpending.int > @q.monthlyIncome.int AND q.carColor.str = @q.favColor.str"));

        assertQueryResult(list(19012, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "NOT q.monthlySpending.int > @q.monthlyIncome.int");
        assertQueryResult(list(19012, 19013, 19018), "STRICT NOT q.monthlySpending.int > @q.monthlyIncome.int");

        assertQueryResult(list(19012, 19013, 19018, 19021), "q.monthlySpending.int < @q.monthlyIncome.int OR q.carOwner.flg = @q.vegetarian.flg");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19020),
                "q.monthlySpending.int < @q.monthlyIncome.int OR q.carOwner.flg != @q.vegetarian.flg");
        assertQueryResult(list(19011, 19012, 19013, 19018), "q.monthlySpending.int < @q.monthlyIncome.int OR STRICT q.carOwner.flg != @q.vegetarian.flg");

        assertQueryResult(list(19011, 19013, 19015, 19017), "pos.quantity > @pos.unitPrice");
        assertQueryResult(list(19012, 19014, 19016, 19018, 19019, 19020, 19021), "NOT pos.quantity > @pos.unitPrice");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19017), "STRICT NOT pos.quantity > @pos.unitPrice");

        assertQueryResult(list(19012, 19013), "bodyTempCelsius < @sizeCM and hobbies any of (origami, football, tennis)");

        assertQueryResult(list(19018), "STRICT home-country != @provider and home-city=Paris");

        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "upd1 = @upd2");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "upd1 <= @upd2");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "upd1 >= @upd2");

        assertQueryResult(list(), "upd1 != @upd2");

        assertQueryResult(list(), "upd1 > @upd2");

        assertQueryResult(list(), "upd1 < @upd2");

    }

    @Test
    void testReferenceMatchMultiDocument() {

        assertThrowsErrorCode(CommonErrors.ERR_2101_REFERENCE_MATCH_NOT_SUPPORTED, () -> assertQueryResult(list(19011), "provider = @fact.provider"));

        assertQueryResult(list(19011, 19018), "bState = @q.carOwner.flg");

        assertQueryResult(list(19012, 19013, 19014, 19015, 19016, 19017, 19019, 19020, 19021), "bState != @q.carOwner.flg");

        assertQueryResult(list(19013), "STRICT bState != @q.carOwner.flg");

    }

    @Test
    void testComplexNesting() {

        assertQueryResult(list(19011, 19013, 19016, 19021), "(home-country=USA AND q.carOwner.flg=1) "
                + "OR (provider=LOGMOTH AND (fact.hasDog.flg=1 OR fact.hasCat.flg=0)) "
                + "OR (demCode=7 AND (fact.yearOfBirth.int=2000 OR fact.dateOfBirth.dt=2000-03-05 OR (q.martialStatus.str=married AND q.monthlyIncome.int=4600)))");

        assertQueryResult(list(19011), "(home-country=USA AND q.carOwner.flg=1 AND provider=LOGMOTH AND (fact.hasDog.flg=1 OR fact.hasCat.flg=0)) "
                + "OR (demCode=7 AND (fact.yearOfBirth.int=2000 OR fact.dateOfBirth.dt=2000-03-05 OR (q.martialStatus.str=married AND q.monthlyIncome.int=4600)))");

    }

    @Test
    void testDefinitionLayout1() throws IOException {
        assertQueryDef("""
                <<<[

                node_type:profile
                AND (
                    (
                        bstate:FALSE
                        AND {!join from=main_id to=id v="\\(node_type\\:survey\\ AND\\ tenant\\:17\\)\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ AND\\ srv_carowner_b\\:FALSE"}
                    )
                    OR (
                        bstate:TRUE
                        AND {!join from=main_id to=id v="\\(node_type\\:survey\\ AND\\ tenant\\:17\\)\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ AND\\ srv_carowner_b\\:TRUE"}
                    )
                )

                ]>>>""", "bState = @q.carOwner.flg");

        assertQueryDef("""
                <<<[

                node_type:profile
                AND (
                    (
                        node_type:profile
                        AND NOT bstate:*
                    )
                    OR (
                        node_type:profile
                        AND NOT {!join from=main_id to=id v="\\(node_type\\:survey\\ AND\\ tenant\\:17\\)\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ AND\\ srv_carowner_b\\:\\*"}
                    )
                    OR (
                        bstate:FALSE
                        AND {!join from=main_id to=id v="\\(node_type\\:survey\\ AND\\ tenant\\:17\\)\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ AND\\ srv_carowner_b\\:TRUE"}
                    )
                    OR (
                        bstate:TRUE
                        AND {!join from=main_id to=id v="\\(node_type\\:survey\\ AND\\ tenant\\:17\\)\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ AND\\ srv_carowner_b\\:FALSE"}
                    )
                )

                ]>>>""", "bState != @q.carOwner.flg");

        assertQueryDef("""
                <<<[

                node_type:profile
                AND (
                    (
                        bstate:FALSE
                        AND {!join from=main_id to=id v="\\(node_type\\:survey\\ AND\\ tenant\\:17\\)\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ AND\\ srv_carowner_b\\:TRUE"}
                    )
                    OR (
                        bstate:TRUE
                        AND {!join from=main_id to=id v="\\(node_type\\:survey\\ AND\\ tenant\\:17\\)\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ AND\\ srv_carowner_b\\:FALSE"}
                    )
                )

                ]>>>""", "STRICT bState != @q.carOwner.flg");

        assertQueryDef("""
                <<<[

                node_type:profile
                AND {!join from=main_id to=id v="node_type\\:pos\\
                \\ \\ \\ \\ AND\\ pos_invdate_dt\\:\\[2024\\\\\\-03\\\\\\-17T00\\\\\\:00\\\\\\:00Z\\ TO\\ 2024\\\\\\-03\\\\\\-18T00\\\\\\:00\\\\\\:00Z\\}\\
                \\ \\ \\ \\ AND\\ pos_invdate_dt\\:\\[2024\\\\\\-03\\\\\\-21T00\\\\\\:00\\\\\\:00Z\\ TO\\ 2024\\\\\\-03\\\\\\-22T00\\\\\\:00\\\\\\:00Z\\}\\
                \\ \\ \\ \\ AND\\ pos_quantity_i\\:\\{1\\ TO\\ \\*\\]"}

                ]>>>""", "pos.date = 2024-03-21 and pos.date = 2024-03-17 and pos.quantity > 1");

        initTestServerWithHybridMapping(EmbeddedSolrServerUtils.createHybridMappingConfigMakePosDataAllMultiDoc());

        assertQueryDef("""
                <<<[

                node_type:profile
                AND {!join from=main_id to=id v="node_type\\:pos\\
                \\ \\ \\ \\ AND\\ pos_invdate_dt\\:\\[2024\\\\\\-03\\\\\\-17T00\\\\\\:00\\\\\\:00Z\\ TO\\ 2024\\\\\\-03\\\\\\-18T00\\\\\\:00\\\\\\:00Z\\}"}

                node_type:profile
                AND {!join from=main_id to=id v="node_type\\:pos\\
                \\ \\ \\ \\ AND\\ pos_invdate_dt\\:\\[2024\\\\\\-03\\\\\\-21T00\\\\\\:00\\\\\\:00Z\\ TO\\ 2024\\\\\\-03\\\\\\-22T00\\\\\\:00\\\\\\:00Z\\}"}

                node_type:profile
                AND {!join from=main_id to=id v="node_type\\:pos\\
                \\ \\ \\ \\ AND\\ pos_quantity_i\\:\\{1\\ TO\\ \\*\\]"}

                ]>>>""", "pos.date = 2024-03-21 and pos.date = 2024-03-17 and pos.quantity > 1");

        initTestServerWithHybridMapping();

        assertQueryDef("""
                <<<[

                node_type:profile
                AND {!join from=main_id to=id v="node_type\\:pos\\
                \\ \\ \\ \\ AND\\ pos_invdate_dt\\:\\[2024\\\\\\-03\\\\\\-17T00\\\\\\:00\\\\\\:00Z\\ TO\\ 2024\\\\\\-03\\\\\\-18T00\\\\\\:00\\\\\\:00Z\\}\\
                \\ \\ \\ \\ AND\\ pos_quantity_i\\:\\{1\\ TO\\ \\*\\]"}
                AND {!join from=main_id to=id v="node_type\\:pos\\
                \\ \\ \\ \\ AND\\ pos_invdate_dt\\:\\[2024\\\\\\-03\\\\\\-21T00\\\\\\:00\\\\\\:00Z\\ TO\\ 2024\\\\\\-03\\\\\\-22T00\\\\\\:00\\\\\\:00Z\\}"}

                ]>>>""", "pos.anyDate = 2024-03-21 and pos.date = 2024-03-17 and pos.quantity > 1");

        assertQueryDef("""
                <<<[

                node_type:profile
                AND {!join from=main_id to=id v="\\(node_type\\:survey\\ AND\\ tenant\\:17\\)\\
                \\ \\ \\ \\ AND\\ \\(\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\(\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\(\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ srv_children_i\\:1\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ OR\\ srv_favcolor_ss\\:blue\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\)\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ AND\\ srv_martialstatus_s\\:married\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\)\\
                \\ \\ \\ \\ \\ \\ \\ \\ OR\\ srv_monthlyspending_i\\:5000\\
                \\ \\ \\ \\ \\)\\
                \\ \\ \\ \\ AND\\ srv_monthlyincome_i\\:\\{4500\\ TO\\ \\*\\]"}

                node_type:profile
                AND {!join from=main_id to=id v="node_type\\:pos\\
                \\ \\ \\ \\ AND\\ pos_invdate_dt\\:\\[2024\\\\\\-03\\\\\\-21T00\\\\\\:00\\\\\\:00Z\\ TO\\ 2024\\\\\\-03\\\\\\-22T00\\\\\\:00\\\\\\:00Z\\}"}

                ]>>>""",
                "q.monthlyIncome.int > 4500 AND pos.anyDate = 2024-03-21 AND (q.monthlySpending.int = 5000 OR (q.martialStatus.str=married AND (q.children.int=1 OR q.favColor.str=blue)))");

    }

    @Test
    void testDefinitionLayout2() {
        assertQueryDef("""
                <<<[

                node_type:profile
                AND country:USA

                node_type:profile
                AND provider:LOGMOTH

                ]>>>""", "provider = LOGMOTH AND home-country = USA");

        assertQueryDef("""
                <<<[

                node_type:profile
                AND (
                    country:USA
                    OR provider:LOGMOTH
                )

                ]>>>""", "provider = LOGMOTH OR home-country = USA");

        assertQueryDef("""
                <<<[

                node_type:profile
                AND (
                    country:USA
                    OR provider:LOGMOTH
                )

                node_type:profile
                AND gender:male

                ]>>>""", "(provider = LOGMOTH OR home-country = USA) AND gender=male");

        assertQueryDef("""
                <<<[

                node_type:profile
                AND (
                    bstate:*
                    OR scode:(11 OR 17)
                )

                node_type:profile
                AND (
                    country:USA
                    OR provider:LOGMOTH
                )

                ]>>>""", "(provider = LOGMOTH OR home-country = USA) AND (sCode any of (11, 17) OR bState IS NOT UNKNOWN)");

        assertQueryDef("""
                <<<[

                node_type:profile
                AND {!parent which="node_type:profile" v="node_type\\:fact\\
                \\ \\ \\ \\ AND\\ fct_hascat_b\\:FALSE"}

                node_type:profile
                AND {!parent which="node_type:profile" v="node_type\\:fact\\
                \\ \\ \\ \\ AND\\ fct_hasdog_b\\:TRUE"}

                ]>>>""", "fact.hasDog.flg=1 AND fact.hasCat.flg=0");

        assertQueryDef("""
                <<<[

                node_type:profile
                AND NOT {!parent which="node_type:profile" v="node_type\\:fact\\
                \\ \\ \\ \\ AND\\ fct_hascat_b\\:TRUE"}

                node_type:profile
                AND {!parent which="node_type:profile" v="node_type\\:fact\\
                \\ \\ \\ \\ AND\\ fct_hasdog_b\\:TRUE"}

                ]>>>""", "fact.hasDog.flg=1 AND fact.hasCat.flg != 1");

        assertQueryDef("""
                <<<[

                node_type:profile
                AND {!join from=main_id to=id v="\\(node_type\\:survey\\ AND\\ tenant\\:17\\)\\
                \\ \\ \\ \\ AND\\ srv_monthlyincome_i\\:\\{4000\\ TO\\ \\*\\]\\
                \\ \\ \\ \\ AND\\ NOT\\ srv_martialstatus_s\\:married"}

                ]>>>""", "q.monthlyIncome.int > 4000 AND q.martialStatus.str != married");

        assertQueryDef("""
                <<<[

                node_type:profile
                AND {!join from=main_id to=id v="\\(node_type\\:survey\\ AND\\ tenant\\:17\\)\\
                \\ \\ \\ \\ AND\\ \\(\\
                \\ \\ \\ \\ \\ \\ \\ \\ srv_children_i\\:\\{1\\ TO\\ \\*\\]\\
                \\ \\ \\ \\ \\ \\ \\ \\ OR\\ \\(\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ srv_monthlyincome_i\\:\\{4000\\ TO\\ \\*\\]\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ AND\\ NOT\\ srv_martialstatus_s\\:married\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\)\\
                \\ \\ \\ \\ \\)"}

                ]>>>""", "(q.monthlyIncome.int > 4000 AND q.martialStatus.str != married) OR q.children.int > 1");

        assertQueryDef("""
                <<<[

                node_type:profile
                AND {!join from=main_id to=id v="\\(node_type\\:survey\\ AND\\ tenant\\:17\\)\\
                \\ \\ \\ \\ AND\\ \\(\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\(\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\(\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ srv_foodpref_s\\:\\*fish\\*\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ OR\\ srv_foodpref_s\\:\\*thai\\*\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\)\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ AND\\ NOT\\ srv_vegan_b\\:TRUE\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\)\\
                \\ \\ \\ \\ \\ \\ \\ \\ OR\\ \\(\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ srv_monthlyincome_i\\:\\{5000\\ TO\\ \\*\\]\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ \\ AND\\ NOT\\ srv_martialstatus_s\\:married\\
                \\ \\ \\ \\ \\ \\ \\ \\ \\)\\
                \\ \\ \\ \\ \\)"}

                ]>>>""",
                "(q.monthlyIncome.int > 5000 AND q.martialStatus.str != married) OR (q.vegan.flg != 1 AND q.foodPref.str contains any of (fish, thai))");

        assertQueryDef("""
                <<<[

                node_type:profile
                AND {!join from=main_id to=id v="node_type\\:pos\\
                \\ \\ \\ \\ AND\\ \\(\\
                \\ \\ \\ \\ \\ \\ \\ \\ pos_description_s\\:\\*CHEESE\\*\\
                \\ \\ \\ \\ \\ \\ \\ \\ OR\\ pos_description_s\\:\\*MELON\\*\\
                \\ \\ \\ \\ \\ \\ \\ \\ OR\\ pos_description_s\\:\\*PUMPKIN\\*\\
                \\ \\ \\ \\ \\)\\
                \\ \\ \\ \\ AND\\ pos_invdate_dt\\:\\[2024\\\\\\-03\\\\\\-16T00\\\\\\:00\\\\\\:00Z\\ TO\\ \\*\\]"}

                ]>>>""", "pos.name contains any of (MELON, PUMPKIN, CHEESE) AND pos.date > 2024-03-15");

        assertQueryDef("""
                <<<[

                node_type:profile
                AND {!join from=main_id to=id v="node_type\\:pos\\
                \\ \\ \\ \\ AND\\ \\(\\
                \\ \\ \\ \\ \\ \\ \\ \\ pos_description_s\\:\\*CHEESE\\*\\
                \\ \\ \\ \\ \\ \\ \\ \\ OR\\ pos_description_s\\:\\*MELON\\*\\
                \\ \\ \\ \\ \\ \\ \\ \\ OR\\ pos_description_s\\:\\*PUMPKIN\\*\\
                \\ \\ \\ \\ \\)"}
                AND {!join from=main_id to=id v="node_type\\:pos\\
                \\ \\ \\ \\ AND\\ pos_invdate_dt\\:\\[2024\\\\\\-03\\\\\\-16T00\\\\\\:00\\\\\\:00Z\\ TO\\ \\*\\]"}

                ]>>>""", "pos.name contains any of (MELON, PUMPKIN, CHEESE) AND pos.anyDate > 2024-03-15");

        assertQueryDef("""
                <<<[

                node_type:profile
                AND (
                    flt_clubmember_b:TRUE
                    OR flt_hobbies_ss:origami
                )

                node_type:profile
                AND NOT flt_sports_ss:tennis

                ]>>>""", "(clubMember = 1 OR hobbies=origami) AND sports!=tennis");

        assertQueryDef("""
                <<<[

                node_type:profile
                AND (
                    flt_clubmember_b:TRUE
                    OR flt_hobbies_ss:origami
                )

                node_type:profile
                AND NOT flt_sports_ss:tennis

                node_type:profile
                AND flt_sports_ss:*

                ]>>>""", "(clubMember = 1 OR hobbies=origami) AND STRICT sports!=tennis");

    }

    @Test
    void testBetweenCase() {
        assertQueryDef("""
                <<<[

                {!join from=main_id to=id v="\\(node_type\\:survey\\ AND\\ tenant\\:17\\)\\
                \\ \\ \\ \\ AND\\ srv_monthlyincome_i\\:\\{4500\\ TO\\ 7000\\}"}

                ]>>>""", "q.monthlyIncome.int > 4500 AND q.monthlyIncome.int < 7000");

    }

    @Test
    void testMultiDocEffect() throws IOException {

        assertQueryResult(list(19013), "pos.date = 2024-03-21 and pos.quantity > 1");

        // what we see here is that the multi-doc property virtually decouples the "any" invoice date from the document
        // So, the same query returns now all records that have ANY entry with the given date and ANY entry with the remaining attributes.
        assertQueryResult(list(19011, 19013, 19014), "pos.anyDate = 2024-03-21 and pos.quantity > 1");

        // A document with multiple fields should only be marked multi-row if the attributes are unrelated,
        // or you give a clear explanation to the user
        // The following query does not make any sense on regular data
        assertQueryResult(list(), "pos.date = 2024-03-21 and pos.date = 2024-03-17 and pos.quantity > 1");

        initTestServerWithHybridMapping(EmbeddedSolrServerUtils.createHybridMappingConfigMakePosDataAllMultiDoc());

        // When we mark the pos.date as multi-doc, it returns a record
        // This record has indeed "any document" matching with the first pos.date and the second one, and there is "any document"
        // that matches the remaining condition "pos.quantity > 1 and pos.country=USA" - confusing!
        assertQueryResult(list(19011), "pos.date = 2024-03-21 and pos.date = 2024-03-17 and pos.quantity > 1 and pos.country=USA");

        initTestServerWithHybridMapping();

        // By mapping an explicit extra attribute (pos.anyDate) you can achieve more clarity here
        assertQueryResult(list(19013), "pos.anyDate = 2024-03-15 and pos.date = 2024-03-21 and pos.quantity > 1");

        // Pitfall: There is still no connection between the conditions because pos.anyDate is multi-doc
        // The query below returns also cases where the (pos.quantity > 1 and pos.country=USA) is not met on that particular date but on any other!
        assertQueryResult(list(19013, 19017), "pos.anyDate = 2024-03-15 and pos.quantity > 1");

        assertQueryResult(list(19011, 19012, 19014, 19015, 19016, 19018, 19019, 19020, 19021), "NOT (pos.anyDate = 2024-03-15 and pos.quantity > 1)");

        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19017), "STRICT NOT (pos.anyDate = 2024-03-15 and pos.quantity > 1)");

    }

    @Test
    void testBug() {

        showExecution("pos.name contains POTATO AND ( pos.unitPrice = 4.89 OR STRICT pos.quantity != 1)");
        assertQueryResult(list(19011), "pos.name contains POTATO AND ( pos.unitPrice = 4.89 OR pos.quantity != 1)");
        assertQueryResult(list(19011), "pos.name contains POTATO AND ( pos.unitPrice = 4.89 OR STRICT pos.quantity != 1)");

        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19017), "STRICT NOT pos.quantity > 2");
        assertQueryResult(list(19012, 19014, 19015, 19016, 19018, 19019, 19020, 19021), "NOT pos.quantity > 2");

        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19017),
                "(pos.name contains any of (MELON, PUMPKIN, CHEESE) AND pos.date > 2024-03-15) OR STRICT NOT pos.quantity > 2");

        assertQueryResult(list(19013), "pos.unitPrice = 4.89 AND pos.quantity != 1");
        assertQueryResult(list(19013), "pos.unitPrice = 4.89 AND STRICT pos.quantity != 1");

        assertQueryResult(list(19013, 19016, 19018, 19019, 19020, 19021), "pos.unitPrice = 4.89 OR pos.quantity != 1");
        assertQueryResult(list(19011, 19013, 19014, 19015, 19017), "pos.unitPrice = 4.89 OR STRICT pos.quantity != 1");
        assertQueryResult(list(19011, 19013, 19014, 19015, 19017), "STRICT pos.quantity != 1");
        assertQueryResult(list(19016, 19018, 19019, 19020, 19021), "pos.quantity != 1");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19017), "pos.quantity = 1");

    }

    @Test
    void testIssue_1_NotAlwaysTranslatesToNotAny() {
        assertQueryResult(list(19013), "pos.unitPrice = 4.89 AND pos.quantity != 1");
        assertQueryResult(list(19013), "pos.unitPrice = 4.89 AND STRICT pos.quantity != 1");

        assertQueryResult(list(19013, 19016, 19018, 19019, 19020, 19021), "pos.unitPrice = 4.89 OR pos.quantity != 1");
        assertQueryResult(list(19011, 19013, 19014, 19015, 19017), "pos.unitPrice = 4.89 OR STRICT pos.quantity != 1");
        assertQueryResult(list(19011, 19013, 19014, 19015, 19017), "STRICT pos.quantity != 1");
        assertQueryResult(list(19016, 19018, 19019, 19020, 19021), "pos.quantity != 1");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19017), "pos.quantity = 1");

        assertQueryResult(list(19011, 19013, 19014, 19015, 19017), "STRICT pos.unitPrice != 4.89 AND STRICT pos.quantity != 1");

        assertQueryResult(list(19011, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "pos.unitPrice != 4.89 AND pos.quantity != 1");

        assertQueryResult(list(19011), "pos.name contains POTATO AND ( pos.unitPrice = 4.89 OR pos.quantity != 1)");
        assertQueryResult(list(19011), "pos.name contains POTATO AND ( pos.unitPrice = 4.89 OR STRICT pos.quantity != 1)");

        // "self-pinning" effect of STRICT NOT
        // all conditions get inlined, applied to the same document
        assertQueryResult(list(19011, 19013, 19014, 19015, 19017), "STRICT pos.unitPrice != 4.89 AND STRICT pos.quantity != 1");
        assertQueryResult(list(19011, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "pos.unitPrice != 4.89 AND pos.quantity != 1");
        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19017), "pos.unitPrice = 4.89 OR pos.quantity = 1");
        assertQueryResult(list(19011, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "NOT (pos.unitPrice = 4.89 OR pos.quantity = 1)");
        assertQueryResult(list(19011, 19013, 19014, 19015, 19017), "STRICT NOT (pos.unitPrice = 4.89 OR pos.quantity = 1)");

        showExecution("pos.unitPrice > 4 AND pos.unitPrice != @pos.quantity");

        showExecution("pos.unitPrice != 4.89 OR pos.quantity != 1");

    }

    @Test
    void testBug2() {
        assertQueryResult(list(19012), "pos.unitPrice = 4.99 AND pos.quantity = 1");
        assertQueryResult(list(19011, 19013, 19014, 19015, 19016, 19017, 19018, 19019, 19020, 19021), "NOT (pos.unitPrice = 4.99 AND pos.quantity = 1)");
    }

    @Test
    void testIssue_2_DefaultNegationCausesRedundantQueryPart() {
        assertQueryDef("""
                <<<[

                (
                    node_type:profile
                    AND NOT {!join from=main_id to=id v="node_type\\:pos\\
                \\ \\ \\ \\ \\ \\ \\ \\ AND\\ pos_quantity_i\\:1"}
                )

                ]>>>""", "pos.quantity != 1");

        assertQueryResult(list(19016, 19018, 19019, 19020, 19021), "pos.quantity != 1");

        assertQueryResult(list(19012, 19016, 19018, 19019, 19020, 19021), "pos.quantity != 2");

        assertQueryDef("""
                <<<[

                (
                    node_type:profile
                    AND {!join from=main_id to=id v="node_type\\:pos\\
                \\ \\ \\ \\ \\ \\ \\ \\ AND\\ pos_quantity_i\\:\\*\\
                \\ \\ \\ \\ \\ \\ \\ \\ AND\\ NOT\\ pos_quantity_i\\:1"}
                )

                ]>>>""", "STRICT pos.quantity != 1");

        assertQueryResult(list(19011, 19013, 19014, 19015, 19017), "STRICT pos.quantity != 1");

        assertQueryResult(list(19011, 19012, 19013, 19014, 19015, 19017), "STRICT pos.quantity != 2");

    }

}
