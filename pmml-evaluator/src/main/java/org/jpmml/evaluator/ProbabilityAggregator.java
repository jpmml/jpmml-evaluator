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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;

class ProbabilityAggregator extends ClassificationAggregator<String> {

	ProbabilityAggregator(){
	}

	public void add(HasProbability hasProbability){
		add(hasProbability, 1d);
	}

	public void add(HasProbability hasProbability, double weight){
		Set<String> categories = hasProbability.getCategoryValues();

		for(String category : categories){
			Double probability = hasProbability.getProbability(category);

			add(category, probability * weight);
		}
	}

	public Map<String, Double> maxMap(){
		Function<List<Double>, Double> function = new Function<List<Double>, Double>(){

			@Override
			public Double apply(List<Double> values){
				return Collections.max(values);
			}
		};

		return transform(function);
	}

	public Map<String, Double> medianMap(){
		Function<List<Double>, Double> function = new Function<List<Double>, Double>(){

			@Override
			public Double apply(List<Double> values){
				return RegressionAggregator.median(values);
			}
		};

		return transform(function);
	}

	public Map<String, Double> averageMap(final double denominator){
		Function<List<Double>, Double> function = new Function<List<Double>, Double>(){

			@Override
			public Double apply(List<Double> values){
				return RegressionAggregator.sum(values) / denominator;
			}
		};

		return transform(function);
	}
}