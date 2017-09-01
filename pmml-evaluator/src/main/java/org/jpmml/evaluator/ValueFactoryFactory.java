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

import java.io.Serializable;

import org.dmg.pmml.MathContext;

public class ValueFactoryFactory implements Serializable {

	protected ValueFactoryFactory(){
	}

	public ValueFactory<?> newValueFactory(MathContext mathContext){

		switch(mathContext){
			case FLOAT:
				return FloatValueFactory.INSTANCE;
			case DOUBLE:
				return DoubleValueFactory.INSTANCE;
			default:
				throw new IllegalArgumentException();
		}
	}

	static
	public ValueFactoryFactory newInstance(){
		return new ValueFactoryFactory();
	}

	static
	protected class FloatValueFactory extends ValueFactory<Float> {

		@Override
		public Value<Float> newValue(){
			return new FloatValue(0f);
		}

		@Override
		public Value<Float> newValue(double value){
			return new FloatValue((float)value);
		}

		@Override
		public Value<Float> newValue(Number value){
			return new FloatValue(value.floatValue());
		}

		@Override
		public Value<Float> newValue(String value){
			return new FloatValue(Float.parseFloat(value));
		}

		@Override
		public Vector<Float> newVector(int capacity){

			if(capacity > 0){
				return new ComplexFloatVector(capacity);
			}

			return new SimpleFloatVector();
		}

		public static final FloatValueFactory INSTANCE = new FloatValueFactory();
	}

	static
	protected class DoubleValueFactory extends ValueFactory<Double> {

		@Override
		public Value<Double> newValue(){
			return new DoubleValue(0d);
		}

		@Override
		public Value<Double> newValue(double value){
			return new DoubleValue(value);
		}

		@Override
		public Value<Double> newValue(Number value){
			return new DoubleValue(value.doubleValue());
		}

		@Override
		public Value<Double> newValue(String value){
			return new DoubleValue(Double.parseDouble(value));
		}

		@Override
		public Vector<Double> newVector(int capacity){

			if(capacity > 0){
				return new ComplexDoubleVector(capacity);
			}

			return new SimpleDoubleVector();
		}

		public static final DoubleValueFactory INSTANCE = new DoubleValueFactory();
	};
}