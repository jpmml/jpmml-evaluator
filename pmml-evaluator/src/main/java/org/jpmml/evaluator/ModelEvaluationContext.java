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

import org.dmg.pmml.DataField;
import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;

public class ModelEvaluationContext extends EvaluationContext {

	private ModelEvaluationContext parent = null;

	private ModelEvaluator<?> modelEvaluator = null;

	private Map<FieldName, ?> arguments = Collections.emptyMap();

	/*
	 * A flag indicating if this evaluation context inherits {@link DataField data field} values from its parent evaluation context as they are (ie. without applying any new treatments).
	 */
	private boolean compatible = false;


	public ModelEvaluationContext(ModelEvaluationContext parent, ModelEvaluator<?> modelEvaluator){
		setParent(parent);
		setModelEvaluator(modelEvaluator);
	}

	@Override
	protected FieldValue createFieldValue(FieldName name, Object value){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		MiningField miningField = modelEvaluator.getMiningField(name);
		if(miningField == null){
			throw new EvaluationException();
		}

		return EvaluatorUtil.prepare(modelEvaluator, name, value);
	}

	@Override
	public FieldValue evaluate(FieldName name){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		Map.Entry<FieldName, FieldValue> entry = getFieldEntry(name);
		if(entry != null){
			return entry.getValue();
		}

		ModelEvaluationContext parent = getParent();

		MiningField miningField = modelEvaluator.getMiningField(name);

		// Fields that either need not or must not be referenced in the MiningSchema element
		if(miningField == null){
			DerivedField localDerivedField = modelEvaluator.getLocalDerivedField(name);
			if(localDerivedField != null){
				FieldValue value = ExpressionUtil.evaluate(localDerivedField, this);

				return declare(name, value);
			}

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
		} else

		// Fields that must be referenced in the MiningSchema element
		{
			Map<FieldName, ?> arguments = getArguments();

			DataField dataField = modelEvaluator.getDataField(name);
			if(dataField != null){

				if(parent != null){
					FieldValue value = parent.evaluate(name);

					if(MiningFieldUtil.isDefault(miningField)){
						return declare(name, value);
					}

					// Unwrap the value so that it is subjected to model-specific field value preparation logic again
					return declare(name, FieldValueUtil.getValue(value));
				}

				Object value = arguments.get(name);

				return declare(name, value);
			} else

			{
				Object value = arguments.get(name);

				if(value instanceof FieldValueReference){
					FieldValueReference fieldValueReference = (FieldValueReference)value;

					return declare(name, fieldValueReference.get());
				}
			}

			DerivedField localDerivedField = modelEvaluator.getLocalDerivedField(name);
			DerivedField derivedField = modelEvaluator.getDerivedField(name);

			if(localDerivedField != null || derivedField != null){
				throw new InvalidFeatureException(miningField);
			}
		}

		throw new MissingFieldException(name);
	}

	@Override
	protected DefineFunction resolveDefineFunction(String name){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		DefineFunction defineFunction = modelEvaluator.getFunction(name);

		return defineFunction;
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