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

import com.loadconfig.configuration.PropertyTransformer;
import com.loadconfig.configuration.TransformationException;

/**
 * Thransforms string that represents short to the short value. Short value can be represented as decimal or hex
 * 
 * @author SoulKeeper
 */
public class ShortTransformer implements PropertyTransformer<Short>
{
	/**
	 * Shared instance of this transformer. It's thread-safe so no need of multiple instances
	 */
	public static final ShortTransformer	SHARED_INSTANCE	= new ShortTransformer();

	/**
	 * Transforms value to short
	 * 
	 * @param value
	 *            value that will be transformed
	 * @param field
	 *            value will be assigned to this field
	 * @return Short object that represents value
	 * @throws TransformationException
	 *             if something went wrong
	 */
	@Override
	public Short transform(String value, Field field) throws TransformationException
	{
		try
		{
			return Short.decode(value);
		}
		catch(Exception e)
		{
			throw new TransformationException(e);
		}
	}
}
