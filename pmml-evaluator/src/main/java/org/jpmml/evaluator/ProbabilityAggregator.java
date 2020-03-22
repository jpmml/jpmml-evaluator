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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;

public class ProbabilityAggregator<V extends Number> extends KeyValueAggregator<Object, V> {

	private List<HasProbability> hasProbabilities = null;

	private int size = 0;

	private Vector<V> weights = null;


	protected ProbabilityAggregator(ValueFactory<V> valueFactory, int capacity){
		this(valueFactory, capacity, false);
	}

	protected ProbabilityAggregator(ValueFactory<V> valueFactory, int capacity, boolean weighted){
		super(valueFactory, capacity);

		if(capacity > 0){
			this.hasProbabilities = new ArrayList<>(capacity);
		} // End if

		if(weighted){
			this.weights = valueFactory.newVector(0);
		}
	}

	public void add(HasProbability hasProbability){

		if(this.weights != null){
			throw new IllegalStateException();
		} // End if

		if(this.hasProbabilities != null){
			this.hasProbabilities.add(hasProbability);
		}

		Set<?> categories = hasProbability.getCategories();
		for(Object category : categories){
			Double probability = hasProbability.getProbability(category);

			add(category, probability);
		}

		this.size++;
	}

	/**
	 * @param probabilities An array of numbers that sum to 1.
	 *
	 * @see #init(Collection)
	 */
	public void add(Number[] probabilities){

		if(this.weights != null || this.hasProbabilities != null){
			throw new IllegalStateException();
		}

		Collection<Vector<V>> mapValues = values();
		if(mapValues.size() != probabilities.length){
			throw new IllegalArgumentException();
		}

		Iterator<Vector<V>> it = mapValues.iterator();
		for(int i = 0; it.hasNext(); i++){
			Vector<V> values = it.next();

			values.add(probabilities[i]);
		}

		this.size++;
	}

	public void add(HasProbability hasProbability, Number weight){

		if(this.weights == null){
			throw new IllegalStateException();
		} // End if

		if(weight.doubleValue() < 0d){
			throw new IllegalArgumentException();
		} // End if

		if(this.hasProbabilities != null){
			this.hasProbabilities.add(hasProbability);
		}

		Set<?> categories = hasProbability.getCategories();
		for(Object category : categories){
			Double probability = hasProbability.getProbability(category);

			add(category, weight, probability);
		}

		this.size++;

		this.weights.add(weight);
	}

	/**
	 * @param probabilities An array of numbers that sum to 1.
	 *
	 * @see #init(Collection)
	 */
	public void add(Number[] probabilities, Number weight){

		if(this.weights == null || this.hasProbabilities != null){
			throw new IllegalStateException();
		}

		Collection<Vector<V>> mapValues = values();
		if(mapValues.size() != probabilities.length){
			throw new IllegalArgumentException();
		} // End if

		if(weight.doubleValue() < 0d){
			throw new IllegalArgumentException();
		}

		Iterator<Vector<V>> it = mapValues.iterator();
		for(int i = 0; it.hasNext(); i++){
			Vector<V> values = it.next();

			if(weight.doubleValue() != 1d){
				values.add(weight, probabilities[i]);
			} else

			{
				values.add(probabilities[i]);
			}
		}

		this.size++;

		this.weights.add(weight);
	}

	public ValueMap<Object, V> averageMap(){

		if(this.weights != null){
			throw new IllegalStateException();
		}

		Function<Vector<V>, Value<V>> function = new Function<Vector<V>, Value<V>>(){

			private int size = ProbabilityAggregator.this.size;


			@Override
			public Value<V> apply(Vector<V> values){

				if(this.size == 0){
					throw new UndefinedResultException();
				}

				return (values.sum()).divide(this.size);
			}
		};

		return new ValueMap<>(asTransformedMap(function));
	}

	public ValueMap<Object, V> weightedAverageMap(){

		if(this.weights == null){
			throw new IllegalStateException();
		}

		Function<Vector<V>, Value<V>> function = new Function<Vector<V>, Value<V>>(){

			private Value<V> weightSum = ProbabilityAggregator.this.weights.sum();


			@Override
			public Value<V> apply(Vector<V> values){

				if(this.weightSum.isZero()){
					throw new UndefinedResultException();
				}

				return (values.sum()).divide(this.weightSum);
			}
		};

		return new ValueMap<>(asTransformedMap(function));
	}

	public ValueMap<Object, V> maxMap(Collection<?> categories){

		if(this.hasProbabilities == null){
			throw new IllegalStateException();
		} // End if

		if(this.weights != null){
			throw new IllegalStateException();
		}

		Map<?, Value<V>> maxMap = asTransformedMap(Vector::max);

		Map.Entry<?, Value<V>> winnerEntry = getWinner(maxMap, categories);
		if(winnerEntry == null){
			return new ValueMap<>();
		}

		Object category = winnerEntry.getKey();
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

	public ValueMap<Object, V> medianMap(Collection<?> categories){

		if(this.hasProbabilities == null){
			throw new IllegalStateException();
		} // End if

		if(this.weights != null){
			throw new IllegalStateException();
		}

		Map<?, Value<V>> medianMap = asTransformedMap(Vector::median);

		Map.Entry<?, Value<V>> winnerEntry = getWinner(medianMap, categories);
		if(winnerEntry == null){
			return new ValueMap<>();
		}

		Object category = winnerEntry.getKey();
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

	private ValueMap<Object, V> averageMap(List<HasProbability> hasProbabilities){
		ValueFactory<V> valueFactory = getValueFactory();

		if(hasProbabilities.size() == 1){
			HasProbability hasProbability = hasProbabilities.get(0);

			ValueMap<Object, V> result = new ValueMap<>();

			Set<?> categories = keySet();
			for(Object category : categories){
				Double probability = hasProbability.getProbability(category);

				Value<V> value = valueFactory.newValue(probability);

				result.put(category, value);
			}

			return result;
		} else

		{
			ProbabilityAggregator<V> aggregator = new ProbabilityAggregator.Average<>(valueFactory);
			aggregator.init(keySet());

			for(HasProbability hasProbability : hasProbabilities){
				aggregator.add(hasProbability);
			}

			return aggregator.averageMap();
		}
	}

	static
	private <V extends Number> Map.Entry<Object, Value<V>> getWinner(Map<?, Value<V>> values, Collection<?> categories){
		Map.Entry<Object, Value<V>> maxEntry = null;

		if(categories == null){
			categories = values.keySet();
		}

		for(Object category : categories){
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

	static
	public class Average<V extends Number> extends ProbabilityAggregator<V> {

		public Average(ValueFactory<V> valueFactory){
			super(valueFactory, 0);
		}
	}

	static
	public class WeightedAverage<V extends Number> extends ProbabilityAggregator<V> {

		public WeightedAverage(ValueFactory<V> valueFactory){
			super(valueFactory, 0, true);
		}
	}

	static
	public class Max<V extends Number> extends ProbabilityAggregator<V> {

		public Max(ValueFactory<V> valueFactory, int capacity){
			super(valueFactory, capacity);
		}
	}

	static
	public class Median<V extends Number> extends ProbabilityAggregator<V> {

		public Median(ValueFactory<V> valueFactory, int capacity){
			super(valueFactory, capacity);
		}
	}
}