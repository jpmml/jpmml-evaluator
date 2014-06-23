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
package org.jpmml.evaluator;

import java.util.Collections;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class CategoricalResidualTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		// "For some row in the test data the expected value may be Y"
		Map<FieldName, ?> arguments = createArguments(evaluator.getTargetField(), "Y");

		ModelEvaluationContext context = evaluator.createContext(null);
		context.declareAll(arguments);

		ProbabilityClassificationMap<String> response = new ProbabilityClassificationMap<String>();
		response.put("Y", 0.8d);
		response.put("N", 0.2d);

		Map<FieldName, ?> prediction = Collections.singletonMap(evaluator.getTargetField(), response);

		Map<FieldName, ?> result = OutputUtil.evaluate(prediction, context);

		assertTrue(VerificationUtil.acceptable(0.2d, (Number)result.get(new FieldName("Residual"))));

		// "For some other row the expected value may be N"
		arguments = createArguments(evaluator.getTargetField(), "N");

		context = evaluator.createContext(null);
		context.declareAll(arguments);

		result = OutputUtil.evaluate(prediction, context);

		assertTrue(VerificationUtil.acceptable(-0.8d, (Number)result.get(new FieldName("Residual"))));
	}
}