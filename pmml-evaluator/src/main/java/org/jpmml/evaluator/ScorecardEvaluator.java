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
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.dmg.pmml.Attribute;
import org.dmg.pmml.Characteristic;
import org.dmg.pmml.Characteristics;
import org.dmg.pmml.ComplexPartialScore;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.Scorecard;

public class ScorecardEvaluator extends ModelEvaluator<Scorecard> {

	public ScorecardEvaluator(PMML pmml){
		super(pmml, Scorecard.class);
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

		VoteAggregator<String> reasonCodePoints = new VoteAggregator<>();

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
						return TargetUtil.evaluateRegressionDefault(context);
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

		Map<FieldName, ?> result = TargetUtil.evaluateRegression(score, context);

		if(useReasonCodes){
			Map.Entry<FieldName, ?> resultEntry = Iterables.getOnlyElement(result.entrySet());

			return Collections.singletonMap(resultEntry.getKey(), createReasonCodeList(reasonCodePoints.sumMap(), resultEntry.getValue()));
		}

		return result;
	}

	static
	private ReasonCodeRanking createReasonCodeList(Map<String, Double> reasonCodes, Object value){
		com.google.common.base.Predicate<Map.Entry<String, Double>> predicate = new com.google.common.base.Predicate<Map.Entry<String, Double>>(){

			@Override
			public boolean apply(Map.Entry<String, Double> entry){
				return Double.compare(entry.getValue(), 0) >= 0;
			}
		};

		Map<String, Double> meaningfulReasonCodes = Maps.filterEntries(reasonCodes, predicate);

		ReasonCodeRanking result = new ReasonCodeRanking(value, meaningfulReasonCodes);

		return result;
	}
}