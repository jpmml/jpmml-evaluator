/*
 * Copyright (c) 2013 KNIME.com AG, Zurich, Switzerland
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
package org.jpmml.evaluator;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClassificationTest {

	@Test
	public void measureSimilarity(){
		Classification.Type type = Classification.Type.SIMILARITY;

		DoubleValue doubleZero = new DoubleValue(Numbers.DOUBLE_ZERO);
		DoubleValue doubleOneHalf = new DoubleValue(Numbers.DOUBLE_ONE_HALF);
		DoubleValue doubleOne = new DoubleValue(Numbers.DOUBLE_ONE);

		assertTrue(type.compareValues(doubleOneHalf, doubleZero) > 0);
		assertTrue(type.compareValues(doubleZero, doubleOneHalf) < 0);

		assertTrue(type.compareValues(doubleOneHalf, doubleOneHalf) == 0);

		assertTrue(type.compareValues(doubleOne, doubleOneHalf) > 0);
		assertTrue(type.compareValues(doubleOneHalf, doubleOne) < 0);

		assertFalse(type.isValidValue(new DoubleValue(-0d)));

		assertTrue(type.isValidValue(new DoubleValue(0d)));
		assertTrue(type.isValidValue(new DoubleValue(Double.POSITIVE_INFINITY)));
	}

	@Test
	public void measureDistance(){
		Classification.Type type = Classification.Type.DISTANCE;

		FloatValue floatZero = new FloatValue(Numbers.FLOAT_ZERO);
		FloatValue floatOneHalf = new FloatValue(0.5f);
		FloatValue floatOne = new FloatValue(Numbers.FLOAT_ONE);

		assertTrue(type.compareValues(floatOneHalf, floatZero) < 0);
		assertTrue(type.compareValues(floatZero, floatOneHalf) > 0);

		assertTrue(type.compareValues(floatOneHalf, floatOneHalf) == 0);

		assertTrue(type.compareValues(floatOne, floatOneHalf) < 0);
		assertTrue(type.compareValues(floatOneHalf, floatOne) > 0);

		assertFalse(type.isValidValue(new FloatValue(-0f)));

		assertTrue(type.isValidValue(new FloatValue(0f)));
		assertTrue(type.isValidValue(new FloatValue(Float.POSITIVE_INFINITY)));
	}
}