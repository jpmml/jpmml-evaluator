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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;

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
			Type type = getType();

			return type.getDefault();
		}

		return result;
	}

	Map.Entry<K, Double> getWinner(){
		Ordering<Map.Entry<K, Double>> ordering = createOrdering();

		try {
			return ordering.max(entrySet());
		} catch(NoSuchElementException nsee){
			return null;
		}
	}

	List<Map.Entry<K, Double>> getWinnerList(){
		Ordering<Map.Entry<K, Double>> ordering = (createOrdering()).reverse();

		return ordering.sortedCopy(entrySet());
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

	Ordering<Map.Entry<K, Double>> createOrdering(){
		Comparator<Map.Entry<K, Double>> comparator = new Comparator<Map.Entry<K, Double>>(){

			private Type type = getType();


			@Override
			public int compare(Map.Entry<K, Double> left, Map.Entry<K, Double> right){
				return this.type.compare(left.getValue(), right.getValue());
			}
		};

		return Ordering.from(comparator);
	}

	@Override
	public String toString(){
		ToStringHelper helper = toStringHelper();

		return helper.toString();
	}

	protected ToStringHelper toStringHelper(){
		Type type = getType();

		ToStringHelper helper = Objects.toStringHelper(this)
			.add("type", type)
			.add(type.entryKey(), entrySet());

		return helper;
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

	private static final Ordering<Double> BIGGER_IS_BETTER = Ordering.<Double>natural();
	private static final Ordering<Double> SMALLER_IS_BETTER = (Ordering.<Double>natural()).reverse();

	static
	public enum Type implements Comparator<Double> {
		PROBABILITY(ClassificationMap.BIGGER_IS_BETTER, Range.closed(0d, 1d)),
		CONFIDENCE(ClassificationMap.BIGGER_IS_BETTER, Range.atLeast(0d)),
		DISTANCE(ClassificationMap.SMALLER_IS_BETTER, Range.atLeast(0d)){

			@Override
			public double getDefault(){
				return Double.POSITIVE_INFINITY;
			}
		},
		SIMILARITY(ClassificationMap.BIGGER_IS_BETTER, Range.atLeast(0d)),
		VOTE(ClassificationMap.BIGGER_IS_BETTER, Range.atLeast(0d)),
		;

		private Ordering<Double> ordering;

		private Range<Double> range;


		private Type(Ordering<Double> ordering, Range<Double> range){
			setOrdering(ordering);
			setRange(range);
		}

		/**
		 * Calculates the order between arguments.
		 *
		 * @param left A value
		 * @param right The reference value
		 */
		@Override
		public int compare(Double left, Double right){

			// The behaviour of missing values in comparison operations is not defined
			if(left == null || right == null){
				throw new EvaluationException();
			}

			Ordering<Double> ordering = getOrdering();

			return ordering.compare(left, right);
		}

		/**
		 * Gets the least optimal value in the range of valid values.
		 */
		public double getDefault(){
			return 0d;
		}

		public boolean isValid(Double value){
			Range<Double> range = getRange();

			return range.contains(value);
		}

		protected String entryKey(){
			String name = name();

			return (name.toLowerCase() + "_entries");
		}

		public Ordering<Double> getOrdering(){
			return this.ordering;
		}

		private void setOrdering(Ordering<Double> ordering){
			this.ordering = ordering;
		}

		public Range<Double> getRange(){
			return this.range;
		}

		private void setRange(Range<Double> range){
			this.range = range;
		}
	}
}