/*
 * Copyright (c) 2016 Villu Ruusmann
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

import java.util.List;
import java.util.Map;

import org.jpmml.evaluator.Deltas;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.TargetField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ModelNestingTest extends ModelEvaluatorTest implements Deltas {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		List<InputField> inputFields = evaluator.getInputFields();
		List<TargetField> targetFields = evaluator.getTargetFields();
		List<OutputField> outputFields = evaluator.getOutputFields();

		assertEquals(1, inputFields.size());
		assertEquals(0, targetFields.size());
		assertEquals(2, outputFields.size());

		assertThrows(EvaluationException.class, () -> evaluator.getTargetField());

		assertEquals(Evaluator.DEFAULT_TARGET_NAME, evaluator.getTargetName());

		Map<String, ?> arguments = createArguments("input", 2d);

		Map<String, ?> results = evaluator.evaluate(arguments);

		assertEquals(3, results.size());

		assertEquals(2d * 2d, (Double)results.get(Evaluator.DEFAULT_TARGET_NAME), DOUBLE_EXACT);

		assertNotNull((Double)results.get("output"));
		assertNull(results.get("report(output)"));
	}
}