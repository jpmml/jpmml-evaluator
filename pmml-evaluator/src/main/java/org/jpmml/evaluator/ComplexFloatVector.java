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

public class ComplexFloatVector extends FloatVector {

	private int size = 0;

	private float[] values = null;


	public ComplexFloatVector(int capacity){
		this.values = new float[capacity];
	}

	@Override
	public int size(){
		return this.size;
	}

	@Override
	public FloatVector add(double value){
		return addInternal((float)value);
	}

	@Override
	public FloatVector add(Number value){
		return addInternal(value.floatValue());
	}

	@Override
	public FloatVector add(double coefficient, Number factor){
		return addInternal((float)coefficient * factor.floatValue());
	}

	private FloatVector addInternal(float value){
		this.values[this.size] = value;

		this.size++;

		return this;
	}

	@Override
	public float floatValue(int index){

		if(this.size <= index){
			throw new IndexOutOfBoundsException();
		}

		return this.values[index];
	}

	@Override
	public float floatSum(){
		float[] values = this.values;

		float result = 0f;

		for(int i = 0, max = this.size; i < max; i++){
			result += values[i];
		}

		return result;
	}

	@Override
	public float floatMax(){

		if(this.size == 0){
			throw new IllegalStateException();
		}

		float[] values = this.values;

		float result = values[0];

		for(int i = 1, max = this.size; i < max; i++){
			result = Math.max(result, values[i]);
		}

		return result;
	}

	@Override
	public float floatMedian(){
		throw new UnsupportedOperationException();
	}
}