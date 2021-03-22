/*
 * Copyright (c) 2021 Villu Ruusmann
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
import java.util.List;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.scorecard.Attribute;
import org.dmg.pmml.scorecard.Characteristic;
import org.dmg.pmml.scorecard.Characteristics;
import org.dmg.pmml.scorecard.PMMLAttributes;
import org.dmg.pmml.scorecard.Scorecard;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.PredicateUtil;
import org.jpmml.evaluator.Regression;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.UndefinedResultException;
import org.jpmml.evaluator.UnsupportedAttributeException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.annotations.Functionality;

@Functionality (
	value = {
		ResultFeature.PREDICTED_VALUE
	}
)
public class SimpleScorecardEvaluator extends ScorecardEvaluator {

	private SimpleScorecardEvaluator(){
	}

	public SimpleScorecardEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, Scorecard.class));
	}

	public SimpleScorecardEvaluator(PMML pmml, Scorecard scorecard){
		super(pmml, scorecard);

		boolean useReasonCodes = scorecard.isUseReasonCodes();
		if(useReasonCodes){
			throw new UnsupportedAttributeException(scorecard, PMMLAttributes.SCORECARD_USEREASONCODES, useReasonCodes);
		}
	}

	@Override
	protected <V extends Number> Map<FieldName, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		Scorecard scorecard = getModel();

		TargetField targetField = getTargetField();

		Value<V> score = valueFactory.newValue(scorecard.getInitialScore());

		List<PartialScore> partialScores = new ArrayList<>();

		Characteristics characteristics = scorecard.getCharacteristics();
		for(Characteristic characteristic : characteristics){
			PartialScore partialScore = null;

			List<Attribute> attributes = characteristic.getAttributes();
			for(Attribute attribute : attributes){
				Boolean status = PredicateUtil.evaluatePredicateContainer(attribute, context);
				if(status == null || !status.booleanValue()){
					continue;
				}

				Number value = evaluatePartialScore(attribute, context);
				if(value == null){
					return TargetUtil.evaluateRegressionDefault(valueFactory, targetField);
				}

				partialScore = new PartialScore(characteristic, attribute, value);

				score.add(value);

				break;
			}

			// "If not even a single Attribute evaluates to "true" for a given Characteristic, then the scorecard as a whole returns an invalid value"
			if(partialScore == null){
				throw new UndefinedResultException()
					.ensureContext(characteristic);
			}
		}

		Regression<V> result = new ScorecardScore<>(score, partialScores);

		return TargetUtil.evaluateRegression(targetField, result);
	}
}