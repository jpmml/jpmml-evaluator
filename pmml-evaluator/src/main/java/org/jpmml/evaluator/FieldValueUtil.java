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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Field;
import org.dmg.pmml.OpType;
import org.dmg.pmml.TypeDefinitionField;

public class FieldValueUtil {

	private FieldValueUtil(){
	}

	static
	public FieldValue create(Object value){
		return create(null, null, value);
	}

	static
	public FieldValue create(Field field, Object value){
		FieldValue result = create(field.getDataType(), field.getOptype(), value);

		if(field instanceof TypeDefinitionField){
			return enhance((TypeDefinitionField)field, result);
		}

		return result;
	}

	static
	public FieldValue create(DataType dataType, OpType opType, Object value){

		if(value == null){
			return null;
		} // End if

		if(value instanceof Collection){

			if(dataType == null){
				dataType = DataType.STRING;
			} // End if

			if(opType == null){
				opType = OpType.CATEGORICAL;
			}
		} else

		{
			if(dataType == null){
				dataType = TypeUtil.getDataType(value);
			} else

			{
				value = TypeUtil.parseOrCast(dataType, value);
			} // End if

			if(opType == null){
				opType = TypeUtil.getOpType(dataType);
			}
		}

		switch(opType){
			case CONTINUOUS:
				return new ContinuousValue(dataType, value);
			case CATEGORICAL:
				return new CategoricalValue(dataType, value);
			case ORDINAL:
				return new OrdinalValue(dataType, value);
			default:
				break;
		}

		throw new EvaluationException();
	}

	static
	public FieldValue refine(Field field, FieldValue value){
		FieldValue result = refine(field.getDataType(), field.getOptype(), value);

		if(field instanceof TypeDefinitionField){
			return enhance((TypeDefinitionField)field, result);
		}

		return result;
	}

	static
	public FieldValue refine(DataType dataType, OpType opType, FieldValue value){

		if(value == null){
			return null;
		}

		DataType refinedDataType = null;
		if(dataType != null && !(dataType).equals(value.getDataType())){
			refinedDataType = dataType;
		}

		OpType refinedOpType = null;
		if(opType != null && !(opType).equals(value.getOpType())){
			refinedOpType = opType;
		}

		boolean refined = (refinedDataType != null) || (refinedOpType != null);
		if(refined){
			return create(refinedDataType, refinedOpType, value.getValue());
		}

		return value;
	}

	static
	public FieldValue enhance(TypeDefinitionField field, FieldValue value){

		if(value == null){
			return null;
		} // End if

		if(value instanceof OrdinalValue){
			OrdinalValue ordinalValue = (OrdinalValue)value;
			ordinalValue.setOrdering(getOrdering(field, ordinalValue.getDataType()));
		}

		return value;
	}

	static
	public Object getValue(FieldValue value){
		return (value != null ? value.getValue() : null);
	}

	static
	private List<?> getOrdering(TypeDefinitionField field, final DataType dataType){
		List<String> values = ArgumentUtil.getValidValues(field);
		if(values.isEmpty()){
			return null;
		}

		Function<String, Object> function = new Function<String, Object>(){

			@Override
			public Object apply(String string){
				return TypeUtil.parse(dataType, string);
			}
		};

		return Lists.newArrayList(Iterables.transform(values, function));
	}
}