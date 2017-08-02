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

abstract
public class FloatVector extends Vector<Float> {

	abstract
	public float floatValue(int index);

	abstract
	public float floatSum();

	abstract
	public float floatMax();

	abstract
	public float floatMedian();

	@Override
	public Vector<Float> add(Number value){
		return add(value.floatValue());
	}

	@Override
	public Vector<Float> add(double coefficient, Number factor){
		return add((float)coefficient * factor.floatValue());
	}

	@Override
	public Value<Float> get(int index){
		return new FloatValue(floatValue(index));
	}

	@Override
	public Value<Float> sum(){
		return new FloatValue(floatSum());
	}

	@Override
	public Value<Float> max(){
		return new FloatValue(floatMax());
	}

	@Override
	public Value<Float> median(){
		return new FloatValue(floatMedian());
	}
}