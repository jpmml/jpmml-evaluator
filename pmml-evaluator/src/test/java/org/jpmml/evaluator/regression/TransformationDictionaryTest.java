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
		assertValueEquals("AM", evaluate("Shift", createArguments("StartTime", 34742)));
	}

	@Test
	public void evaluateGroup() throws Exception {
		assertValueEquals("West", evaluate("Group", createArguments("State", "CA")));
	}

	@Test
	public void evaluatePower() throws Exception {
		Map<String, ?> arguments = createArguments("Value", 2d, "Exponent", 1);

		assertValueEquals(2d, evaluate("Power", arguments));

		arguments = createArguments("Value", 2d, "Exponent", 2);

		assertValueEquals(4d, evaluate("Power", arguments));

		arguments = createArguments("Value", 2d, "Exponent", 5);

		assertValueEquals(32d, evaluate("Power", arguments));

		EvaluationContext.FUNCTION_GUARD_PROVIDER.set(new FunctionNameStack(4));

		try {
			evaluate("Power", arguments);

			fail();
		} catch(EvaluationException ee){
			// Ignored
		} finally {
			EvaluationContext.FUNCTION_GUARD_PROVIDER.set(null);
		}

		assertValueEquals(32d, evaluate("Power", arguments));

		// XXX
		arguments = createArguments("Value", 1d, "Exponent", 1024);

		try {
			evaluate("Power", arguments);

			fail();
		} catch(StackOverflowError soe){
			// Ignored
		}

		EvaluationContext.FUNCTION_GUARD_PROVIDER.set(new FunctionNameStack(16));

		try {
			evaluate("Power", arguments);

			fail();
		} catch(EvaluationException ee){
			// Ignored
		} finally {
			EvaluationContext.FUNCTION_GUARD_PROVIDER.set(null);
		} // End try

		try {
			evaluate("Power", arguments);

			fail();
		} catch(StackOverflowError soe){
			// Ignored
		}
	}

	@Test
	public void evaluateSimpleTable() throws Exception {
		assertValueEquals(null, evaluate("SimpleTable", createArguments("Value", null)));

		assertValueEquals("first", evaluate("SimpleTable", createArguments("Value", 1)));
		assertValueEquals("second", evaluate("SimpleTable", createArguments("Value", 2)));

		assertValueEquals(null, evaluate("SimpleTable", createArguments("Value", 3)));
	}

	@Test
	public void evaluateComplexTable() throws Exception {
		assertValueEquals(null, evaluate("ComplexTable", createArguments("Value", null, "Modifier", null)));

		assertValueEquals("firstTrue", evaluate("ComplexTable", createArguments("Value", 1, "Modifier", true)));
		assertValueEquals("firstFalse", evaluate("ComplexTable", createArguments("Value", 1, "Modifier", false)));
		assertValueEquals("secondTrue", evaluate("ComplexTable", createArguments("Value", 2, "Modifier", true)));
		assertValueEquals("secondFalse", evaluate("ComplexTable", createArguments("Value", 2, "Modifier", false)));

		assertValueEquals(null, evaluate("ComplexTable", createArguments("Value", 3, "Modifier", null)));
		assertValueEquals(null, evaluate("ComplexTable", createArguments("Value", 3, "Modifier", true)));
	}

	@Test
	public void evaluateSelfRef() throws Exception {
		Map<String, ?> arguments = createArguments("Value", 1, "Modifier", false);

		assertValueEquals(1d, evaluate("SelfRef", arguments));

		arguments = createArguments("Value", 1, "Modifier", true);

		try {
			evaluate("SelfRef", arguments);

			fail();
		} catch(StackOverflowError soe){
			// Ignored
		}

		EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(new FieldNameSet());

		try {
			evaluate("SelfRef", arguments);

			fail();
		} catch(EvaluationException ee){
			// Ignored
		} finally {
			EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(null);
		} // End try

		try {
			evaluate("SelfRef", arguments);

			fail();
		} catch(StackOverflowError soe){
			// Ignored
		}
	}

	@Test
	public void evaluateRef() throws Exception {
		Map<String, ?> arguments = createArguments("Value", 1);

		try {
			evaluate("Ref", arguments);

			fail();
		} catch(StackOverflowError soe){
			// Ignored
		}

		EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(new FieldNameSet());

		try {
			evaluate("Ref", arguments);
		} catch(EvaluationException ee){
			// Ignored
		} finally {
			EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(null);
		} // End try

		try {
			evaluate("Ref", arguments);

			fail();
		} catch(StackOverflowError soe){
			// Ignored
		}
	}

	@Test
	public void evaluateChain() throws Exception {
		Map<String, ?> arguments = createArguments("Value", 1);

		EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(new FieldNameSet(2));

		try {
			assertValueEquals(1d, evaluate("StageOne", arguments));

			try {
				evaluate("StageThree", arguments);

				fail();
			} catch(EvaluationException ee){
				// Ignored
			}
		} finally {
			EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(null);
		}

		assertValueEquals(1d, evaluate("StageThree", arguments));
	}

	private FieldValue evaluate(String name, Map<String, ?> arguments) throws Exception {
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