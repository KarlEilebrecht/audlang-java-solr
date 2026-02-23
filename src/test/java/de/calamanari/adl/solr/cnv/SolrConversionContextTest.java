//@formatter:off
/*
 * SolrConversionContextTest
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

import org.junit.jupiter.api.Test;

import static de.calamanari.adl.solr.cnv.ConversionTestUtils.createDryTestContext;
import static de.calamanari.adl.solr.cnv.ConversionTestUtils.wrap;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class SolrConversionContextTest {

    @Test
    void testBasics() {

        SolrConversionProcessContext ctx = createDryTestContext();

        SolrConversionContext context = new SolrConversionContext();

        assertFalse(context.isNegation());

        assertNull(context.getProcessContext());

        context.setProcessContext(ctx);

        assertSame(ctx, context.getProcessContext());

        assertTrue(context.getChildResultElements().isEmpty());
        assertTrue(context.getPinnedIsUnknownArgNames().isEmpty());

        context.markNegation();

        context.getChildResultElements().add(wrap("color = red", ctx));

        context.getPinnedIsUnknownArgNames().add("color");

        assertTrue(context.isNegation());

        assertFalse(context.getChildResultElements().isEmpty());
        assertFalse(context.getPinnedIsUnknownArgNames().isEmpty());

        context.clear();

        assertSame(ctx, context.getProcessContext());

        assertFalse(context.isNegation());
        assertTrue(context.getChildResultElements().isEmpty());
        assertTrue(context.getPinnedIsUnknownArgNames().isEmpty());

    }

}
