//@formatter:off
/*
 * IsNotUnknownPropagatorTest
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

import static de.calamanari.adl.cnv.StandardConversions.parseCoreExpression;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.calamanari.adl.irl.CoreExpression;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class IsNotUnknownPropagatorTest {

    static final Logger LOGGER = LoggerFactory.getLogger(IsNotUnknownPropagatorTest.class);

    @Test
    void testProcess() {

        CoreExpression expression = parseCoreExpression("a=1");

        CoreExpression expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

        expression = parseCoreExpression("(c=3 OR c=4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

        expression = parseCoreExpression("(c=3 AND c=4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

        expression = parseCoreExpression("a=1 and b=2 and (c=3 OR c=4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

        expression = parseCoreExpression("a=1 and b=2 and (STRICT c!=3 OR STRICT c!=4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertEquals("a = 1 AND b = 2 AND STRICT NOT c IS UNKNOWN AND (STRICT NOT c = 3 OR STRICT NOT c = 4)", expressionAfter.toString());

        expression = parseCoreExpression("a=1 and b=2 and (STRICT a!=3 OR STRICT a!=4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertEquals("a = 1 AND b = 2 AND (STRICT NOT a = 3 OR STRICT NOT a = 4)", expressionAfter.toString());

        expression = parseCoreExpression("a=1 and b=2 and (STRICT c!=3 OR STRICT c!=4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertEquals("a = 1 AND b = 2 AND STRICT NOT c IS UNKNOWN AND (STRICT NOT c = 3 OR STRICT NOT c = 4)", expressionAfter.toString());

        expression = parseCoreExpression("a=1 and b=2 and (STRICT c!=3 OR (STRICT c!=4 AND (STRICT d!=5 OR STRICT d!=6)))");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertEquals("a = 1 AND b = 2 AND STRICT NOT c IS UNKNOWN AND (STRICT NOT c = 3 OR (STRICT NOT c = 4 AND STRICT NOT d IS UNKNOWN "
                + "AND (STRICT NOT d = 5 OR STRICT NOT d = 6) ) )", expressionAfter.toString());

        expression = parseCoreExpression("a=1 and b=2 and c is not unknown and (STRICT c!=3 OR STRICT c!=4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertEquals("a = 1 AND b = 2 AND STRICT NOT c IS UNKNOWN AND (STRICT NOT c = 3 OR STRICT NOT c = 4)", expressionAfter.toString());

        expression = parseCoreExpression("a=1 and b=2 and STRICT c != 6 and STRICT c != 7 and (STRICT c!=3 OR STRICT c!=4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        // The following may look incorrect/redundant but it makes sense: technically STRICT c != 6, STRICT c != 7, STRICT c!=a, and STRICT c!=b)
        // are a strict subtractions with the requirement to check for c IS NOT UNKNOWN,
        // by making this explicit, the check can happen "outside", so instead of later creating 4(!) checks we can create only one!
        assertEquals("a = 1 AND b = 2 AND STRICT NOT c = 6 AND STRICT NOT c = 7 AND STRICT NOT c IS UNKNOWN AND (STRICT NOT c = 3 OR STRICT NOT c = 4)",
                expressionAfter.toString());

        expression = parseCoreExpression("STRICT c!=1 OR STRICT c!=3");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertEquals("STRICT NOT c IS UNKNOWN AND (STRICT NOT c = 1 OR STRICT NOT c = 3)", expressionAfter.toString());

        expression = parseCoreExpression("c STRICT NOT ANY OF (1,2,3,4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertEquals("STRICT NOT c = 1 AND STRICT NOT c = 2 AND STRICT NOT c = 3 AND STRICT NOT c = 4 AND STRICT NOT c IS UNKNOWN", expressionAfter.toString());

        expression = parseCoreExpression("(STRICT c!=3 AND STRICT c!=4) OR STRICT c!=@f");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertEquals("STRICT NOT c IS UNKNOWN AND ( (STRICT NOT c = 3 AND STRICT NOT c = 4) OR (STRICT NOT c = @f AND STRICT NOT f IS UNKNOWN) )",
                expressionAfter.toString());

        expression = parseCoreExpression("STRICT c!=@f");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

        expression = parseCoreExpression("a=1 and b=2 and c > 9 and (STRICT c!=@a OR STRICT c!=@b)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

    }

    @Test
    void testProcessLazy() {

        CoreExpression expression = parseCoreExpression("a!=1");

        CoreExpression expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

        expression = parseCoreExpression("(c!=3 OR c!=4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

        expression = parseCoreExpression("(c!=3 AND c!=4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

        expression = parseCoreExpression("a!=1 and b!=2 and (c!=3 OR c!=4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

        expression = parseCoreExpression("a!=1 and b!=2 and (c=3 OR c=4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

        expression = parseCoreExpression("a!=1 and b!=2 and (a=3 OR a=4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertEquals("STRICT NOT a = 1 AND STRICT NOT a IS UNKNOWN AND (STRICT NOT b = 2 OR b IS UNKNOWN) AND (a = 3 OR a = 4)", expressionAfter.toString());

        expression = parseCoreExpression("a!=1 and b!=2 and (c=3 OR c=4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

        expression = parseCoreExpression("a!=1 and b!=2 and (c=3 OR (c=4 AND (d=5 OR d=6)))");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

        expression = parseCoreExpression("a!=1 and b!=2 and (c=3 OR c=4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

        expression = parseCoreExpression("a!=1 and b!=2 and c = 6 and c = 7 and (c=3 OR c=4)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

        expression = parseCoreExpression("(c=3 AND c=4) OR c=@f");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

        expression = parseCoreExpression("c=@f");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertSame(expression, expressionAfter);

        expression = parseCoreExpression("a!=1 and b!=2 and NOT c > 9 and (c=@a OR c=@b)");

        expressionAfter = IsNotUnknownPropagator.process(expression);

        assertEquals("STRICT NOT c > 9 AND STRICT NOT c IS UNKNOWN AND ( (STRICT NOT a = 1 AND a = @c AND b IS UNKNOWN) OR "
                + "(STRICT NOT b = 2 AND STRICT NOT b IS UNKNOWN AND ( (STRICT NOT a = 1 AND a = @c) OR (b = @c AND (STRICT NOT a = 1 OR a IS UNKNOWN) ) ) ) )",
                expressionAfter.toString());

    }

}
