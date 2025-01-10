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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.Ordering;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Model;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMMLAttributes;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.Target;
import org.dmg.pmml.TargetValue;
import org.dmg.pmml.Value;
import org.dmg.pmml.association.AssociationRule;
import org.dmg.pmml.association.Item;
import org.dmg.pmml.association.ItemRef;
import org.dmg.pmml.association.Itemset;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segmentation;
import org.jpmml.evaluator.mining.MiningModelEvaluationContext;
import org.jpmml.evaluator.mining.MiningModelUtil;
import org.jpmml.evaluator.mining.SegmentResult;
import org.jpmml.model.InvalidAttributeException;
import org.jpmml.model.InvalidElementException;
import org.jpmml.model.MisplacedAttributeException;
import org.jpmml.model.MissingAttributeException;
import org.jpmml.model.UnsupportedAttributeException;
import org.jpmml.model.UnsupportedElementException;

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
	static
	public Map<String, ?> evaluate(Map<String, ?> predictions, ModelEvaluationContext context){
		ModelEvaluator<?> modelEvaluator = context.getModelEvaluator();

		Model model = modelEvaluator.getModel();

		Output output = model.getOutput();
		if(output == null || !output.hasOutputFields()){
			return predictions;
		}

		OutputMap result = new OutputMap(predictions);

		OutputFilter outputFilter = modelEvaluator.ensureOutputFilter();

		List<OutputField> outputFields = output.getOutputFields();

		outputFields:
		for(int i = 0, max = outputFields.size(); i < max; i++){
			OutputField outputField = outputFields.get(i);

			String targetFieldName = outputField.getTargetField();

			Object targetValue = null;

			boolean requireTargetValue;

			ResultFeature resultFeature = outputField.getResultFeature();
			switch(resultFeature){
				case TRANSFORMED_VALUE:
				case DECISION:
				case WARNING:
					requireTargetValue = false;
					break;
				default:
					requireTargetValue = true;
					break;
			}

			String segmentId = outputField.getSegmentId();

			SegmentResult segmentPredictions;

			// Load the target value of the specified segment
			if(segmentId != null){

				if(!(model instanceof MiningModel)){
					throw new MisplacedAttributeException(outputField, PMMLAttributes.OUTPUTFIELD_SEGMENTID, segmentId);
				}

				MiningModelEvaluationContext miningModelContext = (MiningModelEvaluationContext)context;

				segmentPredictions = miningModelContext.getResult(segmentId);

				// "If there is no Segment matching segmentId or if the predicate of the matching Segment evaluated to false, then the result delivered by this OutputField is missing"
				if(segmentPredictions == null){
					continue outputFields;
				} // End if

				if(requireTargetValue){

					if(targetFieldName != null){

						if(!segmentPredictions.containsKey(targetFieldName)){
							throw new MissingFieldValueException(targetFieldName, outputField);
						}

						targetValue = segmentPredictions.get(targetFieldName);
					} else

					{
						targetValue = segmentPredictions.getTargetValue();
					}
				}
			} else

			// Load the target value
			{
				segmentPredictions = null;

				targetValue:
				if(requireTargetValue){

					if(model instanceof MiningModel){
						MiningModel miningModel = (MiningModel)model;

						switch(resultFeature){
							case ENTITY_ID:
								{
									if(targetFieldName != null){
										throw new MisplacedAttributeException(outputField, PMMLAttributes.OUTPUTFIELD_TARGETFIELD, targetFieldName);
									}

									// "Result feature entityId returns the id of the winning segment"
									targetValue = TypeUtil.cast(HasEntityId.class, predictions);

									break targetValue;
								}
							default:
								{
									if(targetFieldName != null){
										break;
									}

									Segmentation segmentation = miningModel.requireSegmentation();

									SegmentResult segmentResult = MiningModelUtil.asSegmentResult(segmentation.requireMultipleModelMethod(), predictions);
									if(segmentResult != null){
										targetValue = segmentResult.getTargetValue();

										break targetValue;
									}
								}
								break;
						}
					} // End if

					if(targetFieldName == null){
						targetFieldName = modelEvaluator.getTargetName();
					} // End if

					if(!predictions.containsKey(targetFieldName)){
						throw new MissingFieldValueException(targetFieldName, outputField);
					}

					targetValue = predictions.get(targetFieldName);
				}
			}

			// "If the target value is missing, then the result delivered by this OutputField is missing"
			if(requireTargetValue && targetValue == null){
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
						if(segmentId != null){
							throw new UnsupportedElementException(outputField);
						}

						TargetField targetField = modelEvaluator.findTargetField(targetFieldName);
						if(targetField == null){
							throw new MissingFieldException(targetFieldName, outputField);
						}

						value = getPredictedDisplayValue(targetValue, targetField);
					}
					break;
				case TRANSFORMED_VALUE:
				case DECISION:
					{
						if(segmentId != null){
							Object name = outputField.requireValue();

							value = segmentPredictions.get(TypeUtil.format(name));

							break;
						}

						value = FieldValueUtil.getValue(ExpressionUtil.evaluateExpressionContainer(outputField, context));
					}
					break;
				case PROBABILITY:
					{
						value = getProbability(targetValue, outputField);
					}
					break;
				case CONFIDENCE:
					{
						if(targetValue instanceof HasRuleValues){
							value = getRuleValue(targetValue, outputField, OutputField.RuleFeature.CONFIDENCE);

							break;
						}

						value = getConfidence(targetValue, outputField);
					}
					break;
				case RESIDUAL:
					{
						if(segmentId != null){
							throw new UnsupportedElementException(outputField);
						}

						FieldValue expectedTargetValue = context.evaluate(targetFieldName);
						if(FieldValueUtil.isMissing(expectedTargetValue)){
							throw new MissingFieldValueException(targetFieldName, outputField);
						}

						TargetField targetField = modelEvaluator.findTargetField(targetFieldName);
						if(targetField == null){
							throw new MissingFieldException(targetFieldName, outputField);
						}

						OpType opType = targetField.getOpType();
						switch(opType){
							case CONTINUOUS:
								value = getContinuousResidual(targetValue, expectedTargetValue);
								break;
							case CATEGORICAL:
							case ORDINAL:
								value = getDiscreteResidual(targetValue, expectedTargetValue);
								break;
							default:
								throw new InvalidElementException(outputField);
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
						Object entityId = outputField.getValue();

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
				case REPORT:
					{
						String reportFieldName = outputField.getReportField();
						if(reportFieldName == null){
							throw new MissingAttributeException(outputField, PMMLAttributes.OUTPUTFIELD_REPORTFIELD);
						}

						OutputField reportOutputField = modelEvaluator.getOutputField(reportFieldName);
						if(reportOutputField == null){
							throw new MissingFieldException(reportFieldName);
						}

						value = getReport(targetValue, reportOutputField);
					}
					break;
				case WARNING:
					{
						value = context.getWarnings();
					}
					break;
				default:
					throw new UnsupportedAttributeException(outputField, resultFeature);
			}

			String name = outputField.requireName();

			TypeInfo typeInfo = new TypeInfo(){

				@Override
				public OpType getOpType(){
					OpType opType = outputField.getOpType();
					if(opType == null){
						DataType dataType = getDataType();

						opType = TypeUtil.getOpType(dataType);
					}

					return opType;
				}

				@Override
				public DataType getDataType(){
					DataType dataType = outputField.getDataType();
					if(dataType == null){

						if(value instanceof Collection){
							Collection<?> values = (Collection<?>)value;

							dataType = TypeUtil.getDataType(values);
						} else

						{
							dataType = TypeUtil.getDataType(value);
						}
					}

					return dataType;
				}

				@Override
				public List<?> getOrdering(){
					List<?> ordering = FieldUtil.getValidValues(outputField);

					return ordering;
				}
			};

			FieldValue outputValue = FieldValueUtil.create(typeInfo, value);

			// The result of one output field becomes available to other output fields
			context.declare(name, outputValue);

			if(outputFilter.test(outputField)){
				result.putPublic(name, FieldValueUtil.getValue(outputValue));
			} else

			{
				result.putPrivate(name, FieldValueUtil.getValue(outputValue));
			}
		}

		return result;
	}

	static
	private Object getPredictedValue(Object object){
		return EvaluatorUtil.decode(object);
	}

	static
	private Object getPredictedDisplayValue(Object object, TargetField targetField){

		if(object instanceof HasDisplayValue){
			HasDisplayValue hasDisplayValue = TypeUtil.cast(HasDisplayValue.class, object);

			return hasDisplayValue.getDisplayValue();
		}

		object = getPredictedValue(object);

		Target target = targetField.getTarget();
		if(target != null){
			TargetValue targetValue = TargetUtil.getTargetValue(target, object);

			if(targetValue != null){
				String displayValue = targetValue.getDisplayValue();

				if(displayValue != null){
					return displayValue;
				}
			}
		}

		DataField dataField = targetField.getField();

		OpType opType = targetField.getOpType();
		switch(opType){
			case CONTINUOUS:
				break;
			case CATEGORICAL:
			case ORDINAL:
				{
					Value value = TargetFieldUtil.getValidValue(dataField, object);

					if(value != null){
						String displayValue = value.getDisplayValue();

						if(displayValue != null){
							return displayValue;
						}
					}
				}
				break;
			default:
				throw new UnsupportedAttributeException(dataField, opType);
		}

		// "If the display value is not specified explicitly, then the raw predicted value is used by default"
		return object;
	}

	static
	private Double getProbability(Object object, OutputField outputField){
		HasProbability hasProbability = TypeUtil.cast(HasProbability.class, object);

		Object value = getCategoryValue(object, outputField);

		return hasProbability.getProbability(value);
	}

	static
	private Double getConfidence(Object object, OutputField outputField){
		HasConfidence hasConfidence = TypeUtil.cast(HasConfidence.class, object);

		Object value = getCategoryValue(object, outputField);

		return hasConfidence.getConfidence(value);
	}

	static
	private Object getCategoryValue(Object object, OutputField outputField){
		Object value = outputField.getValue();

		// "If the value attribute is not specified, then the predicted categorical value should be returned as a result"
		if(value == null){
			return getPredictedValue(object);
		}

		return value;
	}

	static
	private Number getContinuousResidual(Object value, FieldValue expectedValue){
		value = getPredictedValue(value);

		return Functions.SUBTRACT.evaluate(expectedValue.asNumber(), (Number)value);
	}

	static
	public Number getDiscreteResidual(Object value, FieldValue expectedValue){
		HasProbability hasProbability = TypeUtil.cast(HasProbability.class, value);

		value = getPredictedValue(value);

		boolean equals = expectedValue.equalsValue(value);

		return Functions.SUBTRACT.evaluate(equals ? Numbers.DOUBLE_ONE : Numbers.DOUBLE_ZERO, hasProbability.getProbability(value));
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
			throw new InvalidAttributeException(outputField, PMMLAttributes.OUTPUTFIELD_RANK, rank);
		} // End if

		if(rank > 1){
			HasEntityIdRanking hasEntityIdRanking = TypeUtil.cast(HasEntityIdRanking.class, object);

			OutputField.RankOrder rankOrder = outputField.getRankOrder();
			switch(rankOrder){
				case DESCENDING:
					break;
				default:
					throw new UnsupportedAttributeException(outputField, rankOrder);
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
			throw new InvalidAttributeException(outputField, PMMLAttributes.OUTPUTFIELD_RANK, rank);
		} // End if

		if(rank > 1){
			HasAffinityRanking hasAffinityRanking = TypeUtil.cast(HasAffinityRanking.class, object);

			OutputField.RankOrder rankOrder = outputField.getRankOrder();
			switch(rankOrder){
				case DESCENDING:
					break;
				default:
					throw new UnsupportedAttributeException(outputField, rankOrder);
			}

			return getElement(hasAffinityRanking.getAffinityRanking(), rank);
		}

		Object value = getCategoryValue(object, outputField);

		return hasAffinity.getAffinity(TypeUtil.format(value));
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
			throw new InvalidAttributeException(outputField, PMMLAttributes.OUTPUTFIELD_RANK, rank);
		}

		return getElement(hasReasonCodeRanking.getReasonCodeRanking(), rank);
	}

	static
	public Object getRuleValue(Object object, OutputField outputField){
		return getRuleValue(object, outputField, outputField.getRuleFeature());
	}

	static
	public Object getRuleValue(Object object, OutputField outputField, OutputField.RuleFeature ruleFeature){
		HasRuleValues hasRuleValues = TypeUtil.cast(HasRuleValues.class, object);

		List<AssociationRule> associationRules = getRuleValues(hasRuleValues, outputField);

		OutputField.MultiValued multiValued = outputField.getMultiValued();
		switch(multiValued){
			// Return a single result
			case ZERO:
				{
					int rank = outputField.getRank();
					if(rank <= 0){
						throw new InvalidAttributeException(outputField, PMMLAttributes.OUTPUTFIELD_RANK, rank);
					}

					AssociationRule associationRule = getElement(associationRules, rank);
					if(associationRule != null){
						return getRuleFeature(hasRuleValues, associationRule, outputField, ruleFeature);
					}

					return null;
				}
			// Return multiple results
			case ONE:
				{
					int size;

					int rank = outputField.getRank();
					if(rank < 0){
						throw new InvalidAttributeException(outputField, PMMLAttributes.OUTPUTFIELD_RANK, rank);
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
						result.add(getRuleFeature(hasRuleValues, associationRule, outputField, ruleFeature));
					}

					return result;
				}
			default:
				throw new UnsupportedAttributeException(outputField, multiValued);
		}
	}

	static
	public String getReport(Object object, OutputField outputField){
		Report report = null;

		ResultFeature resultFeature = outputField.getResultFeature();
		switch(resultFeature){
			case PREDICTED_VALUE:
				{
					HasPrediction hasPrediction;

					try {
						hasPrediction = TypeUtil.cast(HasPrediction.class, object);
					} catch(TypeCheckException tce){
						return null;
					}

					report = hasPrediction.getPredictionReport();
				}
				break;
			case PROBABILITY:
				{
					HasProbability hasProbability = TypeUtil.cast(HasProbability.class, object);

					Object value = getCategoryValue(object, outputField);

					report = hasProbability.getProbabilityReport(value);
				}
				break;
			case CONFIDENCE:
				{
					HasConfidence hasConfidence = TypeUtil.cast(HasConfidence.class, object);

					Object value = getCategoryValue(object, outputField);

					report = hasConfidence.getConfidenceReport(value);
				}
				break;
			case AFFINITY:
				{
					HasAffinity hasAffinity = TypeUtil.cast(HasAffinity.class, object);

					Object value = getCategoryValue(object, outputField);

					report = hasAffinity.getAffinityReport(TypeUtil.format(value));
				}
				break;
			default:
				break;
		}

		return ReportUtil.format(report);
	}

	static
	private List<AssociationRule> getRuleValues(HasRuleValues hasRuleValues, OutputField outputField){
		List<AssociationRule> associationRules;

		OutputField.Algorithm algorithm = outputField.getAlgorithm();
		switch(algorithm){
			case RECOMMENDATION:
			case EXCLUSIVE_RECOMMENDATION:
			case RULE_ASSOCIATION:
				associationRules = hasRuleValues.getRuleValues(algorithm);
				break;
			default:
				throw new UnsupportedAttributeException(outputField, algorithm);
		}

		Comparator<AssociationRule> comparator = new Comparator<AssociationRule>(){

			private OutputField.RankBasis rankBasis = outputField.getRankBasis();

			private OutputField.RankOrder rankOrder = outputField.getRankOrder();


			@Override
			public int compare(AssociationRule left, AssociationRule right){
				Number leftValue;
				Number rightValue;

				switch(this.rankBasis){
					case CONFIDENCE:
						leftValue = left.requireConfidence();
						rightValue = right.requireConfidence();
						break;
					case SUPPORT:
						leftValue = left.requireSupport();
						rightValue = right.requireSupport();
						break;
					case LIFT:
						leftValue = left.requireLift();
						rightValue = right.requireLift();
						break;
					case LEVERAGE:
						leftValue = left.requireLeverage();
						rightValue = right.requireLeverage();
						break;
					case AFFINITY:
						leftValue = left.requireAffinity();
						rightValue = right.requireAffinity();
						break;
					default:
						throw new UnsupportedAttributeException(outputField, this.rankBasis);
				}

				int order = NumberUtil.compare(leftValue, rightValue);

				switch(this.rankOrder){
					case ASCENDING:
						return order;
					case DESCENDING:
						return -order;
					default:
						throw new UnsupportedAttributeException(outputField, this.rankOrder);
				}
			}
		};

		Ordering<AssociationRule> ordering = Ordering.from(comparator);

		return ordering.sortedCopy(associationRules);
	}

	@SuppressWarnings("unchecked")
	static
	private Object getRuleFeature(HasRuleValues hasRuleValues, AssociationRule associationRule, OutputField outputField, OutputField.RuleFeature ruleFeature){

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
				throw new UnsupportedAttributeException(outputField, ruleFeature);
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

			Item item = items.get(itemRef.requireItemRef());

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