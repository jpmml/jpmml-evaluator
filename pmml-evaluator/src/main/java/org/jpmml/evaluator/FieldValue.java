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
import java.util.Objects;

import com.google.common.base.Objects.ToStringHelper;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

/**
 * <p>
 * A field value representation that meets the requirements of PMML type system.
 * </p>
 *
 * <p>
 * Type information has two components to it:
 * <ul>
 *   <li>{@link #getOpType() Operational type}. Determines supported type equality and type comparison operations.</li>
 *   <li>{@link #getDataType() Data type}. Determines supported type conversions.</li>
 * </ul>
 * </p>
 *
 * <p>
 * A field value is created after a {@link Field field}.
 * It may be later refined by {@link Expression transformations} and {@link Function functions}.
 * </p>
 *
 * @see FieldValueUtil
 */
abstract
public class FieldValue implements Comparable<FieldValue>, Serializable {

	private DataType dataType = null;

	private Object value = null;


	FieldValue(DataType dataType, Object value){
		setDataType(dataType);
		setValue(value);
	}

	abstract
	public OpType getOpType();

	@Override
	public int compareTo(FieldValue value){

		if(!(getOpType()).equals(value.getOpType()) || !(getDataType()).equals(value.getDataType())){
			throw new ClassCastException();
		}

		return compareToValue(value);
	}

	/**
	 * <p>
	 * Checks if this value is equal to the reference value.
	 * </p>
	 *
	 * @param string The reference value.
	 */
	public boolean equalsString(String string){
		Object value = parseValue(string);

		if(isScalar()){
			return (getValue()).equals(value);
		}

		return TypeUtil.equals(getDataType(), getValue(), value);
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

		if(sameScalarType(value)){
			return (getValue()).equals(value.getValue());
		}

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
	 * <p>
	 * Calculates the order between this value and the reference value.
	 * </p>
	 *
	 * @param string The reference value.
	 */
	public int compareToString(String string){
		Object value = parseValue(string);

		if(isScalar()){
			return ((Comparable)getValue()).compareTo(value);
		}

		return TypeUtil.compare(getDataType(), getValue(), value);
	}

	public int compareToValue(FieldValue value){

		if(sameScalarType(value)){
			return ((Comparable)getValue()).compareTo(value.getValue());
		}

		DataType dataType = TypeUtil.getResultDataType(getDataType(), value.getDataType());

		return TypeUtil.compare(dataType, getValue(), value.getValue());
	}

	public Object parseValue(String string){
		DataType dataType = getDataType();

		return TypeUtil.parse(dataType, string);
	}

	private boolean isScalar(){
		return (this instanceof Scalar);
	}

	private boolean sameScalarType(FieldValue value){

		if(isScalar()){
			return (getClass()).equals(value.getClass());
		}

		return false;
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
	public int hashCode(){
		return Objects.hash(getOpType(), getDataType(), getValue());
	}

	@Override
	public boolean equals(Object object){

		if(object instanceof FieldValue){
			FieldValue that = (FieldValue)object;

			return Objects.equals(this.getOpType(), that.getOpType()) && Objects.equals(this.getDataType(), that.getDataType()) && Objects.equals(this.getValue(), that.getValue());
		}

		return false;
	}

	@Override
	public String toString(){
		ToStringHelper helper = com.google.common.base.Objects.toStringHelper(this)
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

	static
	interface Scalar<V extends Comparable<V>> {

		V getValue();
	}
}