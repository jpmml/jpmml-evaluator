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

public class CategoricalValue extends DiscreteValue {

	private CategoricalValue(){
	}

	CategoricalValue(DataType dataType, Object value){
		super(dataType, value);
	}

	@Override
	public OpType getOpType(){
		return OpType.CATEGORICAL;
	}

	@Override
	public int compareToValue(Object value){
		throw new EvaluationException("Categorical value cannot be used in comparison operations");
	}

	@Override
	public int compareToValue(FieldValue value){
		throw new EvaluationException("Categorical value cannot be used in comparison operations");
	}

	static
	public FieldValue create(DataType dataType, Object value){

		if(value instanceof Collection){
			return new CollectionValue(dataType, OpType.CATEGORICAL, (Collection<?>)value);
		}

		switch(dataType){
			case STRING:
				return new CategoricalString(value);
			case BOOLEAN:
				return new CategoricalBoolean(value);
			default:
				return new CategoricalValue(dataType, value);
		}
	}

	static
	private class CategoricalString extends CategoricalValue {

		private CategoricalString(){
		}

		CategoricalString(Object value){
			super(DataType.STRING, value);
		}

		@Override
		public boolean equalsValue(Object value){

			if(value instanceof String){
				return (asString()).equals(value);
			}

			return super.equalsValue(value);
		}

		@Override
		public String asString(){
			return (String)getValue();
		}
	}

	static
	private class CategoricalBoolean extends CategoricalValue {

		private CategoricalBoolean(){
		}

		CategoricalBoolean(Object value){
			super(DataType.BOOLEAN, value);
		}

		@Override
		public int compareToValue(Object value){

			if(value instanceof Boolean){
				return Boolean.compare(asBoolean(), (Boolean)value);
			}

			Number number;

			try {
				number = (Number)TypeUtil.parseOrCast(DataType.DOUBLE, value);
			} catch(NumberFormatException nfe){
				throw nfe;
			} catch(TypeCheckException tce){
				throw new TypeCheckException(DataType.BOOLEAN, value);
			}

			return ((Comparable)asDouble()).compareTo(number);
		}

		@Override
		public int compareToValue(FieldValue value){

			if(value instanceof ScalarValue){
				ScalarValue that = (ScalarValue)value;

				if((DataType.BOOLEAN).equals(that.getDataType())){
					return Boolean.compare(this.asBoolean(), that.asBoolean());
				}
			}

			return compareToValue(value.getValue());
		}

		@Override
		public boolean equalsValue(Object value){

			if(value instanceof Boolean){
				return (asBoolean()).equals(value);
			}

			return super.equalsValue(value);
		}

		@Override
		public Boolean asBoolean(){
			return (Boolean)getValue();
		}
	}
}