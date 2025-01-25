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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jpmml.evaluator.Deltas;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.ResidualField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContinuousResidualTest extends ModelEvaluatorTest implements Deltas {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		List<InputField> inputFields = evaluator.getInputFields();

		assertEquals(2, inputFields.size());

		List<InputField> activeFields = evaluator.getActiveFields();
		List<InputField> supplementaryFields = evaluator.getSupplementaryFields();

		assertEquals(1, activeFields.size());
		assertEquals(0, supplementaryFields.size());

		List<ResidualField> residualFields = evaluator.getResidualFields();

		assertEquals(1, residualFields.size());

		inputFields = new ArrayList<>(inputFields);

		inputFields.removeAll(activeFields);

		assertEquals(inputFields, residualFields);

		Map<String, ?> arguments = createArguments("input", 0.8d, "target", 3d);

		Map<String, ?> results = evaluator.evaluate(arguments);

		assertEquals(2.6d, (Double)results.get("residual"), DOUBLE_EXACT);

		arguments = createArguments("input", 0.8d, "target", null);

		results = evaluator.evaluate(arguments);

		assertEquals(1.6d, (Double)results.get("residual"), DOUBLE_EXACT);
	}
}