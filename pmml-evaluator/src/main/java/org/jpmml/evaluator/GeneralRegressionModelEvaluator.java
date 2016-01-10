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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import org.dmg.pmml.BaseCumHazardTables;
import org.dmg.pmml.BaselineCell;
import org.dmg.pmml.BaselineStratum;
import org.dmg.pmml.Categories;
import org.dmg.pmml.Category;
import org.dmg.pmml.CumulativeLinkFunctionType;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.GeneralRegressionModel;
import org.dmg.pmml.LinkFunctionType;
import org.dmg.pmml.Matrix;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PCell;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PPCell;
import org.dmg.pmml.PPMatrix;
import org.dmg.pmml.ParamMatrix;
import org.dmg.pmml.Parameter;
import org.dmg.pmml.ParameterCell;
import org.dmg.pmml.ParameterList;
import org.dmg.pmml.Predictor;
import org.dmg.pmml.PredictorList;

public class GeneralRegressionModelEvaluator extends ModelEvaluator<GeneralRegressionModel> {

	public GeneralRegressionModelEvaluator(PMML pmml){
		super(pmml, GeneralRegressionModel.class);
	}

	public GeneralRegressionModelEvaluator(PMML pmml, GeneralRegressionModel generalRegressionModel){
		super(pmml, generalRegressionModel);
	}

