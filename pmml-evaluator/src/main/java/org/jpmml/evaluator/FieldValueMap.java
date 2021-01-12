/*
 * Copyright (c) 2021 Villu Ruusmann
 *
 * This file is part of JPMML-Evaluator
 *
 * JPMML-Evaluator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-Evaluator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-Evaluator.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.evaluator;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import org.dmg.pmml.FieldName;

class FieldValueMap extends HashMap<FieldName, FieldValue> {

	FieldValueMap(){
	}

	FieldValueMap(int capacity){
		super(capacity);
	}

	@Override
	@IgnoreJRERequirement
	public FieldValue getOrDefault(Object name, FieldValue defaultValue){
		return super.getOrDefault(name, defaultValue);
	}

	@Override
	@IgnoreJRERequirement
	public FieldValue putIfAbsent(FieldName name, FieldValue value){
		return super.putIfAbsent(name, value);
	}

	static
	public FieldValueMap create(){

		if(FieldValueMap.JDK8_API){
			return new FieldValueMap();
		}

		return new AndroidFieldValueMap();
	}

	static
	public FieldValueMap create(int numberOfVisibleFields){
		int initialCapacity;

		if(numberOfVisibleFields <= 256){
			initialCapacity = Math.max(2 * numberOfVisibleFields, 16);
		} else

		{
			initialCapacity = numberOfVisibleFields;
		} // End if

		if(FieldValueMap.JDK8_API){
			return new FieldValueMap(initialCapacity);
		}

		return new AndroidFieldValueMap(initialCapacity);
	}

	protected static boolean JDK8_API;

	static {
		boolean jdk8_api;

		try {
			Map.class.getMethod("getOrDefault", Object.class, Object.class);
			Map.class.getMethod("putIfAbsent", Object.class, Object.class);

			jdk8_api = true;
		} catch(ReflectiveOperationException roe){
			jdk8_api = false;
		}

		JDK8_API = jdk8_api;
	}
}