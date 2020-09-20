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

public class ContinuousValue extends ScalarValue {

	private ContinuousValue(){
	}

	ContinuousValue(DataType dataType, Object value){
		super(dataType, value);
	}

	@Override
	public OpType getOpType(){
		return OpType.CONTINUOUS;
	}

	@Override
	public int compareToValue(Object value){

		try {
			return super.compareToValue(value);
		} catch(IllegalArgumentException | TypeCheckException e){
			Number number;

			try {
				number = (Number)TypeUtil.parseOrCast(DataType.DOUBLE, value);
			} catch(NumberFormatException nfeDouble){
				throw e;
			}

			return ((Comparable)asDouble()).compareTo(number);
		}
	}

	@Override
	public int compareToValue(FieldValue value){
		return super.compareToValue(value);
	}

	static
	public FieldValue create(DataType dataType, Object value){

		if(value instanceof Collection){
			return new CollectionValue(dataType, OpType.CONTINUOUS, (Collection<?>)value);
		}

		switch(dataType){
			case INTEGER:
				return new ContinuousInteger(value);
			case FLOAT:
				return new ContinuousFloat(value);
			case DOUBLE:
				return new ContinuousDouble(value);
			default:
				return new ContinuousValue(dataType, value);
		}
	}

	static
	private class ContinuousInteger extends ContinuousValue {

		private ContinuousInteger(){
		}

		ContinuousInteger(Object value){
			super(DataType.INTEGER, value);
		}

		@Override
		public int compareToValue(Object value){

			if(value instanceof Integer){
				return (asInteger()).compareTo((Integer)value);
			}

			return super.compareToValue(value);
		}

		@Override
		public boolean equalsValue(Object value){

			if(value instanceof Integer){
				return (asInteger()).equals(value);
			}

			return super.equalsValue(value);
		}

		@Override
		public Number asNumber(){
			return (Number)getValue();
		}

		@Override
		public Integer asInteger(){
			return (Integer)getValue();
		}
	}

	static
	private class ContinuousFloat extends ContinuousValue {

		private ContinuousFloat(){
		}

		ContinuousFloat(Object value){
			super(DataType.FLOAT, value);

			Float floatValue = (Float)getValue();
			if(floatValue.isNaN()){
				setValid(false);
			}
		}

		@Override
		public int compareToValue(Object value){

			if(value instanceof Float){
				return (asFloat()).compareTo((Float)value);
			}

			return super.compareToValue(value);
		}

		@Override
		public boolean equalsValue(Object value){

			if(value instanceof Float){
				return (asFloat()).equals(value);
			}

			return super.equalsValue(value);
		}

		@Override
		public Number asNumber(){
			return (Number)getValue();
		}

		@Override
		public Float asFloat(){
			return (Float)getValue();
		}
	}

	static
	private class ContinuousDouble extends ContinuousValue {

		private ContinuousDouble(){
		}

		ContinuousDouble(Object value){
			super(DataType.DOUBLE, value);

			Double doubleValue = (Double)getValue();
			if(doubleValue.isNaN()){
				setValid(false);
			}
		}

		@Override
		public int compareToValue(Object value){

			if(value instanceof Double){
				return (asDouble()).compareTo((Double)value);
			}

			return super.compareToValue(value);
		}

		@Override
		public boolean equalsValue(Object value){

			if(value instanceof Double){
				return (asDouble()).equals(value);
			}

			return super.equalsValue(value);
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