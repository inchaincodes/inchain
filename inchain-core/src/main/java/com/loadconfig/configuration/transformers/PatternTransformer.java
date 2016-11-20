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
package com.loadconfig.configuration.transformers;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

import com.loadconfig.configuration.PropertyTransformer;
import com.loadconfig.configuration.TransformationException;

/**
 * Authomatic pattern transformer for RegExp resolving
 * 
 * @author SoulKeeper
 */
@SuppressWarnings("rawtypes")
public class PatternTransformer implements PropertyTransformer
{
	/**
	 * Shared instance of this transformer
	 */
	public static final PatternTransformer	SHARED_INSTANCE	= new PatternTransformer();

	/**
	 * Transforms String to Pattern object
	 * 
	 * @param value
	 *            value that will be transformed
	 * @param field
	 *            value will be assigned to this field
	 * @return Pattern Object
	 * @throws TransformationException
	 *             if pattern is not valid
	 */
	@Override
	public Pattern transform(String value, Field field) throws TransformationException
	{
		try
		{
			return Pattern.compile(value);
		}
		catch(Exception e)
		{
			throw new TransformationException("Not valid RegExp: " + value, e);
		}
	}
}
