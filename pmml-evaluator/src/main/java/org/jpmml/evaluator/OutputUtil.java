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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.Ordering;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Model;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.Target;
import org.dmg.pmml.TargetValue;
import org.dmg.pmml.Value;
import org.dmg.pmml.association.AssociationRule;
import org.dmg.pmml.association.Item;
import org.dmg.pmml.association.ItemRef;
import org.dmg.pmml.association.Itemset;
import org.dmg.pmml.mining.MiningModel;
import org.jpmml.evaluator.mining.MiningModelEvaluationContext;
import org.jpmml.evaluator.mining.SegmentResult;

public class OutputUtil {

	private OutputUtil(){
	}

	/**
	 * <p>
	 * Evaluates the {@link Output} element.
	 * </p>
	 *
	 * @param predictions A map of {@link Evaluator#getTargetFields() target field} values.
	 *
	 * @return A map of {@link Evaluator#getTargetFields() target field} values together with {@link Evaluator#getOutputFields() output field} values.
	 */
	@SuppressWarnings (
		value = {"fallthrough"}
	)
	static
	public Map<FieldName, ?> evaluate(Map<FieldName, ?> predictions, ModelEvaluationContext context){
		ModelEvaluator<?> modelEvaluator = context.getModelEvaluator();

		Model model = modelEvaluator.getModel();

		Output output = model.getOutput();
		if(output == null || !output.hasOutputFields()){
			return predictions;
		}

		Map<FieldName, Object> result = new LinkedHashMap<>(predictions);

		List<OutputField> outputFields = output.getOutputFields();

		outputFields:
		for(OutputField outputField : outputFields){
			FieldName targetFieldName = outputField.getTargetField();

			Object targetValue = null;

			ResultFeature resultFeature = outputField.getResultFeature();

			String segmentId = outputField.getSegmentId();

			SegmentResult segmentPredictions = null;

			// Load the target value of the specified segment
			if(segmentId != null){

				if(!(model instanceof MiningModel)){
					throw new InvalidFeatureException(outputField);
				}

				MiningModelEvaluationContext miningModelContext = (MiningModelEvaluationContext)context;

				segmentPredictions = miningModelContext.getResult(segmentId);

				// "If there is no Segment matching segmentId or if the predicate of the matching Segment evaluated to false, then the result delivered by this OutputField is missing"
				if(segmentPredictions == null){
					continue outputFields;
				} // End if

				if(targetFieldName != null){

					if(!segmentPredictions.containsKey(targetFieldName)){
						throw new MissingValueException(targetFieldName, outputField);
					}

					targetValue = segmentPredictions.get(targetFieldName);
				} else

				{
					targetValue = segmentPredictions.getTargetValue();
				}
			} else

			// Load the target value
			{
				switch(resultFeature){
					case ENTITY_ID:
						{
							// "Result feature entityId returns the id of the winning segment"
							if(model instanceof MiningModel){
								targetValue = TypeUtil.cast(HasEntityId.class, predictions);

								break;
							}
						}
						// Falls through
					default:
						{
							if(targetFieldName == null){
								targetFieldName = modelEvaluator.getTargetFieldName();
							} // End if

							if(!predictions.containsKey(targetFieldName)){
								throw new MissingValueException(targetFieldName, outputField);
							}

							targetValue = predictions.get(targetFieldName);
						}
						break;
				}
			}

			// "If the target value is missing, then the result delivered by this OutputField is missing"
			if(targetValue == null){
				continue outputFields;
			}

			Object value;

			// Perform the requested computation on the target value
			switch(resultFeature){
				case PREDICTED_VALUE:
					{
						value = getPredictedValue(targetValue);
					}
					break;
				case PREDICTED_DISPLAY_VALUE:
					{
						DataField dataField = modelEvaluator.getDataField(targetFieldName);
						if(dataField == null){
							throw new MissingFieldException(targetFieldName, outputField);
						}

						Target target = modelEvaluator.getTarget(targetFieldName);

						value = getPredictedDisplayValue(targetValue, dataField, target);
					}
					break;
				case TRANSFORMED_VALUE:
				case DECISION:
					{
						if(segmentId != null){
							String name = outputField.getValue();
							if(name == null){
								throw new InvalidFeatureException(outputField);
							}

							Expression expression = outputField.getExpression();
							if(expression != null){
								throw new InvalidFeatureException(outputField);
							}

							value = segmentPredictions.get(FieldName.create(name));

							break;
						}

						Expression expression = outputField.getExpression();
						if(expression == null){
							throw new InvalidFeatureException(outputField);
						}

						value = FieldValueUtil.getValue(ExpressionUtil.evaluate(expression, context));
					}
					break;
				case PROBABILITY:
					{
						value = getProbability(targetValue, outputField);
					}
					break;
				case RESIDUAL:
					{
						FieldValue expectedTargetValue = context.evaluate(targetFieldName);
						if(expectedTargetValue == null){
							throw new MissingValueException(targetFieldName, outputField);
						}

						DataField dataField = modelEvaluator.getDataField(targetFieldName);

						OpType opType = dataField.getOpType();
						switch(opType){
							case CONTINUOUS:
								value = getContinuousResidual(targetValue, expectedTargetValue);
								break;
							case CATEGORICAL:
								value = getCategoricalResidual(targetValue, expectedTargetValue);
								break;
							default:
								throw new UnsupportedFeatureException(dataField, opType);
						}
					}
					break;
				case CLUSTER_ID:
					{
						value = getClusterId(targetValue);
					}
					break;
				case ENTITY_ID:
					{
						if(targetValue instanceof HasRuleValues){
							value = getRuleValue(targetValue, outputField, OutputField.RuleFeature.RULE_ID);

							break;
						}

						value = getEntityId(targetValue, outputField);
					}
					break;
				case AFFINITY:
					{
						value = getAffinity(targetValue, outputField);
					}
					break;
				case CLUSTER_AFFINITY:
				case ENTITY_AFFINITY:
					{
						String entityId = outputField.getValue();

						// Select the specified entity instead of the winning entity
						if(entityId != null){
							value = getAffinity(targetValue, outputField);

							break;
						}

						value = getEntityAffinity(targetValue);
					}
					break;
				case REASON_CODE:
					{
						value = getReasonCode(targetValue, outputField);
					}
					break;
				case RULE_VALUE:
					{
						value = getRuleValue(targetValue, outputField);
					}
					break;
				case ANTECEDENT:
					{
						value = getRuleValue(targetValue, outputField, OutputField.RuleFeature.ANTECEDENT);
					}
					break;
				case CONSEQUENT:
					{
						value = getRuleValue(targetValue, outputField, OutputField.RuleFeature.CONSEQUENT);
					}
					break;
				case RULE:
					{
						value = getRuleValue(targetValue, outputField, OutputField.RuleFeature.RULE);
					}
					break;
				case RULE_ID:
					{
						value = getRuleValue(targetValue, outputField, OutputField.RuleFeature.RULE_ID);
					}
					break;
				case CONFIDENCE:
					{
						value = getRuleValue(targetValue, outputField, OutputField.RuleFeature.CONFIDENCE);
					}
					break;
				case SUPPORT:
					{
						value = getRuleValue(targetValue, outputField, OutputField.RuleFeature.SUPPORT);
					}
					break;
				case LIFT:
					{
						value = getRuleValue(targetValue, outputField, OutputField.RuleFeature.LIFT);
					}
					break;
				case LEVERAGE:
					{
						value = getRuleValue(targetValue, outputField, OutputField.RuleFeature.LEVERAGE);
					}
					break;
				case WARNING:
					{
						value = context.getWarnings();
					}
					break;
				default:
					throw new UnsupportedFeatureException(outputField, resultFeature);
			}

			FieldValue outputValue = FieldValueUtil.create(outputField, value);

			// The result of one output field becomes available to other output fields
			context.declare(outputField.getName(), outputValue);

			result.put(outputField.getName(), FieldValueUtil.getValue(outputValue));
		}

		return result;
	}

