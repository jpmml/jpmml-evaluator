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
package org.jpmml.evaluator;

import java.util.Arrays;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AssociationOutputTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		Evaluator evaluator = createModelEvaluator();

		Map<FieldName, ?> arguments = createArguments("item", Arrays.asList("Cracker", "Coke"));

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		checkOutput(Arrays.asList("Cracker"), result, "antecedent");
		checkOutput(Arrays.asList("Water"), result, "consequent");
		checkOutput("{Cracker}->{Water}", result, "rule");
		checkOutput("1", result, "ruleId");

		checkOutput(1d, result, "support");
		checkOutput(1d, result, "confidence");
		checkOutput(1f, result, "lift");
	}

	static
	private void checkOutput(Object expected, Map<FieldName, ?> result, String name){
		assertEquals(expected, result.get(new FieldName(name)));
		assertEquals(expected, result.get(new FieldName("deprecated_" + name)));
	}
}