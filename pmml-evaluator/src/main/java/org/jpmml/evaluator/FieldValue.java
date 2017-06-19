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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.dmg.pmml.Array;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Expression;
import org.dmg.pmml.Field;
import org.dmg.pmml.HasValue;
import org.dmg.pmml.HasValueSet;
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
		setDataType(Objects.requireNonNull(dataType));
		setValue(filterValue(Objects.requireNonNull(value)));
	}

	abstract
	public OpType getOpType();

	/**
	 * <p>
	 * Checks if this value is equal to the reference value.
	 * </p>
	 */
	public boolean equals(HasValue<?> hasValue){

		if(hasValue instanceof HasParsedValue){
			HasParsedValue<?> hasParsedValue = (HasParsedValue<?>)hasValue;

			return equals(hasParsedValue);
		}

		return equalsString(hasValue.getValue());
	}

	public boolean equals(HasParsedValue<?> hasParsedValue){
		FieldValue value = hasParsedValue.getValue(getDataType(), getOpType());

		return this.equals(value);
	}

	/**
	 * <p>
	 * Checks if this value is contained in the set of reference values.
	 * </p>
	 */
	public boolean isIn(HasValueSet<?> hasValueSet){

		if(hasValueSet instanceof HasParsedValueSet){
			HasParsedValueSet<?> hasParsedValueSet = (HasParsedValueSet<?>)hasValueSet;

			return isIn(hasParsedValueSet);
		}

		Array array = hasValueSet.getArray();

		List<String> content = ArrayUtil.getContent(array);

		return indexInStrings(content) > -1;
	}

	public boolean isIn(HasParsedValueSet<?> hasParsedValueSet){
		Set<FieldValue> values = hasParsedValueSet.getValueSet(getDataType(), getOpType());

		return values.contains(this);
	}

	/**
	 * <p>
	 * Calculates the order between this value and the reference value.
	 * </p>
	 */
	public int compareTo(HasValue<?> hasValue){

		if(hasValue instanceof HasParsedValue){
			HasParsedValue<?> hasParsedValue = (HasParsedValue<?>)hasValue;

			return compareTo(hasParsedValue);
		}

		return compareToString(hasValue.getValue());
	}

	public int compareTo(HasParsedValue<?> hasParsedValue){
		FieldValue value = hasParsedValue.getValue(getDataType(), getOpType());

		return this.compareTo(value);
	}

	public boolean equalsString(String string){
		Object value = parseValue(string);

		if(isScalar()){
			return (getValue()).equals(value);
		}

		return TypeUtil.equals(getDataType(), getValue(), value);
	}

	/**
	 * <p>
	 * A value-safe replacement for {@link #equals(FieldValue)}.
	 * </p>
	 */
	public boolean equalsValue(FieldValue value){

		if(sameScalarType(value)){
			return (getValue()).equals(value.getValue());
		}

		DataType dataType = TypeUtil.getResultDataType(getDataType(), value.getDataType());

		return TypeUtil.equals(dataType, getValue(), value.getValue());
	}

	public int indexInStrings(Iterable<String> strings){
		Predicate<String> predicate = new Predicate<String>(){

			@Override
			public boolean apply(String string){
				return equalsString(string);
			}
		};

		return Iterables.indexOf(strings, predicate);
	}

	public int indexInValues(Iterable<FieldValue> values){
		Predicate<FieldValue> predicate = new Predicate<FieldValue>(){

			@Override
			public boolean apply(FieldValue value){
				return equalsValue(value);
			}
		};

		return Iterables.indexOf(values, predicate);
	}

	public int compareToString(String string){
		Object value = parseValue(string);

		if(isScalar()){
			return ((Comparable)getValue()).compareTo(value);
		}

		return TypeUtil.compare(getDataType(), getValue(), value);
	}

	/**
	 * <p>
	 * A value-safe replacement for {@link #compareTo(FieldValue)}
	 * </p>
	 */
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

	public <V> V getMapping(HasParsedValueMapping<V> hasParsedValueMapping){
		Map<FieldValue, V> values = hasParsedValueMapping.getValueMapping(getDataType(), getOpType());

		return values.get(this);
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

	/**
	 * Getting the value of a field as {@link Double}:
	 * <pre>
	 * FieldValue value = ...;
	 * Double result = value.asDouble();
	 * </pre>
	 *
	 * Getting the value of a field as <code>double</code>:
	 * <pre>
	 * FieldValue value = ...;
	 * double result = (value.asNumber()).doubleValue();
	 * </pre>
	 *
	 * @see #asNumber()
	 */
	public Double asDouble(){
		Number number = asNumber();

		return number.doubleValue();
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
	public int compareTo(FieldValue that){

		if((this.getOpType() != that.getOpType()) || (this.getDataType() != that.getDataType())){
			throw new ClassCastException();
		}

		return compareToValue(that);
	}

	@Override
	public int hashCode(){
		return (31 * (getOpType().hashCode() ^ getDataType().hashCode())) + getValue().hashCode();
	}

	@Override
	public boolean equals(Object object){

		if(object instanceof FieldValue){
			FieldValue that = (FieldValue)object;

			return (this.getOpType() == that.getOpType()) && (this.getDataType() == that.getDataType()) && (this.getValue()).equals(that.getValue());
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
		this.dataType = dataType;
	}

	public Object getValue(){
		return this.value;
	}

	private void setValue(Object value){
		this.value = value;
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

	static
	interface Scalar {
	}
}