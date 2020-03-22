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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import org.dmg.pmml.DataType;
import org.dmg.pmml.MiningFunction;
import org.jpmml.model.ToStringHelper;

/**
 * @see MiningFunction#CLASSIFICATION
 * @see MiningFunction#CLUSTERING
 */
public class Classification<K, V extends Number> extends AbstractComputable implements HasPrediction {

	private Type type = null;

	private ValueMap<K, V> values = null;

	private Object result = null;


	protected Classification(Type type, ValueMap<K, V> values){
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
		Map.Entry<K, Value<V>> entry = getWinner();

		if(entry == null){
			throw new EvaluationException("Empty classification");
		}

		K key = entry.getKey();
		Value<V> value = entry.getValue();

		Object result = TypeUtil.parseOrCast(dataType, key);

		setResult(result);
	}

	@Override
	public Object getPrediction(){
		return getResult();
	}

	@Override
	public Report getPredictionReport(){
		Map.Entry<K, Value<V>> entry = getWinner();

		if(entry == null){
			return null;
		}

		K key = entry.getKey();
		Value<V> value = entry.getValue();

		return ReportUtil.getReport(value);
	}

	@Override
	protected ToStringHelper toStringHelper(){
		Type type = getType();
		ValueMap<K, V> values = getValues();

		ToStringHelper helper = super.toStringHelper()
			.add(type.entryKey(), values.entrySet());

		return helper;
	}

	public void put(K key, Value<V> value){
		ValueMap<K, V> values = getValues();

		if(values.containsKey(key)){
			throw new EvaluationException("Value for key " + PMMLException.formatKey(key) + " has already been defined");
		}

		values.put(key, value);
	}

	public Double getValue(K key){
		Type type = getType();
		ValueMap<K, V> values = getValues();

		Value<V> value = values.get(key);

		return type.getValue(value);
	}

	public Report getValueReport(K key){
		ValueMap<K, V> values = getValues();

		Value<V> value = values.get(key);

		return ReportUtil.getReport(value);
	}

	protected Map.Entry<K, Value<V>> getWinner(){
		return getWinner(getType(), entrySet());
	}

	protected List<Map.Entry<K, Value<V>>> getWinnerRanking(){
		return getWinnerList(getType(), entrySet());
	}

	protected List<K> getWinnerKeys(){
		return entryKeys(getWinnerRanking());
	}

	protected List<Double> getWinnerValues(){
		return Lists.transform(entryValues(getWinnerRanking()), Value::doubleValue);
	}

	protected Set<K> keySet(){
		ValueMap<K, V> values = getValues();

		return values.keySet();
	}

	protected Set<Map.Entry<K, Value<V>>> entrySet(){
		ValueMap<K, V> values = getValues();

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

	public ValueMap<K, V> getValues(){
		return this.values;
	}

	private void setValues(ValueMap<K, V> values){

		if(values == null){
			throw new IllegalArgumentException();
		}

		this.values = values;
	}

	static
	public <K, V extends Number> Map.Entry<K, Value<V>> getWinner(Type type, Collection<Map.Entry<K, Value<V>>> entries){
		Ordering<Map.Entry<K, Value<V>>> ordering = Classification.<K, V>createOrdering(type);

		try {
			return ordering.max(entries);
		} catch(NoSuchElementException nsee){
			return null;
		}
	}

	static
	public <K, V extends Number> List<Map.Entry<K, Value<V>>> getWinnerList(Type type, Collection<Map.Entry<K, Value<V>>> entries){
		Ordering<Map.Entry<K, Value<V>>> ordering = (Classification.<K, V>createOrdering(type)).reverse();

		return ordering.sortedCopy(entries);
	}

	static
	protected <K, V extends Number> Ordering<Map.Entry<K, Value<V>>> createOrdering(Type type){
		return Ordering.from((Map.Entry<K, Value<V>> left, Map.Entry<K, Value<V>> right) -> type.compareValues(left.getValue(), right.getValue()));
	}

	static
	public <K, V> List<K> entryKeys(List<Map.Entry<K, V>> entries){
		return Lists.transform(entries, Map.Entry::getKey);
	}

	static
	public <K, V> List<V> entryValues(List<Map.Entry<K, V>> entries){
		return Lists.transform(entries, Map.Entry::getValue);
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