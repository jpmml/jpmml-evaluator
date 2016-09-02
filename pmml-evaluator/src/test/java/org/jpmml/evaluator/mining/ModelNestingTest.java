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

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.TargetField;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ModelNestingTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		List<TargetField> targetFields = evaluator.getTargetFields();

		assertEquals(0, targetFields.size());

		try {
			evaluator.getTargetField();

			fail();
		} catch(EvaluationException ee){
			// Ignored
		}

		assertEquals(Evaluator.DEFAULT_TARGET_NAME, evaluator.getTargetFieldName());

		Map<FieldName, ?> arguments = createArguments("input", 2d);

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		assertEquals(1, result.size());

		assertEquals(2d * 2d, (Double)getTarget(result, Evaluator.DEFAULT_TARGET_NAME), 1e-8d);
	}
}