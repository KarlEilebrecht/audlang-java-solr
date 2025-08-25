//@formatter:off
/*
 * EmbeddedSolrServerUtilsTest
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

import static de.calamanari.adl.solr.cnv.ConversionTestUtils.toDebugString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class EmbeddedSolrServerUtilsTest extends SolrTestBase {

    static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedSolrServerUtilsTest.class);

    @BeforeAll
    static void before() throws IOException {
        initTestSolrServer("audlang-data-nested.json");
    }

    @Test
    void testSetupAndFeedAudlang() {

        // returns only profile documents, that's by design
        assertEquals(11, EmbeddedSolrServerUtils.queryCount(testServer, "*:*"));

        assertEquals(11, EmbeddedSolrServerUtils.queryCount(testServer, "node_type:profile"));

        SolrDocumentList results = EmbeddedSolrServerUtils.queryDocs(testServer, "node_type:profile");

        List<Integer> ids = results.stream().map(e -> e.getFieldValue("id")).map(String::valueOf).map(Integer::valueOf)
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.sort(ids);

        assertEquals(ids, EmbeddedSolrServerUtils.queryIntIdsSorted(testServer, "node_type:profile"));

    }

    @Test
    void testReInit() throws IOException {
        initTestSolrServer("audlang-data-nested.json");
        assertEquals(11, EmbeddedSolrServerUtils.queryCount(testServer, "*:*"));

        SolrDocumentList results = EmbeddedSolrServerUtils.queryDocs(testServer, "node_type:(*ofile*)");

        assertNotEquals("[]", toDebugString(results));

    }

    @Test
    void testSubq() throws IOException {
        // SolrDocumentList results = EmbeddedSolrServerUtils.queryDocs(testServer,
        // "({!parent which=\"node_type:profile\" v=\"(node_type:fact AND fct_contacttime_dts:2023\\\\-12\\\\-24T19\\\\:36\\\\:12Z)\"} OR {!parent
        // which=\"node_type:profile AND bstate:FALSE\"})");

        SolrDocumentList results = EmbeddedSolrServerUtils.queryDocs(testServer, "updtime:2024\\-09\\-24T17\\:38\\:21Z");

        // SolrDocumentList results = EmbeddedSolrServerUtils.queryDocs(testServer,
        // "({!parent which=\"node_type:profile\" v=\"node_type:fact AND fct_haspet_b:TRUE\"} AND country:USA)");

        // SolrDocumentList results = EmbeddedSolrServerUtils.queryDocs(testServer,
        // "({!parent which=\"node_type:profile\" v=\"node_type:survey AND srv_foodpref_s:\\*ish,\\\\ mediterrainean\"} AND country:USA)");

        // "{!parent which=\"node_type:profile AND bstate:FALSE\"}");

        LOGGER.info("\n{}", toDebugString(results));

        assertNotEquals("[]", toDebugString(results));

    }

    @Test
    void testSubq2() throws IOException {
        SolrQuery solrQuery = new SolrQuery();

        solrQuery.setQuery("node_type:profile");
        // solrQuery.setQuery("*:*");
        // solrQuery.setFilterQueries("({!parent which=\"node_type:profile\" v=\"node_type:fact AND fct_haspet_b:(TRUE OR FALSE)\"} AND country:USA)");

        // alles escapen!!
        // solrQuery.setFilterQueries(
        // "({!parent which=\"node_type\\:profile\" v=\"node_type\\:survey AND srv_foodpref_s\\:\\*ish,\\\\ mediterrainean\"} AND country:USA)");

        solrQuery.setFilterQueries("""
                (
                    {!parent which=\"node_type\\:profile\"
                        v=\"node_type\\:survey
                        AND srv_gender_s\\:female\"
                    }
                    AND NOT {!parent which=\"node_type\\:profile\"
                        v=\"node_type\\:pos\\
                        AND pos_code_s\\:555BC\"
                    }
                    AND country:USA
                )
                """);

        // solrQuery.setFilterQueries("({!parent which=\"node_type:profile\" v=\"node_type\\:survey AND srv_gender_s\\:female\"} AND country:USA)");

        // solrQuery.setFields("id", "survey_data", "tenant", "[child]");
        solrQuery.setFields("*", "[child]");

        solrQuery.setRows(EmbeddedSolrServerUtils.MAX_RETURNED_DOCS);
        solrQuery.setSort("id", ORDER.desc);

        try {
            QueryResponse response = testServer.query(solrQuery);

            LOGGER.info("\n{}", toDebugString(response.getResults()));

            LOGGER.info("\n\n{}", response.getResults().getNumFound());
        }
        catch (SolrServerException | IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    // {!frange l=1 u=1}if(gt(popularity_int_1,popularity_int_2),1,0)

    @Test
    void testCmpQuery3() throws IOException {
        SolrQuery solrQuery = new SolrQuery();

        SolrQuery solrQueryBKP = new SolrQuery();

        solrQuery.setQuery("node_type:profile");
        // solrQuery.setQuery("*:*");
        // solrQuery.setFilterQueries("({!parent which=\"node_type:profile\" v=\"node_type:fact AND fct_haspet_b:(TRUE OR FALSE)\"} AND country:USA)");

        // alles escapen!!
        // solrQuery.setFilterQueries(
        // "({!parent which=\"node_type\\:profile\" v=\"node_type\\:survey\\ AND\\ srv_foodpref_s\\:\\*ish,\\\\\\ mediterrainean\"} AND country:USA)");

        // solrQuery.setFilterQueries("({!parent which=\"node_type\\:profile\" v=\"node_type\\:survey\\ AND\\ NOT srv_foodpref_s\\:*\"})");

        // \\ AND\\ \\{\\!frange\\ l=1\\ u=1\\}if\\(gt\\(srv_monthlyincome_l,srv_monthlyspending_l\\),1,0\\)

        // solrQuery.setFilterQueries(
        // "{!parent which=\"node_type\\:profile\" v=\"node_type\\:survey\\ AND\\ \\{\\!frange\\ l=1\\
        // u=1\\}if\\(gt\\(srv_monthlyincome_l,srv_monthlyspending_l\\),1,0\\)\"}");

        // srv_monthlyincome_l
        // srv_monthlyspending_l

        solrQueryBKP.setFilterQueries("""
                (({!parent which="node_type\\:profile"
                    v="\\{\\!frange\\ l=1\\ u=1\\}
                    if\\(
                            and\\(gt\\(srv_monthlyincome_l,srv_monthlyspending_l\\),eq\\(node_type,\\"survey\\"\\)\\),1,0\\)"
                 }
                  OR NOT
                 {!parent which="node_type\\:profile"
                    v="node_type\\:survey"
                 }
                 )
                AND country:USA)
                """);

        solrQueryBKP.setFilterQueries("""
                (node_type:profile AND provider:LOGMOTH)
                AND (
                {!parent which="node_type\\:profile"
                    v="\\{\\!frange\\ l=1\\ u=1\\}
                    if\\(
                            and\\(gt\\(srv_monthlyincome_l,srv_monthlyspending_l\\),eq\\(node_type,\\"survey\\"\\)\\),1,0\\)"
                }
                OR
                (
                (node_type:profile AND provider:LOGMOTH)
                AND
                NOT
                 {!parent which="node_type\\:profile"
                    v="node_type\\:survey"
                 }))
                """);

        solrQueryBKP.setFilterQueries(
                """
                        (node_type:profile AND provider:LOGMOTH)
                        AND (
                        {!parent which="node_type\\:profile"
                            v="_query_\\:\\"\\\\\\{\\\\\\!frange l=1 u=1\\\\\\}if\\\\\\(gt\\\\\\(srv_monthlyincome_l,srv_monthlyspending_l\\\\\\),1,0\\\\\\)\\"\\ AND\\ srv_vegetarian_b\\:TRUE"
                        }
                        OR
                        (
                        (node_type:profile AND provider:LOGMOTH)
                        AND
                        NOT
                         {!parent which="node_type\\:profile"
                            v="node_type\\:survey"
                         }))
                        """);

        solrQueryBKP.setFilterQueries("""
                (node_type:profile AND country:UK) OR (node_type:profile AND NOT country:Germany)
                """);

        solrQueryBKP.setFilterQueries("_query_:\"{!frange l=1 u=1}if(and(exists(tntcode),gt(demcode,tntcode)),1,0)\" AND country:USA");

        solrQuery.setFilterQueries(
                "(_query_:\"{!frange l=1 u=1}if(and(exists(tntcode),gt(demcode,tntcode)),1,0)\" OR _query_:\"{!frange l=1 u=1}if(eq(id,\\\"19012\\\"),1,0)\")");

        // solrQuery.setFilterQueries("({!parent which=\"node_type:profile\" v=\"node_type\\:survey AND srv_gender_s\\:female\"} AND country:USA)");

        // solrQuery.setFields("id", "survey_data", "tenant", "[child]");
        solrQuery.setFields("*", "[child]");

        solrQuery.setRows(EmbeddedSolrServerUtils.MAX_RETURNED_DOCS);
        solrQuery.setSort("id", ORDER.desc);

        try

        {
            QueryResponse response = testServer.query(solrQuery);

            LOGGER.info("\n{}", toDebugString(response.getResults()));

            LOGGER.info("\n\n{}", response.getResults().getNumFound());
        }
        catch (SolrServerException | IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    @Test
    void testNoNesting() throws IOException {
        initTestSolrServer("audlang-data-join.json");

        SolrQuery solrQuery = new SolrQuery();

        solrQuery.setQuery("node_type:profile");

        solrQuery.setFilterQueries("({!join from=main_id to=id v=\"node_type:survey AND srv_foodpref_s:*ish,\\\\ mediterrainean\"} AND country:USA)");

        solrQuery.setFields("*");

        solrQuery.setRows(EmbeddedSolrServerUtils.MAX_RETURNED_DOCS);
        solrQuery.setSort("id", ORDER.desc);

        try {
            QueryResponse response = testServer.query(solrQuery);

            LOGGER.info("\n{}", toDebugString(response.getResults()));

            LOGGER.info("\n\n{}", response.getResults().getNumFound());
        }
        catch (SolrServerException | IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    @Test
    void testHybrid() throws IOException {
        initTestSolrServer("audlang-data-hybrid.json");

        SolrQuery solrQuery = new SolrQuery();

        solrQuery.setQuery("node_type:profile");

        // 2025-01-01T00:00:00Z

        solrQuery.setFilterQueries(
                "_query_:\"{!frange l=1 u=1}if(eq(sub(ms(updtime),sub(ms(updtime),mul(floor(div(ms(updtime),86400000)),86400000))),sub(ms(upddate),sub(ms(upddate),mul(floor(div(ms(upddate),86400000)),86400000)))),1,0)\"");

        solrQuery.setFields("*");

        solrQuery.setRows(EmbeddedSolrServerUtils.MAX_RETURNED_DOCS);
        solrQuery.setSort("id", ORDER.desc);

        try {
            QueryResponse response = testServer.query(solrQuery);

            LOGGER.info("\n{}", toDebugString(response.getResults()));

            LOGGER.info("\n\n{}", response.getResults().getNumFound());
        }
        catch (SolrServerException | IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    @Test
    void testHybrid2() throws IOException {

        String queryRaw = """
                node_type:profile
                AND (
                    (
                        {!join from=main_id to=id v="node_type\\:pos\\
                \\ \\ \\ \\ \\ \\ \\ \\ AND\\ pos_quantity_i\\:\\*\\
                \\ \\ \\ \\ \\ \\ \\ \\ AND\\ pos_uprice_d\\:\\*"}
                        AND NOT {!join from=main_id to=id v="node_type\\:pos\\
                \\ \\ \\ \\ \\ \\ \\ \\ AND\\ _query_\\:\\"\\\\\\{\\\\\\!frange\\\\\\ l=1\\\\\\ u=1\\\\\\}if\\\\\\(and\\\\\\(exists\\\\\\(pos_quantity_i\\\\\\),exists\\\\\\(pos_uprice_d\\\\\\),gt\\\\\\(pos_quantity_i,pos_uprice_d\\\\\\)\\\\\\),1,0\\\\\\)\\""}

                    )
                    OR (
                        {!join from=main_id to=id v="node_type\\:pos\\
                \\ \\ \\ \\ \\ \\ \\ \\ AND\\ pos_invdate_dt\\:\\*"}
                        AND NOT {!join from=main_id to=id v="node_type\\:pos\\
                \\ \\ \\ \\ \\ \\ \\ \\ AND\\ pos_invdate_dt\\:\\[2024\\\\\\-04\\\\\\-02T00\\\\\\:00\\\\\\:00Z\\ TO\\ \\*\\]"}
                    )
                )
                """;

        initTestSolrServer("audlang-data-hybrid.json");

        SolrQuery solrQuery = new SolrQuery();

        solrQuery.setQuery("node_type:profile");

        // 2025-01-01T00:00:00Z

        solrQuery.setFilterQueries(queryRaw);

        solrQuery.setFields("*");

        solrQuery.setRows(EmbeddedSolrServerUtils.MAX_RETURNED_DOCS);
        solrQuery.setSort("id", ORDER.desc);

        try {
            QueryResponse response = testServer.query(solrQuery);

            LOGGER.info("\n{}", toDebugString(response.getResults()));

            LOGGER.info("\n\n{}", response.getResults().getNumFound());
        }
        catch (SolrServerException | IOException ex) {
            throw new RuntimeException(ex);
        }

    }

}
