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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.scorecard.Attribute;
import org.dmg.pmml.scorecard.Characteristic;
import org.dmg.pmml.scorecard.Characteristics;
import org.dmg.pmml.scorecard.ComplexPartialScore;
import org.dmg.pmml.scorecard.PMMLAttributes;
import org.dmg.pmml.scorecard.PMMLElements;
import org.dmg.pmml.scorecard.Scorecard;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.ExpressionUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.Functions;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.Numbers;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.PredicateUtil;
import org.jpmml.evaluator.Regression;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.UndefinedResultException;
import org.jpmml.evaluator.UnsupportedAttributeException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.evaluator.VoteAggregator;

public class ScorecardEvaluator extends ModelEvaluator<Scorecard> {

	private ScorecardEvaluator(){
	}

	public ScorecardEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, Scorecard.class));
	}

	public ScorecardEvaluator(PMML pmml, Scorecard scorecard){
		super(pmml, scorecard);

		Characteristics characteristics = scorecard.getCharacteristics();
		if(characteristics == null){
			throw new MissingElementException(scorecard, PMMLElements.SCORECARD_CHARACTERISTICS);
		} // End if

		if(!characteristics.hasCharacteristics()){
			throw new MissingElementException(characteristics, PMMLElements.CHARACTERISTICS_CHARACTERISTICS);
		} else

		{
			for(Characteristic characteristic : characteristics){

				if(!characteristic.hasAttributes()){
					throw new MissingElementException(characteristic, PMMLElements.CHARACTERISTIC_ATTRIBUTES);
				}
			}
		}
	}

	@Override
	public String getSummary(){
		return "Scorecard";
	}

	@Override
	protected <V extends Number> Map<FieldName, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		Scorecard scorecard = getModel();

		boolean useReasonCodes = scorecard.isUseReasonCodes();

		TargetField targetField = getTargetField();

		Value<V> value = valueFactory.newValue(scorecard.getInitialScore());

		List<PartialScore> partialScores = new ArrayList<>();

		VoteAggregator<String, V> reasonCodePoints = null;

		if(useReasonCodes){
			reasonCodePoints = new VoteAggregator<>(valueFactory);
		}

		Characteristics characteristics = scorecard.getCharacteristics();
		for(Characteristic characteristic : characteristics){
			Number baselineScore = null;

			if(useReasonCodes){
				baselineScore = characteristic.getBaselineScore(scorecard.getBaselineScore());
				if(baselineScore == null){
					throw new MissingAttributeException(characteristic, PMMLAttributes.CHARACTERISTIC_BASELINESCORE);
				}
			}

			PartialScore partialScore = null;

			List<Attribute> attributes = characteristic.getAttributes();
			for(Attribute attribute : attributes){
				Boolean status = PredicateUtil.evaluatePredicateContainer(attribute, context);
				if(status == null || !status.booleanValue()){
					continue;
				}

				Number score = evaluatePartialScore(attribute, context);
				if(score == null){
					return TargetUtil.evaluateRegressionDefault(valueFactory, targetField);
				}

				partialScore = new PartialScore(characteristic, attribute, score);

				value.add(score);

				if(useReasonCodes){
					String reasonCode = attribute.getReasonCode(characteristic.getReasonCode());
					if(reasonCode == null){
						throw new MissingAttributeException(attribute, PMMLAttributes.ATTRIBUTE_REASONCODE);
					}

					Number difference;

					Scorecard.ReasonCodeAlgorithm reasonCodeAlgorithm = scorecard.getReasonCodeAlgorithm();
					switch(reasonCodeAlgorithm){
						case POINTS_ABOVE:
							difference = Functions.SUBTRACT.evaluate(score, baselineScore);
							break;
						case POINTS_BELOW:
							difference = Functions.SUBTRACT.evaluate(baselineScore, score);
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

			partialScores.add(partialScore);
		}

		if(useReasonCodes){
			ComplexScorecardScore<V> result = createComplexScorecardScore(targetField, value, partialScores, reasonCodePoints.sumMap());

			return TargetUtil.evaluateRegression(targetField, result);
		}

		Regression<V> result = createSimpleScorecardScore(targetField, value, partialScores);

		return TargetUtil.evaluateRegression(targetField, result);
	}

	static
	private Number evaluatePartialScore(Attribute attribute, EvaluationContext context){
		ComplexPartialScore complexPartialScore = attribute.getComplexPartialScore();

		// "If both are defined, the ComplexPartialScore element takes precedence over the partialScore attribute for computing the score points"
		if(complexPartialScore != null){
			FieldValue computedValue = ExpressionUtil.evaluateExpressionContainer(complexPartialScore, context);
			if(FieldValueUtil.isMissing(computedValue)){
				return null;
			}

			return computedValue.asNumber();
		} else

		{
			Number partialScore = attribute.getPartialScore();
			if(partialScore == null){
				throw new MissingAttributeException(attribute, PMMLAttributes.ATTRIBUTE_PARTIALSCORE);
			}

			return partialScore;
		}
	}

	static
	private <V extends Number> SimpleScorecardScore<V> createSimpleScorecardScore(TargetField targetField, Value<V> value, List<PartialScore> partialScores){
		value = TargetUtil.evaluateRegressionInternal(targetField, value);

		return new SimpleScorecardScore<>(value, partialScores);
	}

	static
	private <V extends Number> ComplexScorecardScore<V> createComplexScorecardScore(TargetField targetField, Value<V> value, List<PartialScore> partialScores, ValueMap<String, V> reasonCodePoints){
		value = TargetUtil.evaluateRegressionInternal(targetField, value);

		Collection<Map.Entry<String, Value<V>>> entrySet = reasonCodePoints.entrySet();
		for(Iterator<Map.Entry<String, Value<V>>> it = entrySet.iterator(); it.hasNext(); ){
			Map.Entry<String, Value<V>> entry = it.next();

			String reasonCode = entry.getKey();
			Value<V> points = entry.getValue();

			if(points.compareTo(Numbers.DOUBLE_ZERO) < 0){
				it.remove();
			}
		}

		return new ComplexScorecardScore<>(value, partialScores, reasonCodePoints);
	}
}