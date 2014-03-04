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

import java.util.*;

import org.jpmml.manager.*;

import org.dmg.pmml.*;

public class RegressionModelEvaluator extends ModelEvaluator<RegressionModel> {

	public RegressionModelEvaluator(PMML pmml){
		this(pmml, find(pmml.getModels(), RegressionModel.class));
	}

	public RegressionModelEvaluator(PMML pmml, RegressionModel regressionModel){
		super(pmml, regressionModel);
	}

	@Override
	public String getSummary(){
		return "Regression";
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		RegressionModel regressionModel = getModel();
		if(!regressionModel.isScorable()){
			throw new InvalidResultException(regressionModel);
		}

		Map<FieldName, ?> predictions;

		MiningFunctionType miningFunction = regressionModel.getFunctionName();
		switch(miningFunction){
			case REGRESSION:
				predictions = evaluateRegression(context);
				break;
			case CLASSIFICATION:
				predictions = evaluateClassification(context);
				break;
			default:
				throw new UnsupportedFeatureException(regressionModel, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private Map<FieldName, ? extends Number> evaluateRegression(ModelEvaluationContext context){
		RegressionModel regressionModel = getModel();

		List<RegressionTable> regressionTables = regressionModel.getRegressionTables();
		if(regressionTables.size() != 1){
			throw new InvalidFeatureException(regressionModel);
		}

		RegressionTable regressionTable = regressionTables.get(0);

		Double value = evaluateRegressionTable(regressionTable, context);
		if(value != null){
			value = normalizeRegressionResult(regressionModel, value);
		}

		FieldName targetField = regressionModel.getTargetFieldName();
		if(targetField == null){
			targetField = getTargetField();
		}

		return TargetUtil.evaluateRegression(Collections.singletonMap(targetField, value), context);
	}

	private Map<FieldName, ? extends ClassificationMap<?>> evaluateClassification(ModelEvaluationContext context){
		RegressionModel regressionModel = getModel();

		List<RegressionTable> regressionTables = regressionModel.getRegressionTables();
		if(regressionTables.size() < 1){
			throw new InvalidFeatureException(regressionModel);
		}

		DefaultClassificationMap<String> result = new DefaultClassificationMap<String>();

		double sumExp = 0d;

		for(RegressionTable regressionTable : regressionTables){
			String category = regressionTable.getTargetCategory();
			if(category == null){
				throw new InvalidFeatureException(regressionTable);
			}

			Double value = evaluateRegressionTable(regressionTable, context);
			if(value == null){
				throw new MissingResultException(regressionTable);
			}

			sumExp += Math.exp(value.doubleValue());

			result.put(category, value);
		}

		FieldName targetField = regressionModel.getTargetFieldName();
		if(targetField == null){
			targetField = getTargetField();
		}

		DataField dataField = getDataField(targetField);

		OpType opType = dataField.getOptype();
		switch(opType){
			case CATEGORICAL:
				break;
			default:
				throw new UnsupportedFeatureException(dataField, opType);
		}

		Collection<Map.Entry<String, Double>> entries = result.entrySet();
		for(Map.Entry<String, Double> entry : entries){
			entry.setValue(normalizeClassificationResult(regressionModel, entry.getValue(), sumExp));
		}

		return TargetUtil.evaluateClassification(Collections.singletonMap(targetField, result), context);
	}

	static
	private Double evaluateRegressionTable(RegressionTable regressionTable, EvaluationContext context){
		double result = 0d;

		result += regressionTable.getIntercept();

		List<NumericPredictor> numericPredictors = regressionTable.getNumericPredictors();
		for(NumericPredictor numericPredictor : numericPredictors){
			FieldName name = numericPredictor.getName();

			FieldValue value = ExpressionUtil.evaluate(name, context);

			// "if the input value is missing, then the result evaluates to a missing value"
			if(value == null){
				context.addWarning("Missing argument \"" + name.getValue() + "\"");

				return null;
			}

			result += numericPredictor.getCoefficient() * Math.pow((value.asNumber()).doubleValue(), numericPredictor.getExponent());
		}

		List<CategoricalPredictor> categoricalPredictors = regressionTable.getCategoricalPredictors();
		for(CategoricalPredictor categoricalPredictor : categoricalPredictors){
			FieldName name = categoricalPredictor.getName();

			FieldValue value = ExpressionUtil.evaluate(name, context);

			// "if the input value is missing, then the product is ignored"
			if(value == null){
				context.addWarning("Missing argument \"" + name.getValue() + "\"");

				continue;
			}

			boolean equals = value.equalsString(categoricalPredictor.getValue());

			result += categoricalPredictor.getCoefficient() * (equals ? 1d : 0d);
		}

		List<PredictorTerm> predictorTerms = regressionTable.getPredictorTerms();
		for(PredictorTerm predictorTerm : predictorTerms){
			double product = predictorTerm.getCoefficient();

			List<FieldRef> fieldRefs = predictorTerm.getFieldRefs();
			if(fieldRefs.size() < 1){
				throw new InvalidFeatureException(predictorTerm);
			}

			for(FieldRef fieldRef : fieldRefs){
				FieldValue value = ExpressionUtil.evaluateFieldRef(fieldRef, context);

				// "if the input value is missing, then the result evaluates to a missing value"
				if(value == null){
					return null;
				}

				product *= (value.asNumber()).doubleValue();
			}

			result += product;
		}

		return Double.valueOf(result);
	}

	static
	private Double normalizeRegressionResult(RegressionModel regressionModel, Double value){
		RegressionNormalizationMethodType regressionNormalizationMethod = regressionModel.getNormalizationMethod();

		switch(regressionNormalizationMethod){
			case NONE:
				return value;
			case SOFTMAX:
			case LOGIT:
				return 1d / (1d + Math.exp(-value));
			case EXP:
				return Math.exp(value);
			default:
				throw new UnsupportedFeatureException(regressionModel, regressionNormalizationMethod);
		}
	}

	static
	private Double normalizeClassificationResult(RegressionModel regressionModel, Double value, Double sumExp){
		RegressionNormalizationMethodType regressionNormalizationMethod = regressionModel.getNormalizationMethod();

		switch(regressionNormalizationMethod){
			case NONE:
				return value;
			case SOFTMAX:
				return Math.exp(value) / sumExp;
			case LOGIT:
				return 1d / (1d + Math.exp(-value));
			case CLOGLOG:
				return 1d - Math.exp(-Math.exp(value));
			case LOGLOG:
				return Math.exp(-Math.exp(-value));
			default:
				throw new UnsupportedFeatureException(regressionModel, regressionNormalizationMethod);
		}
	}
}