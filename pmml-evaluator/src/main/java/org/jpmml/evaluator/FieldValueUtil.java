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

public class FieldValueUtil {

	private FieldValueUtil(){
	}

	static
	public FieldValue create(Object value){

		if(value == null){
			return FieldValues.MISSING_VALUE;
		}

		DataType dataType;

		if(value instanceof Collection){
			Collection<?> values = (Collection<?>)value;

			dataType = TypeUtil.getDataType(values);
		} else

		{
			dataType = TypeUtil.getDataType(value);
		}

		OpType opType = TypeUtil.getOpType(dataType);

		return FieldValue.create(dataType, opType, value);
	}

	static
	public FieldValue create(DataType dataType, OpType opType, Object value){

		if(value == null){
			return FieldValues.MISSING_VALUE;
		}

		return FieldValue.create(dataType, opType, value);
	}

	static
	public FieldValue create(TypeInfo typeInfo, Object value){

		if(value == null){
			return FieldValues.MISSING_VALUE;
		}

		return FieldValue.create(typeInfo, value);
	}

	static
	public boolean isMissing(Object value){
		return (value == null);
	}

	static
	public boolean isMissing(FieldValue value){
		return (value == null);
	}

	static
	public Object getValue(FieldValue value){
		return (value != null ? value.getValue() : null);
	}
}