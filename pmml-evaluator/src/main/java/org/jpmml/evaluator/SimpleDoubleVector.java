/*
 * Copyright (c) 2017 Villu Ruusmann
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

public class SimpleDoubleVector extends DoubleVector {

	private int size = 0;

	private double sum = 0d;

	private double max = -Double.MAX_VALUE;


	public SimpleDoubleVector(){
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
		this.sum += value;
		this.max = Math.max(this.max, value);

		this.size++;

		return this;
	}

	@Override
	public double doubleValue(int index){
		throw new UnsupportedOperationException();
	}

	@Override
	public double doubleSum(){
		return this.sum;
	}

	@Override
	public double doubleMax(){

		if(this.size == 0){
			throw new IllegalStateException();
		}

		return this.max;
	}

	@Override
	public double doubleMedian(){
		throw new UnsupportedOperationException();
	}

	@Override
	public double doublePercentile(int percentile){
		throw new UnsupportedOperationException();
	}
}