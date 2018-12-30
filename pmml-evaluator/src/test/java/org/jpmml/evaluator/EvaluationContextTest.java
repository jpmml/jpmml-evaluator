/*
 * Copyright (c) 2018 Villu Ruusmann
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

import java.util.Arrays;

import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EvaluationContextTest {

	@Test
	public void evaluate(){
		FieldName name = FieldName.create("x");

		EvaluationContext context = new VirtualEvaluationContext();

		try {
			context.lookup(name);

			fail();
		} catch(MissingValueException mve){
			// Ignored
		} // End try

		try {
			context.lookup(0);

			fail();
		} catch(IllegalStateException ise){
			// Ignored
		} // End try

		try {
			context.evaluate(name);

			fail();
		} catch(MissingFieldException mfe){
			// Ignored
		}

		FieldValue value = FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, 1d);

		context.declare(name, value);

		assertEquals(value, context.lookup(name));

		try {
			context.lookup(0);

			fail();
		} catch(IllegalStateException ise){
			// Ignored
		}

		assertEquals(value, context.evaluate(name));

		assertEquals(Arrays.asList(value), context.evaluateAll(Arrays.asList(name)));

		assertEquals(value, context.lookup(0));

		try {
			context.lookup(1);

			fail();
		} catch(IndexOutOfBoundsException ioobe){
			// Ignored
		}

		context.reset(false);

		assertEquals(value, context.lookup(name));
		assertEquals(value, context.lookup(0));

		assertEquals(value, context.evaluate(name));

		context.reset(true);

		try {
			context.lookup(name);

			fail();
		} catch(MissingValueException mve){
			// Ignored
		} // End try

		try {
			context.lookup(0);

			fail();
		} catch(IllegalStateException ise){
			// Ignored
		} // End try

		try {
			context.evaluate(name);

			fail();
		} catch(MissingFieldException mfe){
			// Ignored
		}
	}

	@Test
	public void evaluateMissing(){
		FieldName name = FieldName.create("x");

		EvaluationContext context = new VirtualEvaluationContext();
		context.declare(name, FieldValues.MISSING_VALUE);

		assertEquals(FieldValues.MISSING_VALUE, context.lookup(name));

		try {
			context.lookup(0);

			fail();
		} catch(IllegalStateException ise){
			// Ignored
		}

		assertEquals(FieldValues.MISSING_VALUE, context.evaluate(name));
 	}
}