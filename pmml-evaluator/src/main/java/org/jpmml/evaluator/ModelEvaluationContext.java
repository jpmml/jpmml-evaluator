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

import java.util.Map;

import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;

public class ModelEvaluationContext extends EvaluationContext {

	private ModelEvaluationContext parent = null;

	private ModelEvaluator<?> modelEvaluator = null;


	public ModelEvaluationContext(ModelEvaluationContext parent, ModelEvaluator<?> modelEvaluator){
		setParent(parent);
		setModelEvaluator(modelEvaluator);
	}

	@Override
	public Map.Entry<FieldName, FieldValue> getFieldEntry(FieldName name){
		Map.Entry<FieldName, FieldValue> entry = super.getFieldEntry(name);
		if(entry == null){
			ModelEvaluationContext parent = getParent();
			if(parent != null){
				return parent.getFieldEntry(name);
			}

			return null;
		}

		return entry;
	}

	@Override
	public DerivedField resolveDerivedField(FieldName name){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		DerivedField derivedField = modelEvaluator.getLocalDerivedField(name);
		if(derivedField == null){
			ModelEvaluationContext parent = getParent();
			if(parent != null){
				return parent.resolveDerivedField(name);
			}

			return modelEvaluator.getDerivedField(name);
		}

		return derivedField;
	}

	@Override
	public DefineFunction resolveFunction(String name){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		return modelEvaluator.getFunction(name);
	}

	@Override
	public FieldValue createFieldValue(FieldName name, Object value){
		ModelEvaluator<?> modelEvaluator = getModelEvaluator();

		MiningField miningField = modelEvaluator.getMiningField(name);
		if(miningField != null){
			return EvaluatorUtil.prepare(modelEvaluator, name, value);
		}

		return super.createFieldValue(name, value);
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
}