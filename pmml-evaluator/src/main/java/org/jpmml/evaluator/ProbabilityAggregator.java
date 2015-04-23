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
import java.util.Set;

class ProbabilityAggregator extends LinkedHashMap<String, Double> {

	ProbabilityAggregator(){
	}

	public void max(HasProbability hasProbability){
		Set<String> categories = hasProbability.getCategoryValues();

		for(String category : categories){
			Double max = get(category);

			Double probability = hasProbability.getProbability(category);

			if(max == null || (max).compareTo(probability) < 0){
				put(category, probability);
			}
		}
	}

	public void sum(HasProbability hasProbability){
		sum(hasProbability, 1d);
	}

	public void sum(HasProbability hasProbability, double weight){
		Set<String> categories = hasProbability.getCategoryValues();

		for(String category : categories){
			Double sum = get(category);

			Double probability = hasProbability.getProbability(category) * weight;

			put(category, sum != null ? (sum + probability) : probability);
		}
	}

	public void divide(Double value){

		if(isEmpty()){
			return;
		}

		Collection<Map.Entry<String, Double>> entries = entrySet();
		for(Map.Entry<String, Double> entry : entries){
			entry.setValue(entry.getValue() / value);
		}
	}
}