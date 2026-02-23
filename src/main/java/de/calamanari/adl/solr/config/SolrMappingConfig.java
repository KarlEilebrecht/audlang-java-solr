//@formatter:off
/*
 * SolrMappingConfig
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

import java.util.List;

import de.calamanari.adl.ProcessContext;
import de.calamanari.adl.cnv.tps.ArgMetaInfo;
import de.calamanari.adl.cnv.tps.ArgMetaInfoLookup;
import de.calamanari.adl.cnv.tps.LookupException;

import static de.calamanari.adl.solr.config.ConfigUtils.assertValidArgName;
import static de.calamanari.adl.solr.config.ConfigUtils.assertValidSolrName;

/**
 * A concrete {@link SolrMappingConfig} maps argNames to Solr-fields.
 * <p>
 * <b>Notes:</b>
 * <ul>
 * <li>The same attribute (argName) cannot be mapped to multiple Solr-fields.</li>
 * <li>It is possible to map multiple attributes to the same Solr-field.</li>
 * </ul>
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public interface SolrMappingConfig extends ArgMetaInfoLookup {

    /**
     * Determines the column assignment for the given argName.
     * 
     * @param argName
     * @param ctx
     * @return the configured {@link ArgFieldAssignment} if {@link #contains(String)}=true, otherwise throws exception
     * @throws LookupException if there is no assignment for the given argName
     * @throws IllegalArgumentException if argName was null
     */
    ArgFieldAssignment lookupAssignment(String argName, ProcessContext ctx);

    /**
     * Returns the total number of documents in this configuration
     * 
     * @return number of documents this config is based on, &gt;=1
     */
    int numberOfNodeTypes();

    @Override
    default ArgMetaInfo lookup(String argName) {
        assertValidArgName(argName);
        return lookupAssignment(argName, ProcessContext.empty()).arg();
    }

    /**
     * Returns the data field mapped to the given argName.
     * 
     * @param argName
     * @param ctx
     * @return the configured {@link DataField} if {@link #contains(String)}=true, otherwise throws exception
     * @throws LookupException if there is no assignment for the given argName
     * @throws IllegalArgumentException if argName was null
     */
    default DataField lookupField(String argName, ProcessContext ctx) {
        assertValidArgName(argName);
        return lookupAssignment(argName, ctx).field();
    }

    /**
     * Returns the node type meta infos for the field that is mapped to the given argName
     * 
     * @param argName
     * @param ctx
     * @return meta data
     * @throws LookupException if there is no {@link NodeTypeMetaInfo} for the given argName
     * @throws IllegalArgumentException if argName was null
     */
    NodeTypeMetaInfo lookupNodeTypeMetaInfo(String argName, ProcessContext ctx);

    /**
     * Returns the meta information for all the documents in this configuration.
     * <p>
     * <b>Important:</b>
     * <ul>
     * <li>The order of the remaining elements should reflect the order of configuration.</li>
     * <li>In any case the element order must be <i>stable</i> and it should be deterministic because this is the order the default implementations of several
     * other methods relies on. If the result is composed from an underlying <i>set</i>, then the list should be sorted before returning to avoid
     * indeterministic behavior like flaky tests or unexplainable variations when generating queries.</li>
     * </ul>
     * 
     * @return all configured node types, not empty
     */
    List<NodeTypeMetaInfo> allNodeTypeMetaInfos();

    /**
     * @return meta info of the main document configuration, not null
     */
    NodeTypeMetaInfo mainNodeTypeMetaInfo();

    /**
     * Shorthand for finding the meta info in {@link #allNodeTypeMetaInfos()} by node type name
     * 
     * @param nodeType
     * @return node type meta info
     * @throws LookupException if there is no info about the given node type
     * @throws IllegalArgumentException if any given nodeType was null
     */
    default NodeTypeMetaInfo lookupNodeTypeMetaInfoByNodeType(String nodeType) {
        assertValidSolrName(nodeType);
        return allNodeTypeMetaInfos().stream().filter(info -> info.nodeType().equals(nodeType)).findFirst()
                .orElseThrow(() -> new LookupException(String.format("No meta information available for nodeType %s", nodeType)));
    }

}