	/**
	 * @throws TypeAnalysisException If the data type cannot be determined.
	 */
	static
	public DataType getDataType(OutputField outputField, ModelEvaluator<?> modelEvaluator){
		FieldName name = outputField.getName();

		DataType dataType = outputField.getDataType();
		if(dataType != null){
			return dataType;
		}

		String segmentId = outputField.getSegmentId();
		if(segmentId != null){
			throw new TypeAnalysisException(outputField);
		}

		ResultFeature resultFeature = outputField.getResultFeature();
		switch(resultFeature){
			case PREDICTED_VALUE:
			case TRANSFORMED_VALUE:
			case DECISION:
				{
					OutputField evaluatorOutputField = modelEvaluator.getOutputField(name);

					if(!(outputField).equals(evaluatorOutputField)){
						throw new TypeAnalysisException(outputField);
					}
				}
				break;
			default:
				break;
		} // End switch

		switch(resultFeature){
			case PREDICTED_VALUE:
				{
					FieldName targetFieldName = outputField.getTargetField();
					if(targetFieldName == null){
						targetFieldName = modelEvaluator.getTargetFieldName();
					}

					DataField dataField = modelEvaluator.getDataField(targetFieldName);
					if(dataField == null){
						throw new MissingFieldException(targetFieldName, outputField);
					}

					return dataField.getDataType();
				}
			case PREDICTED_DISPLAY_VALUE:
				{
					return DataType.STRING; // XXX
				}
			case TRANSFORMED_VALUE:
			case DECISION:
				{
					Expression expression = outputField.getExpression();
					if(expression == null){
						throw new InvalidFeatureException(outputField);
					}

					return ExpressionUtil.getDataType(expression, modelEvaluator);
				}
			case PROBABILITY:
			case RESIDUAL:
			case STANDARD_ERROR:
				{
					return DataType.DOUBLE;
				}
			case ENTITY_ID:
			case CLUSTER_ID:
				{
					return DataType.STRING;
				}
			case AFFINITY:
			case ENTITY_AFFINITY:
			case CLUSTER_AFFINITY:
				{
					return DataType.DOUBLE;
				}
			case REASON_CODE:
				{
					return DataType.STRING;
				}
			case RULE_VALUE:
				{
					return getRuleDataType(outputField);
				}
			case ANTECEDENT:
				{
					return getRuleDataType(outputField, OutputField.RuleFeature.ANTECEDENT);
				}
			case CONSEQUENT:
				{
					return getRuleDataType(outputField, OutputField.RuleFeature.CONSEQUENT);
				}
			case RULE:
				{
					return getRuleDataType(outputField, OutputField.RuleFeature.RULE);
				}
			case RULE_ID:
				{
					return getRuleDataType(outputField, OutputField.RuleFeature.RULE_ID);
				}
			case SUPPORT:
				{
					return getRuleDataType(outputField, OutputField.RuleFeature.SUPPORT);
				}
			case CONFIDENCE:
				{
					return getRuleDataType(outputField, OutputField.RuleFeature.CONFIDENCE);
				}
			case LIFT:
				{
					return getRuleDataType(outputField, OutputField.RuleFeature.LIFT);
				}
			case LEVERAGE:
				{
					return getRuleDataType(outputField, OutputField.RuleFeature.LEVERAGE);
				}
			case WARNING:
				{
					throw new TypeAnalysisException(outputField);
				}
			default:
				throw new UnsupportedFeatureException(outputField, resultFeature);
		}
	}

