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
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.dmg.pmml.Array;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Expression;
import org.dmg.pmml.Field;
import org.dmg.pmml.HasType;
import org.dmg.pmml.HasValue;
import org.dmg.pmml.HasValueSet;
import org.dmg.pmml.OpType;
import org.jpmml.model.ToStringHelper;
import org.jpmml.model.temporals.Date;
import org.jpmml.model.temporals.DateTime;
import org.jpmml.model.temporals.Instant;
import org.jpmml.model.temporals.Time;

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


	FieldValue(){
	}

	FieldValue(DataType dataType, Object value){
		setDataType(dataType);
		setValue(value);
	}

	abstract
	public boolean isValid();

	abstract
	public int compareToValue(Object value);

	abstract
	public int compareToValue(FieldValue value);

	public FieldValue cast(HasType<?> hasType){
		DataType dataType = hasType.getDataType();
		OpType opType = hasType.getOpType();

		boolean equal = true;

		if(dataType == null){
			dataType = getDataType();
		} else

		{
			equal &= (getDataType() == dataType);
		} // End if

		if(opType == null){
			opType = getOpType();
		} else

		{
			equal &= (getOpType() == opType);
		} // End if

		if(equal){
			return this;
		}

		return FieldValue.create(opType, dataType, getValue());
	}

	public FieldValue cast(TypeInfo typeInfo){
		DataType dataType = typeInfo.getDataType();
		OpType opType = typeInfo.getOpType();

		if((getDataType() == dataType) && (getOpType() == opType)){
			return this;
		}

		return FieldValue.create(typeInfo, getValue());
	}

	/**
	 * <p>
	 * Calculates the order between this value and the reference value.
	 * </p>
	 */
	public int compareTo(HasValue<?> hasValue){
		return compareToValue(hasValue.requireValue());
	}

	/**
	 * <p>
	 * Checks if this value is equal to the reference value.
	 * </p>
	 */
	public boolean equals(HasValue<?> hasValue){
		return equalsValue(hasValue.requireValue());
	}

	/**
	 * <p>
	 * Checks if this value is contained in the set of reference values.
	 * </p>
	 */
	public boolean isIn(HasValueSet<?> hasValueSet){
		Array array = hasValueSet.requireArray();

		if(array instanceof SetHolder){
			SetHolder setHolder = (SetHolder)array;

			return setHolder.contains(getDataType(), getValue());
		}

		List<?> values = ArrayUtil.getContent(array);

		for(int i = 0, max = values.size(); i < max; i++){
			Object value = values.get(i);

			if(equalsValue(value)){
				return true;
			}
		}

		return false;
	}

	public boolean isIn(Collection<FieldValue> values){

		for(FieldValue value : values){

			if(FieldValueUtil.isMissing(value)){
				continue;
			} // End if

			if(equalsValue(value)){
				return true;
			}
		}

		return false;
	}

	public boolean equalsValue(Object value){
		value = TypeUtil.parseOrCast(getDataType(), value);

		return (getValue()).equals(value);
	}

	public boolean equalsValue(FieldValue value){
		return equalsValue(value.getValue());
	}

	public Collection<?> asCollection(){
		return TypeUtil.cast(Collection.class, getValue());
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

	public Date asDate(){
		return (Date)getValue(DataType.DATE);
	}

	public Time asTime(){
		return (Time)getValue(DataType.TIME);
	}

	public DateTime asDateTime(){
		return (DateTime)getValue(DataType.DATE_TIME);
	}

	public Instant<?> asInstant(){
		DataType dataType = getDataType();

		switch(dataType){
			case DATE:
			case TIME:
			case DATE_TIME:
				return TypeUtil.cast(Instant.class, getValue());
			default:
				break;
		}

		try {
			return asDateTime();
		} catch(TypeCheckException tceDateTime){

			try {
				return asDate();
			} catch(TypeCheckException tceDate){
				// Ignored
			} // End try

			try {
				return asTime();
			} catch(TypeCheckException tceTime){
				// Ignored
			}

			throw tceDateTime;
		}
	}

	Object getValue(DataType dataType){

		if(getDataType() == dataType){
			return getValue();
		}

		return TypeUtil.parseOrCast(dataType, getValue());
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
		ToStringHelper helper = toStringHelper();

		return helper.toString();
	}

	protected ToStringHelper toStringHelper(){
		ToStringHelper helper = new ToStringHelper(this)
			.add("opType", getOpType())
			.add("dataType", getDataType())
			.add("valid", isValid())
			.add("value", getValue());

		return helper;
	}

	@Override
	public DataType getDataType(){
		return this.dataType;
	}

	private void setDataType(DataType dataType){
		this.dataType = Objects.requireNonNull(dataType);
	}

	public Object getValue(){
		return this.value;
	}

	private void setValue(Object value){
		this.value = Objects.requireNonNull(value);
	}

	static
	public FieldValue create(OpType opType, DataType dataType, Object value){

		if(opType == null || dataType == null){
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
		}

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

	static final Integer STATUS_UNKNOWN_VALID = Integer.MAX_VALUE;
	static final Integer STATUS_MISSING = 0;
	static final Integer STATUS_UNKNOWN_INVALID = Integer.MIN_VALUE;
}