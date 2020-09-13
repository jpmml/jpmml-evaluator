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

import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.regression.CategoricalPredictor;
import org.dmg.pmml.regression.NumericPredictor;
import org.dmg.pmml.regression.PMMLAttributes;
import org.dmg.pmml.regression.PMMLElements;
import org.dmg.pmml.regression.PredictorTerm;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.regression.RegressionTable;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.ExpressionUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.InvalidAttributeException;
import org.jpmml.evaluator.InvalidElementException;
import org.jpmml.evaluator.InvalidElementListException;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.UnsupportedAttributeException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;

public class RegressionModelEvaluator extends ModelEvaluator<RegressionModel> {

	private RegressionModelEvaluator(){
	}

	public RegressionModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, RegressionModel.class));
	}

	public RegressionModelEvaluator(PMML pmml, RegressionModel regressionModel){
		super(pmml, regressionModel);

		if(!regressionModel.hasRegressionTables()){
			throw new MissingElementException(regressionModel, PMMLElements.REGRESSIONMODEL_REGRESSIONTABLES);
		}
	}

	@Override
	public String getSummary(){
		return "Regression";
	}

	@Override
	protected <V extends Number> Map<FieldName, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		RegressionModel regressionModel = getModel();

		TargetField targetField = getTargetField();

		FieldName targetName = regressionModel.getTargetField();
		if(targetName != null && !Objects.equals(targetField.getFieldName(), targetName)){
			throw new InvalidAttributeException(regressionModel, PMMLAttributes.REGRESSIONMODEL_TARGETFIELD, targetName);
		}

		List<RegressionTable> regressionTables = regressionModel.getRegressionTables();
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
	protected <V extends Number> Map<FieldName, ? extends Classification<?, V>> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		RegressionModel regressionModel = getModel();

		TargetField targetField = getTargetField();

		FieldName targetName = regressionModel.getTargetField();
		if(targetName != null && !Objects.equals(targetField.getFieldName(), targetName)){
			throw new InvalidAttributeException(regressionModel, PMMLAttributes.REGRESSIONMODEL_TARGETFIELD, targetName);
		}

		OpType opType = targetField.getOpType();
		switch(opType){
			case CATEGORICAL:
			case ORDINAL:
				break;
			default:
				throw new InvalidElementException(regressionModel);
		}

		List<RegressionTable> regressionTables = regressionModel.getRegressionTables();
		if(regressionTables.size() < 2){
			throw new InvalidElementListException(regressionTables);
		}

		List<?> targetCategories = targetField.getCategories();
		if(targetCategories != null && targetCategories.size() != regressionTables.size()){
			throw new InvalidElementListException(regressionTables);
		}

		ValueMap<Object, V> values = new ValueMap<>(2 * regressionTables.size());

		for(RegressionTable regressionTable : regressionTables){
			Object targetCategory = regressionTable.getTargetCategory();
			if(targetCategory == null){
				throw new MissingAttributeException(regressionTable, PMMLAttributes.REGRESSIONTABLE_TARGETCATEGORY);
			} // End if

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
		switch(normalizationMethod){
			case NONE:
				if((OpType.CATEGORICAL).equals(opType)){

					if(values.size() == 2){
						RegressionModelUtil.computeBinomialProbabilities(normalizationMethod, values);
					} else

					{
						RegressionModelUtil.computeMultinomialProbabilities(normalizationMethod, values);
					}
				} else

				{
					RegressionModelUtil.computeOrdinalProbabilities(normalizationMethod, values);
				}
				break;
			case SIMPLEMAX:
			case SOFTMAX:
				if((OpType.CATEGORICAL).equals(opType)){

					// XXX: Non-standard behaviour
					if((values.size() == 2 && isDefault(regressionTables.get(1)) && (RegressionModel.NormalizationMethod.SOFTMAX).equals(normalizationMethod))){
						RegressionModelUtil.computeBinomialProbabilities(RegressionModel.NormalizationMethod.LOGIT, values);
					} else

					{
						RegressionModelUtil.computeMultinomialProbabilities(normalizationMethod, values);
					}
				} else

				{
					throw new InvalidElementException(regressionModel);
				}
				break;
			case LOGIT:
			case PROBIT:
			case CLOGLOG:
			case LOGLOG:
			case CAUCHIT:
				if((OpType.CATEGORICAL).equals(opType)){

					if(values.size() == 2){
						RegressionModelUtil.computeBinomialProbabilities(normalizationMethod, values);
					} else

					// XXX: Non-standard behaviour
					if(values.size() > 2 && (RegressionModel.NormalizationMethod.LOGIT).equals(normalizationMethod)){
						RegressionModelUtil.computeMultinomialProbabilities(normalizationMethod, values);
					} else

					{
						throw new InvalidElementException(regressionModel);
					}
				} else

				{
					RegressionModelUtil.computeOrdinalProbabilities(normalizationMethod, values);
				}
				break;
			case EXP:
				throw new InvalidAttributeException(regressionModel, normalizationMethod);
			default:
				throw new UnsupportedAttributeException(regressionModel, normalizationMethod);
		}

		Classification<?, V> result = createClassification(values);

		return TargetUtil.evaluateClassification(targetField, result);
	}

	private <V extends Number> Value<V> evaluateRegressionTable(ValueFactory<V> valueFactory, RegressionTable regressionTable, EvaluationContext context){
		Value<V> result = valueFactory.newValue();

		if(regressionTable.hasNumericPredictors()){
			List<NumericPredictor> numericPredictors = regressionTable.getNumericPredictors();
			for(NumericPredictor numericPredictor : numericPredictors){
				FieldName name = numericPredictor.getField();
				if(name == null){
					throw new MissingAttributeException(numericPredictor, PMMLAttributes.NUMERICPREDICTOR_FIELD);
				}

				Number coefficient = numericPredictor.getCoefficient();
				if(coefficient == null){
					throw new MissingAttributeException(numericPredictor, PMMLAttributes.NUMERICPREDICTOR_COEFFICIENT);
				}

				FieldValue value = context.evaluate(name);

				// "If the input value is missing, then the result evaluates to a missing value"
				if(FieldValueUtil.isMissing(value)){
					return null;
				}

				int exponent = numericPredictor.getExponent();
				if(exponent != 1){
					result.add(coefficient, value.asNumber(), exponent);
				} else

				{
					result.add(coefficient, value.asNumber());
				}
			}
		} // End if

		if(regressionTable.hasCategoricalPredictors()){
			// A categorical field is represented by a list of CategoricalPredictor elements.
			// The iteration over this list can be terminated right after finding the first and only match
			FieldName matchedName = null;

			List<CategoricalPredictor> categoricalPredictors = regressionTable.getCategoricalPredictors();
			for(CategoricalPredictor categoricalPredictor : categoricalPredictors){
				FieldName name = categoricalPredictor.getField();
				if(name == null){
					throw new MissingAttributeException(categoricalPredictor, PMMLAttributes.CATEGORICALPREDICTOR_FIELD);
				}

				Number coefficient = categoricalPredictor.getCoefficient();
				if(coefficient == null){
					throw new MissingAttributeException(categoricalPredictor, PMMLAttributes.CATEGORICALPREDICTOR_COEFFICIENT);
				}

				if(matchedName != null){

					if((matchedName).equals(name)){
						continue;
					}

					matchedName = null;
				}

				FieldValue value = context.evaluate(name);

				// "If the input value is missing, then the categorical field is ignored"
				if(FieldValueUtil.isMissing(value)){
					matchedName = name;

					continue;
				}

				boolean equals = value.equals(categoricalPredictor);
				if(equals){
					matchedName = name;

					result.add(coefficient);
				}
			}
		} // End if

		if(regressionTable.hasPredictorTerms()){
			List<Number> factors = new ArrayList<>();

			List<PredictorTerm> predictorTerms = regressionTable.getPredictorTerms();
			for(PredictorTerm predictorTerm : predictorTerms){
				factors.clear();

				Number coefficient = predictorTerm.getCoefficient();
				if(coefficient == null){
					throw new MissingAttributeException(predictorTerm, PMMLAttributes.PREDICTORTERM_COEFFICIENT);
				}

				List<FieldRef> fieldRefs = predictorTerm.getFieldRefs();
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

		Number intercept = regressionTable.getIntercept();
		if(intercept != null){
			result.add(intercept);
		}

		return result;
	}

	static
	private boolean isDefault(RegressionTable regressionTable){

		if(regressionTable.hasNumericPredictors() || regressionTable.hasCategoricalPredictors() || regressionTable.hasPredictorTerms()){
			return false;
		}

		Number intercept = regressionTable.getIntercept();
		if(intercept != null && intercept.doubleValue() != 0d){
			return false;
		}

		return true;
	}
}