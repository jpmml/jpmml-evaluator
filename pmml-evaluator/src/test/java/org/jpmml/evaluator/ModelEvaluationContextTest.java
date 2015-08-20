/*
 * Copyright (c) 2015 Villu Ruusmann
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

import java.util.Collections;

import com.google.common.collect.Lists;
import org.dmg.pmml.FieldName;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ModelEvaluationContextTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator(FieldScopeTest.class);

		FieldName input = new FieldName("input");

		ModelEvaluationContext parentContext = new ModelEvaluationContext(null, evaluator);
		parentContext.declare(input, 1d);

		assertEquals(Collections.emptyList(), Lists.newArrayList(parentContext.getCompatibleParents()));

		ModelEvaluationContext childContext = new ModelEvaluationContext(parentContext, evaluator);
		childContext.declare(input, 0d);

		childContext.computeDifference();

		assertEquals(Collections.emptyList(), Lists.newArrayList(childContext.getCompatibleParents()));

		ModelEvaluationContext grandChildContext = new ModelEvaluationContext(childContext, evaluator);
		grandChildContext.declare(input, 0d);

		grandChildContext.computeDifference();

		assertEquals(Collections.singletonList(childContext), Lists.newArrayList(grandChildContext.getCompatibleParents()));

		FieldName squaredInput = new FieldName("squaredInput");

		FieldValue squaredInputValue = childContext.evaluate(squaredInput);

		assertNull(parentContext.getFieldEntry(squaredInput));
		assertNotNull(childContext.getFieldEntry(squaredInput));
		assertNull(grandChildContext.getFieldEntry(squaredInput));

		assertNotSame(squaredInputValue, parentContext.evaluate(squaredInput));
		assertSame(squaredInputValue, childContext.evaluate(squaredInput));
		assertSame(squaredInputValue, grandChildContext.evaluate(squaredInput));

		FieldName cubedInput = new FieldName("cubedInput");

		FieldValue cubedInputValue = grandChildContext.evaluate(cubedInput);

		assertNull(parentContext.getFieldEntry(cubedInput));
		assertNotNull(childContext.getFieldEntry(cubedInput));
		assertNotNull(grandChildContext.getFieldEntry(cubedInput));

		assertNotSame(cubedInputValue, parentContext.evaluate(cubedInput));
		assertSame(cubedInputValue, childContext.evaluate(cubedInput));
		assertSame(cubedInputValue, grandChildContext.evaluate(cubedInput));
	}
}