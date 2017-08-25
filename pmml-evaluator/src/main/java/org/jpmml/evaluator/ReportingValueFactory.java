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
public class ReportingValueFactory<V extends Number> extends ValueFactory<V> {

	protected ReportingValueFactory(){
	}

	static
	public ReportingValueFactory<?> getInstance(MathContext mathContext){

		switch(mathContext){
			case FLOAT:
				return ReportingValueFactory.FLOAT;
			case DOUBLE:
				return ReportingValueFactory.DOUBLE;
			default:
				throw new IllegalArgumentException();
		}
	}

	public static final ReportingValueFactory<Float> FLOAT = new ReportingValueFactory<Float>(){

		@Override
		public ReportingFloatValue newValue(double value){
			return new ReportingFloatValue((float)value);
		}

		@Override
		public ReportingFloatValue newValue(Number value){
			return new ReportingFloatValue(value.floatValue());
		}

		@Override
		public ReportingFloatValue newValue(String value){
			return new ReportingFloatValue(Float.parseFloat(value));
		}

		@Override
		public Vector<Float> newVector(int capacity){

			if(capacity > 0){
				return new ReportingComplexFloatVector(capacity);
			}

			return new ReportingSimpleFloatVector();
		}
	};

	public static final ReportingValueFactory<Double> DOUBLE = new ReportingValueFactory<Double>(){

		@Override
		public ReportingDoubleValue newValue(double value){
			return new ReportingDoubleValue(value);
		}

		@Override
		public ReportingDoubleValue newValue(Number value){
			return new ReportingDoubleValue(value.doubleValue());
		}

		@Override
		public ReportingDoubleValue newValue(String value){
			return new ReportingDoubleValue(Double.parseDouble(value));
		}

		@Override
		public Vector<Double> newVector(int capacity){

			if(capacity > 0){
				return new ReportingComplexDoubleVector(capacity);
			}

			return new ReportingSimpleDoubleVector();
		}
	};
}