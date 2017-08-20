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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;

abstract
public class ProbabilityAggregator<V extends Number> extends KeyValueAggregator<String, V> {

	private List<HasProbability> hasProbabilities = null;

	private int size = 0;

	private Vector<V> weights = null;


	public ProbabilityAggregator(int capacity){
		this(capacity, null);
	}

	public ProbabilityAggregator(int capacity, Vector<V> weights){
		super(capacity);

		if(capacity > 0){
			this.hasProbabilities = new ArrayList<>(capacity);
		}

		this.weights = weights;
	}

	public void add(HasProbability hasProbability){

		if(this.weights != null){
			throw new IllegalStateException();
		} // End if

		if(this.hasProbabilities != null){
			this.hasProbabilities.add(hasProbability);
		}

		Set<String> categories = hasProbability.getCategoryValues();
		for(String category : categories){
			Double probability = hasProbability.getProbability(category);

			add(category, probability);
		}

		this.size++;
	}

	public void add(HasProbability hasProbability, double weight){

		if(this.weights == null){
			throw new IllegalStateException();
		} // End if

		if(weight < 0d){
			throw new IllegalArgumentException();
		} // End if

		if(this.hasProbabilities != null){
			this.hasProbabilities.add(hasProbability);
		}

		Set<String> categories = hasProbability.getCategoryValues();
		for(String category : categories){
			Double probability = hasProbability.getProbability(category);

			add(category, weight, probability);
		}

		this.size++;

		this.weights.add(weight);
	}

	public ValueMap<String, V> averageMap(){

		if(this.weights != null){
			throw new IllegalStateException();
		}

		Function<Vector<V>, Value<V>> function = new Function<Vector<V>, Value<V>>(){

			private double denominator = ProbabilityAggregator.this.size;


			@Override
			public Value<V> apply(Vector<V> values){
				return (values.sum()).divide(this.denominator);
			}
		};

		return new ValueMap<>(asTransformedMap(function));
	}

	public ValueMap<String, V> weightedAverageMap(){

		if(this.weights == null){
			throw new IllegalStateException();
		}

		Function<Vector<V>, Value<V>> function = new Function<Vector<V>, Value<V>>(){

			private Value<V> denominator = ProbabilityAggregator.this.weights.sum();


			@Override
			public Value<V> apply(Vector<V> values){
				return (values.sum()).divide(this.denominator);
			}
		};

		return new ValueMap<>(asTransformedMap(function));
	}

	public ValueMap<String, V> maxMap(Collection<String> categories){

		if(this.hasProbabilities == null){
			throw new IllegalStateException();
		} // End if

		if(this.weights != null){
			throw new IllegalStateException();
		}

		Function<Vector<V>, Value<V>> function = new Function<Vector<V>, Value<V>>(){

			@Override
			public Value<V> apply(Vector<V> values){
				return values.max();
			}
		};

		Map<String, Value<V>> maxMap = asTransformedMap(function);

		Map.Entry<String, Value<V>> winnerEntry = getWinner(maxMap, categories);
		if(winnerEntry == null){
			return new ValueMap<>();
		}

		String category = winnerEntry.getKey();
		Value<V> maxProbability = winnerEntry.getValue();

		List<HasProbability> contributors = new ArrayList<>();

		Vector<V> values = get(category);

		for(int i = 0, max = values.size(); i < max; i++){
			Value<V> probability = values.get(i);

			if((maxProbability).compareTo(probability) == 0){
				HasProbability contributor = this.hasProbabilities.get(i);

				contributors.add(contributor);
			}
		}

		return averageMap(contributors);
	}

	public ValueMap<String, V> medianMap(Collection<String> categories){

		if(this.hasProbabilities == null){
			throw new IllegalStateException();
		} // End if

		if(this.weights != null){
			throw new IllegalStateException();
		}

		Function<Vector<V>, Value<V>> function = new Function<Vector<V>, Value<V>>(){

			@Override
			public Value<V> apply(Vector<V> values){
				return values.median();
			}
		};

		Map<String, Value<V>> medianMap = asTransformedMap(function);

		Map.Entry<String, Value<V>> winnerEntry = getWinner(medianMap, categories);
		if(winnerEntry == null){
			return new ValueMap<>();
		}

		String category = winnerEntry.getKey();
		Value<V> medianProbability = winnerEntry.getValue();

		List<HasProbability> contributors = new ArrayList<>();

		double minDifference = Double.MAX_VALUE;

		Vector<V> values = get(category);

		for(int i = 0, max = values.size(); i < max; i++){
			Value<V> probability = values.get(i);

			// Choose models whose probability is closest to the calculated median probability.
			// If the number of models is odd (the calculated median probability equals that of the middle model),
			// then all the chosen models will have the same probability (ie. difference == 0).
			// If the number of models is even (the calculated median probability equals the average of two middle-most models),
			// then some of the chosen models will have lower probabilies (ie. difference > 0), whereas the others will have higher probabilities (ie. difference < 0).
			double difference = Math.abs(medianProbability.doubleValue() - probability.doubleValue());

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

	private ValueMap<String, V> averageMap(List<HasProbability> hasProbabilities){

		if(hasProbabilities.size() == 1){
			HasProbability hasProbability = hasProbabilities.get(0);

			ValueFactory<V> valueFactory = getValueFactory();

			ValueMap<String, V> result = new ValueMap<>();

			Set<String> categories = hasProbability.getCategoryValues();
			for(String category : categories){
				Double probability = hasProbability.getProbability(category);

				Value<V> value = valueFactory.newValue(probability);

				result.put(category, value);
			}

			return result;
		} else

		if(hasProbabilities.size() > 1){
			ProbabilityAggregator<V> aggregator = new ProbabilityAggregator<V>(0){

				@Override
				public ValueFactory<V> getValueFactory(){
					return ProbabilityAggregator.this.getValueFactory();
				}
			};

			for(HasProbability hasProbability : hasProbabilities){
				aggregator.add(hasProbability);
			}

			return aggregator.averageMap();
		} else

		{
			throw new EvaluationException();
		}
	}

	static
	private <V extends Number> Map.Entry<String, Value<V>> getWinner(Map<String, Value<V>> values, Collection<String> categories){

		if(categories == null || categories.isEmpty()){
			throw new EvaluationException();
		}

		Map.Entry<String, Value<V>> maxEntry = null;

		for(String category : categories){
			Value<V> value = values.get(category);

			if(value == null){
				continue;
			} // End if

			if(maxEntry == null || (maxEntry.getValue()).compareTo(value) < 0){
				maxEntry = new AbstractMap.SimpleEntry<>(category, value);
			}
		}

		return maxEntry;
	}
}