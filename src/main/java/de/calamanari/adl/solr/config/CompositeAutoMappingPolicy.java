//@formatter:off
/*
 * CompositeAutoMappingPolicy
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import de.calamanari.adl.AudlangMessage;
import de.calamanari.adl.CommonErrors;
import de.calamanari.adl.ProcessContext;
import de.calamanari.adl.cnv.tps.ConfigException;

/**
 * A {@link CompositeAutoMappingPolicy} allows combining a list of policies into a common policy that can apply different rules or patterns to determine the
 * column assignment for a given argName.
 * <p>
 * The policies will be probed in order of appearance in the configured list.
 * <p>
 * Instances are <i>deeply</i> immutable.
 * 
 * @param members the child policies in the order they should be probed
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public record CompositeAutoMappingPolicy(List<AutoMappingPolicy> members) implements AutoMappingPolicy {

    /**
     * @param members the child policies in the order they should be probed, null gracefully means empty list
     */
    public CompositeAutoMappingPolicy(List<AutoMappingPolicy> members) {
        if (members != null && members.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("The members list must not contain any nulls, given: " + members);
        }
        if (members == null) {
            this.members = Collections.emptyList();
        }
        else {
            this.members = Collections.unmodifiableList(new ArrayList<>(members));
        }
    }

    @Override
    public boolean isApplicable(String argName) {
        return members.stream().anyMatch(member -> member.isApplicable(argName));
    }

    @Override
    public ArgFieldAssignment map(String argName, ProcessContext ctx) {
        return members.stream().filter(member -> member.isApplicable(argName)).findFirst()
                .orElseThrow(() -> new ConfigException("Auto-mapping error: No field assignment configured for argName=" + argName,
                        AudlangMessage.argMsg(CommonErrors.ERR_3000_MAPPING_FAILED, argName)))
                .map(argName, ctx);
    }

}
