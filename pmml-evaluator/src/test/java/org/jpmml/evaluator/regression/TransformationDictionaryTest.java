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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TransformationDictionaryTest extends ModelEvaluatorTest {

	@Test
	public void evaluateShift() throws Exception {
		assertEquals("AM", evaluate("Shift", createArguments("StartTime", 34742)));
	}

	@Test
	public void evaluateGroup() throws Exception {
		assertEquals("West", evaluate("Group", createArguments("State", "CA")));
	}

	@Test
	public void evaluatePower() throws Exception {
		Map<String, ?> arguments = createArguments("Value", 2d, "Exponent", 1);

		assertEquals(2d, evaluate("Power", arguments));

		arguments = createArguments("Value", 2d, "Exponent", 2);

		assertEquals(4d, evaluate("Power", arguments));

		arguments = createArguments("Value", 2d, "Exponent", 5);

		assertEquals(32d, evaluate("Power", arguments));

		EvaluationContext.FUNCTION_GUARD_PROVIDER.set(new FunctionNameStack(4));

		try {
			evaluate("Power", arguments);

			fail();
		} catch(EvaluationException ee){
			// Ignored
		} finally {
			EvaluationContext.FUNCTION_GUARD_PROVIDER.set(null);
		}

		assertEquals(32d, evaluate("Power", arguments));

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
		assertEquals(null, evaluate("SimpleTable", createArguments("Value", null)));

		assertEquals("first", evaluate("SimpleTable", createArguments("Value", 1)));
		assertEquals("second", evaluate("SimpleTable", createArguments("Value", 2)));

		assertEquals(null, evaluate("SimpleTable", createArguments("Value", 3)));
	}

	@Test
	public void evaluateComplexTable() throws Exception {
		assertEquals(null, evaluate("ComplexTable", createArguments("Value", null, "Modifier", null)));

		assertEquals("firstTrue", evaluate("ComplexTable", createArguments("Value", 1, "Modifier", true)));
		assertEquals("firstFalse", evaluate("ComplexTable", createArguments("Value", 1, "Modifier", false)));
		assertEquals("secondTrue", evaluate("ComplexTable", createArguments("Value", 2, "Modifier", true)));
		assertEquals("secondFalse", evaluate("ComplexTable", createArguments("Value", 2, "Modifier", false)));

		assertEquals(null, evaluate("ComplexTable", createArguments("Value", 3, "Modifier", null)));
		assertEquals(null, evaluate("ComplexTable", createArguments("Value", 3, "Modifier", true)));
	}

	@Test
	public void evaluateSelfRef() throws Exception {
		Map<String, ?> arguments = createArguments("Value", 1, "Modifier", false);

		assertEquals(1d, evaluate("SelfRef", arguments));

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
			assertEquals(1d, evaluate("StageOne", arguments));

			try {
				evaluate("StageThree", arguments);

				fail();
			} catch(EvaluationException ee){
				// Ignored
			}
		} finally {
			EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(null);
		}

		assertEquals(1d, evaluate("StageThree", arguments));
	}

	private Object evaluate(String name, Map<String, ?> arguments) throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		ModelEvaluationContext context = evaluator.createEvaluationContext();
		context.setArguments(arguments);

		FieldValue value = context.evaluate(name);

		return FieldValueUtil.getValue(value);
	}
}