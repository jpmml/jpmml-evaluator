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
import java.util.List;
import java.util.Objects;

import com.google.common.collect.Lists;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Field;
import org.dmg.pmml.HasDiscreteDomain;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Target;

public class FieldValueUtil {

	private FieldValueUtil(){
	}

	static
	public OpType getOpType(Field<?> field, MiningField miningField){
		OpType opType = field.getOpType();

		// "A MiningField overrides a (Data)Field"
		if(miningField != null){
			opType = firstNonNull(miningField.getOpType(), opType);
		}

		return opType;
	}

	static
	public OpType getOpType(Field<?> field, MiningField miningField, Target target){
		OpType opType = field.getOpType();

		// "A MiningField overrides a (Data)Field, and a Target overrides a MiningField"
		if(miningField != null){
			opType = firstNonNull(miningField.getOpType(), opType);

			if(target != null){
				opType = firstNonNull(target.getOpType(), opType);
			}
		}

		return opType;
	}

	static
	public FieldValue create(Object value){

		if(value == null){
			return FieldValues.MISSING_VALUE;
		}

		DataType dataType;

		if(value instanceof Collection){
			dataType = TypeUtil.getDataType((Collection<?>)value);
		} else

		{
			dataType = TypeUtil.getDataType(value);
		}

		OpType opType = TypeUtil.getOpType(dataType);

		return createInternal(dataType, opType, value);
	}

	static
	public FieldValue create(Field<?> field, Object value){

		if(value == null){
			return FieldValues.MISSING_VALUE;
		}

		DataType dataType = field.getDataType();
		if(dataType == null){

			if(value instanceof Collection){
				dataType = TypeUtil.getDataType((Collection<?>)value);
			} else

			{
				dataType = TypeUtil.getDataType(value);
			}
		} else

		{
			if(value instanceof Collection){
				// Ignored
			} else

			{
				value = TypeUtil.parseOrCast(dataType, value);
			}
		}

		OpType opType = field.getOpType();
		if(opType == null){
			opType = TypeUtil.getOpType(dataType);
		}

		FieldValue fieldValue = createInternal(dataType, opType, value);

		return enhance(field, fieldValue);
	}

	static
	public FieldValue create(DataType dataType, OpType opType, Object value){

		if(value == null){
			return FieldValues.MISSING_VALUE;
		} // End if

		if(dataType == null || opType == null){
			throw new IllegalArgumentException();
		} // End if

		if(value instanceof Collection){
			// Ignored
		} else

		{
			value = TypeUtil.parseOrCast(dataType, value);
		}

		return createInternal(dataType, opType, value);
	}

	static
	public List<FieldValue> createAll(DataType dataType, OpType opType, List<?> values){
		return Lists.transform(values, value -> create(dataType, opType, value));
	}

	static
	public FieldValue refine(Field<?> field, FieldValue value){
		FieldValue result = refine(field.getDataType(), field.getOpType(), value);

		if(result != value){
			return enhance(field, result);
		}

		return result;
	}

	static
	public FieldValue refine(DataType dataType, OpType opType, FieldValue value){

		if(Objects.equals(FieldValues.MISSING_VALUE, value)){
			return FieldValues.MISSING_VALUE;
		}

		DataType valueDataType = value.getDataType();
		OpType valueOpType = value.getOpType();

		DataType refinedDataType = firstNonNull(dataType, valueDataType);
		OpType refinedOpType = firstNonNull(opType, valueOpType);

		if((refinedDataType).equals(valueDataType)){

			if((refinedOpType).equals(valueOpType)){
				return value;
			}

			return createInternal(refinedDataType, refinedOpType, value.getValue());
		}

		return create(refinedDataType, refinedOpType, value.getValue());
	}

	static
	FieldValue createOrRefine(DataType dataType, OpType opType, Object value){

		if(value instanceof FieldValue){
			FieldValue fieldValue = (FieldValue)value;

			return refine(dataType, opType, fieldValue);
		} else

		{
			return create(dataType, opType, value);
		}
	}

	static
	FieldValue enhance(Field<?> field, FieldValue value){

		if(value instanceof OrdinalValue){
			OrdinalValue ordinalValue = (OrdinalValue)value;

			List<?> ordering = null;

			if(field instanceof HasDiscreteDomain){
				ordering = FieldUtil.getValidValues((Field & HasDiscreteDomain)field);
			}

			ordinalValue.setOrdering(ordering);
		}

		return value;
	}

	static
	FieldValue createInternal(DataType dataType, OpType opType, Object value){

		switch(opType){
			case CONTINUOUS:
				return ContinuousValue.create(dataType, value);
			case CATEGORICAL:
				return CategoricalValue.create(dataType, value);
			case ORDINAL:
				return OrdinalValue.create(dataType, value);
			default:
				throw new IllegalArgumentException();
		}
	}

	static
	public Object getValue(FieldValue value){
		return (value != null ? value.getValue() : null);
	}

	static
	public <V> V getValue(Class<? extends V> clazz, FieldValue value){
		return TypeUtil.cast(clazz, getValue(value));
	}

	static
	public boolean equals(DataType dataType, Object value, String referenceValue){

		try {
			return (TypeUtil.parseOrCast(dataType, value)).equals(TypeUtil.parse(dataType, referenceValue));
		} catch(IllegalArgumentException | TypeCheckException e){

			// The String representation of invalid or missing values (eg. "N/A") may not be parseable to the requested representation
			try {
				return (TypeUtil.parseOrCast(DataType.STRING, value)).equals(referenceValue);
			} catch(TypeCheckException tce){
				// Ignored
			}

			throw e;
		}
	}

	static
	private <V> V firstNonNull(V value, V defaultValue){

		if(value != null){
			return value;
		}

		return defaultValue;
	}
}