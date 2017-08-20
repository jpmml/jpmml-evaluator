/*
 * Copyright (c) 2015 Villu Ruusmann
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValueAggregator<V extends Number> {

	private Vector<V> values = null;

	private Vector<V> weights = null;

	private Vector<V> weightedValues = null;


	public ValueAggregator(Vector<V> values){
		this(values, null);
	}

	public ValueAggregator(Vector<V> values, Vector<V> weights){
		this(values, weights, null);
	}

	public ValueAggregator(Vector<V> values, Vector<V> weights, Vector<V> weightedValues){
		this.values = values;
		this.weights = weights;

		this.weightedValues = weightedValues;
	}

	public void add(Number value){

		if(this.weights != null || this.weightedValues != null){
			throw new IllegalStateException();
		}

		this.values.add(value);
	}

	public void add(Number value, double weight){

		if(this.weights == null){
			throw new IllegalStateException();
		} // End if

		if(weight < 0d){
			throw new IllegalArgumentException();
		}

		this.values.add(value);
		this.weights.add(weight);

		if(this.weightedValues != null){

			if(weight != 1d){
				this.weightedValues.add(weight, value);
			} else

			{
				this.weightedValues.add(value);
			}
		}
	}

	public Value<V> average(){

		if(this.weights != null){
			throw new IllegalStateException();
		}

		return (this.values.sum()).divide(this.values.size());
	}

	public Value<V> weightedAverage(){

		if(this.weights == null || this.weightedValues == null){
			throw new IllegalStateException();
		}

		return (this.weightedValues.sum()).divide(this.weights.sum());
	}

	public Value<V> sum(){

		if(this.weights != null){
			throw new IllegalStateException();
		}

		return this.values.sum();
	}

	public Value<V> weightedSum(){

		if(this.weights == null || this.weightedValues == null){
			throw new IllegalArgumentException();
		}

		return this.weightedValues.sum();
	}

	public Value<V> median(){

		if(this.weights != null){
			throw new IllegalStateException();
		}

		return this.values.median();
	}

	public Value<V> weightedMedian(){

		if(this.weights == null){
			throw new IllegalStateException();
		}

		int size = this.values.size();

		List<Entry> entries = new ArrayList<>(size);

		for(int i = 0; i < size; i++){
			Entry entry = new Entry(this.values.get(i), this.weights.get(i));

			entries.add(entry);
		}

		Collections.sort(entries);

		double weightSumThreshold = 0.5d * (this.weights.sum()).doubleValue();

		double weightSum = 0d;

		// Naive, brute-force search.
		// It is assumed that entries have unique ordering (at least in the area of the weighted median)
		// and that the calculation may be performed using the "whole median" approach
		// (as opposed to the "split median" approach).
		for(Entry entry : entries){
			weightSum += (entry.weight).doubleValue();

			if(weightSum >= weightSumThreshold){
				return entry.value;
			}
		}

		throw new EvaluationException();
	}

	private class Entry implements Comparable<Entry> {

		private Value<V> value;

		private Value<V> weight;


		private Entry(Value<V> value, Value<V> weight){
			this.value = value;
			this.weight = weight;
		}

		@Override
		public int compareTo(Entry that){
			return (this.value).compareTo(that.value);
		}
	}
}