	static
	private Object getPredictedValue(Object object){
		return EvaluatorUtil.decode(object);
	}

	static
	private Object getPredictedDisplayValue(Object object, DataField dataField, Target target){

		if(object instanceof HasDisplayValue){
			HasDisplayValue hasDisplayValue = TypeUtil.cast(HasDisplayValue.class, object);

			return hasDisplayValue.getDisplayValue();
		}

		object = getPredictedValue(object);

		if(target != null){
			TargetValue targetValue = TargetUtil.getTargetValue(target, object);

			if(targetValue != null){
				String displayValue = targetValue.getDisplayValue();

				if(displayValue != null){
					return displayValue;
				}
			}
		}

		OpType opType = dataField.getOpType();
		switch(opType){
			case CONTINUOUS:
				break;
			case CATEGORICAL:
			case ORDINAL:
				{
					Value value = FieldValueUtil.getValidValue(dataField, object);

					if(value != null){
						String displayValue = value.getDisplayValue();

						if(displayValue != null){
							return displayValue;
						}
					}
				}
				break;
			default:
				throw new UnsupportedFeatureException(dataField, opType);
		}

		// "If the display value is not specified explicitly, then the raw predicted value is used by default"
		return object;
	}

	static
	private Double getProbability(Object object, OutputField outputField){
		HasProbability hasProbability = TypeUtil.cast(HasProbability.class, object);

		String value = getCategoryValue(object, outputField);

		return hasProbability.getProbability(value);
	}

