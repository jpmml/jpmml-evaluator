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
package org.jpmml.evaluator.scorecard;

import java.util.Map;

import org.jpmml.evaluator.Deltas;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ComplexPartialScoreTest extends ModelEvaluatorTest implements Deltas {

	@Test
	public void evaluate() throws Exception {
		double score = (-9d) + (-1d);

		assertEquals((score + 3d), evaluateScore(null), DOUBLE_EXACT);
		assertEquals((score + ((0.03d * 1000d) + 11d)), evaluateScore(1000d), DOUBLE_EXACT);
		assertEquals((score + 5d), evaluateScore(1500d), DOUBLE_INEXACT);
		assertEquals((score + ((0.01d * 3000d) - 18d)), evaluateScore(3000d), DOUBLE_EXACT);
	}

	private Double evaluateScore(Double income) throws Exception {
		Evaluator evaluator = createModelEvaluator();

		Map<String, ?> arguments = createArguments("department", null, "age", null, "income", income);

		Map<String, ?> results = evaluator.evaluate(arguments);

		return (Double)results.get("Final Score");
	}
}