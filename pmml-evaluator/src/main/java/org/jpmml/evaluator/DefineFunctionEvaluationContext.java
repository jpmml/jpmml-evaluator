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

import java.util.List;
import java.util.Objects;

import org.dmg.pmml.DataType;
import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMMLAttributes;
import org.dmg.pmml.ParameterField;

public class DefineFunctionEvaluationContext extends EvaluationContext {

	private DefineFunction defineFunction = null;

	private EvaluationContext parent = null;


	public DefineFunctionEvaluationContext(DefineFunction defineFunction, EvaluationContext parent){
		setDefineFunction(Objects.requireNonNull(defineFunction));
		setParent(Objects.requireNonNull(parent));
	}

	@Override
	public FieldValue prepare(FieldName name, Object value){
		ParameterField parameterField = findParameterField(name);
		if(parameterField == null){
			throw new MissingFieldException(name);
		}

		DataType dataType = parameterField.getDataType();
		if(dataType == null){
			throw new MissingAttributeException(parameterField, PMMLAttributes.PARAMETERFIELD_DATATYPE);
		}

		OpType opType = parameterField.getOpType();
		if(opType == null){
			throw new MissingAttributeException(parameterField, PMMLAttributes.PARAMETERFIELD_OPTYPE);
		}

		return FieldValueUtil.create(dataType, opType, value);
	}

	@Override
	protected DefineFunction getDefineFunction(String name){
		EvaluationContext parent = getParent();

		return parent.getDefineFunction(name);
	}

	private ParameterField findParameterField(FieldName name){
		DefineFunction defineFunction = getDefineFunction();

		if(defineFunction.hasParameterFields()){
			List<ParameterField> parameterFields = defineFunction.getParameterFields();

			for(ParameterField parameterField : parameterFields){

				if(Objects.equals(parameterField.getName(), name)){
					return parameterField;
				}
			}
		}

		return null;
	}

	public DefineFunction getDefineFunction(){
		return this.defineFunction;
	}

	private void setDefineFunction(DefineFunction defineFunction){
		this.defineFunction = defineFunction;
	}

	public EvaluationContext getParent(){
		return this.parent;
	}

	private void setParent(EvaluationContext parent){
		this.parent = parent;
	}
}