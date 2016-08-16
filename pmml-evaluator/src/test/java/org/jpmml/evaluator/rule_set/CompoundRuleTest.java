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

import org.dmg.pmml.FieldName;
import org.dmg.pmml.rule_set.RuleSelectionMethod;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CompoundRuleTest extends RuleSelectionMethodTest {

	@Test
	public void evaluate() throws Exception {
		Map<FieldName, ?> arguments = createArguments("BP", "HIGH", "K", 0.0621d, "Age", 36, "Na", 0.5023);

		assertEquals("RULE1", getRuleId(RuleSelectionMethod.Criterion.FIRST_HIT, arguments));
		assertEquals("RULE2", getRuleId(RuleSelectionMethod.Criterion.WEIGHTED_SUM, arguments));
		assertEquals("RULE1", getRuleId(RuleSelectionMethod.Criterion.WEIGHTED_MAX, arguments));
	}

	@Test
	public void evaluateDefault() throws Exception {
		Map<FieldName, ?> arguments = createArguments("BP", "LOW");

		assertEquals("drugY", getScore(arguments));
	}
}