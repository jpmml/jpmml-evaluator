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

import java.util.*;

import org.dmg.pmml.*;

import org.junit.*;

import static org.junit.Assert.*;

public class PriorProbabilitiesTest extends RegressionModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		RegressionModelEvaluator evaluator = createEvaluator();

		ModelEvaluationContext context = new ModelEvaluationContext(evaluator);

		Map<FieldName, ? extends ClassificationMap<?>> predictions = TargetUtil.evaluateClassification((ClassificationMap<?>)null, context);

		assertEquals(1, predictions.size());

		DefaultClassificationMap<?> response = (DefaultClassificationMap<?>)predictions.get(evaluator.getTargetField());

		assertEquals((Double)0.02d, response.getProbability("YES"));
		assertEquals((Double)0.98d, response.getProbability("NO"));

		Map<FieldName, ?> result = OutputUtil.evaluate(predictions, context);

		assertEquals(0.02d, result.get(new FieldName("P_responseYes")));
		assertEquals(0.98d, result.get(new FieldName("P_responseNo")));

		assertEquals("NO", result.get(new FieldName("I_response")));
		assertEquals("No", result.get(new FieldName("U_response")));
	}
}