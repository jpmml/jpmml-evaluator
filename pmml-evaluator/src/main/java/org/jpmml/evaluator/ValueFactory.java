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

import org.dmg.pmml.MathContext;

abstract
public class ValueFactory<V extends Number> {

	protected ValueFactory(){
	}

	abstract
	public Value<V> newValue(double value);

	abstract
	public Value<V> newValue(Number number);

	abstract
	public Value<V> newValue(String string);

	static
	public ValueFactory<?> getInstance(MathContext mathContext){

		switch(mathContext){
			case FLOAT:
				return ValueFactory.FLOAT;
			case DOUBLE:
				return ValueFactory.DOUBLE;
			default:
				throw new IllegalArgumentException();
		}
	}

	public static final ValueFactory<Float> FLOAT = new ValueFactory<Float>(){

		@Override
		public FloatValue newValue(double value){
			return new FloatValue((float)value);
		}

		@Override
		public FloatValue newValue(Number value){
			return new FloatValue(value.floatValue());
		}

		@Override
		public FloatValue newValue(String string){
			return new FloatValue(Float.parseFloat(string));
		}
	};

	public static final ValueFactory<Double> DOUBLE = new ValueFactory<Double>(){

		@Override
		public DoubleValue newValue(double value){
			return new DoubleValue(value);
		}

		@Override
		public DoubleValue newValue(Number value){
			return new DoubleValue(value.doubleValue());
		}

		@Override
		public DoubleValue newValue(String string){
			return new DoubleValue(Double.parseDouble(string));
		}
	};
}