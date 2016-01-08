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

import java.util.Collections;
import java.util.Map;

import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;

public class ModelEvaluationContext extends EvaluationContext {

	private ModelEvaluationContext parent = null;

	private ModelEvaluator<?> modelEvaluator = null;

	private Map<FieldName, ?> arguments = Collections.emptyMap();

	/**
	 * A flag indicating if this evaluation context "sees" field values that correspond to DataField elements exactly the same as its parent evaluation context.
	 */
	private boolean compatible = false;


	public ModelEvaluationContext(ModelEvaluationContext parent, ModelEvaluator<?> modelEvaluator){
		setParent(parent);
		setModelEvaluator(modelEvaluator);
	}

	@Override
	public FieldValue createFieldValue(FieldName name, Object value){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		MiningField miningField = modelEvaluator.getMiningField(name);
		if(miningField == null){
			throw new EvaluationException();
		}

		return EvaluatorUtil.prepare(modelEvaluator, name, value);
	}

	@Override
	public DerivedField resolveDerivedField(FieldName name){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		DerivedField localDerivedField = modelEvaluator.getLocalDerivedField(name);
		if(localDerivedField != null){
			return localDerivedField;
		}

		ModelEvaluationContext parent = getParent();

		if(parent != null){
			return parent.resolveDerivedField(name);
		}

		DerivedField derivedField = modelEvaluator.getDerivedField(name);

		return derivedField;
	}

	@Override
	public DefineFunction resolveFunction(String name){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		DefineFunction defineFunction = modelEvaluator.getFunction(name);

		return defineFunction;
	}

	@Override
	public FieldValue evaluate(FieldName name){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		Map.Entry<FieldName, FieldValue> entry = getFieldEntry(name);
		if(entry != null){
			return entry.getValue();
		}

		DerivedField localDerivedField = modelEvaluator.getLocalDerivedField(name);
		if(localDerivedField != null){
			FieldValue value = ExpressionUtil.evaluate(localDerivedField, this);

			return declare(name, value);
		}

		ModelEvaluationContext parent = getParent();

		DerivedField derivedField = modelEvaluator.getDerivedField(name);
		if(derivedField != null){
			FieldValue value;

			// Perform the evaluation of a global DerivedField element at the highest compatible level
			if(parent != null && isCompatible()){
				value = parent.evaluate(name);
			} else

			{
				value = ExpressionUtil.evaluate(derivedField, this);
			}

			return declare(name, value);
		}

		MiningField miningField = modelEvaluator.getMiningField(name);
		if(miningField != null){

			if(parent != null){
				FieldValue value = parent.evaluate(name);

				// Unwrap the value so that it is subjected to model-specific field value preparation logic again
				if(!MiningFieldUtil.isDefault(miningField)){
					return declare(name, FieldValueUtil.getValue(value));
				}

				return declare(name, value);
			}

			Map<FieldName, ?> arguments = getArguments();

			Object value = arguments.get(name);

			return declare(name, value);
		}

		throw new MissingFieldException(name);
	}

	public ModelEvaluationContext getParent(){
		return this.parent;
	}

	private void setParent(ModelEvaluationContext parent){
		this.parent = parent;
	}

	public ModelEvaluator<?> getModelEvaluator(){
		return this.modelEvaluator;
	}

	private void setModelEvaluator(ModelEvaluator<?> modelEvaluator){
		this.modelEvaluator = modelEvaluator;
	}

	Map<FieldName, ?> getArguments(){
		return this.arguments;
	}

	void setArguments(Map<FieldName, ?> arguments){
		ModelEvaluationContext parent = getParent();

		if(parent != null){
			throw new IllegalStateException();
		}

		this.arguments = arguments;
	}

	boolean isCompatible(){
		return this.compatible;
	}

	void setCompatible(boolean compatible){
		ModelEvaluationContext parent = getParent();

		if(parent == null){
			throw new IllegalStateException();
		}

		this.compatible = compatible;
	}
}