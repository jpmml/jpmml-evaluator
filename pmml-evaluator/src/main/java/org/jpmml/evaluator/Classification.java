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
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import org.dmg.pmml.DataType;
import org.dmg.pmml.MiningFunctionType;

/**
 * @see MiningFunctionType#CLASSIFICATION
 * @see MiningFunctionType#CLUSTERING
 */
public class Classification implements Computable {

	private Map<String, Double> map = new LinkedHashMap<>();

	private Object result = null;

	private Type type = null;


	protected Classification(Type type){
		setType(type);
	}

	@Override
	public Object getResult(){

		if(this.result == null){
			throw new EvaluationException();
		}

		return this.result;
	}

	void computeResult(DataType dataType){
		Map.Entry<String, Double> entry = getWinner();
		if(entry == null){
			throw new EvaluationException();
		}

		Object result = TypeUtil.parseOrCast(dataType, entry.getKey());

		setResult(result);
	}

	void setResult(Object result){
		this.result = result;
	}

	@Override
	public String toString(){
		ToStringHelper helper = toStringHelper();

		return helper.toString();
	}

	protected ToStringHelper toStringHelper(){
		ToStringHelper helper = Objects.toStringHelper(this)
			.add("result", getResult())
			.add(getType().entryKey(), entrySet());

		return helper;
	}

	Double get(String key){
		Double value = this.map.get(key);

		// The specified value was not encountered during scoring
		if(value == null){
			Type type = getType();

			return type.getDefault();
		}

		return value;
	}

	Double put(String key, Double value){
		return this.map.put(key, value);
	}

	void putAll(Map<String, Double> values){
		this.map.putAll(values);
	}

	boolean isEmpty(){
		return this.map.isEmpty();
	}

	Map.Entry<String, Double> getWinner(){
		return getWinner(getType(), entrySet());
	}

	List<Map.Entry<String, Double>> getWinnerRanking(){
		return getWinnerList(getType(), entrySet());
	}

	List<String> getWinnerKeys(){
		return entryKeys(getWinnerRanking());
	}

	List<Double> getWinnerValues(){
		return entryValues(getWinnerRanking());
	}

	Double sumValues(){
		return sum(this.map);
	}

	void normalizeValues(){
		normalize(this.map);
	}

	Set<String> keySet(){
		return this.map.keySet();
	}

	Set<Map.Entry<String, Double>> entrySet(){
		return this.map.entrySet();
	}

	public Type getType(){
		return this.type;
	}

	private void setType(Type type){
		this.type = type;
	}

	static
	Map.Entry<String, Double> getWinner(Type type, Collection<Map.Entry<String, Double>> entries){
		Ordering<Map.Entry<String, Double>> ordering = createOrdering(type);

		try {
			return ordering.max(entries);
		} catch(NoSuchElementException nsee){
			return null;
		}
	}

	static
	List<Map.Entry<String, Double>> getWinnerList(Type type, Collection<Map.Entry<String, Double>> entries){
		Ordering<Map.Entry<String, Double>> ordering = (createOrdering(type)).reverse();

		return ordering.sortedCopy(entries);
	}

	static
	Ordering<Map.Entry<String, Double>> createOrdering(final Type type){
		Comparator<Map.Entry<String, Double>> comparator = new Comparator<Map.Entry<String, Double>>(){

			@Override
			public int compare(Map.Entry<String, Double> left, Map.Entry<String, Double> right){
				return type.compare(left.getValue(), right.getValue());
			}
		};

		return Ordering.from(comparator);
	}

	static
	public <K, V> List<K> entryKeys(List<Map.Entry<K, V>> entries){
		Function<Map.Entry<K, V>, K> function = new Function<Map.Entry<K, V>, K>(){

			@Override
			public K apply(Map.Entry<K, V> entry){
				return entry.getKey();
			}
		};

		return Lists.transform(entries, function);
	}

	static
	public <K, V> List<V> entryValues(List<Map.Entry<K, V>> entries){
		Function<Map.Entry<K, V>, V> function = new Function<Map.Entry<K, V>, V>(){

			@Override
			public V apply(Map.Entry<K, V> entry){
				return entry.getValue();
			}
		};

		return Lists.transform(entries, function);
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
	private static final Ordering<Double> SMALLER_IS_BETTER = Ordering.<Double>natural().reverse();

	static
	public enum Type implements Comparator<Double> {
		PROBABILITY(Classification.BIGGER_IS_BETTER, Range.closed(0d, 1d)),
		CONFIDENCE(Classification.BIGGER_IS_BETTER, Range.atLeast(0d)),
		DISTANCE(Classification.SMALLER_IS_BETTER, Range.atLeast(0d)){

			@Override
			public double getDefault(){
				return Double.POSITIVE_INFINITY;
			}
		},
		SIMILARITY(Classification.BIGGER_IS_BETTER, Range.atLeast(0d)),
		VOTE(Classification.BIGGER_IS_BETTER, Range.atLeast(0d)),
		;

		private Ordering<Double> ordering;

		private Range<Double> range;


		private Type(Ordering<Double> ordering, Range<Double> range){
			setOrdering(ordering);
			setRange(range);
		}

		/**
		 * <p>
		 * Calculates the order between arguments.
		 * </p>
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
		 * <p>
		 * Gets the least optimal value in the range of valid values.
		 * </p>
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