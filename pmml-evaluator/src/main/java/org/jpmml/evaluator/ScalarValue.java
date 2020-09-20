/*
 * Copyright (c) 2019 Villu Ruusmann
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

import org.dmg.pmml.DataType;

abstract
public class ScalarValue extends FieldValue implements Comparable<ScalarValue> {

	private boolean valid = true;


	ScalarValue(){
	}

	ScalarValue(DataType dataType, Object value){
		super(dataType, filterValue(TypeUtil.parseOrCast(dataType, value)));
	}

	@Override
	public boolean isValid(){
		return this.valid;
	}

	void setValid(boolean valid){
		this.valid = valid;
	}

	@Override
	public int compareToValue(Object value){
		value = TypeUtil.parseOrCast(getDataType(), value);

		return ((Comparable)getValue()).compareTo(value);
	}

	@Override
	public int compareToValue(FieldValue value){

		if(value instanceof ScalarValue){
			ScalarValue that = (ScalarValue)value;

			if((this.getDataType()).equals(that.getDataType())){
				return ((Comparable)this.getValue()).compareTo(that.getValue());
			}
		}

		return compareToValue(value.getValue());
	}

	@Override
	public boolean equalsValue(FieldValue value){

		if(value instanceof ScalarValue){
			ScalarValue that = (ScalarValue)value;

			if((this.getDataType()).equals(that.getDataType())){
				return (this.getValue()).equals(that.getValue());
			}
		}

		return super.equalsValue(value);
	}

	@Override
	public int compareTo(ScalarValue that){

		if(!(this.getOpType()).equals(that.getOpType()) || !(this.getDataType()).equals(that.getDataType())){
			throw new ClassCastException();
		}

		return compareToValue(that);
	}

	static
	private Object filterValue(Object value){

		if(value instanceof Float){
			return filterValue((Float)value);
		} else

		if(value instanceof Double){
			return filterValue((Double)value);
		}

		return value;
	}

	static
	private Float filterValue(Float value){

		if(value.doubleValue() == 0f){
			return Numbers.FLOAT_ZERO;
		}

		return value;
	}

	static
	private Double filterValue(Double value){

		if(value.doubleValue() == 0d){
			return Numbers.DOUBLE_ZERO;
		}

		return value;
	}
}