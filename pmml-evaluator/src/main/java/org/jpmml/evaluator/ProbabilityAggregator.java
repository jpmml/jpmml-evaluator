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

class ProbabilityAggregator extends LinkedHashMap<String, Double> {

	ProbabilityAggregator(){
	}

	@SuppressWarnings (
		value = {"unchecked"}
	)
	public void max(HasProbability hasProbability){
		ClassificationMap<String> probabilities = (ClassificationMap<String>)hasProbability;

		Collection<Map.Entry<String, Double>> entries = probabilities.entrySet();
		for(Map.Entry<String, Double> entry : entries){
			Double max = get(entry.getKey());

			if(max == null || (max).compareTo(entry.getValue()) < 0){
				put(entry.getKey(), entry.getValue());
			}
		}
	}

	public void sum(HasProbability hasProbability){
		sum(hasProbability, 1d);
	}

	@SuppressWarnings (
		value = {"unchecked"}
	)
	public void sum(HasProbability hasProbability, double weight){
		ClassificationMap<String> probabilities = (ClassificationMap<String>)hasProbability;

		Collection<Map.Entry<String, Double>> entries = probabilities.entrySet();
		for(Map.Entry<String, Double> entry : entries){
			Double sum = get(entry.getKey());

			put(entry.getKey(), sum != null ? (sum + (entry.getValue() * weight)) : (entry.getValue() * weight));
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