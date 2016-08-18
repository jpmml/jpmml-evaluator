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

public class DoubleVector {

	private int size = 0;

	private double sum = 0d;

	private double max = Double.NaN;

	private double[] values = null;


	public DoubleVector(int capacity){

		if(capacity > 0){
			this.values = new double[capacity];
		}
	}

	public int size(){
		return this.size;
	}

	public double get(int index){

		if(this.values == null){
			throw new IllegalStateException();
		} // End if

		if(this.size <= index){
			throw new IndexOutOfBoundsException();
		}

		return this.values[index];
	}

	public void add(double value){
		this.sum += value;

		if(Double.isNaN(this.max)){
			this.max = value;
		} else

		{
			this.max = Math.max(this.max, value);
		} // End if

		if(this.values != null){
			this.values[this.size] = value;
		}

		this.size++;
	}

	public double sum(){
		return this.sum;
	}

	public double max(){

		if(Double.isNaN(this.max)){
			throw new IllegalStateException();
		}

		return this.max;
	}

	public double median(){
		return percentile(50);
	}

	public double percentile(int percentile){

		if(this.values == null){
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