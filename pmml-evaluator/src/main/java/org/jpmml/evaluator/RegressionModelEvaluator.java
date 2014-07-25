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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.CategoricalPredictor;
import org.dmg.pmml.DataField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.NumericPredictor;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PredictorTerm;
import org.dmg.pmml.RegressionModel;
import org.dmg.pmml.RegressionNormalizationMethodType;
import org.dmg.pmml.RegressionTable;
import org.jpmml.manager.InvalidFeatureException;
import org.jpmml.manager.UnsupportedFeatureException;

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

		Double result = evaluateRegressionTable(regressionTable, context);
		if(result != null){
			result = normalizeRegressionResult(result);
		}

		FieldName targetField = regressionModel.getTargetFieldName();
		if(targetField == null){
			targetField = getTargetField();
		}

		return TargetUtil.evaluateRegression(Collections.singletonMap(targetField, result), context);
	}

	private Map<FieldName, ? extends ClassificationMap<?>> evaluateClassification(ModelEvaluationContext context){
		RegressionModel regressionModel = getModel();

		List<RegressionTable> regressionTables = regressionModel.getRegressionTables();
		if(regressionTables.size() < 1){
			throw new InvalidFeatureException(regressionModel);
		}

		ProbabilityClassificationMap<String> result = new ProbabilityClassificationMap<String>();

		for(RegressionTable regressionTable : regressionTables){
			String category = regressionTable.getTargetCategory();
			if(category == null){
				throw new InvalidFeatureException(regressionTable);
			}

			Double value = evaluateRegressionTable(regressionTable, context);
			if(value == null){
				throw new MissingResultException(regressionTable);
			}

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
				normalizeClassificationResult(result);
				break;
			default:
				throw new UnsupportedFeatureException(dataField, opType);
		}

		result.normalizeValues();

		return TargetUtil.evaluateClassification(Collections.singletonMap(targetField, result), context);
	}

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

	private Double normalizeRegressionResult(Double value){
		RegressionModel regressionModel = getModel();

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

	private void normalizeClassificationResult(Map<String, Double> values){
		RegressionModel regressionModel = getModel();

		RegressionNormalizationMethodType regressionNormalizationMethod = regressionModel.getNormalizationMethod();
		switch(regressionNormalizationMethod){
			case NONE:
				return;
			case SIMPLEMAX:
				ClassificationMap.normalize(values);
				return;
			case SOFTMAX:
				ClassificationMap.normalizeSoftMax(values);
				return;
			default:
				break;
		}

		Collection<Map.Entry<String, Double>> entries = values.entrySet();
		for(Map.Entry<String, Double> entry : entries){
			entry.setValue(normalizeClassificationResult(entry.getValue()));
		}
	}

	private Double normalizeClassificationResult(Double value){
		RegressionModel regressionModel = getModel();

		RegressionNormalizationMethodType regressionNormalizationMethod = regressionModel.getNormalizationMethod();
		switch(regressionNormalizationMethod){
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