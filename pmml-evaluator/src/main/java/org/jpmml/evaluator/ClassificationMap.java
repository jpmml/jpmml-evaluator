/*
 * Copyright (c) 2013 Villu Ruusmann
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

@Beta
public class ClassificationMap<K> extends LinkedHashMap<K, Double> implements Computable {

	private Type type = null;


	protected ClassificationMap(Type type){
		setType(type);
	}

	@Override
	public Object getResult(){
		Map.Entry<K, Double> entry = getWinner();
		if(entry == null){
			throw new EvaluationException();
		}

		return entry.getKey();
	}

	Double getFeature(String value){
		Double result = get(value);

		// The specified value was not encountered during scoring
		if(result == null){
			return 0d;
		}

		return result;
	}

	Map.Entry<K, Double> getWinner(){
		Map.Entry<K, Double> result = null;

		Type type = getType();

		Collection<Map.Entry<K, Double>> entries = entrySet();
		for(Map.Entry<K, Double> entry : entries){

			if(result == null || type.compare(entry.getValue(), result.getValue()) > 0){
				result = entry;
			}
		}

		return result;
	}

	List<Map.Entry<K, Double>> getWinnerList(){
		List<Map.Entry<K, Double>> result = Lists.newArrayList(entrySet());

		final
		Type type = getType();

		Comparator<Map.Entry<K, Double>> comparator = new Comparator<Map.Entry<K, Double>>(){

			@Override
			public int compare(Map.Entry<K, Double> left, Map.Entry<K, Double> right){
				return -1 * type.compare(left.getValue(), right.getValue());
			}
		};
		Collections.sort(result, comparator);

		return result;
	}

	List<K> getWinnerKeys(){
		List<Map.Entry<K, Double>> winners = getWinnerList();

		Function<Map.Entry<K, Double>, K> function = new Function<Map.Entry<K, Double>, K>(){

			@Override
			public K apply(Map.Entry<K, Double> entry){
				return entry.getKey();
			}
		};

		return Lists.transform(winners, function);
	}

	List<Double> getWinnerValues(){
		List<Map.Entry<K, Double>> winners = getWinnerList();

		Function<Map.Entry<K, Double>, Double> function = new Function<Map.Entry<K, Double>, Double>(){

			@Override
			public Double apply(Map.Entry<K, Double> entry){
				return entry.getValue();
			}
		};

		return Lists.transform(winners, function);
	}

	void normalizeValues(){
		normalize(this);
	}

	public Type getType(){
		return this.type;
	}

	private void setType(Type type){
		this.type = type;
	}

	static
	public <K> Double sum(Map<K, Double> map){
		return sum(map, null);
	}

	static
	private <K> Double sum(Map<K, Double> map, Function<Double, Double> function){
		double sum = 0d;

		Collection<Double> values = map.values();
		for(Double value : values){

			if(function != null){
				value = function.apply(value);
			}

			sum += value.doubleValue();
		}

		return sum;
	}

	static
	public <K> void normalize(Map<K, Double> map){
		normalize(map, null);
	}

	static
	public <K> void normalizeSoftMax(Map<K, Double> map){
		Function<Double, Double> function = new Function<Double, Double>(){

			@Override
			public Double apply(Double value){
				return Math.exp(value.doubleValue());
			}
		};

		normalize(map, function);
	}

	static
	private <K> void normalize(Map<K, Double> map, Function<Double, Double> function){
		double sum = sum(map, function);

		Collection<Map.Entry<K, Double>> entries = map.entrySet();
		for(Map.Entry<K, Double> entry : entries){
			Double value = entry.getValue();

			if(function != null){
				value = function.apply(value);
			}

			entry.setValue(value / sum);
		}
	}

	static
	public <K> void subtract(Map<K, Double> map, List<K> keys){
		double offset = 0d;

		for(int i = 0; i < keys.size() - 1; i++){
			K key = keys.get(i);

			Double cumulativeProbability = map.get(key);
			if(cumulativeProbability == null || cumulativeProbability > 1d){
				throw new EvaluationException();
			}

			Double probability = (cumulativeProbability - offset);
			if(probability < 0d){
				throw new EvaluationException();
			}

			map.put(key, probability);

			offset = cumulativeProbability;
		}

		if(keys.size() > 1){
			K key = keys.get(keys.size() - 1);

			map.put(key, 1d - offset);
		}
	}

	static
	public enum Type implements Comparator<Double> {
		PROBABILITY(Ordering.INCREASING),
		CONFIDENCE(Ordering.INCREASING),
		DISTANCE(Ordering.DECREASING),
		SIMILARITY(Ordering.INCREASING),
		VOTE(Ordering.INCREASING),
		;

		private Ordering ordering;


		private Type(Ordering ordering){
			setOrdering(ordering);
		}

		/**
		 * Calculates the order between arguments.
		 *
		 * @param left A value
		 * @param right The reference value
		 */
		@Override
		public int compare(Double left, Double right){
			int order = (left).compareTo(right);

			Ordering ordering = getOrdering();
			switch(ordering){
				case INCREASING:
					return order;
				case DECREASING:
					return -1 * order;
				default:
					throw new IllegalStateException();
			}
		}

		public Ordering getOrdering(){
			return this.ordering;
		}

		private void setOrdering(Ordering ordering){
			this.ordering = ordering;
		}

		static
		private enum Ordering {
			/**
			 * The most positive value represents the optimum.
			 */
			INCREASING,

			/**
			 * The most negative value represents the optimum.
			 */
			DECREASING,
		}
	}
}