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
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.primitives.Doubles;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

class RegressionAggregator {

	private List<Double> values = new ArrayList<>();


	public int size(){
		return this.values.size();
	}

	public void clear(){
		this.values.clear();
	}

	public void add(Double value){
		this.values.add(value);
	}

	public Double sum(){
		Function<List<Double>, Double> function = new Function<List<Double>, Double>(){

			@Override
			public Double apply(List<Double> values){
				return sum(values);
			}
		};

		return transform(function);
	}

	public Double median(){
		Function<List<Double>, Double> function = new Function<List<Double>, Double>(){

			@Override
			public Double apply(List<Double> values){
				return median(values);
			}
		};

		return transform(function);
	}

	public Double average(final double denominator){
		Function<List<Double>, Double> function = new Function<List<Double>, Double>(){

			@Override
			public Double apply(List<Double> values){
				return sum(values) / denominator;
			}
		};

		return transform(function);
	}

	protected Double transform(Function<List<Double>, Double> function){
		return function.apply(this.values);
	}

	static
	double sum(List<Double> values){
		double result = 0d;

		for(Double value : values){
			result += value.doubleValue();
		}

		return result;
	}

	static
	double median(List<Double> values){
		double[] data = Doubles.toArray(values);

		// The data must be ordered
		Arrays.sort(data);

		Percentile percentile = new Percentile();
		percentile.setData(data);

		return percentile.evaluate(50);
	}
}