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
package org.jpmml.evaluator.tree;

import java.util.Map;

import org.jpmml.evaluator.Deltas;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultChildTest extends ModelEvaluatorTest implements Deltas {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		Map<String, ?> arguments = createArguments("Integer", null, "Double", 76.45d);

		Map<String, ?> results = evaluator.evaluate(arguments);

		NodeScoreDistribution<?> targetValue = (NodeScoreDistribution<?>)results.get(evaluator.getTargetName());

		assertEquals("Result1", targetValue.getResult());
		assertEquals("10", targetValue.getEntityId());

		assertEquals(21d / 42d, targetValue.getProbability("Result1"), DOUBLE_EXACT);
		assertEquals(15d / 42d, targetValue.getProbability("Result2"), DOUBLE_EXACT);
		assertEquals(6d / 42d, targetValue.getProbability("Result3"), DOUBLE_EXACT);
	}
}