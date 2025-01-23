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
package org.jpmml.evaluator.regression;

import java.util.Map;

import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PriorProbabilitiesTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		Map<String, ?> arguments = createArguments("input", null);

		Map<String, ?> results = evaluator.evaluate(arguments);

		assertEquals(5, results.size());

		assertEquals("NO", decode(results.get(Evaluator.DEFAULT_TARGET_NAME)));

		assertEquals("NO", results.get("I_response"));
		assertEquals("No", results.get("U_response"));

		assertEquals(0.02d, results.get("P_responseYes"));
		assertEquals(0.98d, results.get("P_responseNo"));

		evaluator = createModelEvaluator(new RemoveTargetValuesTransformer());

		results = evaluator.evaluate(arguments);

		assertEquals(1, results.size());

		assertEquals(null, decode(results.get(Evaluator.DEFAULT_TARGET_NAME)));
	}
}