	@Override
	public String getSummary(){
		GeneralRegressionModel generalRegressionModel = getModel();

		GeneralRegressionModel.ModelType modelType = generalRegressionModel.getModelType();
		switch(modelType){
			case COX_REGRESSION:
				return "Cox regression";
			default:
				return "General regression";
		}
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		GeneralRegressionModel generalRegressionModel = getModel();
		if(!generalRegressionModel.isScorable()){
			throw new InvalidResultException(generalRegressionModel);
		}

		Map<FieldName, ?> predictions;

		MiningFunctionType miningFunction = generalRegressionModel.getFunctionName();
		switch(miningFunction){
			case REGRESSION:
				predictions = evaluateRegression(context);
				break;
			case CLASSIFICATION:
				predictions = evaluateClassification(context);
				break;
			default:
				throw new UnsupportedFeatureException(generalRegressionModel, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private Map<FieldName, ?> evaluateRegression(ModelEvaluationContext context){
		GeneralRegressionModel generalRegressionModel = getModel();

		GeneralRegressionModel.ModelType modelType = generalRegressionModel.getModelType();
		switch(modelType){
			case COX_REGRESSION:
				return evaluateCoxRegression(context);
			default:
				return evaluateGeneralRegression(context);
		}
	}

	private Map<FieldName, ? extends Double> evaluateCoxRegression(ModelEvaluationContext context){
		GeneralRegressionModel generalRegressionModel = getModel();

		BaseCumHazardTables baseCumHazardTables = generalRegressionModel.getBaseCumHazardTables();
		if(baseCumHazardTables == null){
			throw new InvalidFeatureException(generalRegressionModel);
		}

		FieldName targetField = getTargetField();

		List<BaselineCell> baselineCells;

		Double maxTime;

		FieldName baselineStrataVariable = generalRegressionModel.getBaselineStrataVariable();

		if(baselineStrataVariable != null){
			FieldValue value = getVariable(baselineStrataVariable, context);

			BaselineStratum baselineStratum = getBaselineStratum(baseCumHazardTables, value);

			// "If the value does not have a corresponding BaselineStratum element, then the result is a missing value"
			if(baselineStratum == null){
				return null;
			}

			baselineCells = baselineStratum.getBaselineCells();

			maxTime = baselineStratum.getMaxTime();
		} else

		{
			baselineCells = baseCumHazardTables.getBaselineCells();

			maxTime = baseCumHazardTables.getMaxTime();
			if(maxTime == null){
				throw new InvalidFeatureException(baseCumHazardTables);
			}
		}

		Comparator<BaselineCell> comparator = new Comparator<BaselineCell>(){

			@Override
			public int compare(BaselineCell left, BaselineCell right){
				return Double.compare(left.getTime(), right.getTime());
			}
		};

		Ordering<BaselineCell> ordering = Ordering.from(comparator);

		double baselineCumHazard;

		FieldName startTimeVariable = generalRegressionModel.getStartTimeVariable();
		FieldName endTimeVariable = generalRegressionModel.getEndTimeVariable();

		if(endTimeVariable != null){
			BaselineCell minBaselineCell = ordering.min(baselineCells);

			Double minTime = minBaselineCell.getTime();

			final
			FieldValue value = getVariable(endTimeVariable, context);

			FieldValue minTimeValue = FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, minTime);

			// "If the value is less than the minimum time, then cumulative hazard is 0 and predicted survival is 1"
			if(value.compareToValue(minTimeValue) < 0){
				return Collections.singletonMap(targetField, 0d);
			}

			FieldValue maxTimeValue = FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, maxTime);

			// "If the value is greater than the maximum time, then the result is a missing value"
			if(value.compareToValue(maxTimeValue) > 0){
				return null;
			}

			Predicate<BaselineCell> predicate = new Predicate<BaselineCell>(){

				private double time = (value.asNumber()).doubleValue();


				@Override
				public boolean apply(BaselineCell baselineCell){
					return (baselineCell.getTime() <= this.time);
				}
			};

			// "Select the BaselineCell element that has the largest time attribute value that is not greater than the value"
			BaselineCell baselineCell = ordering.max(Iterables.filter(baselineCells, predicate));

			baselineCumHazard = baselineCell.getCumHazard();
		} else

		{
			throw new InvalidFeatureException(generalRegressionModel);
		}

		Double r = computeDotProduct(context);

		Double s = computeReferencePoint();

		if(r == null || s == null){
			return null;
		}

		Double cumHazard = baselineCumHazard * Math.exp(r - s);

		return Collections.singletonMap(targetField, cumHazard);
	}

	private Map<FieldName, ?> evaluateGeneralRegression(ModelEvaluationContext context){
		GeneralRegressionModel generalRegressionModel = getModel();

		Double result = computeDotProduct(context);
		if(result == null){
			return TargetUtil.evaluateRegressionDefault(context);
		}

		GeneralRegressionModel.ModelType modelType = generalRegressionModel.getModelType();
		switch(modelType){
			case REGRESSION:
			case GENERAL_LINEAR:
				break;
			case GENERALIZED_LINEAR:
				result = computeLink(result, context);
				break;
			default:
				throw new UnsupportedFeatureException(generalRegressionModel, modelType);
		}

		return TargetUtil.evaluateRegression(result, context);
	}

	private Map<FieldName, ? extends Classification> evaluateClassification(ModelEvaluationContext context){
		GeneralRegressionModel generalRegressionModel = getModel();

		FieldName targetField = getTargetField();

		DataField dataField = getDataField(targetField);
		if(dataField == null){
			throw new MissingFieldException(targetField, generalRegressionModel);
		}

		OpType opType = dataField.getOpType();
		switch(opType){
			case CONTINUOUS:
				throw new InvalidFeatureException(dataField);
			case CATEGORICAL:
			case ORDINAL:
				break;
			default:
				throw new UnsupportedFeatureException(dataField, opType);
		}

		List<String> targetCategories = FieldValueUtil.getTargetCategories(dataField);
		if(targetCategories.size() > 0 && targetCategories.size() < 2){
			throw new InvalidFeatureException(dataField);
		}

		Map<FieldName, FieldValue> arguments = getArguments(context);

		Map<String, Map<String, Row>> ppMatrixMap = getPPMatrixMap();

		final
		Map<String, List<PCell>> paramMatrixMap = getParamMatrixMap();

		String targetReferenceCategory = generalRegressionModel.getTargetReferenceCategory();

		GeneralRegressionModel.ModelType modelType = generalRegressionModel.getModelType();
		switch(modelType){
			case GENERALIZED_LINEAR:
			case MULTINOMIAL_LOGISTIC:
				if(targetReferenceCategory == null){
					Predicate<String> filter = new Predicate<String>(){

						@Override
						public boolean apply(String string){
							return !paramMatrixMap.containsKey(string);
						}
					};

					// "The reference category is the one from DataDictionary that does not appear in the ParamMatrix"
					Set<String> targetReferenceCategories = Sets.newLinkedHashSet(Iterables.filter(targetCategories, filter));
					if(targetReferenceCategories.size() != 1){
						throw new InvalidFeatureException(generalRegressionModel.getParamMatrix());
					}

					targetReferenceCategory = Iterables.getOnlyElement(targetReferenceCategories);
				}
				break;
			case ORDINAL_MULTINOMIAL:
				break;
			default:
				throw new UnsupportedFeatureException(generalRegressionModel, modelType);
		}

		if(targetReferenceCategory != null){
			targetCategories = new ArrayList<>(targetCategories);

			// Move the element from any position to the last position
			if(targetCategories.remove(targetReferenceCategory)){
				targetCategories.add(targetReferenceCategory);
			}
		}

		ProbabilityDistribution result = new ProbabilityDistribution();

		Double previousValue = null;

		for(int i = 0; i < targetCategories.size(); i++){
			String targetCategory = targetCategories.get(i);

			Double value;

			// Categories from the first category to the second-to-last category
			if(i < (targetCategories.size() - 1)){
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
						throw new InvalidFeatureException(generalRegressionModel.getPPMatrix());
					}
				}

				Iterable<PCell> parameterCells;

				switch(modelType){
					case GENERALIZED_LINEAR:
					case MULTINOMIAL_LOGISTIC:
						// PCell elements must have non-null targetCategory attribute in case of multinomial categories, but can do without in case of binomial categories
						parameterCells = paramMatrixMap.get(targetCategory);
						if(parameterCells == null && targetCategories.size() == 2){
							parameterCells = paramMatrixMap.get(null);
						} // End if

						if(parameterCells == null){
							throw new InvalidFeatureException(generalRegressionModel.getParamMatrix());
						}
						break;
					case ORDINAL_MULTINOMIAL:
						// "ParamMatrix specifies different values for the intercept parameter: one for each target category except one"
						List<PCell> interceptCells = paramMatrixMap.get(targetCategory);
						if(interceptCells == null || interceptCells.size() != 1){
							throw new InvalidFeatureException(generalRegressionModel.getParamMatrix());
						}

						// "Values for all other parameters are constant across all target variable values"
						parameterCells = paramMatrixMap.get(null);
						if(parameterCells == null){
							throw new InvalidFeatureException(generalRegressionModel.getParamMatrix());
						}

						parameterCells = Iterables.concat(interceptCells, parameterCells);
						break;
					default:
						throw new UnsupportedFeatureException(generalRegressionModel, modelType);
				}

				value = computeDotProduct(parameterCells, parameterPredictorRows, arguments);
				if(value == null){
					return TargetUtil.evaluateClassificationDefault(context);
				}

				switch(modelType){
					case GENERALIZED_LINEAR:
						value = computeLink(value, context);
						break;
					case MULTINOMIAL_LOGISTIC:
						value = Math.exp(value);
						break;
					case ORDINAL_MULTINOMIAL:
						value = computeCumulativeLink(value, context);
						break;
					default:
						throw new UnsupportedFeatureException(generalRegressionModel, modelType);
				}
			} else

			// The last category
			{
				switch(modelType){
					case GENERALIZED_LINEAR:
						value = (1d - previousValue);
						break;
					case MULTINOMIAL_LOGISTIC:
						// "By convention, the vector of Parameter estimates for the last category is 0"
						value = Math.exp(0d);
						break;
					case ORDINAL_MULTINOMIAL:
						value = 1d;
						break;
					default:
						throw new UnsupportedFeatureException(generalRegressionModel, modelType);
				}
			}

			switch(modelType){
				case GENERALIZED_LINEAR:
				case MULTINOMIAL_LOGISTIC:
					{
						result.put(targetCategory, value);
					}
					break;
				case ORDINAL_MULTINOMIAL:
					if(previousValue == null){
						result.put(targetCategory, value);
					} else

					{
						result.put(targetCategory, value - previousValue);
					}
					break;
				default:
					throw new UnsupportedFeatureException(generalRegressionModel, modelType);
			}

			previousValue = value;
		}

