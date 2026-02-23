//@formatter:off
/*
 * CompositeAutoMappingPolicyTest
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

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import de.calamanari.adl.cnv.tps.ArgMetaInfo;
import de.calamanari.adl.cnv.tps.ConfigException;
import de.calamanari.adl.cnv.tps.DefaultAdlType;
import de.calamanari.adl.solr.DefaultAdlSolrType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
class CompositeAutoMappingPolicyTest {

    @Test
    void testBasics() {

        AutoMappingPolicy policyNever = mock(AutoMappingPolicy.class);

        doReturn(Boolean.FALSE).when(policyNever).isApplicable(anyString());
        doThrow(new ConfigException("Auto-mapping error: No field assignment configured")).when(policyNever).map(anyString(), any());

        AutoMappingPolicy policyPassThrough = mock(AutoMappingPolicy.class);
        doReturn(Boolean.TRUE).when(policyPassThrough).isApplicable(anyString());
        when(policyPassThrough.map(anyString(), any())).thenAnswer(args -> selfAssigned(args.getArgument(0)));

        CompositeAutoMappingPolicy compositePolicy = new CompositeAutoMappingPolicy(Arrays.asList(policyNever, policyPassThrough));

        assertEquals(selfAssigned("foo"), compositePolicy.map("foo", null));

        verify(policyNever, times(1)).isApplicable("foo");
        verify(policyNever, never()).map(eq("foo"), any());

        verify(policyPassThrough, times(1)).isApplicable("foo");
        verify(policyPassThrough, times(1)).map(eq("foo"), any());

        clearInvocations(policyNever, policyPassThrough);

        CompositeAutoMappingPolicy compositePolicy2 = new CompositeAutoMappingPolicy(Arrays.asList(policyNever));
        assertThrows(ConfigException.class, () -> compositePolicy2.map("foo", null));

        CompositeAutoMappingPolicy compositePolicy3 = new CompositeAutoMappingPolicy(null);
        assertThrows(ConfigException.class, () -> compositePolicy3.map("foo", null));

        clearInvocations(policyNever, policyPassThrough);

        CompositeAutoMappingPolicy compositePolicy4 = new CompositeAutoMappingPolicy(Arrays.asList(policyPassThrough, policyNever));

        assertEquals(selfAssigned("foo"), compositePolicy4.map("foo", null));

        verify(policyNever, never()).isApplicable("foo");
        verify(policyNever, never()).map(eq("foo"), any());

        verify(policyPassThrough, times(1)).isApplicable("foo");
        verify(policyPassThrough, times(1)).map(eq("foo"), any());

    }

    private static ArgFieldAssignment selfAssigned(String argName) {

        DataField field = new DataField("node1", "argName", DefaultAdlSolrType.SOLR_STRING, false);

        return new ArgFieldAssignment(new ArgMetaInfo(argName, DefaultAdlType.STRING, false, false), field, false);

    }

}
