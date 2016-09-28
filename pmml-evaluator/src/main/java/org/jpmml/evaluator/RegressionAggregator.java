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

public class RegressionAggregator {

	private DoubleVector values = null;

	private DoubleVector weights = null;

	private DoubleVector weightedValues = null;


	public RegressionAggregator(){
		this(0);
	}

	public RegressionAggregator(int capacity){
		this.values = new DoubleVector(capacity);
		this.weights = new DoubleVector(capacity);

		this.weightedValues = new DoubleVector(0);
	}

	public int size(){
		return this.weightedValues.size();
	}

	public void add(double value){
		add(value, 1d);
	}

	public void add(double value, double weight){

		if(weight < 0d){
			throw new IllegalArgumentException();
		}

		this.values.add(value);
		this.weights.add(weight);

		this.weightedValues.add(value * weight);
	}

	public double average(){
		return this.values.sum() / this.weights.sum();
	}

	public double weightedAverage(){
		return this.weightedValues.sum() / this.weights.sum();
	}

	public double sum(){
		return this.values.sum();
	}

	public double weightedSum(){
		return this.weightedValues.sum();
	}

	public double median(){
		return this.values.median();
	}

	public double weightedMedian(){
		int size = size();

		List<Entry> entries = new ArrayList<>(size);

		for(int i = 0; i < size; i++){
			Entry entry = new Entry(this.values.get(i), this.weights.get(i));

			entries.add(entry);
		}

		Collections.sort(entries);

		double weightSumThreshold = 0.5d * this.weights.sum();

		double weightSum = 0d;

		// Naive, brute-force search.
		// It is assumed that entries have unique ordering (at least in the area of the weighted median)
		// and that the calculation may be performed using the "whole median" approach
		// (as opposed to the "split median" approach).
		for(Entry entry : entries){
			weightSum += entry.weight;

			if(weightSum >= weightSumThreshold){
				return entry.value;
			}
		}

		throw new EvaluationException();
	}

	static
	private class Entry implements Comparable<Entry> {

		private double value;

		private double weight;


		private Entry(double value){
			this(value, 1d);
		}

		private Entry(double value, double weight){
			this.value = value;
			this.weight = weight;
		}

		@Override
		public int compareTo(Entry that){
			return Double.compare(this.value, that.value);
		}
	}
}