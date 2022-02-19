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
package org.jpmml.evaluator.general_regression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Matrix;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.general_regression.BaseCumHazardTables;
import org.dmg.pmml.general_regression.BaselineCell;
import org.dmg.pmml.general_regression.BaselineStratum;
import org.dmg.pmml.general_regression.Categories;
import org.dmg.pmml.general_regression.Category;
import org.dmg.pmml.general_regression.GeneralRegressionModel;
import org.dmg.pmml.general_regression.PCell;
import org.dmg.pmml.general_regression.PMMLAttributes;
import org.dmg.pmml.general_regression.PPCell;
import org.dmg.pmml.general_regression.PPMatrix;
import org.dmg.pmml.general_regression.ParamMatrix;
import org.dmg.pmml.general_regression.Parameter;
import org.dmg.pmml.general_regression.ParameterCell;
import org.dmg.pmml.general_regression.ParameterList;
import org.dmg.pmml.general_regression.Predictor;
import org.dmg.pmml.general_regression.PredictorList;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.MapHolder;
import org.jpmml.evaluator.MatrixUtil;
import org.jpmml.evaluator.MissingFieldValueException;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.NumberUtil;
import org.jpmml.evaluator.Numbers;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.TypeInfo;
import org.jpmml.evaluator.TypeUtil;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.evaluator.ValueUtil;
import org.jpmml.model.InvalidAttributeException;
import org.jpmml.model.InvalidElementException;
import org.jpmml.model.MissingAttributeException;
import org.jpmml.model.UnsupportedAttributeException;
import org.jpmml.model.UnsupportedElementException;

public class GeneralRegressionModelEvaluator extends ModelEvaluator<GeneralRegressionModel> {

	private BiMap<String, Parameter> parameterRegistry = ImmutableBiMap.of();

	private Map<Object, Map<String, Row>> ppMatrixMap = Collections.emptyMap();

	private Map<Object, List<PCell>> paramMatrixMap = Collections.emptyMap();

	private List<Object> targetCategories = null;


	private GeneralRegressionModelEvaluator(){
	}

