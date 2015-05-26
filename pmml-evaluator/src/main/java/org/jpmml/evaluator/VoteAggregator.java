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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;

class VoteAggregator<K> extends ClassificationAggregator<K> {

	public VoteAggregator(){
	}

	public Map<K, Double> sumMap(){
		Function<List<Double>, Double> function = new Function<List<Double>, Double>(){

			@Override
			public Double apply(List<Double> values){
				return RegressionAggregator.sum(values);
			}
		};

		return transform(function);
	}

	public Set<K> getWinners(){
		Set<K> result = new LinkedHashSet<>();

		Map<K, Double> sumMap = sumMap();

		final
		Double max = Collections.max(sumMap.values());

		Collection<Map.Entry<K, Double>> entries = sumMap.entrySet();
		for(Map.Entry<K, Double> entry : entries){

			if((entry.getValue()).equals(max)){
				result.add(entry.getKey());
			}
		}

		return result;
	}
}