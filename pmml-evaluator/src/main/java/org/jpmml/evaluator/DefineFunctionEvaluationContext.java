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
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Field;
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
	protected FieldValue resolve(String name){
		DerivedField derivedField = findDerivedField(name);
		if(derivedField == null){
			throw new MissingFieldException(name);
		}

		FieldValue value = ExpressionUtil.evaluate(derivedField, this);

		return declareInternal(name, value);
	}

	@Override
	protected DefineFunction getDefineFunction(String name){
		EvaluationContext parent = getParent();

		return parent.getDefineFunction(name);
	}

	private ParameterField findParameterField(String name){
		DefineFunction defineFunction = getDefineFunction();

		if(defineFunction.hasParameterFields()){
			return findField(name, defineFunction.getParameterFields());
		}

		return null;
	}

	private DerivedField findDerivedField(String name){
		DefineFunction defineFunction = getDefineFunction();

		if(defineFunction.hasDerivedFields()){
			return findField(name, defineFunction.getDerivedFields());
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

	static
	private <F extends Field<?>> F findField(String name, List<F> fields){

		for(int i = 0, max = fields.size(); i < max; i++){
			F field = fields.get(i);

			if(Objects.equals(field.requireName(), name)){
				return field;
			}
		}

		return null;
	}
}