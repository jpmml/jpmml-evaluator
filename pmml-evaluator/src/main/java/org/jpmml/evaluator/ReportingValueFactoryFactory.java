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

public class ReportingValueFactoryFactory extends ValueFactoryFactory {

	protected ReportingValueFactoryFactory(){
	}

	@Override
	public ReportingValueFactory<?> newValueFactory(MathContext mathContext){

		switch(mathContext){
			case FLOAT:
				return ReportingFloatValueFactory.INSTANCE;
			case DOUBLE:
				return ReportingDoubleValueFactory.INSTANCE;
			default:
				throw new IllegalArgumentException();
		}
	}

	static
	public ReportingValueFactoryFactory newInstance(){
		return new ReportingValueFactoryFactory();
	}

	static
	protected class ReportingFloatValueFactory extends ReportingValueFactory<Float> {

		@Override
		public Value<Float> newValue(double value){
			return new ReportingFloatValue((float)value);
		}

		@Override
		public Value<Float> newValue(Number value){
			return new ReportingFloatValue(value.floatValue());
		}

		@Override
		public Value<Float> newValue(String value){
			return new ReportingFloatValue(Float.parseFloat(value));
		}

		@Override
		public Vector<Float> newVector(int capacity){

			if(capacity > 0){
				return new ReportingComplexFloatVector(capacity);
			}

			return new ReportingSimpleFloatVector();
		}

		public static final ReportingFloatValueFactory INSTANCE = new ReportingFloatValueFactory();
	}

	static
	protected class ReportingDoubleValueFactory extends ReportingValueFactory<Double> {

		@Override
		public Value<Double> newValue(double value){
			return new ReportingDoubleValue(value);
		}

		@Override
		public Value<Double> newValue(Number value){
			return new ReportingDoubleValue(value.doubleValue());
		}

		@Override
		public Value<Double> newValue(String value){
			return new ReportingDoubleValue(Double.parseDouble(value));
		}

		@Override
		public Vector<Double> newVector(int capacity){

			if(capacity > 0){
				return new ReportingComplexDoubleVector(capacity);
			}

			return new ReportingSimpleDoubleVector();
		}

		public static final ReportingDoubleValueFactory INSTANCE = new ReportingDoubleValueFactory();
	}
}