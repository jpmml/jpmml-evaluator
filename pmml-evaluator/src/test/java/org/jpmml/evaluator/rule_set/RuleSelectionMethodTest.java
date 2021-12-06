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
package org.jpmml.evaluator.rule_set;

import java.util.Map;

import org.dmg.pmml.rule_set.RuleSelectionMethod;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;

abstract
public class RuleSelectionMethodTest extends ModelEvaluatorTest {

	public String getScore(Map<String, ?> arguments) throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		Map<String, ?> results = evaluator.evaluate(arguments);

		SimpleRuleScoreDistribution<?> targetValue = (SimpleRuleScoreDistribution<?>)results.get(evaluator.getTargetName());

		return (String)targetValue.getResult();
	}

	public String getRuleId(RuleSelectionMethod.Criterion criterion, Map<String, ?> arguments) throws Exception {
		RuleSelectionMethod ruleSelectionMethod = new RuleSelectionMethod(criterion);

		ModelEvaluator<?> evaluator = createModelEvaluator(new RuleSelectionMethodTransformer(ruleSelectionMethod));

		Map<String, ?> results = evaluator.evaluate(arguments);

		SimpleRuleScoreDistribution<?> targetValue = (SimpleRuleScoreDistribution<?>)results.get(evaluator.getTargetName());

		return targetValue.getEntityId();
	}
}