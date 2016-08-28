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

import java.util.Map;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TransformationDictionaryTest extends ModelEvaluatorTest {

	@Test
	public void evaluateAmPm() throws Exception {
		assertValueEquals("AM", evaluate(FieldName.create("Shift"), createArguments("StartTime", 34742)));
	}

	@Test
	public void evaluateStategroup() throws Exception {
		assertValueEquals("West", evaluate(FieldName.create("Group"), createArguments("State", "CA")));
	}

	@Test
	public void evaluateSimpleTable() throws Exception {
		FieldName name = FieldName.create("SimpleTable");

		assertValueEquals(null, evaluate(name, createArguments("Value", null)));

		assertValueEquals("first", evaluate(name, createArguments("Value", 1)));
		assertValueEquals("second", evaluate(name, createArguments("Value", 2)));

		assertValueEquals(null, evaluate(name, createArguments("Value", 3)));
	}

	@Test
	public void evaluateComplexTable() throws Exception {
		FieldName name = FieldName.create("ComplexTable");

		assertValueEquals(null, evaluate(name, createArguments("Value", null, "Modifier", null)));

		assertValueEquals("firstTrue", evaluate(name, createArguments("Value", 1, "Modifier", true)));
		assertValueEquals("firstFalse", evaluate(name, createArguments("Value", 1, "Modifier", false)));
		assertValueEquals("secondTrue", evaluate(name, createArguments("Value", 2, "Modifier", true)));
		assertValueEquals("secondFalse", evaluate(name, createArguments("Value", 2, "Modifier", false)));

		assertValueEquals(null, evaluate(name, createArguments("Value", 3, "Modifier", null)));
		assertValueEquals(null, evaluate(name, createArguments("Value", 3, "Modifier", true)));
	}

	private FieldValue evaluate(FieldName name, Map<FieldName, ?> arguments) throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		ModelEvaluationContext context = new ModelEvaluationContext(evaluator);
		context.setArguments(arguments);

		return context.evaluate(name);
	}

	static
	private void assertValueEquals(Object expected, FieldValue actual){
		assertEquals(expected, FieldValueUtil.getValue(actual));
	}
}