		switch(modelType){
			case GENERALIZED_LINEAR:
				break;
			case MULTINOMIAL_LOGISTIC:
				result.normalizeValues();
				break;
			case ORDINAL_MULTINOMIAL:
				break;
			default:
				throw new UnsupportedFeatureException(generalRegressionModel, modelType);
		}

		return TargetUtil.evaluateClassification(Collections.singletonMap(targetField, result), context);
	}

	private Double computeDotProduct(EvaluationContext context){
		GeneralRegressionModel generalRegressionModel = getModel();

		Map<FieldName, FieldValue> arguments = getArguments(context);

		Map<String, Map<String, Row>> ppMatrixMap = getPPMatrixMap();

		Map<String, Row> parameterPredictorRows;

		if(ppMatrixMap.isEmpty()){
			parameterPredictorRows = Collections.emptyMap();
		} else

		{
			parameterPredictorRows = ppMatrixMap.get(null);
			if(parameterPredictorRows == null){
				throw new InvalidFeatureException(generalRegressionModel.getPPMatrix());
			}
		}

		Map<String, List<PCell>> paramMatrixMap = getParamMatrixMap();
		if(paramMatrixMap.size() != 1 || !paramMatrixMap.containsKey(null)){
			throw new InvalidFeatureException(generalRegressionModel.getParamMatrix());
		}

		Iterable<PCell> parameterCells = paramMatrixMap.get(null);

		return computeDotProduct(parameterCells, parameterPredictorRows, arguments);
	}

	private Double computeDotProduct(Iterable<PCell> parameterCells, Map<String, Row> parameterPredictorRows, Map<FieldName, FieldValue> arguments){
		Double sum = null;

		for(PCell parameterCell : parameterCells){
			double value;

			Row parameterPredictorRow = parameterPredictorRows.get(parameterCell.getParameterName());
			if(parameterPredictorRow != null){
				Double x = parameterPredictorRow.evaluate(arguments);
				if(x == null){
					return null;
				}

				value = (x.doubleValue() * parameterCell.getBeta());
			} else

			// The row is empty
			{
				value = (1d * parameterCell.getBeta());
			}

			sum = (sum != null ? (sum + value) : value);
		}

		return sum;
	}

	private Double computeReferencePoint(){
		GeneralRegressionModel generalRegressionModel = getModel();

		BiMap<String, Parameter> parameters = getParameterRegistry();

		Map<String, List<PCell>> paramMatrixMap = getParamMatrixMap();
		if(paramMatrixMap.size() != 1 || !paramMatrixMap.containsKey(null)){
			throw new InvalidFeatureException(generalRegressionModel.getParamMatrix());
		}

		Iterable<PCell> parameterCells = paramMatrixMap.get(null);

		Double sum = null;

		for(PCell parameterCell : parameterCells){
			Parameter parameter = parameters.get(parameterCell.getParameterName());
			if(parameter == null){
				return null;
			}

			double value = (parameter.getReferencePoint() * parameterCell.getBeta());

			sum = (sum != null ? (sum + value) : value);
		}

		return sum;
	}

	private Double computeLink(Double value, EvaluationContext context){
		GeneralRegressionModel generalRegressionModel = getModel();

		LinkFunctionType linkFunction = generalRegressionModel.getLinkFunction();
		if(linkFunction == null){
			throw new InvalidFeatureException(generalRegressionModel);
		}

		Double a = getOffset(generalRegressionModel, context);
		Integer b = getTrials(generalRegressionModel, context);

		Double c = generalRegressionModel.getDistParameter();
		Double d = generalRegressionModel.getLinkParameter();

		switch(linkFunction){
			case CLOGLOG:
				return (1d - Math.exp(-Math.exp(value + a))) * b;
			case IDENTITY:
				return (value + a) * b;
			case LOG:
				return Math.exp(value + a) * b;
			case LOGC:
				return (1d - Math.exp(value + a)) * b;
			case LOGIT:
				return (1d / (1d + Math.exp(-(value + a)))) * b;
			case LOGLOG:
				return Math.exp(-Math.exp(-(value + a))) * b;
			case NEGBIN:
				if(c == null){
					throw new InvalidFeatureException(generalRegressionModel);
				}
				return (1d / (c * (Math.exp(-(value + a)) - 1d))) * b;
			case ODDSPOWER:
				if(d == null){
					throw new InvalidFeatureException(generalRegressionModel);
				} // End if

				if(d < 0d || d > 0d){
					return (1d / (1d + Math.pow(1d + d * (value + a), -(1d / d)))) * b;
				}
				return (1d / (1d + Math.exp(-(value + a)))) * b;
			case POWER:
				if(d == null){
					throw new InvalidFeatureException(generalRegressionModel);
				} // End if

				if(d < 0d || d > 0d){
					return Math.pow(value + a, 1d / d) * b;
				}
				return Math.exp(value + a) * b;
			case PROBIT:
				return NormalDistributionUtil.cumulativeProbability(value + a) * b;
			default:
				throw new UnsupportedFeatureException(generalRegressionModel, linkFunction);
		}
	}

	private Double computeCumulativeLink(Double value, EvaluationContext context){
		GeneralRegressionModel generalRegressionModel = getModel();

		CumulativeLinkFunctionType cumulativeLinkFunction = generalRegressionModel.getCumulativeLinkFunction();
		if(cumulativeLinkFunction == null){
			throw new InvalidFeatureException(generalRegressionModel);
		}

		Double a = getOffset(generalRegressionModel, context);

		switch(cumulativeLinkFunction){
			case LOGIT:
				return 1d / (1d + Math.exp(-(value + a)));
			case PROBIT:
				return NormalDistributionUtil.cumulativeProbability(value + a);
			case CLOGLOG:
				return 1d - Math.exp(-Math.exp(value + a));
			case LOGLOG:
				return Math.exp(-Math.exp(-(value + a)));
			case CAUCHIT:
				return 0.5d + (1d / Math.PI) * Math.atan(value + a);
			default:
				throw new UnsupportedFeatureException(generalRegressionModel, cumulativeLinkFunction);
		}
	}

	private Map<FieldName, FieldValue> getArguments(EvaluationContext context){
		BiMap<FieldName, Predictor> factors = getFactorRegistry();
		BiMap<FieldName, Predictor> covariates = getCovariateRegistry();

		Map<FieldName, FieldValue> result = new HashMap<>();

		Iterable<Predictor> predictors = Iterables.concat(factors.values(), covariates.values());
		for(Predictor predictor : predictors){
			FieldName name = predictor.getName();

			FieldValue value = context.evaluate(name);

			result.put(name, value);
		}

		return result;
	}

	public BiMap<String, Parameter> getParameterRegistry(){
		return getValue(GeneralRegressionModelEvaluator.parameterCache);
	}

	public BiMap<FieldName, Predictor> getFactorRegistry(){
		return getValue(GeneralRegressionModelEvaluator.factorCache);
	}

	public BiMap<FieldName, Predictor> getCovariateRegistry(){
		return getValue(GeneralRegressionModelEvaluator.covariateCache);
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
	private Map<String, Map<String, Row>> getPPMatrixMap(){
		return getValue(GeneralRegressionModelEvaluator.ppMatrixCache);
	}

	/**
	 * @return A map of parameter matrices.
	 */
	private Map<String, List<PCell>> getParamMatrixMap(){
		return getValue(GeneralRegressionModelEvaluator.paramMatrixCache);
	}

	static
	private Double getOffset(GeneralRegressionModel generalRegressionModel, EvaluationContext context){
		FieldName offsetVariable = generalRegressionModel.getOffsetVariable();
		if(offsetVariable != null){
			FieldValue value = getVariable(offsetVariable, context);

			return (value.asNumber()).doubleValue();
		}

		Double offsetValue = generalRegressionModel.getOffsetValue();
		if(offsetValue != null){
			return offsetValue;
		}

		return 0d;
	}

	static
	private Integer getTrials(GeneralRegressionModel generalRegressionModel, EvaluationContext context){
		FieldName trialsVariable = generalRegressionModel.getTrialsVariable();
		if(trialsVariable != null){
			FieldValue value = getVariable(trialsVariable, context);

			return value.asInteger();
		}

		Integer trialsValue = generalRegressionModel.getTrialsValue();
		if(trialsValue != null){
			return trialsValue;
		}

		return 1;
	}

	static
	private FieldValue getVariable(FieldName name, EvaluationContext context){
		FieldValue value = context.evaluate(name);
		if(value == null){
			throw new MissingValueException(name);
		}

		return value;
	}

	static
	private BaselineStratum getBaselineStratum(BaseCumHazardTables baseCumHazardTables, FieldValue value){

		if(baseCumHazardTables instanceof HasParsedValueMapping){
			HasParsedValueMapping<?> hasParsedValueMapping = (HasParsedValueMapping<?>)baseCumHazardTables;

			return (BaselineStratum)value.getMapping(hasParsedValueMapping);
		}

		List<BaselineStratum> baselineStrata = baseCumHazardTables.getBaselineStrata();
		for(BaselineStratum baselineStratum : baselineStrata){

			if(value.equalsString(baselineStratum.getValue())){
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
			result.put(parameter.getName(), parameter);
		}

		return result;
	}

	static
	private BiMap<FieldName, Predictor> parsePredictorRegistry(PredictorList predictorList){
		BiMap<FieldName, Predictor> result = HashBiMap.create();

		if(predictorList == null || !predictorList.hasPredictors()){
			return result;
		}

		List<Predictor> predictors = predictorList.getPredictors();
		for(Predictor predictor : predictors){
			result.put(predictor.getName(), predictor);
		}

		return result;
	}

	static
	private Map<String, Map<String, Row>> parsePPMatrix(final GeneralRegressionModel generalRegressionModel){
		Function<List<PPCell>, Row> function = new Function<List<PPCell>, Row>(){

			private BiMap<FieldName, Predictor> factors = CacheUtil.getValue(generalRegressionModel, GeneralRegressionModelEvaluator.factorCache);

			private BiMap<FieldName, Predictor> covariates = CacheUtil.getValue(generalRegressionModel, GeneralRegressionModelEvaluator.covariateCache);


			@Override
			public Row apply(List<PPCell> ppCells){
				Row result = new Row();

				ppCells:
				for(PPCell ppCell : ppCells){
					FieldName name = ppCell.getPredictorName();

					Predictor factor = this.factors.get(name);
					if(factor != null){
						result.addFactor(ppCell, factor);

						continue ppCells;
					}

					Predictor covariate = this.covariates.get(name);
					if(covariate != null){
						result.addCovariate(ppCell);

						continue ppCells;
					}

					throw new InvalidFeatureException(ppCell);
				}

				return result;
			}
		};

		PPMatrix ppMatrix = generalRegressionModel.getPPMatrix();

		ListMultimap<String, PPCell> targetCategoryMap = groupByTargetCategory(ppMatrix.getPPCells());

		Map<String, Map<String, Row>> result = new LinkedHashMap<>();

		Collection<Map.Entry<String, List<PPCell>>> targetCategoryEntries = (asMap(targetCategoryMap)).entrySet();
		for(Map.Entry<String, List<PPCell>> targetCategoryEntry : targetCategoryEntries){
			Map<String, Row> predictorMap = new LinkedHashMap<>();

			ListMultimap<String, PPCell> parameterNameMap = groupByParameterName(targetCategoryEntry.getValue());

			Collection<Map.Entry<String, List<PPCell>>> parameterNameEntries = (asMap(parameterNameMap)).entrySet();
			for(Map.Entry<String, List<PPCell>> parameterNameEntry : parameterNameEntries){
				predictorMap.put(parameterNameEntry.getKey(), function.apply(parameterNameEntry.getValue()));
			}

			result.put(targetCategoryEntry.getKey(), predictorMap);
		}

		return result;
	}

	static
	private Map<String, List<PCell>> parseParamMatrix(GeneralRegressionModel generalRegressionModel){
		ParamMatrix paramMatrix = generalRegressionModel.getParamMatrix();

		ListMultimap<String, PCell> targetCategoryCells = groupByTargetCategory(paramMatrix.getPCells());

		return asMap(targetCategoryCells);
	}

	@SuppressWarnings (
		value = {"rawtypes", "unchecked"}
	)
	static
	private <C extends ParameterCell> Map<String, List<C>> asMap(ListMultimap<String, C> multimap){
		return (Map)multimap.asMap();
	}

	static
	private <C extends ParameterCell> ListMultimap<String, C> groupByParameterName(List<C> cells){
		Function<C, String> function = new Function<C, String>(){

			@Override
			public String apply(C cell){
				return cell.getParameterName();
			}
		};

		return groupCells(cells, function);
	}

	static
	private <C extends ParameterCell> ListMultimap<String, C> groupByTargetCategory(List<C> cells){
		Function<C, String> function = new Function<C, String>(){

			@Override
			public String apply(C cell){
				return cell.getTargetCategory();
			}
		};

		return groupCells(cells, function);
	}

	static
	private <C extends ParameterCell> ListMultimap<String, C> groupCells(List<C> cells, Function<C, String> function){
		ListMultimap<String, C> result = ArrayListMultimap.create();

		for(C cell : cells){
			result.put(function.apply(cell), cell);
		}

		return result;
	}

	static
	private class Row {

		private List<FactorHandler> factorHandlers = new ArrayList<>();

		private List<CovariateHandler> covariateHandlers = new ArrayList<>();


		public Double evaluate(Map<FieldName, FieldValue> arguments){
			List<FactorHandler> factorHandlers = getFactorHandlers();
			List<CovariateHandler> covariateHandlers = getCovariateHandlers();

			// The row is empty
			if(factorHandlers.isEmpty() && covariateHandlers.isEmpty()){
				return 1d;
			}

			Double factorProduct = computeProduct(factorHandlers, arguments);
			Double covariateProduct = computeProduct(covariateHandlers, arguments);

			if(covariateHandlers.isEmpty()){
				return factorProduct;
			} else

			if(factorHandlers.isEmpty()){
				return covariateProduct;
			} else

			{
				if(factorProduct != null && covariateProduct != null){
					return (factorProduct * covariateProduct);
				}

				return null;
			}
		}

		public void addFactor(PPCell ppCell, Predictor predictor){
			List<FactorHandler> factorHandlers = getFactorHandlers();

			Matrix matrix = predictor.getMatrix();
			if(matrix != null){
				Categories categories = predictor.getCategories();
				if(categories == null){
					throw new UnsupportedFeatureException(predictor);
				}

				Function<Category, String> function = new Function<Category, String>(){

					@Override
					public String apply(Category category){
						return category.getValue();
					}
				};

				List<String> values = Lists.transform(categories.getCategories(), function);

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

		static
		private Double computeProduct(List<? extends PredictorHandler> predictorHandlers, Map<FieldName, FieldValue> arguments){
			Double result = null;

			for(PredictorHandler predictorHandler : predictorHandlers){
				FieldValue value = arguments.get(predictorHandler.getPredictorName());
				if(value == null){
					return null;
				} // End if

				if(result == null){
					result = predictorHandler.evaluate(value);
				} else

				{
					result = result * predictorHandler.evaluate(value);
				}
			}

			return result;
		}

		abstract
		private class PredictorHandler {

			private PPCell ppCell = null;


			private PredictorHandler(PPCell ppCell){
				setPPCell(ppCell);
			}

			abstract
			public Double evaluate(FieldValue value);

			public FieldName getPredictorName(){
				PPCell ppCell = getPPCell();

				return ppCell.getPredictorName();
			}

			public PPCell getPPCell(){
				return this.ppCell;
			}

			private void setPPCell(PPCell ppCell){
				this.ppCell = ppCell;
			}
		}

		private class FactorHandler extends PredictorHandler {

			private FactorHandler(PPCell ppCell){
				super(ppCell);
			}

			@Override
			public Double evaluate(FieldValue value){
				PPCell ppCell = getPPCell();

				boolean equals = value.equals(ppCell);

				return (equals ? 1d : 0d);
			}

			public String getCategory(){
				PPCell ppCell = getPPCell();

				return ppCell.getValue();
			}
		}

		private class ContrastMatrixHandler extends FactorHandler {

			private Matrix matrix = null;

			private List<String> categories = null;

			private List<FieldValue> parsedValueList = null;


			private ContrastMatrixHandler(PPCell ppCell, Matrix matrix, List<String> categories){
				super(ppCell);

				setMatrix(matrix);
				setCategories(categories);
			}

			@Override
			public Double evaluate(FieldValue value){
				Matrix matrix = getMatrix();

				int row = getIndex(value);
				int column = getIndex(getCategory());

				if(row < 0 || column < 0){
					throw new EvaluationException();
				}

				Number result = MatrixUtil.getElementAt(matrix, row + 1, column + 1);
				if(result == null){
					throw new EvaluationException();
				}

				return result.doubleValue();
			}

			public int getIndex(FieldValue value){

				if(this.parsedValueList == null){
					this.parsedValueList = ImmutableList.copyOf(parseCategories(value.getDataType(), value.getOpType()));
				}

				return this.parsedValueList.indexOf(value);
			}

			public int getIndex(String category){
				List<String> categories = getCategories();

				return categories.indexOf(category);
			}

			private List<FieldValue> parseCategories(final DataType dataType, final OpType opType){
				List<String> categories = getCategories();

				Function<String, FieldValue> function = new Function<String, FieldValue>(){

					@Override
					public FieldValue apply(String value){
						return FieldValueUtil.create(dataType, opType, value);
					}
				};

				return Lists.transform(categories, function);
			}

			public Matrix getMatrix(){
				return this.matrix;
			}

			private void setMatrix(Matrix matrix){
				this.matrix = matrix;
			}

			public List<String> getCategories(){
				return this.categories;
			}

			private void setCategories(List<String> categories){
				this.categories = categories;
			}
		}

		private class CovariateHandler extends PredictorHandler {

			private CovariateHandler(PPCell ppCell){
				super(ppCell);
			}

			@Override
			public Double evaluate(FieldValue value){
				return Math.pow((value.asNumber()).doubleValue(), getMultiplicity());
			}

			public Double getMultiplicity(){
				PPCell ppCell = getPPCell();

				return Double.valueOf(ppCell.getValue());
			}
		}
	}

	private static final LoadingCache<GeneralRegressionModel, BiMap<String, Parameter>> parameterCache = CacheUtil.buildLoadingCache(new CacheLoader<GeneralRegressionModel, BiMap<String, Parameter>>(){

		@Override
		public BiMap<String, Parameter> load(GeneralRegressionModel generalRegressionModel){
			return ImmutableBiMap.copyOf(parseParameterRegistry(generalRegressionModel.getParameterList()));
		}
	});

	private static final LoadingCache<GeneralRegressionModel, BiMap<FieldName, Predictor>> factorCache = CacheUtil.buildLoadingCache(new CacheLoader<GeneralRegressionModel, BiMap<FieldName, Predictor>>(){

		@Override
		public BiMap<FieldName, Predictor> load(GeneralRegressionModel generalRegressionModel){
			return ImmutableBiMap.copyOf(parsePredictorRegistry(generalRegressionModel.getFactorList()));
		}
	});

	private static final LoadingCache<GeneralRegressionModel, BiMap<FieldName, Predictor>> covariateCache = CacheUtil.buildLoadingCache(new CacheLoader<GeneralRegressionModel, BiMap<FieldName, Predictor>>(){

		@Override
		public BiMap<FieldName, Predictor> load(GeneralRegressionModel generalRegressionModel){
			return ImmutableBiMap.copyOf(parsePredictorRegistry(generalRegressionModel.getCovariateList()));
		}
	});

	private static final LoadingCache<GeneralRegressionModel, Map<String, Map<String, Row>>> ppMatrixCache = CacheUtil.buildLoadingCache(new CacheLoader<GeneralRegressionModel, Map<String, Map<String, Row>>>(){

		@Override
		public Map<String, Map<String, Row>> load(GeneralRegressionModel generalRegressionModel){
			// Cannot use Guava's ImmutableMap, because it is null-hostile
			return Collections.unmodifiableMap(parsePPMatrix(generalRegressionModel));
		}
	});

	private static final LoadingCache<GeneralRegressionModel, Map<String, List<PCell>>> paramMatrixCache = CacheUtil.buildLoadingCache(new CacheLoader<GeneralRegressionModel, Map<String, List<PCell>>>(){

		@Override
		public Map<String, List<PCell>> load(GeneralRegressionModel generalRegressionModel){
			// Cannot use Guava's ImmutableMap, because it is null-hostile
			return Collections.unmodifiableMap(parseParamMatrix(generalRegressionModel));
		}
	});
}