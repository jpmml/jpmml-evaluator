/*
 * Copyright (c) 2016 Villu Ruusmann
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

import java.util.Arrays;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

public class ComplexDoubleVector extends DoubleVector {

	private int size = 0;

	private double[] values = null;


	public ComplexDoubleVector(int capacity){
		this.values = new double[capacity];
	}

	@Override
	public int size(){
		return this.size;
	}

	@Override
	public DoubleVector add(double value){
		return addInternal(value);
	}

	@Override
	public DoubleVector add(Number value){
		return addInternal(value.doubleValue());
	}

	@Override
	public DoubleVector add(double coefficient, Number factor){
		return addInternal(coefficient * factor.doubleValue());
	}

	private DoubleVector addInternal(double value){
		this.values[this.size] = value;

		this.size++;

		return this;
	}

	@Override
	public double doubleValue(int index){

		if(this.size <= index){
			throw new IndexOutOfBoundsException();
		}

		return this.values[index];
	}

	@Override
	public double doubleSum(){
		double[] values = this.values;

		double result = 0d;

		for(int i = 0, max = this.size; i < max; i++){
			result += values[i];
		}

		return result;
	}

	@Override
	public double doubleMax(){

		if(this.size == 0){
			throw new IllegalStateException();
		}

		double[] values = this.values;

		double result = values[0];

		for(int i = 1, max = this.size; i < max; i++){
			result = Math.max(result, values[i]);
		}

		return result;
	}

	@Override
	public double doubleMedian(){
		return doublePercentile(50);
	}

	@Override
	public double doublePercentile(int percentile){

		if(this.size == 0){
			throw new IllegalStateException();
		}

		double[] data = new double[this.size];

		System.arraycopy(this.values, 0, data, 0, data.length);

		Arrays.sort(data);

		Percentile statistic = new Percentile();
		statistic.setData(data);

		return statistic.evaluate(percentile);
	}
}