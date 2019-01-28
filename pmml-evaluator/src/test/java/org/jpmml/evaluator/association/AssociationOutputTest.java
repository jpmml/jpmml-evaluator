/*
 * Copyright (c) 2014 Villu Ruusmann
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
package org.jpmml.evaluator.association;

import java.util.Arrays;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AssociationOutputTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		Map<FieldName, ?> arguments = createArguments("item", Arrays.asList("Cracker", "Coke"));

		Map<FieldName, ?> results = evaluator.evaluate(arguments);

		assertEquals("1", getOutput(results, "entityId"));

		checkValue(Arrays.asList("Cracker"), results, "antecedent");
		checkValue(Arrays.asList("Water"), results, "consequent");

		checkValue("{Cracker}->{Water}", results, "rule");
		checkValue("1", results, "ruleId");

		checkValue(1d, results, "support");
		checkValue(1d, results, "confidence");
		checkValue(1d, results, "lift");
	}

	static
	private void checkValue(Object expected, Map<FieldName, ?> result, String name){
		assertEquals(expected, getOutput(result, name));
		assertEquals(expected, getOutput(result, "deprecated_" + name));
	}
}