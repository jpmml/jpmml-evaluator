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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.dmg.pmml.Array;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Expression;
import org.dmg.pmml.Field;
import org.dmg.pmml.HasValue;
import org.dmg.pmml.HasValueSet;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMMLObject;
import org.jpmml.model.ToStringHelper;

/**
 * <p>
 * A field value representation that meets the requirements of PMML type system.
 * </p>
 *
 * Type information has two components to it:
 * <ul>
 *   <li>{@link #getOpType() Operational type}. Determines supported type equality and type comparison operations.</li>
 *   <li>{@link #getDataType() Data type}. Determines supported type conversions.</li>
 * </ul>
 *
 * <p>
 * A field value is created after a {@link Field field}.
 * It may be later refined by {@link Expression transformations} and {@link Function functions}.
 * </p>
 *
 * @see FieldValueUtil
 */
abstract
public class FieldValue implements TypeInfo, Serializable {

	private DataType dataType = null;

	private Object value = null;


	FieldValue(DataType dataType, Object value){
		setDataType(Objects.requireNonNull(dataType));
		setValue(Objects.requireNonNull(value));
	}

	abstract
	public int compareToValue(Object value);

	abstract
	public int compareToValue(FieldValue value);

	public FieldValue cast(DataType dataType, OpType opType){
		boolean compatible = true;

		if(dataType == null){
			dataType = getDataType();
		} else

		if(dataType != null && !(dataType).equals(getDataType())){
			compatible = false;
		} // End if

		if(opType == null){
			opType = getOpType();
		} else

		if(opType != null && !(opType).equals(getOpType())){
			compatible = false;
		} // End if

		if(compatible){
			return this;
		}

		return FieldValue.create(dataType, opType, getValue());
	}

	public FieldValue cast(TypeInfo typeInfo){
		DataType dataType = typeInfo.getDataType();
		OpType opType = typeInfo.getOpType();

		if((dataType).equals(getDataType()) && (opType).equals(getOpType())){
			return this;
		}

		return FieldValue.create(typeInfo, getValue());
	}

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

		return equalsValue(ensureValue(hasValue));
	}

	public boolean equals(HasParsedValue<?> hasParsedValue){
		FieldValue value = hasParsedValue.getValue(this);

		return equalsValue(value);
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
		if(array == null){
			throw new MissingElementException(MissingElementException.formatMessage(XPathUtil.formatElement((Class)hasValueSet.getClass()) + "/" + XPathUtil.formatElement(Array.class)), (PMMLObject)hasValueSet);
		}

		List<?> values = ArrayUtil.getContent(array);

		return values.stream()
			.anyMatch(value -> equalsValue(value));
	}

	public boolean isIn(HasParsedValueSet<?> hasParsedValueSet){
		Set<FieldValue> values = hasParsedValueSet.getValueSet(this);

		// XXX
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

		return compareToValue(ensureValue(hasValue));
	}

	public int compareTo(HasParsedValue<?> hasParsedValue){
		FieldValue value = hasParsedValue.getValue(this);

		return compareToValue(value);
	}

	public boolean equalsValue(Object value){
		value = TypeUtil.parseOrCast(getDataType(), value);

		return (getValue()).equals(value);
	}

	public boolean equalsValue(FieldValue value){
		return equalsValue(value.getValue());
	}

	public boolean isIn(Collection<FieldValue> values){
		Predicate<FieldValue> predicate = new Predicate<FieldValue>(){

			@Override
			public boolean test(FieldValue value){

				if(FieldValueUtil.isMissing(value)){
					return false;
				}

				return equalsValue(value);
			}
		};

		return values.stream()
			.anyMatch(predicate);
	}

	public <V> V getMapping(HasParsedValueMapping<V> hasParsedValueMapping){
		Map<FieldValue, V> values = hasParsedValueMapping.getValueMapping(this);

		return values.get(this);
	}

	public String asString(){
		return (String)getValue(DataType.STRING);
	}

	public Number asNumber(){
		Object value = getValue();

		if(value instanceof Number){
			return (Number)value;
		}

		return (Number)getValue(DataType.DOUBLE);
	}

	public Integer asInteger(){
		return (Integer)getValue(DataType.INTEGER);
	}

	public Float asFloat(){
		Number number = asNumber();

		return number.floatValue();
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

	public ZonedDateTime asZonedDateTime(ZoneId zoneId){

		try {
			LocalDateTime dateTime = asLocalDateTime();

			return dateTime.atZone(zoneId);
		} catch(TypeCheckException tceDateTime){

			try {
				LocalDate localDate = asLocalDate();
				LocalTime localTime = LocalTime.MIDNIGHT;

				return ZonedDateTime.of(localDate, localTime, zoneId);
			} catch(TypeCheckException tceDate){
				// Ignored
			}

			try {
				LocalDate localDate = LocalDate.now();
				LocalTime localTime = asLocalTime();

				return ZonedDateTime.of(localDate, localTime, zoneId);
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
		return (31 * (getOpType().hashCode() ^ getDataType().hashCode())) + getValue().hashCode();
	}

	@Override
	public boolean equals(Object object){

		if(object instanceof FieldValue){
			FieldValue that = (FieldValue)object;

			return (this.getOpType()).equals(that.getOpType()) && (this.getDataType()).equals(that.getDataType()) && (this.getValue()).equals(that.getValue());
		}

		return false;
	}

	@Override
	public String toString(){
		ToStringHelper helper = toStringHelper();

		return helper.toString();
	}

	protected ToStringHelper toStringHelper(){
		ToStringHelper helper = new ToStringHelper(this)
			.add("opType", getOpType())
			.add("dataType", getDataType())
			.add("value", getValue());

		return helper;
	}

	@Override
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
	public FieldValue create(DataType dataType, OpType opType, Object value){

		if(dataType == null || opType == null){
			throw new IllegalArgumentException();
		}

		switch(opType){
			case CONTINUOUS:
				return ContinuousValue.create(dataType, value);
			case CATEGORICAL:
				return CategoricalValue.create(dataType, value);
			case ORDINAL:
				return OrdinalValue.create(dataType, (List<?>)null, value);
			default:
				throw new IllegalArgumentException();
		}
	}

	static
	public FieldValue create(TypeInfo typeInfo, Object value){

		if(typeInfo == null){
			throw new IllegalArgumentException();
		} // End if

		DataType dataType = typeInfo.getDataType();
		OpType opType = typeInfo.getOpType();

		switch(opType){
			case CONTINUOUS:
				return ContinuousValue.create(dataType, value);
			case CATEGORICAL:
				return CategoricalValue.create(dataType, value);
			case ORDINAL:
				List<?> ordering = typeInfo.getOrdering();

				return OrdinalValue.create(dataType, ordering, value);
			default:
				throw new IllegalArgumentException();
		}
	}

	static
	private Object ensureValue(HasValue<?> hasValue){
		Object value = hasValue.getValue();
		if(value == null){
			throw new MissingAttributeException(MissingAttributeException.formatMessage(XPathUtil.formatElement((Class)hasValue.getClass()) + "@value"), (PMMLObject)hasValue);
		}

		return value;
	}
}