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
package org.jpmml.evaluator.scorecard;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.PMML;
import org.dmg.pmml.scorecard.Attribute;
import org.dmg.pmml.scorecard.Characteristic;
import org.dmg.pmml.scorecard.Characteristics;
import org.dmg.pmml.scorecard.ComplexPartialScore;
import org.dmg.pmml.scorecard.Scorecard;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.ExpressionUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InvalidAttributeException;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.PMMLAttributes;
import org.jpmml.evaluator.PMMLElements;
import org.jpmml.evaluator.PredicateUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.UndefinedResultException;
import org.jpmml.evaluator.UnsupportedAttributeException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.evaluator.VoteAggregator;

public class ScorecardEvaluator extends ModelEvaluator<Scorecard> {

	public ScorecardEvaluator(PMML pmml){
		this(pmml, selectModel(pmml, Scorecard.class));
	}

	public ScorecardEvaluator(PMML pmml, Scorecard scorecard){
		super(pmml, scorecard);

		Characteristics characteristics = scorecard.getCharacteristics();
		if(characteristics == null){
			throw new MissingElementException(scorecard, PMMLElements.SCORECARD_CHARACTERISTICS);
		} // End if

		if(!characteristics.hasCharacteristics()){
			throw new MissingElementException(characteristics, PMMLElements.CHARACTERISTICS_CHARACTERISTICS);
		}
	}

	@Override
	public String getSummary(){
		return "Scorecard";
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		Scorecard scorecard = ensureScorableModel();

		ValueFactory<?> valueFactory;

		MathContext mathContext = scorecard.getMathContext();
		switch(mathContext){
			case FLOAT:
			case DOUBLE:
				valueFactory = getValueFactory();
				break;
			default:
				throw new UnsupportedAttributeException(scorecard, mathContext);
		}

		Map<FieldName, ?> predictions;

		MiningFunction miningFunction = scorecard.getMiningFunction();
		switch(miningFunction){
			case REGRESSION:
				predictions = evaluateRegression(valueFactory, context);
				break;
			case ASSOCIATION_RULES:
			case SEQUENCES:
			case CLASSIFICATION:
			case CLUSTERING:
			case TIME_SERIES:
			case MIXED:
				throw new InvalidAttributeException(scorecard, miningFunction);
			default:
				throw new UnsupportedAttributeException(scorecard, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private <V extends Number> Map<FieldName, ?> evaluateRegression(final ValueFactory<V> valueFactory, EvaluationContext context){
		Scorecard scorecard = getModel();

		boolean useReasonCodes = scorecard.isUseReasonCodes();

		TargetField targetField = getTargetField();

		Value<V> score = valueFactory.newValue(scorecard.getInitialScore());

		VoteAggregator<String, V> reasonCodePoints = null;

		if(useReasonCodes){
			reasonCodePoints = new VoteAggregator<String, V>(){

				@Override
				public ValueFactory<V> getValueFactory(){
					return valueFactory;
				}
			};
		}

		Characteristics characteristics = scorecard.getCharacteristics();
		for(Characteristic characteristic : characteristics){
			Double baselineScore = null;

			if(useReasonCodes){
				baselineScore = characteristic.getBaselineScore();
				if(baselineScore == null){
					baselineScore = scorecard.getBaselineScore();
				} // End if

				if(baselineScore == null){
					throw new MissingAttributeException(characteristic, PMMLAttributes.CHARACTERISTIC_BASELINESCORE);
				}
			}

			Double partialScore = null;

			List<Attribute> attributes = characteristic.getAttributes();
			for(Attribute attribute : attributes){
				Boolean status = PredicateUtil.evaluatePredicateContainer(attribute, context);
				if(status == null || !status.booleanValue()){
					continue;
				}

				ComplexPartialScore complexPartialScore = attribute.getComplexPartialScore();
				if(complexPartialScore != null){
					FieldValue computedValue = ExpressionUtil.evaluateExpressionContainer(complexPartialScore, context);
					if(computedValue == null){
						return TargetUtil.evaluateRegressionDefault(valueFactory, targetField);
					}

					partialScore = computedValue.asDouble();
				} else

				{
					partialScore = attribute.getPartialScore();
					if(partialScore == null){
						throw new MissingAttributeException(attribute, PMMLAttributes.ATTRIBUTE_PARTIALSCORE);
					}
				}

				score.add(partialScore);

				if(useReasonCodes){
					String reasonCode = attribute.getReasonCode();
					if(reasonCode == null){
						reasonCode = characteristic.getReasonCode();
					} // End if

					if(reasonCode == null){
						throw new MissingAttributeException(attribute, PMMLAttributes.ATTRIBUTE_REASONCODE);
					}

					double difference;

					Scorecard.ReasonCodeAlgorithm reasonCodeAlgorithm = scorecard.getReasonCodeAlgorithm();
					switch(reasonCodeAlgorithm){
						case POINTS_ABOVE:
							difference = (partialScore - baselineScore);
							break;
						case POINTS_BELOW:
							difference = (baselineScore - partialScore);
							break;
						default:
							throw new UnsupportedAttributeException(scorecard, reasonCodeAlgorithm);
					}

					reasonCodePoints.add(reasonCode, difference);
				}

				break;
			}

			// "If not even a single Attribute evaluates to "true" for a given Characteristic, then the scorecard as a whole returns an invalid value"
			if(partialScore == null){
				throw new UndefinedResultException()
					.ensureContext(characteristic);
			}
		}

		if(useReasonCodes){
			ReasonCodeRanking<V> result = createReasonCodeRanking(targetField, score, reasonCodePoints.sumMap());

			return TargetUtil.evaluateRegression(targetField, result);
		}

		return TargetUtil.evaluateRegression(targetField, score);
	}

	static
	private <V extends Number> ReasonCodeRanking<V> createReasonCodeRanking(TargetField targetField, Value<V> score, ValueMap<String, V> reasonCodePoints){
		score = TargetUtil.evaluateRegressionInternal(targetField, score);

		Collection<Map.Entry<String, Value<V>>> entrySet = reasonCodePoints.entrySet();
		for(Iterator<Map.Entry<String, Value<V>>> it = entrySet.iterator(); it.hasNext(); ){
			Map.Entry<String, Value<V>> entry = it.next();

			Value<V> value = entry.getValue();
			if(value.compareTo(0d) < 0){
				it.remove();
			}
		}

		return new ReasonCodeRanking<>(score, reasonCodePoints);
	}
}