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
 * Thransforms string that represents float in decimal format
 * 
 * @author SoulKeeper
 */
public class FloatTransformer implements PropertyTransformer<Float>
{
	/**
	 * Shared instance of this transformer. It's thread-safe so no need of multiple instances
	 */
	public static final FloatTransformer	SHARED_INSTANCE	= new FloatTransformer();

	/**
	 * Thransforms string to float
	 * 
	 * @param value
	 *            value that will be transformed
	 * @param field
	 *            value will be assigned to this field
	 * @return Float that represents value
	 * @throws TransformationException
	 *             if something went wrong
	 */
	@Override
	public Float transform(String value, Field field) throws TransformationException
	{
		try
		{
			return Float.parseFloat(value);
		}
		catch(Exception e)
		{
			throw new TransformationException(e);
		}
	}
}
