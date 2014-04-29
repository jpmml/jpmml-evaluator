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

import org.dmg.pmml.*;

abstract
public class RuleSelectionMethodTest extends ModelEvaluatorTest {

	public String getRuleId(RuleSelectionMethod.Criterion criterion) throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		RuleSetModel ruleSetModel = (RuleSetModel)evaluator.getModel();

		RuleSet ruleSet = ruleSetModel.getRuleSet();

		List<RuleSelectionMethod> ruleSelectionMethods = ruleSet.getRuleSelectionMethods();

		// Move the specified criterion to the first place in the list
		for(Iterator<RuleSelectionMethod> it = ruleSelectionMethods.iterator(); it.hasNext(); ){
			RuleSelectionMethod ruleSelectionMethod = it.next();

			if((ruleSelectionMethod.getCriterion()).equals(criterion)){
				break;
			}

			it.remove();
		}

		Map<FieldName, ?> arguments = createArguments("BP", "HIGH", "K", 0.0621d, "Age", 36, "Na", 0.5023);

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		return getEntityId(result.get(evaluator.getTargetField()));
	}
}