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
package org.jpmml.evaluator.support_vector_machine;

import java.util.Map;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AlternateBinaryTargetCategoryTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		assertEquals("no", evaluate(0d, 0d));
		assertEquals("yes", evaluate(0d, 1d));
		assertEquals("yes", evaluate(1d, 0d));
		assertEquals("no", evaluate(1d, 1d));
	}

	private String evaluate(double x1, double x2) throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		Map<FieldName, ?> arguments = createArguments("x1", x1, "x2", x2);

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		Classification targetValue = (Classification)result.get(evaluator.getTargetFieldName());

		return (String)targetValue.getResult();
	}
}