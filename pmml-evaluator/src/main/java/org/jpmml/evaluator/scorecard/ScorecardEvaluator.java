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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.scorecard.Attribute;
import org.dmg.pmml.scorecard.Characteristic;
import org.dmg.pmml.scorecard.Characteristics;
import org.dmg.pmml.scorecard.ComplexPartialScore;
import org.dmg.pmml.scorecard.Scorecard;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.ExpressionUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InvalidFeatureException;
import org.jpmml.evaluator.InvalidResultException;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.PredicateUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.UnsupportedFeatureException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.VoteAggregator;

public class ScorecardEvaluator extends ModelEvaluator<Scorecard> {

	public ScorecardEvaluator(PMML pmml){
		this(pmml, selectModel(pmml, Scorecard.class));
	}

	public ScorecardEvaluator(PMML pmml, Scorecard scorecard){
		super(pmml, scorecard);

		Characteristics characteristics = scorecard.getCharacteristics();
		if(characteristics == null){
			throw new InvalidFeatureException(scorecard);
		} // End if

		if(!characteristics.hasCharacteristics()){
			throw new InvalidFeatureException(characteristics);
		}
	}

	@Override
	public String getSummary(){
		return "Scorecard";
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		Scorecard scorecard = getModel();
		if(!scorecard.isScorable()){
			throw new InvalidResultException(scorecard);
		}

		MathContext mathContext = scorecard.getMathContext();
		switch(mathContext){
			case DOUBLE:
				break;
			default:
				throw new UnsupportedFeatureException(scorecard, mathContext);
		}

		Map<FieldName, ?> predictions;

		MiningFunction miningFunction = scorecard.getMiningFunction();
		switch(miningFunction){
			case REGRESSION:
				predictions = evaluateRegression(context);
				break;
			default:
				throw new UnsupportedFeatureException(scorecard, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private Map<FieldName, ?> evaluateRegression(EvaluationContext context){
		Scorecard scorecard = getModel();

		TargetField targetField = getTargetField();

		double score = scorecard.getInitialScore();

		boolean useReasonCodes = scorecard.isUseReasonCodes();

		VoteAggregator<String, Double> reasonCodePoints = new VoteAggregator<String, Double>(){

			@Override
			public ValueFactory<Double> getValueFactory(){
				return ValueFactory.DOUBLE;
			}
		};

		Characteristics characteristics = scorecard.getCharacteristics();
		for(Characteristic characteristic : characteristics){
			Double baselineScore = characteristic.getBaselineScore();
			if(baselineScore == null){
				baselineScore = scorecard.getBaselineScore();
			} // End if

			if(useReasonCodes){

				if(baselineScore == null){
					throw new InvalidFeatureException(characteristic);
				}
			}

			boolean hasTrueAttribute = false;

			List<Attribute> attributes = characteristic.getAttributes();
			for(Attribute attribute : attributes){
				Predicate predicate = attribute.getPredicate();
				if(predicate == null){
					throw new InvalidFeatureException(attribute);
				}

				Boolean status = PredicateUtil.evaluate(predicate, context);
				if(status == null || !status.booleanValue()){
					continue;
				}

				Double partialScore = null;

				ComplexPartialScore complexPartialScore = attribute.getComplexPartialScore();
				if(complexPartialScore != null){
					Expression expression = complexPartialScore.getExpression();
					if(expression == null){
						throw new InvalidFeatureException(complexPartialScore);
					}

					FieldValue computedValue = ExpressionUtil.evaluate(expression, context);
					if(computedValue == null){
						return TargetUtil.evaluateRegressionDefault(ValueFactory.DOUBLE, targetField);
					}

					partialScore = computedValue.asDouble();
				} else

				{
					partialScore = attribute.getPartialScore();
				} // End if

				if(partialScore == null){
					throw new InvalidFeatureException(attribute);
				}

				score += partialScore;

				String reasonCode = attribute.getReasonCode();
				if(reasonCode == null){
					reasonCode = characteristic.getReasonCode();
				} // End if

				if(useReasonCodes){

					if(reasonCode == null){
						throw new InvalidFeatureException(attribute);
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
							throw new UnsupportedFeatureException(scorecard, reasonCodeAlgorithm);
					}

					reasonCodePoints.add(reasonCode, difference);
				}

				hasTrueAttribute = true;

				break;
			}

			// "If not even a single Attribute evaluates to "true" for a given Characteristic, the scorecard as a whole returns an invalid value"
			if(!hasTrueAttribute){
				throw new InvalidResultException(characteristic);
			}
		}

		Object result = TargetUtil.evaluateRegressionInternal(targetField, score);

		if(useReasonCodes){
			result = createReasonCodeList(reasonCodePoints.sumMap(), result);
		}

		return Collections.singletonMap(targetField.getName(), result);
	}

	static
	private ReasonCodeRanking createReasonCodeList(Map<String, Value<Double>> reasonCodes, Object value){
		Map<String, Double> meaningfulReasonCodes = new LinkedHashMap<>();

		Collection<Map.Entry<String, Value<Double>>> entrySet = reasonCodes.entrySet();
		for(Map.Entry<String, Value<Double>> entry : entrySet){
			String reasonCode = entry.getKey();
			Value<Double> points = entry.getValue();

			if(points.doubleValue() >= 0d){
				meaningfulReasonCodes.put(reasonCode, points.doubleValue());
			}
		}

		ReasonCodeRanking result = new ReasonCodeRanking(value, meaningfulReasonCodes);

		return result;
	}
}