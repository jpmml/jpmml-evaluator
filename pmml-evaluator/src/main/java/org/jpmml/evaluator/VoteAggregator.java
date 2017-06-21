/*
 * Copyright (c) 2015 Villu Ruusmann
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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;

abstract
public class VoteAggregator<K, V extends Number> extends KeyValueAggregator<K, V> {

	public VoteAggregator(){
		super(0);
	}

	public ValueMap<K, V> sumMap(){
		Function<Vector<V>, Value<V>> function = new Function<Vector<V>, Value<V>>(){

			@Override
			public Value<V> apply(Vector<V> values){
				return values.sum();
			}
		};

		return new ValueMap<>(asTransformedMap(function));
	}

	public Set<K> getWinners(){
		Set<K> result = new LinkedHashSet<>();

		Map<K, Value<V>> sumMap = sumMap();

		Value<V> maxValue = Collections.max(sumMap.values());

		Collection<Map.Entry<K, Value<V>>> entries = sumMap.entrySet();
		for(Map.Entry<K, Value<V>> entry : entries){
			K key = entry.getKey();
			Value<V> value = entry.getValue();

			if((maxValue).compareTo(value) == 0){
				result.add(key);
			}
		}

		return result;
	}
}