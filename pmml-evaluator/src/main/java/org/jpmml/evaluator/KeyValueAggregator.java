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

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

abstract
public class KeyValueAggregator<K, V extends Number> {

	private Map<K, Vector<V>> map = new LinkedHashMap<>();

	private int capacity = 0;


	public KeyValueAggregator(int capacity){
		this.capacity = capacity;
	}

	abstract
	public ValueFactory<V> getValueFactory();

	public void add(K key){
		add(key, 1d);
	}

	public void add(K key, double value){
		Vector<V> values = ensureVector(key);

		values.add(value);
	}

	public void add(K key, double coefficient, Number factor){
		Vector<V> values = ensureVector(key);

		if(coefficient != 1d){
			values.add(coefficient, factor);
		} else

		{
			values.add(factor);
		}
	}

	public void clear(){
		this.map.clear();
	}

	protected Vector<V> get(K key){
		return this.map.get(key);
	}

	protected Map<K, Value<V>> asTransformedMap(Function<Vector<V>, Value<V>> function){
		return Maps.transformValues(this.map, function);
	}

	private Vector<V> ensureVector(K key){
		Vector<V> values = this.map.get(key);

		if(values == null){
			ValueFactory<V> valueFactory = getValueFactory();

			values = valueFactory.newVector(this.capacity);

			this.map.put(key, values);
		}

		return values;
	}
}