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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.calamanari.adl.solr.cnv.ConversionTestUtils.toDebugString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

}
