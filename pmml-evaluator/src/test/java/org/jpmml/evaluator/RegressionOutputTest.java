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

import java.util.Map;

import org.dmg.pmml.FieldName;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RegressionOutputTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		Map<FieldName, ?> arguments = createArguments("x", 8d);

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		assertEquals(8d, result.get(new FieldName("TargetResult")));

		assertEquals(8d, result.get(new FieldName("RawResult")));
		assertEquals(8, result.get(new FieldName("RawIntegerResult")));
		assertEquals(35d, result.get(new FieldName("FinalResult")));
		assertEquals(35, result.get(new FieldName("FinalIntegerResult")));
		assertEquals("waive", result.get(new FieldName("BusinessDecision")));
	}
}