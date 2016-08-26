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
package org.jpmml.evaluator.regression;

import java.util.Map;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.ProbabilityDistribution;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmptyTargetCategoryTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		Map<FieldName, ?> arguments = createArguments("x1", 3d, "x2", 3d);

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		ProbabilityDistribution targetValue = (ProbabilityDistribution)result.get(evaluator.getTargetFieldName());

		assertEquals("yes", targetValue.getResult());

		double value = (3d * -28.6617384d + 3d * -20.42027426d + 125.56601826d);

		assertEquals(Math.exp(0d) / (Math.exp(0d) + Math.exp(value)), targetValue.getProbability("yes"), 1.e-8);
		assertEquals(Math.exp(value) / (Math.exp(0d) + Math.exp(value)), targetValue.getProbability("no"), 1.e-8);
	}
}