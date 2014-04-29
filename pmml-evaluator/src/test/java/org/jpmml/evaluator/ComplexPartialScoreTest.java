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

import java.util.*;

import org.dmg.pmml.*;

import org.junit.*;

import static org.junit.Assert.*;

public class ComplexPartialScoreTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		double score = (-9) + (-1);

		assertEquals(Double.valueOf(score + 3), evaluateScore(null));
		assertEquals(Double.valueOf(score + ((0.03f * 1000d) + 11)), evaluateScore(1000d));
		assertEquals(Double.valueOf(score + 5), evaluateScore(1500d));
		assertEquals(Double.valueOf(score + ((0.01f * 3000d) - 18)), evaluateScore(3000d));
	}

	private Double evaluateScore(Double income) throws Exception {
		Evaluator evaluator = createModelEvaluator();

		Map<FieldName, ?> arguments = createArguments("department", null, "age", null, "income", income);

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		return (Double)result.get(new FieldName("Final Score"));
	}
}