	static
	private String getCategoryValue(Object object, OutputField outputField){
		String value = outputField.getValue();

		// "If the value attribute is not specified, then the predicted categorical value should be returned as a result"
		if(value == null){
			return TypeUtil.format(getPredictedValue(object));
		}

		return value;
	}

	static
	private Double getContinuousResidual(Object object, FieldValue expectedObject){
		Number value = (Number)getPredictedValue(object);
		Number expectedValue = (Number)FieldValueUtil.getValue(expectedObject);

		return Double.valueOf(expectedValue.doubleValue() - value.doubleValue());
	}

	static
	public Double getCategoricalResidual(Object object, FieldValue expectedObject){
		HasProbability hasProbability = TypeUtil.cast(HasProbability.class, object);

		String value = TypeUtil.format(getPredictedValue(object));
		String expectedValue = TypeUtil.format(FieldValueUtil.getValue(expectedObject));

		boolean equals = TypeUtil.equals(DataType.STRING, value, expectedValue);

		return Double.valueOf((equals ? 1d : 0d) - hasProbability.getProbability(value));
	}

	static
	private String getClusterId(Object object){
		HasEntityId hasEntityId = TypeUtil.cast(HasEntityId.class, object);

		return hasEntityId.getEntityId();
	}

	static
	private String getEntityId(Object object, OutputField outputField){
		HasEntityId hasEntityId = TypeUtil.cast(HasEntityId.class, object);

		int rank = outputField.getRank();
		if(rank <= 0){
			throw new InvalidFeatureException(outputField);
		}

		if(rank > 1){
			HasEntityIdRanking hasEntityIdRanking = TypeUtil.cast(HasEntityIdRanking.class, object);

			OutputField.RankOrder rankOrder = outputField.getRankOrder();
			switch(rankOrder){
				case DESCENDING:
					break;
				default:
					throw new UnsupportedFeatureException(outputField, rankOrder);
			}

			return getElement(hasEntityIdRanking.getEntityIdRanking(), rank);
		}

		return hasEntityId.getEntityId();
	}

	static
	public Double getAffinity(Object object, OutputField outputField){
		HasAffinity hasAffinity = TypeUtil.cast(HasAffinity.class, object);

		int rank = outputField.getRank();
		if(rank <= 0){
			throw new InvalidFeatureException(outputField);
		}

		if(rank > 1){
			HasAffinityRanking hasAffinityRanking = TypeUtil.cast(HasAffinityRanking.class, object);

			OutputField.RankOrder rankOrder = outputField.getRankOrder();
			switch(rankOrder){
				case DESCENDING:
					break;
				default:
					throw new UnsupportedFeatureException(outputField, rankOrder);
			}

			return getElement(hasAffinityRanking.getAffinityRanking(), rank);
		}

		String value = getCategoryValue(object, outputField);

		return hasAffinity.getAffinity(value);
	}

	static
	public Double getEntityAffinity(Object object){
		HasEntityAffinity hasEntityAffinity = TypeUtil.cast(HasEntityAffinity.class, object);

		return hasEntityAffinity.getEntityAffinity();
	}

	static
	public String getReasonCode(Object object, OutputField outputField){
		HasReasonCodeRanking hasReasonCodeRanking = TypeUtil.cast(HasReasonCodeRanking.class, object);

		int rank = outputField.getRank();
		if(rank <= 0){
			throw new InvalidFeatureException(outputField);
		}

		return getElement(hasReasonCodeRanking.getReasonCodeRanking(), rank);
	}

