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
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;

public class ModelEvaluationContext extends EvaluationContext {

	private ModelEvaluationContext parent = null;

	private ModelEvaluator<?> modelEvaluator = null;

	private boolean compatible = false;


	public ModelEvaluationContext(ModelEvaluationContext parent, ModelEvaluator<?> modelEvaluator){
		setParent(parent);
		setModelEvaluator(modelEvaluator);
	}

	@Override
	public FieldValue evaluate(FieldName name){
		Map.Entry<FieldName, FieldValue> entry = getFieldEntry(name);

		if(entry != null){
			return entry.getValue();
		}

		Iterator<ModelEvaluationContext> parents = getCompatibleParents();
		while(parents.hasNext()){
			ModelEvaluationContext parent = parents.next();

			entry = parent.getFieldEntry(name);
			if(entry != null){
				FieldValue value = entry.getValue();

				declare(name, value);

				return value;
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

		return null;
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

	void computeDifference(){
		ModelEvaluationContext parent = getParent();

		if(parent != null){
			setCompatible(isSubset(getFields(), parent.getFields()));
		} else

		{
			setCompatible(false);
		}
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

	boolean isCompatible(){
		return this.compatible;
	}

	private void setCompatible(boolean compatible){
		this.compatible = compatible;
	}

	static
	private <K, V> boolean isSubset(Map<K, V> left, Map<K, V> right){
		Collection<Map.Entry<K, V>> entries = left.entrySet();

		for(Map.Entry<K, V> entry : entries){
			K key = entry.getKey();
			V value = entry.getValue();

			if(!right.containsKey(key)){
				return false;
			}

			boolean equal = Objects.equals(value, right.get(key));
			if(!equal){
				return false;
			}
		}

		return true;
	}
}