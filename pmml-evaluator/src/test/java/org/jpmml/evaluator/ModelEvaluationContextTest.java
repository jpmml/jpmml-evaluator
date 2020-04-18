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
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.jpmml.evaluator.mining.MissingPredictionTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ModelEvaluationContextTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		FieldName name = FieldName.create("x");

		ModelEvaluator<?> evaluator = createModelEvaluator(MissingPredictionTest.class);

		ModelEvaluationContext context = evaluator.createEvaluationContext();
		context.setArguments(Collections.singletonMap(name, 1d));

		FieldValue value = FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, 1d);

		try {
			context.lookup(name);

			fail();
		} catch(MissingValueException mve){
			// Ignored
		}

		assertEquals(value, context.evaluate(name));
		assertEquals(Arrays.asList(value), context.evaluateAll(Arrays.asList(name)));

		context.reset(false);

		Map<FieldName, ?> arguments = context.getArguments();

		assertEquals(0, arguments.size());

		assertEquals(value, context.lookup(name));

		assertEquals(value, context.evaluate(name));
		assertEquals(Arrays.asList(value), context.evaluateAll(Arrays.asList(name)));

		context.reset(true);

		try {
			context.lookup(name);

			fail();
		} catch(MissingValueException mve){
			// Ignored
		}

		assertEquals(FieldValues.MISSING_VALUE, context.evaluate(name));
	}

	@Test
	public void evaluateMissing() throws Exception {
		FieldName name = FieldName.create("x");

		ModelEvaluator<?> evaluator = createModelEvaluator(MissingPredictionTest.class);

		ModelEvaluationContext context = evaluator.createEvaluationContext();
		context.setArguments(Collections.emptyMap());

		try {
			context.lookup(name);

			fail();
		} catch(MissingValueException mve){
			// Ignored
		}

		assertEquals(FieldValues.MISSING_VALUE, context.evaluate(name));
		assertEquals(Arrays.asList(FieldValues.MISSING_VALUE), context.evaluateAll(Arrays.asList(name)));
 	}
}