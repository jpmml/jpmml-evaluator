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

public class ValueUtil {

	private ValueUtil(){
	}

	static
	public <V extends Number> void normalizeSimpleMax(Iterable<Value<V>> values){
		Value<V> sum = null;

		for(Value<V> value : values){

			if(sum == null){
				sum = value.copy();
			} else

			{
				sum.add(value);
			}
		}

		if((sum == null) || (sum != null && sum.doubleValue() == 1d)){
			return;
		}

		for(Value<V> value : values){
			value.divide(sum);
		}
	}

	static
	public <V extends Number> void normalizeSoftMax(Iterable<Value<V>> values){
		Value<V> max = null;

		for(Value<V> value : values){

			if(max == null || (max).compareTo(value) < 0){
				max = value;
			}
		}

		if(max == null){
			return;
		} else

		{
			max = max.copy();
		}

		Value<V> sum = null;

		for(Value<V> value : values){
			value = (value.subtract(max)).exp();

			if(sum == null){
				sum = value.copy();
			} else

			{
				sum.add(value);
			}
		}

		if((sum == null) || (sum != null && sum.doubleValue() == 1d)){
			return;
		}

		for(Value<V> value : values){
			value.divide(sum);
		}
	}
}