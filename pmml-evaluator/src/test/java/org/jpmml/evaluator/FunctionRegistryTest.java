/*
 * Copyright (c) 2015 Villu Ruusmann
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

import org.jpmml.evaluator.functions.EchoFunction;
import org.jpmml.evaluator.functions.MaliciousEchoFunction;
import org.junit.Test;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FunctionRegistryTest {

	@Test
	public void getFunction(){

		try {
			FunctionRegistry.getFunction(Thread.class.getName());

			fail();
		} catch(TypeCheckException tce){
			// Ignored
		}

		try {
			FunctionRegistry.getFunction(MaliciousThread.class.getName());

			fail();
		} catch(TypeCheckException tce){
			// Ignored
		}

		Function firstEcho = FunctionRegistry.getFunction(EchoFunction.class.getName());
		Function secondEcho = FunctionRegistry.getFunction(EchoFunction.class.getName());

		assertNotSame(firstEcho, secondEcho);

		try {
			FunctionRegistry.getFunction(MaliciousEchoFunction.class.getName());

			fail();
		} catch(EvaluationException ee){
			Throwable cause = ee.getCause();

			assertTrue(cause instanceof ExceptionInInitializerError);
		}
	}
}