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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Field;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OutputField;
import org.jpmml.evaluator.mining.MiningModelEvaluationContext;
import org.jpmml.model.UnsupportedAttributeException;

public class ModelEvaluationContext extends EvaluationContext {

	private ModelEvaluator<?> modelEvaluator = null;

	private MiningModelEvaluationContext parent = null;

	private Map<String, ?> arguments = Collections.emptyMap();


	public ModelEvaluationContext(ModelEvaluator<?> modelEvaluator){
		super(modelEvaluator.getNumberOfVisibleFields());

		setModelEvaluator(modelEvaluator);
	}

	@Override
	public void reset(boolean clearValues){
		super.reset(clearValues);

		this.arguments = Collections.emptyMap();
	}

	@Override
	protected FieldValue prepare(String name, Object value){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		DataField dataField = modelEvaluator.getDataField(name);
		if(dataField == null){
			throw new MissingFieldException(name);
		}

		MiningField miningField = modelEvaluator.getMiningField(name);
		if(miningField == null){
			throw new InvisibleFieldException(name);
		}

		MiningField.UsageType usageType = miningField.getUsageType();
		switch(usageType){
			case ACTIVE:
			case SUPPLEMENTARY:
			case GROUP:
			case ORDER:
				{
					return InputFieldUtil.prepareInputValue(dataField, miningField, value);
				}
			case PREDICTED:
			case TARGET:
				{
					return InputFieldUtil.prepareResidualInputValue(dataField, miningField, value);
				}
			default:
				throw new UnsupportedAttributeException(miningField, usageType);
		}
	}

	@Override
	protected FieldValue resolve(String name){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		MiningModelEvaluationContext parent = getParent();

		MiningField miningField = modelEvaluator.getMiningField(name);

		// Fields that either need not or must not be referenced in the MiningSchema element
		if(miningField == null){
			DerivedField localDerivedField = modelEvaluator.getLocalDerivedField(name);
			if(localDerivedField != null){
				FieldValue value = ExpressionUtil.evaluate(localDerivedField, this);

				return declareInternal(name, value);
			}

			DerivedField derivedField = modelEvaluator.getDerivedField(name);
			if(derivedField != null){
				FieldValue value;

				// Perform the evaluation of a global DerivedField element at the highest compatible level
				if(parent != null && modelEvaluator.isParentCompatible()){
					value = parent.evaluate(name);
				} else

				{
					value = ExpressionUtil.evaluate(derivedField, this);
				}

				return declareInternal(name, value);
			}
		} else

		// Fields that must be referenced in the MiningSchema element
		{
			DataField dataField = modelEvaluator.getDataField(name);
			if(dataField != null){
				Map<String, ?> arguments = getArguments();

				if(parent != null){
					FieldValue value = parent.evaluate(name);

					return declareInternal(name, inheritOrPrepareInputValue(dataField, miningField, value));
				}

				Object value = arguments.get(name);

				return declare(name, value);
			} // End if

			if(parent != null){
				Field<?> field = resolveField(name, parent);
				if(field != null){
					FieldValue value = parent.evaluate(name);

					return declareInternal(name, inheritOrPrepareInputValue(field, miningField, value));
				}
			}
		}

		throw new MissingFieldException(name);
	}

	@Override
	protected FieldValue resolveLagged(LagKey lagKey){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		MiningModelEvaluationContext parent = getParent();

		String name = lagKey.getName();
		int n = lagKey.getN();
		List<String> blockIndicatorFields = lagKey.getBlockIndicatorFields();

		MiningField miningField = modelEvaluator.getMiningField(name);
		if(miningField == null){
			DerivedField localDerivedField = modelEvaluator.getLocalDerivedField(name);
			if(localDerivedField != null){
				throw new UnlaggableFieldException(localDerivedField.requireName());
			}

			DerivedField derivedField = modelEvaluator.getDerivedField(name);
			if(derivedField != null){
				throw new UnlaggableFieldException(derivedField.requireName());
			}
		} else

		{
			DataField dataField = modelEvaluator.getDataField(name);
			if(dataField != null){
				Map<String, ?> arguments = getArguments();

				if(parent != null){
					FieldValue value = parent.evaluateLagged(lagKey);

					return declareInternal(name, inheritOrPrepareInputValue(dataField, miningField, value));
				} // End if

				if(!(arguments instanceof LaggableMap)){
					throw new EvaluationException("The user-supplied arguments object class does not implement " + LaggableMap.class.getName() + " interface");
				}

				LaggableMap<String, ?> laggableArguments = (LaggableMap<String, ?>)arguments;

				Object value = laggableArguments.getLagged(name, n, blockIndicatorFields);

				return declareLagged(lagKey, value);
			} // End if

			if(parent != null){
				Field<?> field = resolveField(name, parent);
				if(field != null){
					throw new UnlaggableFieldException(field.requireName());
				}
			}
		}

		throw new MissingFieldException(name);
	}

	@Override
	protected FieldValue resolveAggregated(AggregateKey aggregateKey){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		MiningModelEvaluationContext parent = getParent();

		String name = aggregateKey.getName();
		String function = aggregateKey.getFunction();
		int n = aggregateKey.getN();
		List<String> blockIndicatorFields = aggregateKey.getBlockIndicatorFields();

		MiningField miningField = modelEvaluator.getMiningField(name);
		if(miningField == null){
			DerivedField localDerivedField = modelEvaluator.getLocalDerivedField(name);
			if(localDerivedField != null){
				throw new UnaggregableFieldException(localDerivedField.requireName());
			}

			DerivedField derivedField = modelEvaluator.getDerivedField(name);
			if(derivedField != null){
				throw new UnaggregableFieldException(derivedField.requireName());
			}
		} else

		{
			DataField dataField = modelEvaluator.getDataField(name);
			if(dataField != null){
				Map<String, ?> arguments = getArguments();

				if(parent != null){
					FieldValue value = parent.evaluateAggregated(aggregateKey);

					return declareInternal(name, inheritOrPrepareInputValue(dataField, miningField, value));
				} // End if

				if(!(arguments instanceof AggregableMap)){
					throw new EvaluationException("The user-supplied arguments object class does not implement " + AggregableMap.class.getName() + " interface");
				}

				AggregableMap<String, ?> aggregableArguments = (AggregableMap<String, ?>)arguments;

				Object value = aggregableArguments.getAggregated(name, function, n, blockIndicatorFields);

				return declareAggregated(aggregateKey, value);
			} // End if

			if(parent != null){
				Field<?> field = resolveField(name, parent);
				if(field != null){
					throw new UnaggregableFieldException(field.requireName());
				}
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

	public ModelEvaluator<?> getModelEvaluator(){
		return this.modelEvaluator;
	}

	public void setModelEvaluator(ModelEvaluator<?> modelEvaluator){
		this.modelEvaluator = Objects.requireNonNull(modelEvaluator);
	}

	public MiningModelEvaluationContext getParent(){
		return this.parent;
	}

	public void setParent(MiningModelEvaluationContext parent){
		this.parent = parent;
	}

	public Map<String, ?> getArguments(){
		return this.arguments;
	}

	public void setArguments(Map<String, ?> arguments){
		this.arguments = Objects.requireNonNull(arguments);
	}

	static
	private Field<?> resolveField(String name, MiningModelEvaluationContext context){

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
	private FieldValue inheritOrPrepareInputValue(Field<?> field, MiningField miningField, FieldValue value){

		if(InputFieldUtil.isDefault(field, miningField)){
			return value;
		}

		return InputFieldUtil.prepareInputValue(field, miningField, FieldValueUtil.getValue(value));
	}
}