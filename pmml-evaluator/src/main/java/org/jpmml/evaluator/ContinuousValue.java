/*
 * Copyright (c) 2013 Villu Ruusmann
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

import java.util.Collection;

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;

public class ContinuousValue extends FieldValue {

	ContinuousValue(DataType dataType, Object value){
		super(dataType, value);
	}

	@Override
	public OpType getOpType(){
		return OpType.CONTINUOUS;
	}

	@Override
	public int compareToString(String string){

		try {
			return super.compareToString(string);
		} catch(NumberFormatException nfeDefault){
			Object value;

			try {
				value = TypeUtil.parse(DataType.DOUBLE, string);
			} catch(NumberFormatException nfeDouble){
				throw nfeDefault;
			}

			return TypeUtil.compare(DataType.DOUBLE, asNumber(), value);
		}
	}

	@Override
	public int compareToValue(FieldValue value){
		return super.compareToValue(value);
	}

	static
	public ContinuousValue create(DataType dataType, Object value){

		if(value instanceof Collection){
			return new ContinuousValue(dataType, value);
		}

		switch(dataType){
			case INTEGER:
				return new ContinuousInteger((Integer)value);
			case FLOAT:
				return new ContinuousFloat((Float)value);
			case DOUBLE:
				return new ContinuousDouble((Double)value);
			default:
				return new ContinuousValue(dataType, value);
		}
	}

	static
	private class ContinuousInteger extends ContinuousValue implements Scalar {

		ContinuousInteger(Integer value){
			super(DataType.INTEGER, value);
		}

		@Override
		public Integer asInteger(){
			return (Integer)getValue();
		}

		@Override
		public Number asNumber(){
			return (Number)getValue();
		}
	}

	static
	private class ContinuousFloat extends ContinuousValue implements Scalar {

		ContinuousFloat(Float value){
			super(DataType.FLOAT, value);
		}

		@Override
		public Number asNumber(){
			return (Number)getValue();
		}
	}

	static
	private class ContinuousDouble extends ContinuousValue implements Scalar {

		ContinuousDouble(Double value){
			super(DataType.DOUBLE, value);
		}

		@Override
		public Number asNumber(){
			return (Number)getValue();
		}

		@Override
		public Double asDouble(){
			return (Double)getValue();
		}
	}
}