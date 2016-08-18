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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;

public class ProbabilityAggregator extends ClassificationAggregator<String> {

	private List<HasProbability> hasProbabilities = null;


	public ProbabilityAggregator(){
		this(0);
	}

	public ProbabilityAggregator(int capacity){
		super(capacity);

		if(capacity > 0){
			this.hasProbabilities = new ArrayList<>(capacity);
		}
	}

	public void add(HasProbability hasProbability){
		add(hasProbability, 1d);
	}

	public void add(HasProbability hasProbability, double weight){

		if(this.hasProbabilities != null){
			this.hasProbabilities.add(hasProbability);
		}

		Set<String> categories = hasProbability.getCategoryValues();
		for(String category : categories){
			Double probability = hasProbability.getProbability(category);

			add(category, weight != 1d ? (probability * weight) : probability);
		}
	}

	public Map<String, Double> maxMap(Collection<String> categories){

		if(this.hasProbabilities == null){
			throw new IllegalStateException();
		}

		Function<DoubleVector, Double> function = new Function<DoubleVector, Double>(){

			@Override
			public Double apply(DoubleVector values){
				return values.max();
			}
		};

		Map<String, Double> maxValues = transform(function);

		Map.Entry<String, Double> maxMaxValue = getWinner(maxValues, categories);
		if(maxMaxValue == null){
			return Collections.emptyMap();
		}

		String category = maxMaxValue.getKey();
		double maxProbability = maxMaxValue.getValue();

		List<HasProbability> contributors = new ArrayList<>();

		DoubleVector values = get(category);
		for(int i = 0; i < values.size(); i++){
			double probability = values.get(i);

			if(probability == maxProbability){
				HasProbability contributor = this.hasProbabilities.get(i);

				contributors.add(contributor);
			}
		}

		return averageMap(contributors);
	}

	public Map<String, Double> medianMap(Collection<String> categories){

		if(this.hasProbabilities == null){
			throw new IllegalStateException();
		}

		Function<DoubleVector, Double> function = new Function<DoubleVector, Double>(){

			@Override
			public Double apply(DoubleVector values){
				return values.median();
			}
		};

		Map<String, Double> medianValues = transform(function);

		Map.Entry<String, Double> maxMedianValue = getWinner(medianValues, categories);
		if(maxMedianValue == null){
			return Collections.emptyMap();
		}

		String category = maxMedianValue.getKey();
		double medianProbability = maxMedianValue.getValue();

		List<HasProbability> contributors = new ArrayList<>();

		double minDifference = Double.MAX_VALUE;

		DoubleVector values = get(category);
		for(int i = 0; i < values.size(); i++){
			double probability = values.get(i);

			// Choose models whose probability is closest to the calculated median probability.
			// If the number of models is odd (the calculated median probability equals that of the middle model),
			// then all the chosen models will have the same probability (ie. difference == 0).
			// If the number of models is even (the calculated median probability equals the average of two middle-most models),
			// then some of the chosen models will have lower probabilies (ie. difference > 0), whereas the others will have higher probabilities (ie. difference < 0).
			double difference = Math.abs(medianProbability - probability);

			if(difference < minDifference){
				contributors.clear();

				minDifference = difference;
			} // End if

			if(difference <= minDifference){
				HasProbability contributor = this.hasProbabilities.get(i);

				contributors.add(contributor);
			}
		}

		return averageMap(contributors);
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

	static
	private Map.Entry<String, Double> getWinner(Map<String, Double> values, Collection<String> categories){

		if(categories == null || categories.isEmpty()){
			throw new EvaluationException();
		}

		Map.Entry<String, Double> maxEntry = null;

		for(String category : categories){
			Double value = values.get(category);

			if(value == null){
				continue;
			} // End if

			if(maxEntry == null || (maxEntry.getValue()).compareTo(value) < 0){
				maxEntry = new AbstractMap.SimpleEntry<>(category, value);
			}
		}

		return maxEntry;
	}

	static
	private Map<String, Double> averageMap(List<HasProbability> hasProbabilities){

		if(hasProbabilities.size() == 1){
			HasProbability hasProbability = hasProbabilities.get(0);

			Map<String, Double> result = new LinkedHashMap<>();

			Set<String> categories = hasProbability.getCategoryValues();
			for(String category : categories){
				Double probability = hasProbability.getProbability(category);

				result.put(category, probability);
			}

			return result;
		} else

		{
			ProbabilityAggregator aggregator = new ProbabilityAggregator();

			for(HasProbability hasProbability : hasProbabilities){
				aggregator.add(hasProbability);
			}

			return aggregator.averageMap(hasProbabilities.size());
		}
	}
}