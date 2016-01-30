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

class RegressionAggregator {

	private DoubleVector values = null;


	RegressionAggregator(){
		this(0);
	}

	RegressionAggregator(int capacity){
		this.values = new DoubleVector(capacity);
	}

	public int size(){
		return this.values.size();
	}

	public void add(double value){
		this.values.add(value);
	}

	public double sum(){
		return this.values.sum();
	}

	public double average(double denominator){
		return this.values.sum() / denominator;
	}

	public double median(){
		return this.values.median();
	}
}