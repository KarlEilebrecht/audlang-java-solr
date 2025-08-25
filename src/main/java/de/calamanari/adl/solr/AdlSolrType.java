//@formatter:off
/*
 * AdlSolrType
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

import de.calamanari.adl.AudlangMessage;
import de.calamanari.adl.CommonErrors;
import de.calamanari.adl.cnv.tps.AdlType;
import de.calamanari.adl.cnv.tps.ArgValueFormatter;
import de.calamanari.adl.cnv.tps.ConfigException;
import de.calamanari.adl.cnv.tps.NativeTypeCaster;
import de.calamanari.adl.cnv.tps.PassThroughTypeCaster;

/**
 * This sub-type of {@link AdlType} is especially made for Apache Solr conversion.
 * <p>
 * If the given set of {@link DefaultAdlSolrType}s is insufficient or any special case handling is required, type behavior can be adjusted. By decorating types
 * implementors can adapt to further types.<br>
 * For Solr there is no native type casting, thus {@link #getNativeTypeCaster()} should always return the default ({@link PassThroughTypeCaster}) to stay
 * compatible with the interface.
 * <p>
 * {@link AdlSolrType}s must not be used to configure the logical data model.
 * 
 * @author <a href="mailto:Karl.Eilebrecht(a/t)calamanari.de">Karl Eilebrecht</a>
 */
public interface AdlSolrType extends AdlType {

    @Override
    default AdlSolrType getBaseType() {
        return this;
    }

    @Override
    default AdlSolrType withNativeTypeCaster(String name, NativeTypeCaster nativeTypeCaster) {
        throw new ConfigException(String.format("""
                Illegal attempt to decorate type {} (specific decorator name: {}) with a native type caster.
                Native type casting is unsupported for Solr types.
                """, this.name(), name == null ? "<N/A>" : name), AudlangMessage.msg(CommonErrors.ERR_4002_CONFIG_ERROR));
    }

    @Override
    default AdlSolrType withNativeTypeCaster(NativeTypeCaster nativeTypeCaster) {
        if (nativeTypeCaster == null) {
            return this;
        }
        return withNativeTypeCaster(null, nativeTypeCaster);
    }

    @Override
    default AdlSolrType withFormatter(String name, ArgValueFormatter formatter) {
        if (formatter == null) {
            return this;
        }
        return new AdlSolrTypeDecorator(name, this, formatter);
    }

    @Override
    default AdlSolrType withFormatter(ArgValueFormatter formatter) {
        return withFormatter(null, formatter);
    }

    /**
     * Tells whether there is a chance that a value of the given argument type can be translated into a parameter of this type.
     * <p>
     * <b>Important:</b> This method only tells that a conversion from the given type to this type <i>might</i> be possible. It can still happen that a
     * particular value will be rejected at runtime.
     * <p>
     * Examples:
     * <ul>
     * <li>A string to integer conversion is only possible if the input value is the textual representation of an integer. This method should return
     * <b>true</b></li>
     * <li>It is principally impossible to turn a boolean into a date. This method should return <b>false</b>.</li>
     * </ul>
     * By definition, {@link AdlSolrType}s shall be <b>incompatible</b> (return false) to any {@link AdlSolrType} (even itself) to ensure Solr-types cannot be
     * abused for the logical data model.
     * <p>
     * The default implementation delegates to {@link #getBaseType()}.
     * 
     * @param type to be tested for compatibility with this type
     * @return true if there are any values of the given type that can be converted into this type
     */
    default boolean isCompatibleWith(AdlType type) {
        if (getBaseType() == this) {
            throw new IllegalStateException("Implementation error detected (endless loop): base type {} does not implement compatibility check.");
        }
        return getBaseType().isCompatibleWith(type);
    }

}
