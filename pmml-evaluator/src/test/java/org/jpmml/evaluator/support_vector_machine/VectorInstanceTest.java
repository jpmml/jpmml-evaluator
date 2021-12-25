/*
 * Copyright (c) 2013 KNIME.com AG, Zurich, Switzerland
 * Copyright (c) 2014 Villu Ruusmann
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jpmml.evaluator.support_vector_machine;

import java.util.Map;

import org.jpmml.evaluator.Deltas;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VectorInstanceTest extends ModelEvaluatorTest implements Deltas {

	@Test
	public void evaluate() throws Exception {
		assertEquals(0.1004236d, evaluate(0d, 0d), DOUBLE_INEXACT);
		assertEquals(0.8995764d, evaluate(0d, 1d), DOUBLE_INEXACT);
		assertEquals(0.8995764d, evaluate(1d, 0d), DOUBLE_INEXACT);
		assertEquals(0.1004236d, evaluate(1d, 1d), DOUBLE_INEXACT);
	}

	private double evaluate(double x1, double x2) throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		Map<String, ?> arguments = createArguments("x1", x1, "x2", x2);

		Map<String, ?> results = evaluator.evaluate(arguments);

		return (Double)results.get(evaluator.getTargetName());
	}
}