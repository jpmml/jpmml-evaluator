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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.BiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.dmg.pmml.AssociationRule;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Item;
import org.dmg.pmml.ItemRef;
import org.dmg.pmml.Itemset;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.ResultFeatureType;
import org.dmg.pmml.RuleFeatureType;
import org.dmg.pmml.Target;
import org.dmg.pmml.TargetValue;
import org.jpmml.manager.InvalidFeatureException;
import org.jpmml.manager.UnsupportedFeatureException;

public class OutputUtil {

	private OutputUtil(){
	}

	/**
	 * Evaluates the {@link Output} element.
	 *
	 * @param predictions Map of {@link Evaluator#getTargetFields() target field} values.
	 *
	 * @return Map of {@link Evaluator#getTargetFields() target field} values together with {@link Evaluator#getOutputFields() output field} values.
	 */
	@SuppressWarnings (
		value = {"fallthrough"}
	)
	static
	public Map<FieldName, ?> evaluate(Map<FieldName, ?> predictions, ModelEvaluationContext context){
		ModelEvaluator<?> modelEvaluator = context.getModelEvaluator();

		Output output = modelEvaluator.getOutput();
		if(output == null){
			return predictions;
		}

		Map<FieldName, Object> result = Maps.newLinkedHashMap(predictions);

		List<OutputField> outputFields = output.getOutputFields();

		outputFields:
		for(OutputField outputField : outputFields){
			Map<FieldName, ?> segmentPredictions = predictions;

			String segmentId = outputField.getSegmentId();
			if(segmentId != null){
				MiningModelEvaluationContext miningModelContext = (MiningModelEvaluationContext)context;

				segmentPredictions = miningModelContext.getResult(segmentId);
			} // End if

			// "If there is no Segment matching segmentId or if the predicate of the matching Segment evaluated to false, then the result delivered by this OutputField is missing."
			if(segmentPredictions == null){
				continue outputFields;
			}

			// "Attribute targetField is required in case the model has multiple target fields."
			FieldName targetField = outputField.getTargetField();
			if(targetField == null){
				targetField = modelEvaluator.getTargetField();
			}

			Object value = null;

			ResultFeatureType resultFeature = outputField.getFeature();

			// "If the attribute feature is not specified then the output value is a copy of the target field value."
			if(resultFeature == null){
				resultFeature = ResultFeatureType.PREDICTED_VALUE;
			}

			// Load the mining result
			switch(resultFeature){
				case ENTITY_ID:
					{
						if(isSegmentId(segmentPredictions, outputField)){
							break;
						}
					}
					// Falls through
				case PREDICTED_VALUE:
				case PREDICTED_DISPLAY_VALUE:
				case PROBABILITY:
				case RESIDUAL:
				case CLUSTER_ID:
				case AFFINITY:
				case ENTITY_AFFINITY:
				case CLUSTER_AFFINITY:
				case RULE_VALUE:
				case REASON_CODE:
				case ANTECEDENT:
				case CONSEQUENT:
				case RULE:
				case RULE_ID:
				case CONFIDENCE:
				case SUPPORT:
				case LIFT:
				case LEVERAGE:
					{
						if(!segmentPredictions.containsKey(targetField)){
							throw new MissingFieldException(targetField, outputField);
						}

						// A target value could be either simple or complex values
						value = segmentPredictions.get(targetField);

						// If the target value is missing, then the result delivered by this OutputField is missing
						if(value == null){
							continue outputFields;
						}
					}
					break;
				default:
					break;
			} // End switch

			// Perform the requested computation on the mining result
			switch(resultFeature){
				case PREDICTED_VALUE:
					{
						value = getPredictedValue(value);
					}
					break;
				case PREDICTED_DISPLAY_VALUE:
					{
						Target target = modelEvaluator.getTarget(targetField);

						value = getPredictedDisplayValue(value, target);
					}
					break;
				case TRANSFORMED_VALUE:
				case DECISION:
					{
						if(segmentId != null){
							throw new UnsupportedFeatureException(outputField);
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
						value = getProbability(value, outputField);
					}
					break;
				case RESIDUAL:
					{
						FieldValue expectedValue = context.getField(targetField);
						if(expectedValue == null){
							throw new MissingFieldException(targetField, outputField);
						}

						DataField dataField = modelEvaluator.getDataField(targetField);

						OpType opType = dataField.getOptype();
						switch(opType){
							case CONTINUOUS:
								value = getContinuousResidual(value, expectedValue);
								break;
							case CATEGORICAL:
								value = getCategoricalResidual(value, expectedValue);
								break;
							default:
								throw new UnsupportedFeatureException(outputField, opType);
						}
					}
					break;
				case ENTITY_ID:
					{
						// "Result feature entityId returns the id of the winning segment"
						if(isSegmentId(segmentPredictions, outputField)){
							SegmentResultMap segmentResult = (SegmentResultMap)segmentPredictions;

							value = segmentResult.getId();

							break;
						}

						value = getEntityId(value, outputField);
					}
					break;
				case CLUSTER_ID:
					{
						value = getClusterId(value);
					}
					break;
				case AFFINITY:
				case ENTITY_AFFINITY:
					{
						value = getAffinity(value, outputField);
					}
					break;
				case CLUSTER_AFFINITY:
					{
						value = getClusterAffinity(value);
					}
					break;
				case RULE_VALUE:
					{
						value = getRuleValue(value, outputField);
					}
					break;
				case REASON_CODE:
					{
						value = getReasonCode(value, outputField);
					}
					break;
				case ANTECEDENT:
					{
						value = getRuleValue(value, outputField, RuleFeatureType.ANTECEDENT);
					}
					break;
				case CONSEQUENT:
					{
						value = getRuleValue(value, outputField, RuleFeatureType.CONSEQUENT);
					}
					break;
				case RULE:
					{
						value = getRuleValue(value, outputField, RuleFeatureType.RULE);
					}
					break;
				case RULE_ID:
					{
						value = getRuleValue(value, outputField, RuleFeatureType.RULE_ID);
					}
					break;
				case CONFIDENCE:
					{
						value = getRuleValue(value, outputField, RuleFeatureType.CONFIDENCE);
					}
					break;
				case SUPPORT:
					{
						value = getRuleValue(value, outputField, RuleFeatureType.SUPPORT);
					}
					break;
				case LIFT:
					{
						value = getRuleValue(value, outputField, RuleFeatureType.LIFT);
					}
					break;
				case LEVERAGE:
					{
						value = getRuleValue(value, outputField, RuleFeatureType.LEVERAGE);
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

			// The result of one output field becomes available to other other output fields
			context.declare(outputField.getName(), FieldValueUtil.create(outputField, value));

			result.put(outputField.getName(), value);
		}

		return result;
	}

	static
	private boolean isSegmentId(Map<FieldName, ?> predictions, OutputField outputField){
		FieldName targetField = outputField.getTargetField();

		if(targetField == null){
			return (predictions instanceof SegmentResultMap);
		}

		return false;
	}

	static
	private Object getPredictedValue(Object object){
		return EvaluatorUtil.decode(object);
	}

	static
	private Object getPredictedDisplayValue(Object object, Target target){

		if(object instanceof HasDisplayValue){
			HasDisplayValue hasDisplayValue = asResultFeature(HasDisplayValue.class, object);

			return hasDisplayValue.getDisplayValue();
		}

		object = getPredictedValue(object);

		if(target != null){
			TargetValue targetValue = TargetUtil.getTargetValue(target, object);

			if(targetValue != null){
				return targetValue.getDisplayValue();
			}
		}

		return object;
	}

	static
	private Double getProbability(Object object, final OutputField outputField){
		HasProbability hasProbability = asResultFeature(HasProbability.class, object);

		return hasProbability.getProbability(outputField.getValue());
	}

	static
	private Double getContinuousResidual(Object object, FieldValue expectedObject){
		object = getPredictedValue(object);

		Number value = (Number)object;
		Number expectedValue = (Number)FieldValueUtil.getValue(expectedObject);

		return Double.valueOf(expectedValue.doubleValue() - value.doubleValue());
	}

	static
	public Double getCategoricalResidual(Object object, FieldValue expectedObject){
		HasProbability hasProbability = asResultFeature(HasProbability.class, object);

		object = getPredictedValue(object);

		String value = TypeUtil.format(object);
		String expectedValue = TypeUtil.format(FieldValueUtil.getValue(expectedObject));

		boolean equals = TypeUtil.equals(DataType.STRING, value, expectedValue);

		return Double.valueOf((equals ? 1d : 0d) - hasProbability.getProbability(value));
	}

	static
	private String getEntityId(Object object, final OutputField outputField){
		HasEntityId hasEntityId = asResultFeature(HasEntityId.class, object);

		int rank = outputField.getRank();
		if(rank <= 0){
			throw new InvalidFeatureException(outputField);
		}

		if(rank > 1){
			HasEntityIdRanking hasEntityIdRanking = asResultFeature(HasEntityIdRanking.class, object);

			return getElement(hasEntityIdRanking.getEntityIdRanking(), rank);
		}

		return hasEntityId.getEntityId();
	}

	static
	private String getClusterId(Object object){
		HasClusterId hasClusterId = asResultFeature(HasClusterId.class, object);

		return hasClusterId.getClusterId();
	}

	static
	public Double getAffinity(Object object, final OutputField outputField){
		HasAffinity hasAffinity = asResultFeature(HasAffinity.class, object);

		int rank = outputField.getRank();
		if(rank <= 0){
			throw new InvalidFeatureException(outputField);
		}

		if(rank > 1){
			HasAffinityRanking hasAffinityRanking = asResultFeature(HasAffinityRanking.class, object);

			return getElement(hasAffinityRanking.getAffinityRanking(), rank);
		}

		return hasAffinity.getAffinity(outputField.getValue());
	}

	static
	public Double getClusterAffinity(Object object){
		HasClusterAffinity hasClusterAffinity = asResultFeature(HasClusterAffinity.class, object);

		return hasClusterAffinity.getClusterAffinity();
	}

	static
	public Object getRuleValue(Object object, OutputField outputField, RuleFeatureType ruleFeature){
		HasRuleValues hasRuleValues = asResultFeature(HasRuleValues.class, object);

		List<AssociationRule> associationRules = hasRuleValues.getRuleValues(outputField.getAlgorithm());
		sortRules(associationRules, outputField);

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
		HasRuleValues hasRuleValues = asResultFeature(HasRuleValues.class, object);

		List<AssociationRule> associationRules = hasRuleValues.getRuleValues(outputField.getAlgorithm());
		sortRules(associationRules, outputField);

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

			// "a zero value indicates that all output values are to be returned"
			if(rank == 0){
				size = associationRules.size();
			} else

			// "a positive value indicates the number of output values to be returned"
			{
				size = Math.min(rank, associationRules.size());
			}

			associationRules = associationRules.subList(0, size);

			List<Object> result = Lists.newArrayList();

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
	public String getReasonCode(Object object, final OutputField outputField){
		HasReasonCodeRanking hasReasonCodeRanking = asResultFeature(HasReasonCodeRanking.class, object);

		int rank = outputField.getRank();
		if(rank <= 0){
			throw new InvalidFeatureException(outputField);
		}

		return getElement(hasReasonCodeRanking.getReasonCodeRanking(), rank);
	}

	static
	private void sortRules(List<AssociationRule> associationRules, final OutputField outputField){
		Comparator<AssociationRule> comparator = new Comparator<AssociationRule>(){

			private OutputField.RankBasis rankBasis = outputField.getRankBasis();

			private OutputField.RankOrder rankOrder = outputField.getRankOrder();


			@Override
			public int compare(AssociationRule left, AssociationRule right){
				int order;

				switch(this.rankBasis){
					case CONFIDENCE:
						order = Double.compare(left.getConfidence(), right.getConfidence());
						break;
					case SUPPORT:
						order = Double.compare(left.getSupport(), right.getSupport());
						break;
					case LIFT:
						order = (left.getLift()).compareTo(right.getLift());
						break;
					case LEVERAGE:
						order = (left.getLeverage()).compareTo(right.getLeverage());
						break;
					case AFFINITY:
						order = (left.getAffinity()).compareTo(right.getAffinity());
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
		};
		Collections.sort(associationRules, comparator);
	}

	static
	private Object getRuleFeature(HasRuleValues hasRuleValues, AssociationRule associationRule, OutputField outputField){
		return getRuleFeature(hasRuleValues, associationRule, outputField, outputField.getRuleFeature());
	}

	static
	private Object getRuleFeature(HasRuleValues hasRuleValues, AssociationRule associationRule, PMMLObject element, RuleFeatureType ruleFeature){

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
					String id = associationRule.getId();
					if(id == null){
						BiMap<String, AssociationRule> associationRuleRegistry = hasRuleValues.getAssociationRuleRegistry();

						id = (associationRuleRegistry.inverse()).get(associationRule);
					}

					return id;
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
	private List<String> getItemValues(HasRuleValues hasRuleValues, String id){
		List<String> result = Lists.newArrayList();

		BiMap<String, Item> itemRegistry = hasRuleValues.getItemRegistry();
		BiMap<String, Itemset> itemsetRegistry = hasRuleValues.getItemsetRegistry();

		Itemset itemset = itemsetRegistry.get(id);

		List<ItemRef> itemRefs = itemset.getItemRefs();
		for(ItemRef itemRef : itemRefs){
			Item item = itemRegistry.get(itemRef.getItemRef());

			result.add(item.getValue());
		}

		return result;
	}

	static
	private <E extends ResultFeature> E asResultFeature(Class<? extends E> clazz, Object object){

		if(!clazz.isInstance(object)){
			throw new TypeCheckException(clazz, object);
		}

		return clazz.cast(object);
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