	static
	public Object getRuleValue(Object object, OutputField outputField, OutputField.RuleFeature ruleFeature){
		HasRuleValues hasRuleValues = TypeUtil.cast(HasRuleValues.class, object);

		List<AssociationRule> associationRules = getRuleValues(hasRuleValues, outputField);

		String isMultiValued = outputField.getIsMultiValued();
		if(!("0").equals(isMultiValued)){
			throw new UnsupportedFeatureException(outputField);
		}

		int rank = outputField.getRank();
		if(rank <= 0){
			throw new InvalidFeatureException(outputField);
		}

		AssociationRule associationRule = getElement(associationRules, rank);
		if(associationRule != null){
			return getRuleFeature(hasRuleValues, associationRule, outputField, ruleFeature);
		}

		return null;
	}

	static
	public Object getRuleValue(Object object, OutputField outputField){
		HasRuleValues hasRuleValues = TypeUtil.cast(HasRuleValues.class, object);

		List<AssociationRule> associationRules = getRuleValues(hasRuleValues, outputField);

		String isMultiValued = outputField.getIsMultiValued();

		// Return a single result
		if(("0").equals(isMultiValued)){
			int rank = outputField.getRank();
			if(rank <= 0){
				throw new InvalidFeatureException(outputField);
			}

			AssociationRule associationRule = getElement(associationRules, rank);
			if(associationRule != null){
				return getRuleFeature(hasRuleValues, associationRule, outputField);
			}

			return null;
		} else

		// Return multiple results
		if(("1").equals(isMultiValued)){
			int size;

			int rank = outputField.getRank();
			if(rank < 0){
				throw new InvalidFeatureException(outputField);
			} else

			// "A zero value indicates that all output values are to be returned"
			if(rank == 0){
				size = associationRules.size();
			} else

			// "A positive value indicates the number of output values to be returned"
			{
				size = Math.min(rank, associationRules.size());
			}

			associationRules = associationRules.subList(0, size);

			List<Object> result = new ArrayList<>(associationRules.size());

			for(AssociationRule associationRule : associationRules){
				result.add(getRuleFeature(hasRuleValues, associationRule, outputField));
			}

			return result;
		} else

		{
			throw new InvalidFeatureException(outputField);
		}
	}

	static
	private List<AssociationRule> getRuleValues(HasRuleValues hasRuleValues, final OutputField outputField){
		List<AssociationRule> associationRules = hasRuleValues.getRuleValues(outputField.getAlgorithm());

		Comparator<AssociationRule> comparator = new Comparator<AssociationRule>(){

			private OutputField.RankBasis rankBasis = outputField.getRankBasis();

			private OutputField.RankOrder rankOrder = outputField.getRankOrder();


			@Override
			public int compare(AssociationRule left, AssociationRule right){
				int order;

				switch(this.rankBasis){
					case CONFIDENCE:
						order = (getConfidence(left)).compareTo(getConfidence(right));
						break;
					case SUPPORT:
						order = (getSupport(left)).compareTo(getSupport(right));
						break;
					case LIFT:
						order = (getLift(left)).compareTo(getLift(right));
						break;
					case LEVERAGE:
						order = (getLeverage(left)).compareTo(getLeverage(right));
						break;
					case AFFINITY:
						order = (getAffinity(left)).compareTo(getAffinity(right));
						break;
					default:
						throw new UnsupportedFeatureException(outputField, this.rankBasis);
				} // End switch

				switch(this.rankOrder){
					case ASCENDING:
						return order;
					case DESCENDING:
						return -order;
					default:
						throw new UnsupportedFeatureException(outputField, this.rankOrder);
				}
			}

			private Double getConfidence(AssociationRule rule){
				return checkRuleFeature(rule, rule.getConfidence());
			}

			private Double getSupport(AssociationRule rule){
				return checkRuleFeature(rule, rule.getSupport());
			}

			private Double getLift(AssociationRule rule){
				return checkRuleFeature(rule, rule.getLift());
			}

			private Double getLeverage(AssociationRule rule){
				return checkRuleFeature(rule, rule.getLeverage());
			}

			private Double getAffinity(AssociationRule rule){
				return checkRuleFeature(rule, rule.getAffinity());
			}

			private <V> V checkRuleFeature(AssociationRule rule, V value){

				if(value == null){
					throw new InvalidFeatureException(rule);
				}

				return value;
			}
		};

		Ordering<AssociationRule> ordering = Ordering.from(comparator);

		return ordering.sortedCopy(associationRules);
	}

