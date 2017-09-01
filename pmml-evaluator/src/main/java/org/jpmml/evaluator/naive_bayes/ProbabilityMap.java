/*
 * Copyright (c) 2017 Villu Ruusmann
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
package org.jpmml.evaluator.naive_bayes;

import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;

abstract
class ProbabilityMap<K, V extends Number> extends ValueMap<K, V> {

	abstract
	public ValueFactory<V> getValueFactory();

	public Value<V> ensureValue(K key){
		Value<V> value = get(key);

		if(value == null){
			ValueFactory<V> valueFactory = getValueFactory();

			value = valueFactory.newValue();

			put(key, value);
		}

		return value;
	}

	public void multiply(K key, double probability){
		Value<V> value = ensureValue(key);

		value.multiply(probability);
	}
}