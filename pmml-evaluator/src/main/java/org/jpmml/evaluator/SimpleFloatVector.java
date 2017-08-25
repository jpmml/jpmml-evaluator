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

public class SimpleFloatVector extends FloatVector {

	private int size = 0;

	private float sum = 0f;

	private float max = -Float.MAX_VALUE;


	public SimpleFloatVector(){
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
		this.sum += value;
		this.max = Math.max(this.max, value);

		this.size++;

		return this;
	}

	@Override
	public float floatValue(int index){
		throw new UnsupportedOperationException();
	}

	@Override
	public float floatSum(){
		return this.sum;
	}

	@Override
	public float floatMax(){

		if(this.size == 0){
			throw new IllegalStateException();
		}

		return this.max;
	}

	@Override
	public float floatMedian(){
		throw new UnsupportedOperationException();
	}
}