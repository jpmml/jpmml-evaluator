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

public class CategoricalValue extends FieldValue {

	CategoricalValue(DataType dataType, Object value){
		super(dataType, value);
	}

	@Override
	public OpType getOpType(){
		return OpType.CATEGORICAL;
	}

	@Override
	public int compareToString(String string){
		throw new EvaluationException();
	}

	@Override
	public int compareToValue(FieldValue value){
		throw new EvaluationException();
	}

	static
	public CategoricalValue create(DataType dataType, Object value){

		if(value instanceof Collection){
			return new CategoricalValue(dataType, value);
		}

		switch(dataType){
			case STRING:
				return new CategoricalString((String)value);
			case BOOLEAN:
				return new CategoricalBoolean((Boolean)value);
			default:
				return new CategoricalValue(dataType, value);
		}
	}

	static
	private class CategoricalString extends CategoricalValue implements Scalar {

		CategoricalString(String value){
			super(DataType.STRING, value);
		}

		@Override
		public String asString(){
			return (String)getValue();
		}
	}

	static
	private class CategoricalBoolean extends CategoricalValue implements Scalar {

		CategoricalBoolean(Boolean value){
			super(DataType.BOOLEAN, value);
		}

		@Override
		public int compareToString(String string){
			Number number;

			try {
				number = (Number)TypeUtil.parse(DataType.DOUBLE, string);
			} catch(NumberFormatException nfe){
				throw new TypeCheckException(DataType.DOUBLE, string);
			}

			return TypeUtil.compare(DataType.DOUBLE, asBoolean(), number);
		}

		@Override
		public int compareToValue(FieldValue value){
			Number number;

			try {
				number = value.asNumber();
			} catch(TypeCheckException tce){
				throw new TypeCheckException(DataType.DOUBLE, value.getValue());
			}

			return TypeUtil.compare(DataType.DOUBLE, asBoolean(), number);
		}

		@Override
		public Boolean asBoolean(){
			return (Boolean)getValue();
		}
	}
}