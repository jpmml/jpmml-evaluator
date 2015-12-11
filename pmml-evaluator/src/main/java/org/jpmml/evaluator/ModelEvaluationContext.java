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
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;

public class ModelEvaluationContext extends EvaluationContext {

	private ModelEvaluationContext parent = null;

	private ModelEvaluator<?> modelEvaluator = null;

	private Map<FieldName, ?> arguments = Collections.emptyMap();

	private boolean compatible = false;


	public ModelEvaluationContext(ModelEvaluationContext parent, ModelEvaluator<?> modelEvaluator){
		setParent(parent);
		setModelEvaluator(modelEvaluator);
	}

	@Override
	public FieldValue evaluate(FieldName name){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		Map.Entry<FieldName, FieldValue> entry = getFieldEntry(name);
		if(entry != null){
			return entry.getValue();
		}

		MiningField miningField = modelEvaluator.getMiningField(name);
		if(miningField != null){
			ModelEvaluationContext parent = getParent();

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

		Iterator<ModelEvaluationContext> parents = getCompatibleParents();
		while(parents.hasNext()){
			ModelEvaluationContext parent = parents.next();

			entry = parent.getFieldEntry(name);
			if(entry != null){
				FieldValue value = entry.getValue();

				return declare(name, value);
			}
		}

		Result<DerivedField> result = resolveDerivedField(name);
		if(result != null){
			FieldValue value = ExpressionUtil.evaluate(result.getElement(), this);

			declare(name, value);

			parents = getCompatibleParents();
			while(parents.hasNext()){
				ModelEvaluationContext parent = parents.next();

				parent.declare(name, value);

				if((result.getContext()).equals(parent)){
					break;
				}
			}

			return value;
		}

		throw new MissingFieldException(name);
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
	public Result<DerivedField> resolveDerivedField(FieldName name){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		DerivedField derivedField = modelEvaluator.getLocalDerivedField(name);
		if(derivedField == null){
			ModelEvaluationContext parent = getParent();

			// The resolution of DerivedField elements must be handled by ModelEvaluationContext that is "closest" to them
			if(parent != null){
				return parent.resolveDerivedField(name);
			}

			derivedField = modelEvaluator.getDerivedField(name);
		}

		return createResult(derivedField);
	}

	@Override
	public Result<DefineFunction> resolveFunction(String name){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		DefineFunction defineFunction = modelEvaluator.getFunction(name);

		return createResult(defineFunction);
	}

	Iterator<ModelEvaluationContext> getCompatibleParents(){
		Iterator<ModelEvaluationContext> result = new Iterator<ModelEvaluationContext>(){

			private ModelEvaluationContext parent = getParent(ModelEvaluationContext.this);


			@Override
			public boolean hasNext(){
				return (this.parent != null);
			}

			@Override
			public ModelEvaluationContext next(){
				ModelEvaluationContext result = this.parent;

				if(result == null){
					throw new NoSuchElementException();
				}

				this.parent = getParent(result);

				return result;
			}

			@Override
			public void remove(){
				throw new UnsupportedOperationException();
			}

			private ModelEvaluationContext getParent(ModelEvaluationContext context){
				ModelEvaluationContext parent = context.getParent();

				if(parent != null){

					if(!context.isCompatible()){
						return null;
					}

					return parent;
				}

				return null;
			}
		};

		return result;
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