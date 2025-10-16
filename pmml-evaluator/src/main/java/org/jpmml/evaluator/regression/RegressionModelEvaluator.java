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
package org.jpmml.evaluator.regression;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.regression.PMMLAttributes;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.regression.RegressionTable;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.model.InvalidAttributeException;
import org.jpmml.model.InvalidElementException;

public class RegressionModelEvaluator extends ModelEvaluator<RegressionModel> {

	private RegressionModelEvaluator(){
	}

	public RegressionModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, RegressionModel.class));
	}

	public RegressionModelEvaluator(PMML pmml, RegressionModel regressionModel){
		super(pmml, regressionModel);

		@SuppressWarnings("unused")
		List<RegressionTable> regressionTables = regressionModel.requireRegressionTables();
	}

	@Override
	public String getSummary(){
		return "Regression";
	}

	@Override
	protected <V extends Number> Map<String, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		RegressionModel regressionModel = getModel();

		TargetField targetField = getTargetField();

		String targetFieldName = regressionModel.getTargetField();
		if(targetFieldName != null && !Objects.equals(targetField.getName(), targetFieldName)){
			throw new InvalidAttributeException(regressionModel, PMMLAttributes.REGRESSIONMODEL_TARGETFIELD, targetFieldName);
		}

		Value<V> value = RegressionTableUtil.evaluateRegression(valueFactory, regressionModel, context);
		if(value == null){
			return TargetUtil.evaluateRegressionDefault(valueFactory, targetField);
		}

		return TargetUtil.evaluateRegression(targetField, value);
	}

	@Override
	protected <V extends Number> Map<String, ? extends Classification<?, V>> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		RegressionModel regressionModel = getModel();

		TargetField targetField = getTargetField();

		String targetFieldName = regressionModel.getTargetField();
		if(targetFieldName != null && !Objects.equals(targetField.getName(), targetFieldName)){
			throw new InvalidAttributeException(regressionModel, PMMLAttributes.REGRESSIONMODEL_TARGETFIELD, targetFieldName);
		}

		OpType opType = targetField.getOpType();
		switch(opType){
			case CATEGORICAL:
			case ORDINAL:
				break;
			default:
				throw new InvalidElementException(regressionModel);
		}

		List<?> targetCategories = targetField.getCategories();

		ValueMap<Object, V> values = RegressionTableUtil.evaluateClassification(valueFactory, regressionModel, opType, targetCategories, context);

		// "If one or more RegressionTable elements cannot be evaluated, then the predictions are defined by the priorProbability values of the Target element"
		if(values == null){
			return TargetUtil.evaluateClassificationDefault(valueFactory, targetField);
		}

		Classification<?, V> result = createClassification(values);

		return TargetUtil.evaluateClassification(targetField, result);
	}
}