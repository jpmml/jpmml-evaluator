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
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.Target;
import org.jpmml.evaluator.mining.MiningModelEvaluationContext;

public class ModelEvaluationContext extends EvaluationContext {

	private MiningModelEvaluationContext parent = null;

	private ModelEvaluator<?> modelEvaluator = null;

	private Map<FieldName, ?> arguments = Collections.emptyMap();

	/*
	 * A flag indicating if this evaluation context inherits {@link DataField data field} values from its parent evaluation context exactly as they are.
	 */
	private boolean compatible = false;


	public ModelEvaluationContext(ModelEvaluator<?> modelEvaluator){
		this(null, modelEvaluator);
	}

	public ModelEvaluationContext(MiningModelEvaluationContext parent, ModelEvaluator<?> modelEvaluator){
		setParent(parent);
		setModelEvaluator(modelEvaluator);
	}

	@Override
	protected FieldValue createFieldValue(FieldName name, Object value){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		DataField dataField = modelEvaluator.getDataField(name);
		if(dataField == null){
			throw new MissingFieldException(name);
		}

		MiningField miningField = modelEvaluator.getMiningField(name);
		if(miningField == null){
			throw new EvaluationException();
		}

		MiningField.UsageType usageType = miningField.getUsageType();
		switch(usageType){
			case ACTIVE:
			case GROUP:
			case ORDER:
				{
					return FieldValueUtil.prepareInputValue(dataField, miningField, value);
				}
			case PREDICTED:
			case TARGET:
				{
					Target target = modelEvaluator.getTarget(name);

					return FieldValueUtil.prepareTargetValue(dataField, miningField, target, value);
				}
			default:
				throw new UnsupportedFeatureException(miningField, usageType);
		}
	}

	@Override
	public void reset(){
		super.reset();

		this.arguments = Collections.emptyMap();

		this.compatible = false;
	}

	public void reset(ModelEvaluator<?> modelEvaluator){
		reset();

		setModelEvaluator(modelEvaluator);
	}

	@Override
	protected FieldValue resolve(FieldName name){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		MiningModelEvaluationContext parent = getParent();

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
			DataField dataField = modelEvaluator.getDataField(name);
			if(dataField != null){
				Map<FieldName, ?> arguments = getArguments();

				if(parent != null){
					FieldValue value = parent.evaluate(name);

					return declare(name, performValueTreatment(dataField, miningField, value));
				}

				Object value = arguments.get(name);

				return declare(name, value);
			} // End if

			if(parent != null){
				Field field = resolveField(name, parent);
				if(field != null){
					FieldValue value = parent.evaluate(name);

					return declare(name, performValueTreatment(field, miningField, value));
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
	protected DefineFunction getDefineFunction(String name){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		DefineFunction defineFunction = modelEvaluator.getDefineFunction(name);

		return defineFunction;
	}

	public MiningModelEvaluationContext getParent(){
		return this.parent;
	}

	private void setParent(MiningModelEvaluationContext parent){
		this.parent = parent;
	}

	public ModelEvaluator<?> getModelEvaluator(){
		return this.modelEvaluator;
	}

	private void setModelEvaluator(ModelEvaluator<?> modelEvaluator){
		this.modelEvaluator = modelEvaluator;
	}

	public Map<FieldName, ?> getArguments(){
		return this.arguments;
	}

	public void setArguments(Map<FieldName, ?> arguments){
		this.arguments = arguments;
	}

	public boolean isCompatible(){
		return this.compatible;
	}

	public void setCompatible(boolean compatible){
		MiningModelEvaluationContext parent = getParent();

		if(parent == null){
			throw new IllegalStateException();
		}

		this.compatible = compatible;
	}

	static
	private Field resolveField(FieldName name, MiningModelEvaluationContext context){

		while(context != null){
			OutputField outputField = context.getOutputField(name);
			if(outputField != null){
				return outputField;
			}

			DerivedField localDerivedField = context.getLocalDerivedField(name);
			if(localDerivedField != null){
				return localDerivedField;
			}

			context = context.getParent();
		}

		return null;
	}

	static
	private FieldValue performValueTreatment(Field field, MiningField miningField, FieldValue value){

		if(MiningFieldUtil.isDefault(miningField)){
			return value;
		} // End if

		if(value == null){
			return FieldValueUtil.performMissingValueTreatment(field, miningField);
		} else

		{
			return FieldValueUtil.performValidValueTreatment(field, miningField, value);
		}
	}
}