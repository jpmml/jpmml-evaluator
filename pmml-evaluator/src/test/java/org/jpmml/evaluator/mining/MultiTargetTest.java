/*
 * Copyright (c) 2020 Villu Ruusmann
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
import java.util.EnumSet;
import java.util.Map;

import org.dmg.pmml.ResultFeature;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.HasProbability;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.ProbabilityDistribution;
import org.jpmml.evaluator.tree.NodeScoreDistribution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiTargetTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		assertTrue(evaluator.hasResultFeature(ResultFeature.PREDICTED_VALUE));

		checkResultFields(Arrays.asList("y1", "y2"), Arrays.asList("decision"), evaluator);

		Map<String, ?> arguments = createArguments("x", -1.0d);

		Map<String, ?> results = evaluator.evaluate(arguments);

		assertNotNull(results.get("y1"));
		assertNull(decode(results.get("y2")));

		Object classification = results.get("y1");

		assertFalse(classification instanceof HasProbability);

		assertEquals(0, results.get("decision"));

		arguments = createArguments("x", 1.0d);

		results = evaluator.evaluate(arguments);

		assertNull(decode(results.get("y1")));
		assertNotNull(decode(results.get("y2")));

		classification = results.get("y2");

		assertFalse(classification instanceof HasProbability);

		assertEquals(1, results.get("decision"));
	}

	@Test
	public void evaluateWithProbability() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		evaluator.addResultFeatures(EnumSet.of(ResultFeature.PROBABILITY));

		Map<String, ?> arguments = createArguments("x", -1.0d);

		Map<String, ?> results = evaluator.evaluate(arguments);

		Classification<?, ?> classification = (Classification<?, ?>)results.get("y1");

		assertTrue(classification instanceof NodeScoreDistribution);

		arguments = createArguments("x", 1.0d);

		results = evaluator.evaluate(arguments);

		classification = (Classification<?, ?>)results.get("y2");

		assertTrue(classification instanceof ProbabilityDistribution);
	}
}