/*
 * Copyright (c) 2014 Villu Ruusmann
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

class ProbabilityAggregator<K> extends LinkedHashMap<K, Double> {

	ProbabilityAggregator(){
	}

	public void max(K key, Double value){
		Double max = get(key);
		if(max == null || (max).compareTo(value) < 0){
			max = value;
		}

		put(key, max);
	}

	public void add(K key, Double value){
		Double sum = get(key);

		put(key, sum != null ? (sum + value) : value);
	}

	public void divide(Double value){
		Collection<Map.Entry<K, Double>> entries = entrySet();
		for(Map.Entry<K, Double> entry : entries){
			entry.setValue(entry.getValue() / value);
		}
	}
}