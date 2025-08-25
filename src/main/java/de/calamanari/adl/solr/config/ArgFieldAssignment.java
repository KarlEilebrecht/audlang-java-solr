//@formatter:off
/*
 * ArgFieldAssignment
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

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.calamanari.adl.AudlangMessage;
import de.calamanari.adl.CommonErrors;
import de.calamanari.adl.cnv.tps.ArgMetaInfo;
import de.calamanari.adl.cnv.tps.ConfigException;

/**
 * {@link ArgFieldAssignment}s assign arguments to Solr fields.
 * <p>
 * <b>Important:</b> the property {@link ArgMetaInfo#isCollection()} will always be overridden by {@link DataField#isCollection()}.
 * <p>
 * <b>About the {@link #isMultiDoc} property</b>
 * <p>
 * There can be two different motivations for having a dependent or nested document with further attributes. Let's say we are storing additional facts.
 * <ul>
 * <li><b>Case 1:</b> There may or may not exist <i>a single document</i> with all the facts.</li>
 * <li><b>Case 2:</b> You decided (for whatever reason) to onboard each fact as a separate document. Consequently, there may or may not exist multiple documents
 * of node type <i>fact</i>.</li>
 * </ul>
 * Your choice has dramatic consequences on the queries. Let <b><code>color, taste</code></b> be two facts. <b>Case 1</b> allows you to naturally query
 * <code>color:red AND taste:good</code> <i>inside</i> a join to the document storing the facts. <b>Case 2</b> on the other hand requires <i>separate joins</i>
 * to two different documents.
 * <p>
 * The {@link #isMultiDoc} property tells the system which case is correct. A mistake here can have serious consequences:
 * <ul>
 * <li>Queries not returning results (<b>Case 2</b> with <code>{@link #isMultiDoc}==false</code>)</li>
 * <li>Unexpected decomposition (<b>Case 1</b> with <code>{@link #isMultiDoc}==true</code>). Any (intended) relation between fields of the sub-document gets
 * lost. An example could be SKUs, where the unit's name and the unit's price are associated by intention.</li>
 * </ul>
 * <b>Clarification:</b> The property {@link #isMultiDoc} is <i>irrelevant</i> (without any effect) for any fields sitting on the main document, because there
 * can be only a single main document per individual.
 * 
 * @param arg argument meta data
 * @param field Solr field the argument is mapped to
 * @param isMultiDoc see class description for details
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public record ArgFieldAssignment(ArgMetaInfo arg, DataField field, boolean isMultiDoc) implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArgFieldAssignment.class);

    /**
     * @param arg argument meta data
     * @param field Solr field the argument is mapped to
     * @param isMultiDoc see class description for details
     */
    public ArgFieldAssignment(ArgMetaInfo arg, DataField field, boolean isMultiDoc) {
        if (arg == null || field == null) {
            throw new IllegalArgumentException(String.format("Arguments must not be null, given: arg=%s, field=%s, isMultiDoc=%s", arg, field, isMultiDoc));
        }
        if (!field.fieldType().isCompatibleWith(arg.type())) {
            throw new ConfigException(String.format("Incompatible types: cannot map this attribute to the given field, given: arg=%s, field=%s, isMultiDoc=%s",
                    arg, field, isMultiDoc), AudlangMessage.argMsg(CommonErrors.ERR_3000_MAPPING_FAILED, arg.argName()));
        }
        if (arg.isCollection() != field.isCollection()) {
            // effective setting comes from the field
            this.arg = new ArgMetaInfo(arg.argName(), arg.type(), arg.isAlwaysKnown(), field.isCollection());
            LOGGER.trace("ArgMetaInfo auto-adjustment from column: {} -> {} (reason: {})", arg, this.arg, field);
        }
        else {
            this.arg = arg;
        }
        this.field = field;
        this.isMultiDoc = isMultiDoc;
    }

}
