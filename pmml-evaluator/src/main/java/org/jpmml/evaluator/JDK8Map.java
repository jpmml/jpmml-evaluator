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

import java.util.Map;

interface JDK8Map<K, V> extends Map<K, V> {

	@Override
	default
	public V getOrDefault(Object key, V defaultValue){

		if(containsKey(key)){
			return get(key);
		}

		return defaultValue;
	}

	@Override
	default
	public V putIfAbsent(K key, V value){
		V prevValue = get(key);

		if(prevValue == null){
			return put(key, value);
		}

		return prevValue;
	}
}