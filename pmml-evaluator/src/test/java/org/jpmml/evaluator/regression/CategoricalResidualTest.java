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

import java.util.Collections;
import java.util.Map;

import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.ProbabilityDistribution;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CategoricalResidualTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		FieldName targetFieldName = evaluator.getTargetFieldName();

		// "For some row in the test data the expected value may be Y"
		Map<FieldName, ?> arguments = createArguments(targetFieldName, "Y");

		ModelEvaluationContext context = new ModelEvaluationContext(evaluator);
		context.declareAll(arguments);

		ProbabilityDistribution response = new ProbabilityDistribution();
		response.put("Y", 0.8d);
		response.put("N", 0.2d);

		response.computeResult(DataType.STRING);

		Map<FieldName, ?> prediction = Collections.singletonMap(targetFieldName, response);

		Map<FieldName, ?> result = OutputUtil.evaluate(prediction, context);

		FieldName field = FieldName.create("Residual");

		assertEquals(0.2d, (Double)getOutput(result, field), 1.e-8);

		assertEquals(DataType.DOUBLE, OutputUtil.getDataType(evaluator.getOutputField(field), evaluator));

		// "For some other row the expected value may be N"
		arguments = createArguments(targetFieldName, "N");

		context = new ModelEvaluationContext(evaluator);
		context.declareAll(arguments);

		result = OutputUtil.evaluate(prediction, context);

		assertEquals(-0.8d, (Double)getOutput(result, field), 1.e-8);
	}
}