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

	private ReportFactory reportFactory = null;


	protected ReportingValueFactoryFactory(){
	}

	@Override
	public ReportingValueFactory<?> newValueFactory(MathContext mathContext){
		ReportFactory reportFactory = getReportFactory();

		switch(mathContext){
			case FLOAT:
				return new ReportingFloatValueFactory(reportFactory);
			case DOUBLE:
				return new ReportingDoubleValueFactory(reportFactory);
			default:
				throw new IllegalArgumentException();
		}
	}

	public ReportFactory getReportFactory(){
		return this.reportFactory;
	}

	public void setReportFactory(ReportFactory reportFactory){
		this.reportFactory = reportFactory;
	}

	static
	public ReportingValueFactoryFactory newInstance(){
		return new ReportingValueFactoryFactory();
	}

	static
	protected class ReportingFloatValueFactory extends ReportingValueFactory<Float> {

		protected ReportingFloatValueFactory(ReportFactory reportFactory){
			super(reportFactory);
		}

		@Override
		public Value<Float> newValue(){
			return new ReportingFloatValue(0f, newReport(), null);
		}

		@Override
		public Value<Float> newValue(double value){
			return new ReportingFloatValue((float)value, newReport());
		}

		@Override
		public Value<Float> newValue(Number value){
			return new ReportingFloatValue(value.floatValue(), newReport());
		}

		@Override
		public Value<Float> newValue(String value){
			return new ReportingFloatValue(Float.parseFloat(value), newReport());
		}

		@Override
		public Vector<Float> newVector(int capacity){

			if(capacity > 0){
				return new ReportingComplexFloatVector(capacity){

					@Override
					protected Report newReport(){
						return ReportingFloatValueFactory.this.newReport();
					}
				};
			}

			return new ReportingSimpleFloatVector(){

				@Override
				protected Report newReport(){
					return ReportingFloatValueFactory.this.newReport();
				}
			};
		}
	}

	static
	protected class ReportingDoubleValueFactory extends ReportingValueFactory<Double> {

		protected ReportingDoubleValueFactory(ReportFactory reportFactory){
			super(reportFactory);
		}

		@Override
		public Value<Double> newValue(){
			return new ReportingDoubleValue(0d, newReport(), null);
		}

		@Override
		public Value<Double> newValue(double value){
			return new ReportingDoubleValue(value, newReport());
		}

		@Override
		public Value<Double> newValue(Number value){
			return new ReportingDoubleValue(value.doubleValue(), newReport());
		}

		@Override
		public Value<Double> newValue(String value){
			return new ReportingDoubleValue(Double.parseDouble(value), newReport());
		}

		@Override
		public Vector<Double> newVector(int capacity){

			if(capacity > 0){
				return new ReportingComplexDoubleVector(capacity){

					@Override
					protected  Report newReport(){
						return ReportingDoubleValueFactory.this.newReport();
					}
				};
			}

			return new ReportingSimpleDoubleVector(){

				@Override
				protected  Report newReport(){
					return ReportingDoubleValueFactory.this.newReport();
				}
			};
		}
	}
}