	public GeneralRegressionModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, GeneralRegressionModel.class));
	}

	public GeneralRegressionModelEvaluator(PMML pmml, GeneralRegressionModel generalRegressionModel){
		super(pmml, generalRegressionModel);

		@SuppressWarnings("unused")
		GeneralRegressionModel.ModelType modelType = generalRegressionModel.requireModelType();

		BiMap<String, Predictor> factorRegistry = ImmutableBiMap.of();
		BiMap<String, Predictor> covariateRegistry = ImmutableBiMap.of();

		ParameterList parameterList = generalRegressionModel.requireParameterList();

		this.parameterRegistry = ImmutableBiMap.copyOf(parseParameterRegistry(parameterList));

		PredictorList factorList = generalRegressionModel.getFactorList();
		if(factorList != null && factorList.hasPredictors()){
			factorRegistry = parsePredictorRegistry(factorList);
		}

		PredictorList covariateList = generalRegressionModel.getCovariateList();
		if(covariateList != null && covariateList.hasPredictors()){
			covariateRegistry = parsePredictorRegistry(covariateList);
		}

		PPMatrix ppMatrix = generalRegressionModel.requirePPMatrix();

		// Cannot use Guava's ImmutableMap, because it is null-hostile
		this.ppMatrixMap = Collections.unmodifiableMap(new LinkedHashMap<>(toImmutableMapMap(parsePPMatrix(ppMatrix, factorRegistry, covariateRegistry))));

		ParamMatrix paramMatrix = generalRegressionModel.requireParamMatrix();

		// Cannot use Guava's ImmutableMap, because it is null-hostile
		this.paramMatrixMap = Collections.unmodifiableMap(new LinkedHashMap<>(toImmutableListMap(parseParamMatrix(paramMatrix))));
	}

	@Override
	public String getSummary(){
		GeneralRegressionModel generalRegressionModel = getModel();

		GeneralRegressionModel.ModelType modelType = generalRegressionModel.requireModelType();
		switch(modelType){
			case COX_REGRESSION:
				return "Cox regression";
			default:
				return "General regression";
		}
	}

	@Override
	protected <V extends Number> Map<String, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		GeneralRegressionModel generalRegressionModel = getModel();

		GeneralRegressionModel.ModelType modelType = generalRegressionModel.requireModelType();
		switch(modelType){
			case COX_REGRESSION:
				return evaluateCoxRegression(valueFactory, context);
			default:
				return evaluateGeneralRegression(valueFactory, context);
		}
	}

	private <V extends Number> Map<String, ?> evaluateCoxRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		GeneralRegressionModel generalRegressionModel = getModel();

		TargetField targetField = getTargetField();

		String startTimeVariable = generalRegressionModel.getStartTimeVariable();
		String endTimeVariable = generalRegressionModel.getEndTimeVariable();
		if(endTimeVariable == null){
			throw new MissingAttributeException(generalRegressionModel, PMMLAttributes.GENERALREGRESSIONMODEL_ENDTIMEVARIABLE);
		}

		BaseCumHazardTables baseCumHazardTables = generalRegressionModel.requireBaseCumHazardTables();

		List<BaselineCell> baselineCells;

		Number maxTime;

		String baselineStrataVariable = generalRegressionModel.getBaselineStrataVariable();

		if(baselineStrataVariable != null){
			FieldValue value = getVariable(baselineStrataVariable, context);

			BaselineStratum baselineStratum = getBaselineStratum(baseCumHazardTables, value);

			// "If the value does not have a corresponding BaselineStratum element, then the result is a missing value"
			if(baselineStratum == null){
				return TargetUtil.evaluateRegressionDefault(valueFactory, targetField);
			}

			baselineCells = baselineStratum.requireBaselineCells();
			maxTime = baselineStratum.requireMaxTime();
		} else

		{
			baselineCells = baseCumHazardTables.requireBaselineCells();
			maxTime = baseCumHazardTables.requireMaxTime();
		}

		Comparator<BaselineCell> comparator = new Comparator<BaselineCell>(){

			@Override
			public int compare(BaselineCell left, BaselineCell right){
				Number leftTime = left.requireTime();
				Number rightTime = right.requireTime();

				return NumberUtil.compare(leftTime, rightTime);
			}
		};

		BaselineCell minBaselineCell = Collections.min(baselineCells, comparator);

		Number minTime = minBaselineCell.getTime();

		FieldValue value = getVariable(endTimeVariable, context);

		// "If the value is greater than the maximum time, then the result is a missing value"
		if(value.compareToValue(maxTime) > 0){
			return TargetUtil.evaluateRegressionDefault(valueFactory, targetField);
		} // End if

		// "If the value is less than the minimum time, then cumulative hazard is 0 and predicted survival is 1"
		if(value.compareToValue(minTime) < 0){
			Value<V> cumHazard = valueFactory.newValue(Numbers.DOUBLE_ZERO);

			return TargetUtil.evaluateRegression(targetField, cumHazard);
		}

		Number time = value.asNumber();

		// "Select the BaselineCell element that has the largest time attribute value that is not greater than the value"
		Predicate<BaselineCell> predicate = new Predicate<BaselineCell>(){

			@Override
			public boolean apply(BaselineCell baselineCell){
				return NumberUtil.compare(baselineCell.getTime(), time) <= 0;
			}
		};

		BaselineCell maxTimeBaselineCell = Collections.max(Lists.newArrayList(Iterables.filter(baselineCells, predicate)), comparator);

		Number maxTimeCumHazard = maxTimeBaselineCell.getCumHazard();
		if(maxTimeCumHazard == null){
			throw new MissingAttributeException(maxTimeBaselineCell, PMMLAttributes.BASELINECELL_CUMHAZARD);
		}

		Value<V> r = computeDotProduct(valueFactory, context);

		Value<V> s = computeReferencePoint(valueFactory);

		if(r == null || s == null){
			return TargetUtil.evaluateRegressionDefault(valueFactory, targetField);
		}

		Value<V> cumHazard = r.subtract(s)
			.exp()
			.multiply(maxTimeCumHazard);

		return TargetUtil.evaluateRegression(targetField, cumHazard);
	}

	private <V extends Number> Map<String, ?> evaluateGeneralRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		GeneralRegressionModel generalRegressionModel = getModel();

		TargetField targetField = getTargetField();

		Value<V> result = computeDotProduct(valueFactory, context);
		if(result == null){
			return TargetUtil.evaluateRegressionDefault(valueFactory, targetField);
		}

		GeneralRegressionModel.ModelType modelType = generalRegressionModel.requireModelType();
		switch(modelType){
			case REGRESSION:
			case GENERAL_LINEAR:
				break;
			case GENERALIZED_LINEAR:
				result = computeLink(result, context);
				break;
			case MULTINOMIAL_LOGISTIC:
			case ORDINAL_MULTINOMIAL:
			case COX_REGRESSION:
				throw new InvalidAttributeException(generalRegressionModel, modelType);
			default:
				throw new UnsupportedAttributeException(generalRegressionModel, modelType);
		}

		return TargetUtil.evaluateRegression(targetField, result);
	}

	@Override
	protected <V extends Number> Map<String, ? extends Classification<?, V>> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		GeneralRegressionModel generalRegressionModel = getModel();

		TargetField targetField = getTargetField();

		List<?> targetCategories = getTargetCategories();

		GeneralRegressionModel.ModelType modelType = generalRegressionModel.requireModelType();

		Map<?, Map<String, Row>> ppMatrixMap = getPPMatrixMap();

		Map<?, List<PCell>> paramMatrixMap = getParamMatrixMap();

		ValueMap<Object, V> values = new ValueMap<>(2 * targetCategories.size());

		Value<V> previousLinkValue = null;
		Value<V> previousCumulativeLinkValue = null;

		for(int i = 0, max = targetCategories.size(); i < max; i++){
			Object targetCategory = targetCategories.get(i);

			Value<V> value;

			// Categories from the first category to the second-to-last category
			if(i < (max - 1)){
				Map<String, Row> parameterPredictorRows;

				if(ppMatrixMap.isEmpty()){
					parameterPredictorRows = Collections.emptyMap();
				} else

				{
					parameterPredictorRows = ppMatrixMap.get(targetCategory);
					if(parameterPredictorRows == null){
						parameterPredictorRows = ppMatrixMap.get(null);
					} // End if

					if(parameterPredictorRows == null){
						PPMatrix ppMatrix = generalRegressionModel.getPPMatrix();

						throw new InvalidElementException(ppMatrix);
					}
				}

				List<PCell> parameterCells;

				switch(modelType){
					case GENERALIZED_LINEAR:
					case MULTINOMIAL_LOGISTIC:
						// PCell elements must have non-null targetCategory attribute in case of multinomial categories, but can do without in case of binomial categories
						parameterCells = paramMatrixMap.get(targetCategory);
						if(parameterCells == null && targetCategories.size() == 2){
							parameterCells = paramMatrixMap.get(null);
						} // End if

						if(parameterCells == null){
							ParamMatrix paramMatrix = generalRegressionModel.getParamMatrix();

							throw new InvalidElementException(paramMatrix);
						}
						break;
					case ORDINAL_MULTINOMIAL:
						// "ParamMatrix specifies different values for the intercept parameter: one for each target category except one"
						List<PCell> interceptCells = paramMatrixMap.get(targetCategory);
						if(interceptCells == null || interceptCells.size() != 1){
							ParamMatrix paramMatrix = generalRegressionModel.getParamMatrix();

							throw new InvalidElementException(paramMatrix);
						}

						// "Values for all other parameters are constant across all target variable values"
						parameterCells = paramMatrixMap.get(null);
						if(parameterCells == null){
							ParamMatrix paramMatrix = generalRegressionModel.getParamMatrix();

							throw new InvalidElementException(paramMatrix);
						}

						// XXX
						parameterCells = ImmutableList.copyOf(Iterables.concat(parameterCells, interceptCells));
						break;
					case REGRESSION:
					case GENERAL_LINEAR:
					case COX_REGRESSION:
						throw new InvalidAttributeException(generalRegressionModel, modelType);
					default:
						throw new UnsupportedAttributeException(generalRegressionModel, modelType);
				}

				value = computeDotProduct(valueFactory, parameterCells, parameterPredictorRows, context);
				if(value == null){
					return TargetUtil.evaluateClassificationDefault(valueFactory, targetField);
				}

				switch(modelType){
					case GENERALIZED_LINEAR:
						value = computeLink(value, context);
						break;
					case MULTINOMIAL_LOGISTIC:
						value.exp();
						break;
					case ORDINAL_MULTINOMIAL:
						value = computeCumulativeLink(value, context);
						break;
					case REGRESSION:
					case GENERAL_LINEAR:
					case COX_REGRESSION:
						throw new InvalidAttributeException(generalRegressionModel, modelType);
					default:
						throw new UnsupportedAttributeException(generalRegressionModel, modelType);
				}
			} else

			// The last category
			{
				switch(modelType){
					case GENERALIZED_LINEAR:
						value = valueFactory.newValue(Numbers.DOUBLE_ONE);
						if(previousLinkValue != null){
							value.subtract(previousLinkValue);
						}
						break;
					case MULTINOMIAL_LOGISTIC:
						// "By convention, the vector of Parameter estimates for the last category is 0"
						value = valueFactory.newValue(Numbers.DOUBLE_ZERO)
							.exp();
						break;
					case ORDINAL_MULTINOMIAL:
						value = valueFactory.newValue(Numbers.DOUBLE_ONE);
						break;
					case REGRESSION:
					case GENERAL_LINEAR:
					case COX_REGRESSION:
						throw new InvalidAttributeException(generalRegressionModel, modelType);
					default:
						throw new UnsupportedAttributeException(generalRegressionModel, modelType);
				}
			}

			switch(modelType){
				case GENERALIZED_LINEAR:
					previousLinkValue = value;
					break;
				case MULTINOMIAL_LOGISTIC:
					break;
				case ORDINAL_MULTINOMIAL:
					Value<V> cumulativeLinkValue = value.copy();
					if(previousCumulativeLinkValue != null){
						value.subtract(previousCumulativeLinkValue);
					}
					previousCumulativeLinkValue = cumulativeLinkValue;
					break;
				case REGRESSION:
				case GENERAL_LINEAR:
				case COX_REGRESSION:
					throw new InvalidAttributeException(generalRegressionModel, modelType);
				default:
					throw new UnsupportedAttributeException(generalRegressionModel, modelType);
			}

			values.put(targetCategory, value);
		}

		switch(modelType){
			case GENERALIZED_LINEAR:
				break;
			case MULTINOMIAL_LOGISTIC:
				ValueUtil.normalizeSimpleMax(values);
				break;
			case ORDINAL_MULTINOMIAL:
				break;
			case REGRESSION:
			case GENERAL_LINEAR:
			case COX_REGRESSION:
				throw new InvalidAttributeException(generalRegressionModel, modelType);
			default:
				throw new UnsupportedAttributeException(generalRegressionModel, modelType);
		}

		Classification<?, V> result = createClassification(values);

		return TargetUtil.evaluateClassification(targetField, result);
	}

	private <V extends Number> Value<V> computeDotProduct(ValueFactory<V> valueFactory, EvaluationContext context){
		GeneralRegressionModel generalRegressionModel = getModel();

		Map<?, Map<String, Row>> ppMatrixMap = getPPMatrixMap();

		Map<String, Row> parameterPredictorRows;

		if(ppMatrixMap.isEmpty()){
			parameterPredictorRows = Collections.emptyMap();
		} else

		{
			parameterPredictorRows = ppMatrixMap.get(null);
			if(parameterPredictorRows == null){
				PPMatrix ppMatrix = generalRegressionModel.getPPMatrix();

				throw new InvalidElementException(ppMatrix);
			}
		}

		Map<?, List<PCell>> paramMatrixMap = getParamMatrixMap();

		List<PCell> parameterCells = paramMatrixMap.get(null);

		if(paramMatrixMap.size() != 1 || parameterCells == null){
			ParamMatrix paramMatrix = generalRegressionModel.getParamMatrix();

			throw new InvalidElementException(paramMatrix);
		}

		return computeDotProduct(valueFactory, parameterCells, parameterPredictorRows, context);
	}

	private <V extends Number> Value<V> computeDotProduct(ValueFactory<V> valueFactory, List<PCell> parameterCells, Map<String, Row> parameterPredictorRows, EvaluationContext context){
		Value<V> result = null;

		for(int i = 0, max = parameterCells.size(); i < max; i++){
			PCell parameterCell = parameterCells.get(i);

			String parameterName = parameterCell.requireParameterName();
			Number beta = parameterCell.requireBeta();

			if(result == null){
				result = valueFactory.newValue();
			}

			Row parameterPredictorRow = parameterPredictorRows.get(parameterName);
			if(parameterPredictorRow != null){
				Value<V> x = parameterPredictorRow.evaluate(valueFactory, context);

				if(x == null){
					return null;
				}

				result.add(beta, x.getValue());
			} else

			{
				result.add(beta);
			}
		}

		return result;
	}

	private <V extends Number> Value<V> computeReferencePoint(ValueFactory<V> valueFactory){
		GeneralRegressionModel generalRegressionModel = getModel();

		BiMap<String, Parameter> parameters = getParameterRegistry();

		Map<?, List<PCell>> paramMatrixMap = getParamMatrixMap();

		List<PCell> parameterCells = paramMatrixMap.get(null);

		if(paramMatrixMap.size() != 1 || parameterCells == null){
			ParamMatrix paramMatrix = generalRegressionModel.getParamMatrix();

			throw new InvalidElementException(paramMatrix);
		}

		Value<V> result = null;

		for(int i = 0, max = parameterCells.size(); i < max; i++){
			PCell parameterCell = parameterCells.get(i);

			String parameterName = parameterCell.requireParameterName();
			Number beta = parameterCell.requireBeta();

			if(result == null){
				result = valueFactory.newValue();
			}

			Parameter parameter = parameters.get(parameterName);
			if(parameter != null){
				result.add(beta, parameter.getReferencePoint());
			} else

			{
				return null;
			}
		}

		return result;
	}

	private <V extends Number> Value<V> computeLink(Value<V> value, EvaluationContext context){
		GeneralRegressionModel generalRegressionModel = getModel();

		Number distParameter = null;
		Number linkParameter = null;

		GeneralRegressionModel.LinkFunction linkFunction = generalRegressionModel.requireLinkFunction();
		switch(linkFunction){
			case CLOGLOG:
			case IDENTITY:
			case LOG:
			case LOGC:
			case LOGIT:
			case LOGLOG:
				break;
			case NEGBIN:
				distParameter = generalRegressionModel.requireDistParameter();
				break;
			case ODDSPOWER:
			case POWER:
				linkParameter = generalRegressionModel.requireLinkParameter();
				break;
			case PROBIT:
				break;
			default:
				throw new UnsupportedAttributeException(generalRegressionModel, linkFunction);
		}

		Number offset = getOffset(generalRegressionModel, context);
		if(offset != null){
			value.add(offset);
		}

		GeneralRegressionModelUtil.computeLink(linkFunction, distParameter, linkParameter, value);

		Integer trials = getTrials(generalRegressionModel, context);
		if(trials != null){
			value.multiply(trials);
		}

		return value;
	}

	private <V extends Number> Value<V> computeCumulativeLink(Value<V> value, EvaluationContext context){
		GeneralRegressionModel generalRegressionModel = getModel();

		Number offset = getOffset(generalRegressionModel, context);
		if(offset != null){
			value.add(offset);
		}

		GeneralRegressionModel.CumulativeLinkFunction cumulativeLinkFunction = generalRegressionModel.requireCumulativeLinkFunction();
		switch(cumulativeLinkFunction){
			case LOGIT:
			case PROBIT:
			case CLOGLOG:
			case LOGLOG:
			case CAUCHIT:
				GeneralRegressionModelUtil.computeCumulativeLink(cumulativeLinkFunction, value);
				break;
			default:
				throw new UnsupportedAttributeException(generalRegressionModel, cumulativeLinkFunction);
		}

		return value;
	}

	public BiMap<String, Parameter> getParameterRegistry(){
		return this.parameterRegistry;
	}

	/**
	 * <p>
	 * A PPMatrix element may encode zero or more matrices.
	 * Regression models return a singleton map, whereas classification models
	 * may return a singleton map or a multi-valued map, which overrides the default
	 * matrix for one or more target categories.
	 * </p>
	 *
	 * <p>
	 * The default matrix is mapped to the <code>null</code> key.
	 * </p>
	 *
	 * @return A map of predictor-to-parameter correlation matrices.
	 */
	private Map<Object, Map<String, Row>> getPPMatrixMap(){
		return this.ppMatrixMap;
	}

	/**
	 * @return A map of parameter matrices.
	 */
	private Map<Object, List<PCell>> getParamMatrixMap(){
		return this.paramMatrixMap;
	}

	private List<Object> getTargetCategories(){

		if(this.targetCategories == null){
			this.targetCategories = ImmutableList.copyOf(parseTargetCategories());
		}

		return this.targetCategories;
	}

	private List<Object> parseTargetCategories(){
		GeneralRegressionModel generalRegressionModel = getModel();

		TargetField targetField = getTargetField();

		OpType opType = targetField.getOpType();
		switch(opType){
			case CATEGORICAL:
			case ORDINAL:
				break;
			default:
				throw new InvalidElementException(generalRegressionModel);
		}

		List<Object> targetCategories = targetField.getCategories();
		if(targetCategories == null || targetCategories.size() < 2){
			throw new InvalidElementException(generalRegressionModel);
		}

		Object targetReferenceCategory = generalRegressionModel.getTargetReferenceCategory();

		GeneralRegressionModel.ModelType modelType = generalRegressionModel.requireModelType();
		switch(modelType){
			case GENERALIZED_LINEAR:
			case MULTINOMIAL_LOGISTIC:
				if(targetReferenceCategory == null){
					Map<?, List<PCell>> paramMatrixMap = getParamMatrixMap();

					// "The reference category is the one from DataDictionary that does not appear in the ParamMatrix"
					Predicate<Object> predicate = new Predicate<Object>(){

						@Override
						public boolean apply(Object targetCategory){
							return !paramMatrixMap.containsKey(targetCategory);
						}
					};

					Set<?> targetReferenceCategories = Sets.newHashSet(Iterables.filter(targetCategories, predicate));

					if(targetReferenceCategories.size() != 1){
						ParamMatrix paramMatrix = generalRegressionModel.getParamMatrix();

						throw new InvalidElementException(paramMatrix);
					}

					targetReferenceCategory = Iterables.getOnlyElement(targetReferenceCategories);
				}
				break;
			case ORDINAL_MULTINOMIAL:
				break;
			case REGRESSION:
			case GENERAL_LINEAR:
			case COX_REGRESSION:
				throw new InvalidAttributeException(generalRegressionModel, modelType);
			default:
				throw new UnsupportedAttributeException(generalRegressionModel, modelType);
		}

		if(targetReferenceCategory != null){
			targetCategories = new ArrayList<>(targetCategories);

			// Move the element from any position to the last position
			if(targetCategories.remove(targetReferenceCategory)){
				targetCategories.add(targetReferenceCategory);
			}
		}

		return targetCategories;
	}

	static
	private Number getOffset(GeneralRegressionModel generalRegressionModel, EvaluationContext context){
		String offsetVariable = generalRegressionModel.getOffsetVariable();

		if(offsetVariable != null){
			FieldValue value = getVariable(offsetVariable, context);

			return value.asNumber();
		}

		return generalRegressionModel.getOffsetValue();
	}

	static
	private Integer getTrials(GeneralRegressionModel generalRegressionModel, EvaluationContext context){
		String trialsVariable = generalRegressionModel.getTrialsVariable();

		if(trialsVariable != null){
			FieldValue value = getVariable(trialsVariable, context);

			return value.asInteger();
		}

		return generalRegressionModel.getTrialsValue();
	}

	static
	private FieldValue getVariable(String name, EvaluationContext context){
		FieldValue value = context.evaluate(name);

		if(FieldValueUtil.isMissing(value)){
			throw new MissingFieldValueException(name);
		}

		return value;
	}

	static
	private BaselineStratum getBaselineStratum(BaseCumHazardTables baseCumHazardTables, FieldValue value){

		if(baseCumHazardTables instanceof MapHolder){
			MapHolder<?> mapHolder = (MapHolder<?>)baseCumHazardTables;

			return (BaselineStratum)mapHolder.get(value.getDataType(), value.getValue());
		}

		List<BaselineStratum> baselineStrata = baseCumHazardTables.requireBaselineStrata();
		for(int i = 0, max = baselineStrata.size(); i < max; i++){
			BaselineStratum baselineStratum = baselineStrata.get(i);

			Object category = baselineStratum.requireValue();

			if(value.equalsValue(category)){
				return baselineStratum;
			}
		}

		return null;
	}

	static
	private BiMap<String, Parameter> parseParameterRegistry(ParameterList parameterList){
		BiMap<String, Parameter> result = HashBiMap.create();

		if(!parameterList.hasParameters()){
			return result;
		}

		List<Parameter> parameters = parameterList.getParameters();
		for(Parameter parameter : parameters){
			result.put(parameter.requireName(), parameter);
		}

		return result;
	}

	static
	private BiMap<String, Predictor> parsePredictorRegistry(PredictorList predictorList){
		BiMap<String, Predictor> result = HashBiMap.create();

		List<Predictor> predictors = predictorList.getPredictors();
		for(Predictor predictor : predictors){
			result.put(predictor.requireField(), predictor);
		}

		return result;
	}

	static
	private Map<Object, Map<String, Row>> parsePPMatrix(PPMatrix ppMatrix, BiMap<String, Predictor> factors, BiMap<String, Predictor> covariates){
		Function<List<PPCell>, Row> function = new Function<List<PPCell>, Row>(){

			@Override
			public Row apply(List<PPCell> ppCells){
				Row result = new Row();

				ppCells:
				for(PPCell ppCell : ppCells){
					String fieldName = ppCell.requireField();

					Predictor factor = factors.get(fieldName);
					if(factor != null){
						result.addFactor(ppCell, factor);

						continue ppCells;
					}

					Predictor covariate = covariates.get(fieldName);
					if(covariate != null){
						result.addCovariate(ppCell);

						continue ppCells;
					}

					throw new InvalidAttributeException(ppCell, PMMLAttributes.PPCELL_FIELD, fieldName);
				}

				return result;
			}
		};

		ListMultimap<?, PPCell> targetCategoryMap = groupByTargetCategory(ppMatrix.getPPCells());

		Map<Object, Map<String, Row>> result = new LinkedHashMap<>();

		Collection<? extends Map.Entry<?, List<PPCell>>> targetCategoryEntries = (Multimaps.asMap(targetCategoryMap)).entrySet();
		for(Map.Entry<?, List<PPCell>> targetCategoryEntry : targetCategoryEntries){
			Map<String, Row> predictorMap = new LinkedHashMap<>();

			ListMultimap<String, PPCell> parameterNameMap = groupByParameterName(targetCategoryEntry.getValue());

			Collection<Map.Entry<String, List<PPCell>>> parameterNameEntries = (Multimaps.asMap(parameterNameMap)).entrySet();
			for(Map.Entry<String, List<PPCell>> parameterNameEntry : parameterNameEntries){
				predictorMap.put(parameterNameEntry.getKey(), function.apply(parameterNameEntry.getValue()));
			}

			result.put(targetCategoryEntry.getKey(), predictorMap);
		}

		return result;
	}

	static
	private Map<Object, List<PCell>> parseParamMatrix(ParamMatrix paramMatrix){
		ListMultimap<Object, PCell> targetCategoryCells = groupByTargetCategory(paramMatrix.getPCells());

		return new LinkedHashMap<>(Multimaps.asMap(targetCategoryCells));
	}

	static
	private <C extends ParameterCell> ListMultimap<String, C> groupByParameterName(List<C> cells){
		return groupCells(cells, C::getParameterName);
	}

	static
	private <C extends ParameterCell> ListMultimap<Object, C> groupByTargetCategory(List<C> cells){
		return groupCells(cells, C::getTargetCategory);
	}

	static
	private <K, C extends ParameterCell> ListMultimap<K, C> groupCells(List<C> cells, Function<C, K> function){
		ListMultimap<K, C> result = ArrayListMultimap.create();

		for(C cell : cells){
			result.put(function.apply(cell), cell);
		}

		return result;
	}

	static
	private class Row implements Serializable {

		private List<FactorHandler> factorHandlers = new ArrayList<>();

		private List<CovariateHandler> covariateHandlers = new ArrayList<>();


		public <V extends Number> Value<V> evaluate(ValueFactory<V> valueFactory, EvaluationContext context){
			Value<V> result = valueFactory.newValue(Numbers.DOUBLE_ONE);

			List<FactorHandler> factorHandlers = getFactorHandlers();
			for(int i = 0, max = factorHandlers.size(); i < max; i++){
				FactorHandler factorHandler = factorHandlers.get(i);

				FieldValue value = context.evaluate(factorHandler.getField());
				if(FieldValueUtil.isMissing(value)){
					return null;
				}

				factorHandler.updateProduct(result, value);
			}

			if(result.isZero()){
				return result;
			}

			List<CovariateHandler> covariateHandlers = getCovariateHandlers();
			for(int i = 0, max = covariateHandlers.size(); i < max; i++){
				CovariateHandler covariateHandler = covariateHandlers.get(i);

				FieldValue value = context.evaluate(covariateHandler.getField());
				if(FieldValueUtil.isMissing(value)){
					return null;
				}

				covariateHandler.updateProduct(result, value);
			}

			return result;
		}

		public void addFactor(PPCell ppCell, Predictor predictor){
			List<FactorHandler> factorHandlers = getFactorHandlers();

			Matrix matrix = predictor.getMatrix();
			if(matrix != null){
				Categories categories = predictor.getCategories();
				if(categories == null){
					throw new UnsupportedElementException(predictor);
				}

				List<Object> values = new ArrayList<>();

				for(Category category : categories){
					values.add(category.requireValue());
				}

				factorHandlers.add(new ContrastMatrixHandler(ppCell, matrix, values));
			} else

			{
				factorHandlers.add(new FactorHandler(ppCell));
			}
		}

		private void addCovariate(PPCell ppCell){
			List<CovariateHandler> covariateHandlers = getCovariateHandlers();

			covariateHandlers.add(new CovariateHandler(ppCell));
		}

		public List<FactorHandler> getFactorHandlers(){
			return this.factorHandlers;
		}

		public List<CovariateHandler> getCovariateHandlers(){
			return this.covariateHandlers;
		}

		abstract
		static
		private class PredictorHandler implements Serializable {

			private PPCell ppCell = null;


			private PredictorHandler(){
			}

			private PredictorHandler(PPCell ppCell){
				setPPCell(ppCell);

				@SuppressWarnings("unused")
				String fieldName = ppCell.requireField();
			}

			abstract
			public <V extends Number> Value<V> updateProduct(Value<V> product, FieldValue value);

			public String getField(){
				PPCell ppCell = getPPCell();

				return ppCell.requireField();
			}

			public PPCell getPPCell(){
				return this.ppCell;
			}

			private void setPPCell(PPCell ppCell){
				this.ppCell = ppCell;
			}
		}

		static
		private class FactorHandler extends PredictorHandler {

			private Object category = null;


			private FactorHandler(){
			}

			private FactorHandler(PPCell ppCell){
				super(ppCell);

				Object value = ppCell.requireValue();

				setCategory(value);
			}

			@Override
			public <V extends Number> Value<V> updateProduct(Value<V> product, FieldValue value){
				PPCell ppCell = getPPCell();

				boolean equals = value.equals(ppCell);

				return (equals ? product : product.multiply(Numbers.DOUBLE_ZERO));
			}

			public Object getCategory(){
				return this.category;
			}

			private void setCategory(Object category){
				this.category = category;
			}
		}

		static
		private class ContrastMatrixHandler extends FactorHandler {

			private Matrix matrix = null;

			private List<Object> categories = null;

			private List<FieldValue> parsedCategories = null;


			private ContrastMatrixHandler(){
			}

			private ContrastMatrixHandler(PPCell ppCell, Matrix matrix, List<Object> categories){
				super(ppCell);

				setMatrix(matrix);
				setCategories(categories);
			}

			@Override
			public <V extends Number> Value<V> updateProduct(Value<V> product, FieldValue value){
				Matrix matrix = getMatrix();

				int row = getIndex(value);
				int column = getIndex(getCategory());
				if(row < 0 || column < 0){
					PPCell ppCell = getPPCell();

					throw new InvalidElementException(ppCell);
				}

				Number result = MatrixUtil.getElementAt(matrix, row + 1, column + 1);
				if(result == null){
					throw new InvalidElementException(matrix);
				}

				return product.multiply(result);
			}

			public int getIndex(FieldValue value){

				if(this.parsedCategories == null){
					this.parsedCategories = parseCategories(value);
				}

				return this.parsedCategories.indexOf(value);
			}

			public int getIndex(Object category){
				List<Object> categories = getCategories();

				return categories.indexOf(category);
			}

			private List<FieldValue> parseCategories(TypeInfo typeInfo){
				List<Object> categories = getCategories();

				return new ArrayList<>(Lists.transform(categories, category -> FieldValueUtil.create(typeInfo, category)));
			}

			public Matrix getMatrix(){
				return this.matrix;
			}

			private void setMatrix(Matrix matrix){
				this.matrix = matrix;
			}

			public List<Object> getCategories(){
				return this.categories;
			}

			private void setCategories(List<Object> categories){
				this.categories = categories;
			}
		}

		static
		private class CovariateHandler extends PredictorHandler {

			private Number exponent = null;


			private CovariateHandler(){
			}

			private CovariateHandler(PPCell ppCell){
				super(ppCell);

				Object value = ppCell.requireValue();

				Number exponent = (Number)TypeUtil.parseOrCast(DataType.DOUBLE, value);
				if(exponent.doubleValue() == 1d){
					exponent = null;
				}

				setExponent(exponent);
			}

			@Override
			public <V extends Number> Value<V> updateProduct(Value<V> product, FieldValue value){
				Number exponent = getExponent();

				if(exponent != null){
					return product.multiply(value.asNumber(), exponent);
				} else

				{
					return product.multiply(value.asNumber());
				}
			}

			public Number getExponent(){
				return this.exponent;
			}

			private void setExponent(Number exponent){
				this.exponent = exponent;
			}
		}
	}
}