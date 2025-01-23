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
package org.jpmml.evaluator.general_regression;

import java.util.Map;

import org.jpmml.evaluator.Deltas;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContrastMatrixTest extends ModelEvaluatorTest implements Deltas {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		Map<String, ?> arguments = createArguments("gender", "f", "educ", 19d, "jobcat", "3", "salbegin", 45000d);

		Map<String, ?> results = evaluator.evaluate(arguments);

		// Expected values have been calculated by hand
		assertEquals(0.81956470d, (Double)results.get("Probability_Low"), DOUBLE_INEXACT);
		assertEquals(0.18043530d, (Double)results.get("Probability_High"), DOUBLE_INEXACT);
	}
}