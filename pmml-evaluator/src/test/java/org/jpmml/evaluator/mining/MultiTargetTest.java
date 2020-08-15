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

import org.dmg.pmml.FieldName;
import org.dmg.pmml.ResultFeature;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.HasProbability;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.ProbabilityDistribution;
import org.jpmml.evaluator.tree.NodeScoreDistribution;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MultiTargetTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		assertTrue(evaluator.hasResultFeature(ResultFeature.PREDICTED_VALUE));

		checkResultFields(Arrays.asList("y1", "y2"), Arrays.asList("decision"), evaluator);

		Map<FieldName, ?> arguments = createArguments("x", -1.0d);

		Map<FieldName, ?> results = evaluator.evaluate(arguments);

		assertNotNull(getTarget(results, "y1"));
		assertNull(getTarget(results, "y2"));

		Object classification = results.get(FieldName.create("y1"));

		assertFalse(classification instanceof HasProbability);

		assertEquals(0, getOutput(results, "decision"));

		arguments = createArguments("x", 1.0d);

		results = evaluator.evaluate(arguments);

		assertNull(getTarget(results, "y1"));
		assertNotNull(getTarget(results, "y2"));

		classification = results.get(FieldName.create("y2"));

		assertFalse(classification instanceof HasProbability);

		assertEquals(1, getOutput(results, "decision"));
	}

	@Test
	public void evaluateWithProbability() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		evaluator.addResultFeatures(EnumSet.of(ResultFeature.PROBABILITY));

		Map<FieldName, ?> arguments = createArguments("x", -1.0d);

		Map<FieldName, ?> results = evaluator.evaluate(arguments);

		Classification<?, ?> classification = (Classification<?, ?>)results.get(FieldName.create("y1"));

		assertTrue(classification instanceof NodeScoreDistribution);

		arguments = createArguments("x", 1.0d);

		results = evaluator.evaluate(arguments);

		classification = (Classification<?, ?>)results.get(FieldName.create("y2"));

		assertTrue(classification instanceof ProbabilityDistribution);
	}
}