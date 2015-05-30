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

		assertTrue(type.compare(0.5d, 0d) > 0);
		assertTrue(type.compare(0d, 0.5d) < 0);

		assertTrue(type.compare(0.5d, 0.5d) == 0);

		assertTrue(type.compare(1d, 0.5d) > 0);
		assertTrue(type.compare(0.5d, 1d) < 0);

		assertFalse(type.isValid(-0d));

		assertTrue(type.isValid(0d));
		assertTrue(type.isValid(Double.POSITIVE_INFINITY));
	}

	@Test
	public void measureDistance(){
		Classification.Type type = Classification.Type.DISTANCE;

		assertTrue(type.compare(0.5d, 0d) < 0);
		assertTrue(type.compare(0d, 0.5d) > 0);

		assertTrue(type.compare(0.5d, 0.5d) == 0);

		assertTrue(type.compare(1d, 0.5d) < 0);
		assertTrue(type.compare(0.5d, 1d) > 0);

		assertFalse(type.isValid(-0d));

		assertTrue(type.isValid(0d));
		assertTrue(type.isValid(Double.POSITIVE_INFINITY));
	}
}