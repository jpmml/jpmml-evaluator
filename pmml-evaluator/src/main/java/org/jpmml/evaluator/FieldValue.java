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

import java.io.Serializable;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

abstract
public class FieldValue implements Serializable {

	private DataType dataType = null;

	private Object value = null;


	public FieldValue(DataType dataType, Object value){
		setDataType(dataType);
		setValue(value);
	}

	abstract
	public OpType getOpType();

	/**
	 * Checks if this value is equal to the reference value.
	 *
	 * @param string The reference value.
	 */
	public boolean equalsString(String string){
		return TypeUtil.equals(getDataType(), getValue(), parseValue(string));
	}

	public boolean equalsAnyString(Iterable<String> strings){

		for(String string : strings){
			boolean equals = equalsString(string);

			if(equals){
				return true;
			}
		}

		return false;
	}

	public boolean equalsValue(FieldValue value){
		DataType dataType = TypeUtil.getResultDataType(getDataType(), value.getDataType());

		return TypeUtil.equals(dataType, getValue(), value.getValue());
	}

	public boolean equalsAnyValue(Iterable<FieldValue> values){

		for(FieldValue value : values){
			boolean equals = equalsValue(value);

			if(equals){
				return true;
			}
		}

		return false;
	}

	/**
	 * Calculates the order between this value and the reference value.
	 *
	 * @param string The reference value.
	 */
	public int compareToString(String string){
		return TypeUtil.compare(getDataType(), getValue(), parseValue(string));
	}

	public int compareToValue(FieldValue value){
		DataType dataType = TypeUtil.getResultDataType(getDataType(), value.getDataType());

		return TypeUtil.compare(dataType, getValue(), value.getValue());
	}

	protected Object parseValue(String string){
		DataType dataType = getDataType();

		return TypeUtil.parse(dataType, string);
	}

	public String asString(){
		return (String)getValue(DataType.STRING);
	}

	public Integer asInteger(){
		return (Integer)getValue(DataType.INTEGER);
	}

	public Number asNumber(){
		Object value = getValue();

		if(value instanceof Number){
			return (Number)value;
		}

		return (Double)getValue(DataType.DOUBLE);
	}

	public Boolean asBoolean(){
		return (Boolean)getValue(DataType.BOOLEAN);
	}

	public LocalDateTime asLocalDateTime(){
		return (LocalDateTime)getValue(DataType.DATE_TIME);
	}

	public LocalDate asLocalDate(){
		return (LocalDate)getValue(DataType.DATE);
	}

	public LocalTime asLocalTime(){
		return (LocalTime)getValue(DataType.TIME);
	}

	public DateTime asDateTime(){

		try {
			LocalDateTime dateTime = asLocalDateTime();

			return dateTime.toDateTime();
		} catch(TypeCheckException tceDateTime){

			try {
				LocalDate date = asLocalDate();

				return date.toDateTimeAtStartOfDay();
			} catch(TypeCheckException tceDate){
				// Ignored
			}

			try {
				LocalTime time = asLocalTime();

				return time.toDateTimeToday();
			} catch(TypeCheckException tceTime){
				// Ignored
			}

			throw tceDateTime;
		}
	}

	private Object getValue(DataType dataType){
		Object value = getValue();

		try {
			return TypeUtil.cast(dataType, value);
		} catch(TypeCheckException tce){

			try {
				if(value instanceof String){
					String string = (String)value;

					return TypeUtil.parse(dataType, string);
				}
			} catch(IllegalArgumentException iae){
				// Ignored
			}

			throw tce;
		}
	}

	@Override
	public String toString(){
		ToStringHelper helper = Objects.toStringHelper(getClass())
			.add("opType", getOpType())
			.add("dataType", getDataType())
			.add("value", getValue());

		return helper.toString();
	}

	public DataType getDataType(){
		return this.dataType;
	}

	private void setDataType(DataType dataType){

		if(dataType == null){
			throw new NullPointerException();
		}

		this.dataType = dataType;
	}

	public Object getValue(){
		return this.value;
	}

	private void setValue(Object value){

		if(value == null){
			throw new NullPointerException();
		}

		this.value = value;
	}
}