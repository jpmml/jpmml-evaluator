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

import org.dmg.pmml.PMML;
import org.dmg.pmml.scorecard.Attribute;
import org.dmg.pmml.scorecard.Characteristics;
import org.dmg.pmml.scorecard.ComplexPartialScore;
import org.dmg.pmml.scorecard.PMMLAttributes;
import org.dmg.pmml.scorecard.PMMLElements;
import org.dmg.pmml.scorecard.Scorecard;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.ExpressionUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.PMMLUtil;

abstract
public class ScorecardEvaluator extends ModelEvaluator<Scorecard> {

	protected ScorecardEvaluator(){
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
		}
	}

	@Override
	public String getSummary(){
		return "Scorecard";
	}

	static
	protected Number evaluatePartialScore(Attribute attribute, EvaluationContext context){
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
}