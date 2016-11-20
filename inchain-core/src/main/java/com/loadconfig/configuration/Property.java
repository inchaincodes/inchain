/*
 * This file is part of aion-emu <aion-emu.com>.
 *
 * aion-emu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * aion-emu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with aion-emu.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.loadconfig.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark field that should be processed by
 * <br>
 * 
 * This annotation is Documented, all definitions with it will appear in javadoc
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Property
{
	/**
	 * This string shows to {@link com.loadconfig.configuration.ConfigurableProcessor} that init value of the
	 * object should not be overriden.
	 */
	public static final String	DEFAULT_VALUE	= "DO_NOT_OVERWRITE_INITIALIAZION_VALUE";

	/**
	 * Property name in configuration
	 * 
	 * @return name of the property that will be used
	 */
	public String key();

	/**
	 * PropertyTransformer to use.<br>
	 * List of automaticly transformed types:<br>
	 * <ul>
	 * <li>{@link Boolean} and boolean by {@link com.loadconfig.configuration.transformers.BooleanTransformer}</li>
	 * <li>{@link Byte} and byte by {@link com.loadconfig.configuration.transformers.ByteTransformer}</li>
	 * <li>{@link Character} and char by {@link com.loadconfig.configuration.transformers.CharTransformer}</li>
	 * <li>{@link Short} and short by {@link com.loadconfig.configuration.transformers.ShortTransformer}</li>
	 * <li>{@link Integer} and int by {@link com.loadconfig.configuration.transformers.IntegerTransformer}</li>
	 * <li>{@link Float} and float by {@link com.loadconfig.configuration.transformers.FloatTransformer}</li>
	 * <li>{@link Long} and long by {@link com.loadconfig.configuration.transformers.LongTransformer}</li>
	 * <li>{@link Double} and double by {@link com.loadconfig.configuration.transformers.DoubleTransformer}</li>
	 * <li>{@link String} by {@link com.loadconfig.configuration.transformers.StringTransformer}</li>
	 * <li>{@link Enum} and enum by {@link com.loadconfig.configuration.transformers.EnumTransformer}</li>
	 * <li>{@link java.io.File} by {@link com.loadconfig.configuration.transformers.FileTransformer}</li>
	 * <li>{@link java.net.InetSocketAddress} by
	 * {@link com.loadconfig.configuration.transformers.InetSocketAddressTransformer}</li>
	 * <li>{@link java.util.regex.Pattern} by {@link com.loadconfig.configuration.transformers.PatternTransformer}
	 * </ul>
	 * <p/>
	 * If your value is one of this types - just leave this field empty
	 * 
	 * @return returns class that will be used to transform value
	 */
	@SuppressWarnings("rawtypes")
	public Class<? extends PropertyTransformer> propertyTransformer() default PropertyTransformer.class;

	/**
	 * Represents default value that will be parsed if key not found. If this key equals(default) {@link #DEFAULT_VALUE}
	 * init value of the object won't be overriden
	 * 
	 * @return default value of the property
	 */
	public String defaultValue() default DEFAULT_VALUE;
}
