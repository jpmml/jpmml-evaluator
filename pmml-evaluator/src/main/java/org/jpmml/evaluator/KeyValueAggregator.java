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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

public class KeyValueAggregator<K, V extends Number> {

	private ValueFactory<V> valueFactory = null;

	private int capacity = 0;

	private Map<K, Vector<V>> map = new LinkedHashMap<>();


	protected KeyValueAggregator(ValueFactory<V> valueFactory, int capacity){
		this.valueFactory = valueFactory;

		this.capacity = capacity;
	}

	public void init(Collection<K> keys){

		if(!this.map.isEmpty()){
			throw new IllegalStateException();
		}

		for(K key : keys){
			ensureVector(key);
		}
	}

	public void add(K key, Number value){
		Vector<V> values = ensureVector(key);

		values.add(value);
	}

	public void add(K key, Number coefficient, Number factor){
		Vector<V> values = ensureVector(key);

		if(coefficient.doubleValue() != 1d){
			values.add(coefficient, factor);
		} else

		{
			values.add(factor);
		}
	}

	protected Vector<V> get(K key){
		return this.map.get(key);
	}

	public void clear(){
		this.map.clear();
	}

	protected Set<K> keySet(){
		return this.map.keySet();
	}

	protected Collection<Vector<V>> values(){
		return this.map.values();
	}

	protected Set<Map.Entry<K, Vector<V>>> entrySet(){
		return this.map.entrySet();
	}

	protected Map<K, Value<V>> asTransformedMap(Function<Vector<V>, Value<V>> function){
		return Maps.transformValues(this.map, function);
	}

	public ValueFactory<V> getValueFactory(){
		return this.valueFactory;
	}

	private Vector<V> ensureVector(K key){
		Vector<V> values = this.map.get(key);

		if(values == null){
			values = this.valueFactory.newVector(this.capacity);

			this.map.put(key, values);
		}

		return values;
	}
}