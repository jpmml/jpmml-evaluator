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

import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;

class ProbabilityAggregator extends ClassificationAggregator<String> {

	ProbabilityAggregator(){
		this(0);
	}

	ProbabilityAggregator(int capacity){
		super(capacity);
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
		Function<DoubleVector, Double> function = new Function<DoubleVector, Double>(){

			@Override
			public Double apply(DoubleVector values){
				return values.max();
			}
		};

		return transform(function);
	}

	public Map<String, Double> medianMap(){
		Function<DoubleVector, Double> function = new Function<DoubleVector, Double>(){

			@Override
			public Double apply(DoubleVector values){
				return values.median();
			}
		};

		return transform(function);
	}

	public Map<String, Double> averageMap(final double denominator){
		Function<DoubleVector, Double> function = new Function<DoubleVector, Double>(){

			@Override
			public Double apply(DoubleVector values){
				return values.sum() / denominator;
			}
		};

		return transform(function);
	}
}