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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FunctionRegistryTest {

	@Test
	public void getFunction(){
		assertNotNull(FunctionRegistry.getFunction("if"));

		assertNull(FunctionRegistry.getFunction("x-sin"));
		assertNotNull(FunctionRegistry.getFunction("sin"));

		assertThrows(TypeCheckException.class, () -> FunctionRegistry.getFunction(Thread.class.getName()));
		assertThrows(TypeCheckException.class, () -> FunctionRegistry.getFunction(MaliciousThread.class.getName()));

		Function firstEcho = FunctionRegistry.getFunction(EchoFunction.class.getName());
		Function secondEcho = FunctionRegistry.getFunction(EchoFunction.class.getName());

		assertNotSame(firstEcho, secondEcho);

		EvaluationException exception = assertThrows(EvaluationException.class, () -> FunctionRegistry.getFunction(MaliciousEchoFunction.class.getName()));

		assertTrue(exception.getCause() instanceof ExceptionInInitializerError);
	}
}