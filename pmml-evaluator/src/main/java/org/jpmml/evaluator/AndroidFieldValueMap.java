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

/**
 * <p>
 * Android 5.0 and 6.0 compatible implementation of {@link FieldValueMap}.
 * </p>
 */
class AndroidFieldValueMap extends FieldValueMap {

	AndroidFieldValueMap(){
	}

	AndroidFieldValueMap(int initialCapacity){
		super(initialCapacity);
	}

	@Override
	public FieldValue getOrDefault(Object key, FieldValue defaultValue){

		if(containsKey(key)){
			return get(key);
		}

		return defaultValue;
	}

	@Override
	public FieldValue putIfAbsent(Object key, FieldValue value){
		FieldValue prevValue = get(key);

		if(prevValue == null){
			return put(key, value);
		}

		return prevValue;
	}
}