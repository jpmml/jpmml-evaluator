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

import org.dmg.pmml.FieldName;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PMMLEvaluationContextTest extends PMMLManagerTest {

	@Test
	public void evaluateAmPm() throws Exception {
		PMMLManager pmmlManager = createPMMLManager();

		PMMLEvaluationContext context = new PMMLEvaluationContext(pmmlManager);
		context.declareAll(createArguments("StartTime", 34742));

		assertValueEquals("AM", evaluateDerivedField(new FieldName("Shift"), context));
	}

	@Test
	public void evaluateStategroup() throws Exception {
		PMMLManager pmmlManager = createPMMLManager();

		PMMLEvaluationContext context = new PMMLEvaluationContext(pmmlManager);
		context.declareAll(createArguments("State", "CA"));

		assertValueEquals("West", evaluateDerivedField(new FieldName("Group"), context));
	}

	static
	private void assertValueEquals(Object expected, FieldValue actual){
		assertEquals(expected, FieldValueUtil.getValue(actual));
	}

	static
	private FieldValue evaluateDerivedField(FieldName name, EvaluationContext context){
		return ExpressionUtil.evaluate(name, context);
	}
}