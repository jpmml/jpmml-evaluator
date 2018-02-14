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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import org.dmg.pmml.DataType;
import org.dmg.pmml.MiningFunction;

/**
 * @see MiningFunction#CLASSIFICATION
 * @see MiningFunction#CLUSTERING
 */
public class Classification<V extends Number> extends AbstractComputable implements HasPrediction {

	private Type type = null;

	private ValueMap<String, V> values = null;

	private Object result = null;


	protected Classification(Type type, ValueMap<String, V> values){
		setType(type);
		setValues(values);
	}

	@Override
	public Object getResult(){

		if(this.result == null){
			throw new EvaluationException("Classification result has not been computed");
		}

		return this.result;
	}

	protected void setResult(Object result){
		this.result = result;
	}

	protected void computeResult(DataType dataType){
		Map.Entry<String, Value<V>> entry = getWinner();

		if(entry == null){
			throw new EvaluationException("Empty classification");
		}

		String key = entry.getKey();
		Value<V> value = entry.getValue();

		Object result = TypeUtil.parse(dataType, key);

		setResult(result);
	}

	@Override
	public Object getPrediction(){
		return getResult();
	}

	@Override
	public Report getPredictionReport(){
		Map.Entry<String, Value<V>> entry = getWinner();

		if(entry == null){
			return null;
		}

		String key = entry.getKey();
		Value<V> value = entry.getValue();

		return ReportUtil.getReport(value);
	}

	@Override
	protected ToStringHelper toStringHelper(){
		Type type = getType();
		ValueMap<String, V> values = getValues();

		ToStringHelper helper = super.toStringHelper()
			.add(type.entryKey(), values.entrySet());

		return helper;
	}

	public void put(String key, Value<V> value){
		ValueMap<String, V> values = getValues();

		if(values.containsKey(key)){
			throw new EvaluationException("Value for key " + PMMLException.formatKey(key) + " has already been defined");
		}

		values.put(key, value);
	}

	public Double getValue(String key){
		Type type = getType();
		ValueMap<String, V> values = getValues();

		Value<V> value = values.get(key);

		return type.getValue(value);
	}

	public Report getValueReport(String key){
		ValueMap<String, V> values = getValues();

		Value<V> value = values.get(key);

		return ReportUtil.getReport(value);
	}

	protected Map.Entry<String, Value<V>> getWinner(){
		return getWinner(getType(), entrySet());
	}

	protected List<Map.Entry<String, Value<V>>> getWinnerRanking(){
		return getWinnerList(getType(), entrySet());
	}

	protected List<String> getWinnerKeys(){
		return entryKeys(getWinnerRanking());
	}

	protected List<Double> getWinnerValues(){
		Function<Value<V>, Double> function = new Function<Value<V>, Double>(){

			@Override
			public Double apply(Value<V> value){
				return value.doubleValue();
			}
		};

		return Lists.transform(entryValues(getWinnerRanking()), function);
	}

	protected Set<String> keySet(){
		ValueMap<String, V> values = getValues();

		return values.keySet();
	}

	protected Set<Map.Entry<String, Value<V>>> entrySet(){
		ValueMap<String, V> values = getValues();

		return values.entrySet();
	}

	public Type getType(){
		return this.type;
	}

	private void setType(Type type){

		if(type == null){
			throw new IllegalArgumentException();
		}

		this.type = type;
	}

	public ValueMap<String, V> getValues(){
		return this.values;
	}

	private void setValues(ValueMap<String, V> values){

		if(values == null){
			throw new IllegalArgumentException();
		}

		this.values = values;
	}

	static
	public <V extends Number> Map.Entry<String, Value<V>> getWinner(Type type, Collection<Map.Entry<String, Value<V>>> entries){
		Ordering<Map.Entry<String, Value<V>>> ordering = Classification.<V>createOrdering(type);

		try {
			return ordering.max(entries);
		} catch(NoSuchElementException nsee){
			return null;
		}
	}

	static
	public <V extends Number> List<Map.Entry<String, Value<V>>> getWinnerList(Type type, Collection<Map.Entry<String, Value<V>>> entries){
		Ordering<Map.Entry<String, Value<V>>> ordering = (Classification.<V>createOrdering(type)).reverse();

		return ordering.sortedCopy(entries);
	}

	static
	protected <V extends Number> Ordering<Map.Entry<String, Value<V>>> createOrdering(final Type type){
		Comparator<Map.Entry<String, Value<V>>> comparator = new Comparator<Map.Entry<String, Value<V>>>(){

			@Override
			public int compare(Map.Entry<String, Value<V>> left, Map.Entry<String, Value<V>> right){
				return type.compareValues(left.getValue(), right.getValue());
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
	public enum Type {
		PROBABILITY(true, Range.closed(Numbers.DOUBLE_ZERO, Numbers.DOUBLE_ONE)),
		CONFIDENCE(true, Range.atLeast(Numbers.DOUBLE_ZERO)),
		DISTANCE(false, Range.atLeast(Numbers.DOUBLE_ZERO)){

			@Override
			public Double getDefaultValue(){
				return Double.POSITIVE_INFINITY;
			}
		},
		SIMILARITY(true, Range.atLeast(Numbers.DOUBLE_ZERO)),
		VOTE(true, Range.atLeast(Numbers.DOUBLE_ZERO)),
		;

		private boolean ordering;

		private Range<Double> range;


		private Type(boolean ordering, Range<Double> range){
			setOrdering(ordering);
			setRange(range);
		}

		public <V extends Number> Double getValue(Value<V> value){

			// The specified value was not encountered during scoring
			if(value == null){
				return getDefaultValue();
			}

			return value.doubleValue();
		}

		public <V extends Number> int compareValues(Value<V> left, Value<V> right){
			boolean ordering = getOrdering();

			int result = (left).compareTo(right);

			return (ordering ? result : -result);
		}

		public <V extends Number> boolean isValidValue(Value<V> value){
			Range<Double> range = getRange();

			return range.contains(value.doubleValue());
		}

		/**
		 * <p>
		 * Gets the least optimal value in the range of valid values.
		 * </p>
		 */
		public Double getDefaultValue(){
			return Numbers.DOUBLE_ZERO;
		}

		public String entryKey(){
			String name = name();

			return (name.toLowerCase() + "_entries");
		}

		public boolean getOrdering(){
			return this.ordering;
		}

		private void setOrdering(boolean ordering){
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