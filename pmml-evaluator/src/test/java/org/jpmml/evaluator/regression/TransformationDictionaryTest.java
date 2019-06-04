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
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.FieldNameSet;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.FunctionNameStack;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TransformationDictionaryTest extends ModelEvaluatorTest {

	@Test
	public void evaluateShift() throws Exception {
		assertValueEquals("AM", evaluate(FieldName.create("Shift"), createArguments("StartTime", 34742)));
	}

	@Test
	public void evaluateGroup() throws Exception {
		assertValueEquals("West", evaluate(FieldName.create("Group"), createArguments("State", "CA")));
	}

	@Test
	public void evaluatePower() throws Exception {
		FieldName name = FieldName.create("Power");

		Map<FieldName, ?> arguments = createArguments("Value", 2d, "Exponent", 1);

		assertValueEquals(2d, evaluate(name, arguments));

		arguments = createArguments("Value", 2d, "Exponent", 2);

		assertValueEquals(4d, evaluate(name, arguments));

		arguments = createArguments("Value", 2d, "Exponent", 5);

		assertValueEquals(32d, evaluate(name, arguments));

		EvaluationContext.FUNCTION_GUARD_PROVIDER.set(new FunctionNameStack(4));

		try {
			evaluate(name, arguments);

			fail();
		} catch(EvaluationException ee){
			// Ignored
		} finally {
			EvaluationContext.FUNCTION_GUARD_PROVIDER.set(null);
		}

		assertValueEquals(32d, evaluate(name, arguments));

		// XXX
		arguments = createArguments("Value", 1d, "Exponent", 1024);

		try {
			evaluate(name, arguments);

			fail();
		} catch(StackOverflowError soe){
			// Ignored
		}

		EvaluationContext.FUNCTION_GUARD_PROVIDER.set(new FunctionNameStack(16));

		try {
			evaluate(name, arguments);

			fail();
		} catch(EvaluationException ee){
			// Ignored
		} finally {
			EvaluationContext.FUNCTION_GUARD_PROVIDER.set(null);
		} // End try

		try {
			evaluate(name, arguments);

			fail();
		} catch(StackOverflowError soe){
			// Ignored
		}
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

	@Test
	public void evaluateSelfRef() throws Exception {
		FieldName name = FieldName.create("SelfRef");

		Map<FieldName, ?> arguments = createArguments("Value", 1, "Modifier", false);

		assertValueEquals(1d, evaluate(name, arguments));

		arguments = createArguments("Value", 1, "Modifier", true);

		try {
			evaluate(name, arguments);

			fail();
		} catch(StackOverflowError soe){
			// Ignored
		}

		EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(new FieldNameSet());

		try {
			evaluate(name, arguments);

			fail();
		} catch(EvaluationException ee){
			// Ignored
		} finally {
			EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(null);
		} // End try

		try {
			evaluate(name, arguments);

			fail();
		} catch(StackOverflowError soe){
			// Ignored
		}
	}

	@Test
	public void evaluateRef() throws Exception {
		FieldName name = FieldName.create("Ref");

		Map<FieldName, ?> arguments = createArguments("Value", 1);

		try {
			evaluate(name, arguments);

			fail();
		} catch(StackOverflowError soe){
			// Ignored
		}

		EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(new FieldNameSet());

		try {
			evaluate(name, arguments);
		} catch(EvaluationException ee){
			// Ignored
		} finally {
			EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(null);
		} // End try

		try {
			evaluate(name, arguments);

			fail();
		} catch(StackOverflowError soe){
			// Ignored
		}
	}

	@Test
	public void evaluateChain() throws Exception {
		Map<FieldName, ?> arguments = createArguments("Value", 1);

		EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(new FieldNameSet(2));

		try {
			assertValueEquals(1d, evaluate(FieldName.create("StageOne"), arguments));

			try {
				evaluate(FieldName.create("StageThree"), arguments);

				fail();
			} catch(EvaluationException ee){
				// Ignored
			}
		} finally {
			EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(null);
		}

		assertValueEquals(1d, evaluate(FieldName.create("StageThree"), arguments));
	}

	private FieldValue evaluate(FieldName name, Map<FieldName, ?> arguments) throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		ModelEvaluationContext context = evaluator.createEvaluationContext();
		context.setArguments(arguments);

		return context.evaluate(name);
	}

	static
	private void assertValueEquals(Object expected, FieldValue actual){
		assertEquals(expected, FieldValueUtil.getValue(actual));
	}
}