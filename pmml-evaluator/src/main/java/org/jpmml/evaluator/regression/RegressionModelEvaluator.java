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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.dmg.pmml.FieldRef;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.regression.CategoricalPredictor;
import org.dmg.pmml.regression.NumericPredictor;
import org.dmg.pmml.regression.PMMLAttributes;
import org.dmg.pmml.regression.PredictorTerm;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.regression.RegressionTable;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.ExpressionUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.model.InvalidAttributeException;
import org.jpmml.model.InvalidElementException;
import org.jpmml.model.InvalidElementListException;
import org.jpmml.model.UnsupportedAttributeException;

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
		if(targetFieldName != null && !Objects.equals(targetField.getFieldName(), targetFieldName)){
			throw new InvalidAttributeException(regressionModel, PMMLAttributes.REGRESSIONMODEL_TARGETFIELD, targetFieldName);
		}

		List<RegressionTable> regressionTables = regressionModel.requireRegressionTables();
		if(regressionTables.size() != 1){
			throw new InvalidElementListException(regressionTables);
		}

		RegressionTable regressionTable = regressionTables.get(0);

		Value<V> result = evaluateRegressionTable(valueFactory, regressionTable, context);
		if(result == null){
			return TargetUtil.evaluateRegressionDefault(valueFactory, targetField);
		}

		RegressionModel.NormalizationMethod normalizationMethod = regressionModel.getNormalizationMethod();
		switch(normalizationMethod){
			case NONE:
			case SOFTMAX:
			case LOGIT:
			case EXP:
			case PROBIT:
			case CLOGLOG:
			case LOGLOG:
			case CAUCHIT:
				RegressionModelUtil.normalizeRegressionResult(normalizationMethod, result);
				break;
			case SIMPLEMAX:
				throw new InvalidAttributeException(regressionModel, normalizationMethod);
			default:
				throw new UnsupportedAttributeException(regressionModel, normalizationMethod);
		}

		return TargetUtil.evaluateRegression(targetField, result);
	}

	@Override
	protected <V extends Number> Map<String, ? extends Classification<?, V>> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		RegressionModel regressionModel = getModel();

		TargetField targetField = getTargetField();

		String targetFieldName = regressionModel.getTargetField();
		if(targetFieldName != null && !Objects.equals(targetField.getFieldName(), targetFieldName)){
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

		List<RegressionTable> regressionTables = regressionModel.requireRegressionTables();
		if(regressionTables.size() < 2){
			throw new InvalidElementListException(regressionTables);
		}

		List<?> targetCategories = targetField.getCategories();
		if(targetCategories != null && targetCategories.size() != regressionTables.size()){
			throw new InvalidElementListException(regressionTables);
		}

		ValueMap<Object, V> values = new ValueMap<>(2 * regressionTables.size());

		for(int i = 0, max = regressionTables.size(); i < max; i++){
			RegressionTable regressionTable = regressionTables.get(i);

			Object targetCategory = regressionTable.requireTargetCategory();

			if(targetCategories != null && targetCategories.indexOf(targetCategory) < 0){
				throw new InvalidAttributeException(regressionTable, PMMLAttributes.REGRESSIONTABLE_TARGETCATEGORY, targetCategory);
			}

			Value<V> value = evaluateRegressionTable(valueFactory, regressionTable, context);

			// "If one or more RegressionTable elements cannot be evaluated, then the predictions are defined by the priorProbability values of the Target element"
			if(value == null){
				return TargetUtil.evaluateClassificationDefault(valueFactory, targetField);
			}

			values.put(targetCategory, value);
		}

		RegressionModel.NormalizationMethod normalizationMethod = regressionModel.getNormalizationMethod();

		switch(opType){
			case CATEGORICAL:

				if(values.size() == 2){

					switch(normalizationMethod){
						case NONE:
						case LOGIT:
						case PROBIT:
						case CLOGLOG:
						case LOGLOG:
						case CAUCHIT:
							RegressionModelUtil.computeBinomialProbabilities(normalizationMethod, values);
							break;
						case SIMPLEMAX:
						case SOFTMAX:
							// XXX: Non-standard behaviour
							if(isDefault(regressionTables.get(1)) && (normalizationMethod == RegressionModel.NormalizationMethod.SOFTMAX)){
								RegressionModelUtil.computeBinomialProbabilities(RegressionModel.NormalizationMethod.LOGIT, values);
							} else

							{
								RegressionModelUtil.computeMultinomialProbabilities(normalizationMethod, values);
							}
							break;
						case EXP:
							throw new InvalidAttributeException(regressionModel, normalizationMethod);
						default:
							throw new UnsupportedAttributeException(regressionModel, normalizationMethod);
					}
				} else

				{
					switch(normalizationMethod){
						case NONE:
						case SIMPLEMAX:
						case SOFTMAX:
							RegressionModelUtil.computeMultinomialProbabilities(normalizationMethod, values);
							break;
						case LOGIT:
						case PROBIT:
						case CLOGLOG:
						case EXP:
						case LOGLOG:
						case CAUCHIT:
							// XXX: Non-standard behaviour
							if((RegressionModel.NormalizationMethod.LOGIT).equals(normalizationMethod)){
								RegressionModelUtil.computeMultinomialProbabilities(normalizationMethod, values);

								break;
							}
							throw new InvalidAttributeException(regressionModel, normalizationMethod);
						default:
							throw new UnsupportedAttributeException(regressionModel, normalizationMethod);
					}
				}
				break;
			case ORDINAL:
				switch(normalizationMethod){
					case NONE:
					case LOGIT:
					case PROBIT:
					case CLOGLOG:
					case LOGLOG:
					case CAUCHIT:
						RegressionModelUtil.computeOrdinalProbabilities(normalizationMethod, values);
						break;
					case SIMPLEMAX:
					case SOFTMAX:
					case EXP:
						throw new InvalidAttributeException(regressionModel, normalizationMethod);
					default:
						throw new UnsupportedAttributeException(regressionModel, normalizationMethod);
				}
				break;
			default:
				throw new InvalidElementException(regressionModel);
		}

		Classification<?, V> result = createClassification(values);

		return TargetUtil.evaluateClassification(targetField, result);
	}

	private <V extends Number> Value<V> evaluateRegressionTable(ValueFactory<V> valueFactory, RegressionTable regressionTable, EvaluationContext context){
		Value<V> result = valueFactory.newValue();

		if(regressionTable.hasNumericPredictors()){
			List<NumericPredictor> numericPredictors = regressionTable.getNumericPredictors();

			for(int i = 0, max = numericPredictors.size(); i < max; i++){
				NumericPredictor numericPredictor = numericPredictors.get(i);

				FieldValue value = context.evaluate(numericPredictor.requireField());

				// "If the input value is missing, then the result evaluates to a missing value"
				if(FieldValueUtil.isMissing(value)){
					return null;
				}

				int exponent = numericPredictor.getExponent();
				if(exponent != 1){
					result.add(numericPredictor.requireCoefficient(), value.asNumber(), exponent);
				} else

				{
					result.add(numericPredictor.requireCoefficient(), value.asNumber());
				}
			}
		} // End if

		if(regressionTable.hasCategoricalPredictors()){
			List<CategoricalPredictor> categoricalPredictors = regressionTable.getCategoricalPredictors();

			// A categorical field is represented by a list of CategoricalPredictor elements.
			// The iteration over this list can be terminated right after finding the first and only match
			String matchedFieldName = null;

			for(int i = 0, max = categoricalPredictors.size(); i < max; i++){
				CategoricalPredictor categoricalPredictor = categoricalPredictors.get(i);

				String fieldName = categoricalPredictor.requireField();

				if(matchedFieldName != null){

					if((matchedFieldName).equals(fieldName)){
						continue;
					}

					matchedFieldName = null;
				}

				FieldValue value = context.evaluate(fieldName);

				// "If the input value is missing, then the categorical field is ignored"
				if(FieldValueUtil.isMissing(value)){
					matchedFieldName = fieldName;

					continue;
				}

				boolean equals = value.equals(categoricalPredictor);
				if(equals){
					matchedFieldName = fieldName;

					result.add(categoricalPredictor.requireCoefficient());
				}
			}
		} // End if

		if(regressionTable.hasPredictorTerms()){
			List<PredictorTerm> predictorTerms = regressionTable.getPredictorTerms();

			List<Number> factors = new ArrayList<>();

			for(int i = 0, max = predictorTerms.size(); i < max; i++){
				PredictorTerm predictorTerm = predictorTerms.get(i);

				factors.clear();

				Number coefficient = predictorTerm.requireCoefficient();

				List<FieldRef> fieldRefs = predictorTerm.requireFieldRefs();
				for(FieldRef fieldRef : fieldRefs){
					FieldValue value = ExpressionUtil.evaluate(fieldRef, context);

					// "If the input value is missing, then the result evaluates to a missing value"
					if(FieldValueUtil.isMissing(value)){
						return null;
					}

					factors.add(value.asNumber());
				}

				if(factors.size() == 1){
					result.add(coefficient, factors.get(0));
				} else

				if(factors.size() == 2){
					result.add(coefficient, factors.get(0), factors.get(1));
				} else

				{
					result.add(coefficient, factors.toArray(new Number[factors.size()]));
				}
			}
		}

		Number intercept = regressionTable.requireIntercept();
		if(intercept.doubleValue() != 0d){
			result.add(intercept);
		}

		return result;
	}

	static
	private boolean isDefault(RegressionTable regressionTable){

		if(regressionTable.hasNumericPredictors() || regressionTable.hasCategoricalPredictors() || regressionTable.hasPredictorTerms()){
			return false;
		}

		Number intercept = regressionTable.requireIntercept();

		return (intercept.doubleValue() == 0d);
	}
}