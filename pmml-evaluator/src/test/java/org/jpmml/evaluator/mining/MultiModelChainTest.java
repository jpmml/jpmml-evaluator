/*
 * Copyright (c) 2022 Villu Ruusmann
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
package org.jpmml.evaluator.mining;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MultiModelChainTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		checkResultFields(Arrays.asList("y1", "y2", "y3"), Arrays.asList("probability(0)", "probability(1)"), evaluator);

		Map<String, ?> arguments = Collections.emptyMap();

		Map<String, ?> results = evaluator.evaluate(arguments);

		assertFalse(results.containsKey("y1"));
		assertNull(results.get("y1"));
		assertTrue(results.containsKey("y2"));
		assertNull(results.get("y2"));
		assertNotNull(results.get("y3"));

		arguments = Collections.singletonMap("x", 0.75d);

		results = evaluator.evaluate(arguments);

		assertNotNull(results.get("y1"));
		assertNotNull(results.get("y2"));
		assertNotNull(results.get("y3"));
	}
}