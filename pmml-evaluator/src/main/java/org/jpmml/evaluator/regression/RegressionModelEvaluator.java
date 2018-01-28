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
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.regression.CategoricalPredictor;
import org.dmg.pmml.regression.NumericPredictor;
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
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.PMMLAttributes;
import org.jpmml.evaluator.PMMLElements;
import org.jpmml.evaluator.ProbabilityDistribution;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.UnsupportedAttributeException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;

public class RegressionModelEvaluator extends ModelEvaluator<RegressionModel> {

	public RegressionModelEvaluator(PMML pmml){
		this(pmml, selectModel(pmml, RegressionModel.class));
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
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		RegressionModel regressionModel = ensureScorableModel();

		ValueFactory<?> valueFactory;

		MathContext mathContext = regressionModel.getMathContext();
		switch(mathContext){
			case FLOAT:
			case DOUBLE:
				valueFactory = getValueFactory();
				break;
			default:
				throw new UnsupportedAttributeException(regressionModel, mathContext);
		}

		Map<FieldName, ?> predictions;

		MiningFunction miningFunction = regressionModel.getMiningFunction();
		switch(miningFunction){
			case REGRESSION:
				predictions = evaluateRegression(valueFactory, context);
				break;
			case CLASSIFICATION:
				predictions = evaluateClassification(valueFactory, context);
				break;
			case ASSOCIATION_RULES:
			case SEQUENCES:
			case CLUSTERING:
			case TIME_SERIES:
			case MIXED:
				throw new InvalidAttributeException(regressionModel, miningFunction);
			default:
				throw new UnsupportedAttributeException(regressionModel, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private <V extends Number> Map<FieldName, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		RegressionModel regressionModel = getModel();

		TargetField targetField = getTargetField();

		FieldName targetFieldName = regressionModel.getTargetFieldName();
		if(targetFieldName != null && !Objects.equals(targetField.getName(), targetFieldName)){
			throw new InvalidAttributeException(regressionModel, PMMLAttributes.REGRESSIONMODEL_TARGETFIELDNAME, targetFieldName);
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
				RegressionModelUtil.normalizeRegressionResult(result, normalizationMethod);
				break;
			case SIMPLEMAX:
				throw new InvalidAttributeException(regressionModel, normalizationMethod);
			default:
				throw new UnsupportedAttributeException(regressionModel, normalizationMethod);
		}

		return TargetUtil.evaluateRegression(targetField, result);
	}

	private <V extends Number> Map<FieldName, ? extends Classification<V>> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		RegressionModel regressionModel = getModel();

		TargetField targetField = getTargetField();

		FieldName targetFieldName = regressionModel.getTargetFieldName();
		if(targetFieldName != null && !Objects.equals(targetField.getName(), targetFieldName)){
			throw new InvalidAttributeException(regressionModel, PMMLAttributes.REGRESSIONMODEL_TARGETFIELDNAME, targetFieldName);
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

		List<String> targetCategories = FieldValueUtil.getTargetCategories(targetField);
		if(targetCategories.size() > 0 && targetCategories.size() != regressionTables.size()){
			throw new InvalidElementListException(regressionTables);
		}

		ValueMap<String, V> values = new ValueMap<>(2 * regressionTables.size());

		for(RegressionTable regressionTable : regressionTables){
			String targetCategory = regressionTable.getTargetCategory();
			if(targetCategory == null){
				throw new MissingAttributeException(regressionTable, PMMLAttributes.REGRESSIONTABLE_TARGETCATEGORY);
			} // End if

			if(targetCategories.size() > 0 && targetCategories.indexOf(targetCategory) < 0){
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
						RegressionModelUtil.computeBinomialProbabilities(values, normalizationMethod);
					} else

					{
						RegressionModelUtil.computeMultinomialProbabilities(values, normalizationMethod);
					}
				} else

				{
					RegressionModelUtil.computeOrdinalProbabilities(values, normalizationMethod);
				}
				break;
			case SIMPLEMAX:
			case SOFTMAX:
				if((OpType.CATEGORICAL).equals(opType)){

					// XXX: Non-standard behaviour
					if((values.size() == 2 && isDefault(regressionTables.get(1)) && (RegressionModel.NormalizationMethod.SOFTMAX).equals(normalizationMethod))){
						RegressionModelUtil.computeBinomialProbabilities(values, RegressionModel.NormalizationMethod.LOGIT);
					} else

					{
						RegressionModelUtil.computeMultinomialProbabilities(values, normalizationMethod);
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
						RegressionModelUtil.computeBinomialProbabilities(values, normalizationMethod);
					} else

					// XXX: Non-standard behaviour
					if(values.size() > 2 && (RegressionModel.NormalizationMethod.LOGIT).equals(normalizationMethod)){
						RegressionModelUtil.computeMultinomialProbabilities(values, normalizationMethod);
					} else

					{
						throw new InvalidElementException(regressionModel);
					}
				} else

				{
					RegressionModelUtil.computeOrdinalProbabilities(values, normalizationMethod);
				}
				break;
			case EXP:
				throw new InvalidAttributeException(regressionModel, normalizationMethod);
			default:
				throw new UnsupportedAttributeException(regressionModel, normalizationMethod);
		}

		ProbabilityDistribution<V> result = new ProbabilityDistribution<>(values);

		return TargetUtil.evaluateClassification(targetField, result);
	}

	private <V extends Number> Value<V> evaluateRegressionTable(ValueFactory<V> valueFactory, RegressionTable regressionTable, EvaluationContext context){
		Value<V> result = valueFactory.newValue();

		if(regressionTable.hasNumericPredictors()){
			List<NumericPredictor> numericPredictors = regressionTable.getNumericPredictors();
			for(NumericPredictor numericPredictor : numericPredictors){
				FieldName name = numericPredictor.getName();
				if(name == null){
					throw new MissingAttributeException(numericPredictor, PMMLAttributes.NUMERICPREDICTOR_NAME);
				}

				FieldValue value = context.evaluate(name);

				// "If the input value is missing, then the result evaluates to a missing value"
				if(value == null){
					return null;
				}

				int exponent = numericPredictor.getExponent();
				if(exponent != 1){
					result.add(numericPredictor.getCoefficient(), value.asNumber(), exponent);
				} else

				{
					result.add(numericPredictor.getCoefficient(), value.asNumber());
				}
			}
		} // End if

		if(regressionTable.hasCategoricalPredictors()){
			// A categorical field is represented by a list of CategoricalPredictor elements.
			// The iteration over this list can be terminated right after finding the first and only match
			FieldName matchedName = null;

			List<CategoricalPredictor> categoricalPredictors = regressionTable.getCategoricalPredictors();
			for(CategoricalPredictor categoricalPredictor : categoricalPredictors){
				FieldName name = categoricalPredictor.getName();
				if(name == null){
					throw new MissingAttributeException(categoricalPredictor, PMMLAttributes.CATEGORICALPREDICTOR_NAME);
				}

				if(matchedName != null){

					if((matchedName).equals(name)){
						continue;
					}

					matchedName = null;
				}

				FieldValue value = context.evaluate(name);

				// "If the input value is missing, then the categorical field is ignored"
				if(value == null){
					matchedName = name;

					continue;
				}

				boolean equals = value.equals(categoricalPredictor);
				if(equals){
					matchedName = name;

					result.add(categoricalPredictor.getCoefficient());
				}
			}
		} // End if

		if(regressionTable.hasPredictorTerms()){
			List<Number> factors = new ArrayList<>();

			List<PredictorTerm> predictorTerms = regressionTable.getPredictorTerms();
			for(PredictorTerm predictorTerm : predictorTerms){
				factors.clear();

				List<FieldRef> fieldRefs = predictorTerm.getFieldRefs();
				for(FieldRef fieldRef : fieldRefs){
					FieldValue value = ExpressionUtil.evaluate(fieldRef, context);

					// "If the input value is missing, then the result evaluates to a missing value"
					if(value == null){
						return null;
					}

					factors.add(value.asNumber());
				}

				result.add(predictorTerm.getCoefficient(), factors);
			}
		}

		result.add(regressionTable.getIntercept());

		return result;
	}

	static
	private boolean isDefault(RegressionTable regressionTable){

		if(regressionTable.hasExtensions()){
			return false;
		} // End if

		if(regressionTable.hasNumericPredictors() || regressionTable.hasCategoricalPredictors() || regressionTable.hasPredictorTerms()){
			return false;
		}

		return (regressionTable.getIntercept() == 0d);
	}
}