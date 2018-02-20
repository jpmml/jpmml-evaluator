/*
 * Copyright (c) 2017 Villu Ruusmann
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
package org.jpmml.evaluator.java;

import java.util.Map;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.JavaModel;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.model.annotations.Property;

abstract
public class JavaClassifierModel extends JavaModel {

	public JavaClassifierModel(){
		super();
	}

	public JavaClassifierModel(@Property("miningFunction") MiningFunction miningFunction, @Property("miningSchema") MiningSchema miningSchema){
		super(miningFunction, miningSchema);
	}

	public JavaClassifierModel(Model model){
		super(model);
	}

	abstract
	public <V extends Number> Classification<V> evaluateClassification(ValueFactory<V> valueFactory, JavaModelEvaluationContext context);

	@Override
	public MiningFunction getMiningFunction(){
		MiningFunction miningFunction = super.getMiningFunction();

		if(miningFunction == null){
			return MiningFunction.CLASSIFICATION;
		}

		return miningFunction;
	}

	@Override
	public JavaModel setMiningFunction(@Property("miningFunction") MiningFunction miningFunction){

		if(miningFunction != null && !(MiningFunction.CLASSIFICATION).equals(miningFunction)){
			throw new IllegalArgumentException();
		}

		return super.setMiningFunction(miningFunction);
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		Map<FieldName, ?> predictions = evaluateClassification((JavaModelEvaluationContext)context);

		return OutputUtil.evaluate(predictions, context);
	}

	protected Map<FieldName, ? extends Classification<?>> evaluateClassification(JavaModelEvaluationContext context){
		JavaModelEvaluator modelEvaluator = context.getModelEvaluator();

		ValueFactory<?> valueFactory = modelEvaluator.ensureValueFactory();

		TargetField targetField = modelEvaluator.getTargetField();

		Classification<?> result = evaluateClassification(valueFactory, context);
		if(result == null){
			return TargetUtil.evaluateClassificationDefault(valueFactory, targetField);
		}

		return TargetUtil.evaluateClassification(targetField, result);
	}
}