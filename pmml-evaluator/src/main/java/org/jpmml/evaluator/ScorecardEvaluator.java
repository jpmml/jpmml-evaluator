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

import java.util.*;

import org.jpmml.manager.*;

import org.dmg.pmml.*;

import com.google.common.collect.*;

public class ScorecardEvaluator extends ModelEvaluator<Scorecard> {

	public ScorecardEvaluator(PMML pmml){
		this(pmml, find(pmml.getModels(), Scorecard.class));
	}

	public ScorecardEvaluator(PMML pmml, Scorecard scorecard){
		super(pmml, scorecard);
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

		Map<FieldName, ?> predictions;

		MiningFunctionType miningFunction = scorecard.getFunctionName();
		switch(miningFunction){
			case REGRESSION:
				predictions = evaluateRegression(context);
				break;
			default:
				throw new UnsupportedFeatureException(scorecard, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private Map<FieldName, ?> evaluateRegression(ModelEvaluationContext context){
		Scorecard scorecard = getModel();

		double score = scorecard.getInitialScore();

		boolean useReasonCodes = scorecard.isUseReasonCodes();

		VoteCounter<String> reasonCodePoints = new VoteCounter<String>();

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
						throw new MissingResultException(expression);
					}

					partialScore = (computedValue.asNumber()).doubleValue();
				} else

				{
					partialScore = attribute.getPartialScore();
				} // End if

				if(partialScore == null){
					throw new InvalidFeatureException(attribute);
				}

				score += partialScore.doubleValue();

				String reasonCode = attribute.getReasonCode();
				if(reasonCode == null){
					reasonCode = characteristic.getReasonCode();
				} // End if

				if(useReasonCodes){

					if(reasonCode == null){
						throw new InvalidFeatureException(attribute);
					}

					Double difference;

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

					reasonCodePoints.increment(reasonCode, difference);
				}

				hasTrueAttribute = true;

				break;
			}

			// "If not even a single Attribute evaluates to "true" for a given Characteristic, the scorecard as a whole returns an invalid value"
			if(!hasTrueAttribute){
				throw new InvalidResultException(characteristic);
			}
		}

		Map<FieldName, ? extends Number> result = TargetUtil.evaluateRegression(score, context);

		if(useReasonCodes){
			Map.Entry<FieldName, ? extends Number> resultEntry = Iterables.getOnlyElement(result.entrySet());

			return Collections.singletonMap(resultEntry.getKey(), createScoreMap(resultEntry.getValue(), reasonCodePoints));
		}

		return result;
	}

	static
	private ScoreClassificationMap createScoreMap(Number value, Map<String, Double> reasonCodePoints){
		ScoreClassificationMap result = new ScoreClassificationMap(value);

		// Filter out meaningless (ie. negative values) explanations
		com.google.common.base.Predicate<Map.Entry<String, Double>> predicate = new com.google.common.base.Predicate<Map.Entry<String, Double>>(){

			@Override
			public boolean apply(Map.Entry<String, Double> entry){
				return Double.compare(entry.getValue(), 0) >= 0;
			}
		};
		result.putAll(Maps.filterEntries(reasonCodePoints, predicate));

		return result;
	}
}