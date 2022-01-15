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
import org.dmg.pmml.OpType;
import org.dmg.pmml.ParameterField;

public class DefineFunctionEvaluationContext extends EvaluationContext {

	private DefineFunction defineFunction = null;

	private EvaluationContext parent = null;


	public DefineFunctionEvaluationContext(DefineFunction defineFunction, EvaluationContext parent){
		setDefineFunction(defineFunction);
		setParent(parent);
	}

	@Override
	public FieldValue prepare(String name, Object value){
		ParameterField parameterField = findParameterField(name);
		if(parameterField == null){
			throw new MissingFieldException(name);
		}

		DataType dataType = parameterField.requireDataType();
		OpType opType = parameterField.requireOpType();

		return FieldValueUtil.create(opType, dataType, value);
	}

	@Override
	protected DefineFunction getDefineFunction(String name){
		EvaluationContext parent = getParent();

		return parent.getDefineFunction(name);
	}

	private ParameterField findParameterField(String name){
		DefineFunction defineFunction = getDefineFunction();

		if(defineFunction.hasParameterFields()){
			List<ParameterField> parameterFields = defineFunction.getParameterFields();

			for(int i = 0, max = parameterFields.size(); i < max; i++){
				ParameterField parameterField = parameterFields.get(i);

				if(Objects.equals(parameterField.requireName(), name)){
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
		this.defineFunction = Objects.requireNonNull(defineFunction);
	}

	public EvaluationContext getParent(){
		return this.parent;
	}

	private void setParent(EvaluationContext parent){
		this.parent = Objects.requireNonNull(parent);
	}
}