	static
	private Object getRuleFeature(HasRuleValues hasRuleValues, AssociationRule associationRule, OutputField outputField){
		return getRuleFeature(hasRuleValues, associationRule, outputField, outputField.getRuleFeature());
	}

	@SuppressWarnings (
		value = {"unchecked"}
	)
	static
	private Object getRuleFeature(HasRuleValues hasRuleValues, AssociationRule associationRule, PMMLObject element, OutputField.RuleFeature ruleFeature){

		switch(ruleFeature){
			case ANTECEDENT:
				return getItemValues(hasRuleValues, associationRule.getAntecedent());
			case CONSEQUENT:
				return getItemValues(hasRuleValues, associationRule.getConsequent());
			case RULE:
				{
					Joiner joiner = Joiner.on(',');

					StringBuilder sb = new StringBuilder();

					String left = joiner.join(getItemValues(hasRuleValues, associationRule.getAntecedent()));
					sb.append('{').append(left).append('}');

					sb.append("->");

					String right = joiner.join(getItemValues(hasRuleValues, associationRule.getConsequent()));
					sb.append('{').append(right).append('}');

					return sb.toString();
				}
			case RULE_ID:
				{
					HasEntityRegistry<AssociationRule> hasEntityRegistry = (HasEntityRegistry<AssociationRule>)hasRuleValues;

					return EntityUtil.getId(associationRule, hasEntityRegistry);
				}
			case CONFIDENCE:
				return associationRule.getConfidence();
			case SUPPORT:
				return associationRule.getSupport();
			case LIFT:
				return associationRule.getLift();
			case LEVERAGE:
				return associationRule.getLeverage();
			case AFFINITY:
				return associationRule.getAffinity();
			default:
				throw new UnsupportedFeatureException(element, ruleFeature);
		}
	}

	static
	private DataType getRuleDataType(OutputField outputField){
		return getRuleDataType(outputField, outputField.getRuleFeature());
	}

	static
	private DataType getRuleDataType(OutputField outputField, OutputField.RuleFeature ruleFeature){
		String isMultiValued = outputField.getIsMultiValued();
		if(!("0").equals(isMultiValued)){
			throw new TypeAnalysisException(outputField);
		}

		switch(ruleFeature){
			case ANTECEDENT:
			case CONSEQUENT:
				throw new TypeAnalysisException(outputField);
			case RULE:
			case RULE_ID:
				return DataType.STRING;
			case SUPPORT:
			case CONFIDENCE:
			case LIFT:
			case LEVERAGE:
			case AFFINITY:
				return DataType.DOUBLE;
			default:
				throw new UnsupportedFeatureException(outputField, ruleFeature);
		}
	}

	static
	private List<String> getItemValues(HasRuleValues hasRuleValues, String id){
		Map<String, Item> items = hasRuleValues.getItems();
		Map<String, Itemset> itemsets = hasRuleValues.getItemsets();

		Itemset itemset = itemsets.get(id);

		List<ItemRef> itemRefs = itemset.getItemRefs();

		List<String> result = new ArrayList<>(itemRefs.size());

		for(int i = 0, max = itemRefs.size(); i < max; i++){
			ItemRef itemRef = itemRefs.get(i);

			Item item = items.get(itemRef.getItemRef());

			result.add(item.getValue());
		}

		return result;
	}

	static
	private <E> E getElement(List<E> elements, int rank){
		int index = (rank - 1);

		if(index < elements.size()){
			return elements.get(index);
		}

		return null;
	}
}