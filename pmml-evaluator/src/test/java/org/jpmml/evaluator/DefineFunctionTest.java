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
import java.util.List;

import org.dmg.pmml.Apply;
import org.dmg.pmml.FieldName;
import org.jpmml.manager.PMMLManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefineFunctionTest extends PMMLManagerTest {

	@Test
	public void evaluateAmPm() throws Exception {
		PMMLManager pmmlManager = createPMMLManager();

		PMMLEvaluationContext context = new PMMLEvaluationContext(pmmlManager);

		assertValueEquals("AM", evaluateAmPm(34742, context));

		context.declareAll(createArguments("StartTime", 34742));

		assertValueEquals("AM", evaluateField(new FieldName("Shift"), context));
	}

	@Test
	public void evaluateStategroup() throws Exception {
		PMMLManager pmmlManager = createPMMLManager();

		PMMLEvaluationContext context = new PMMLEvaluationContext(pmmlManager);

		assertValueEquals("West", evaluateStategroup("CA", context));
		assertValueEquals("West", evaluateStategroup("OR", context));
		assertValueEquals("East", evaluateStategroup("NC", context));

		context.declareAll(createArguments("State", "CA"));

		assertValueEquals("West", evaluateField(new FieldName("Group"), context));
	}

	static
	private void assertValueEquals(Object expected, FieldValue actual){
		assertEquals(expected, FieldValueUtil.getValue(actual));
	}

	static
	private FieldValue evaluateAmPm(Integer time, EvaluationContext context){
		List<FieldValue> values = Collections.singletonList(FieldValueUtil.create(time));

		return evaluateFunction("AMPM", values, context);
	}

	static
	private FieldValue evaluateStategroup(String state, EvaluationContext context){
		List<FieldValue> values = Collections.singletonList(FieldValueUtil.create(state));

		return evaluateFunction("STATEGROUP", values, context);
	}

	static
	private FieldValue evaluateField(FieldName name, EvaluationContext context){
		return ExpressionUtil.evaluate(name, context);
	}

	static
	private FieldValue evaluateFunction(String function, List<FieldValue> values, EvaluationContext context){
		Apply apply = new Apply(function);

		return FunctionUtil.evaluate(apply, values, context);
	}
}