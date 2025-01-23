/*
 * Copyright (c) 2018 Villu Ruusmann
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.jpmml.evaluator.mining.MissingPredictionTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ModelEvaluationContextTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator(MissingPredictionTest.class);

		ModelEvaluationContext context = evaluator.createEvaluationContext();
		context.setArguments(Collections.singletonMap("x", 1d));

		FieldValue value = FieldValueUtil.create(OpType.CONTINUOUS, DataType.DOUBLE, 1d);

		assertThrows(MissingFieldValueException.class, () -> context.lookup("x"));

		assertEquals(value, context.evaluate("x"));
		assertEquals(Arrays.asList(value), context.evaluateAll(Arrays.asList("x")));

		context.reset(false);

		Map<String, ?> arguments = context.getArguments();

		assertEquals(0, arguments.size());

		assertEquals(value, context.lookup("x"));

		assertEquals(value, context.evaluate("x"));
		assertEquals(Arrays.asList(value), context.evaluateAll(Arrays.asList("x")));

		context.reset(true);

		assertThrows(MissingFieldValueException.class, () -> context.lookup("x"));

		assertEquals(FieldValues.MISSING_VALUE, context.evaluate("x"));
	}

	@Test
	public void evaluateMissing() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator(MissingPredictionTest.class);

		ModelEvaluationContext context = evaluator.createEvaluationContext();
		context.setArguments(Collections.emptyMap());

		assertThrows(MissingFieldValueException.class, () -> context.lookup("x"));

		assertEquals(FieldValues.MISSING_VALUE, context.evaluate("x"));
		assertEquals(Arrays.asList(FieldValues.MISSING_VALUE), context.evaluateAll(Arrays.asList("x")));
 	}
}