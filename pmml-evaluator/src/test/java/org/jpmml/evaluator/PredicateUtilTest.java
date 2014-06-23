/*
 * Copyright (c) 2009 University of Tartu
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

import static org.junit.Assert.assertEquals;

public class PredicateUtilTest {

	@Test
	public void binaryAnd(){
		assertEquals(Boolean.TRUE, PredicateUtil.binaryAnd(Boolean.TRUE, Boolean.TRUE));
		assertEquals(Boolean.FALSE, PredicateUtil.binaryAnd(Boolean.TRUE, Boolean.FALSE));
		assertEquals(null, PredicateUtil.binaryAnd(Boolean.TRUE, null));
		assertEquals(Boolean.FALSE, PredicateUtil.binaryAnd(Boolean.FALSE, Boolean.TRUE));
		assertEquals(Boolean.FALSE, PredicateUtil.binaryAnd(Boolean.FALSE, Boolean.FALSE));
		assertEquals(Boolean.FALSE, PredicateUtil.binaryAnd(Boolean.FALSE, null));
		assertEquals(null, PredicateUtil.binaryAnd(null, Boolean.TRUE));
		assertEquals(Boolean.FALSE, PredicateUtil.binaryAnd(null, Boolean.FALSE));
		assertEquals(null, PredicateUtil.binaryAnd(null, null));
	}

	@Test
	public void binaryOr(){
		assertEquals(Boolean.TRUE, PredicateUtil.binaryOr(Boolean.TRUE, Boolean.TRUE));
		assertEquals(Boolean.TRUE, PredicateUtil.binaryOr(Boolean.TRUE, Boolean.FALSE));
		assertEquals(Boolean.TRUE, PredicateUtil.binaryOr(Boolean.TRUE, null));
		assertEquals(Boolean.TRUE, PredicateUtil.binaryOr(Boolean.FALSE, Boolean.TRUE));
		assertEquals(Boolean.FALSE, PredicateUtil.binaryOr(Boolean.FALSE, Boolean.FALSE));
		assertEquals(null, PredicateUtil.binaryOr(Boolean.FALSE, null));
		assertEquals(Boolean.TRUE, PredicateUtil.binaryOr(null, Boolean.TRUE));
		assertEquals(null, PredicateUtil.binaryOr(null, Boolean.FALSE));
		assertEquals(null, PredicateUtil.binaryOr(null, null));
	}

	@Test
	public void binaryXor(){
		assertEquals(Boolean.FALSE, PredicateUtil.binaryXor(Boolean.TRUE, Boolean.TRUE));
		assertEquals(Boolean.TRUE, PredicateUtil.binaryXor(Boolean.TRUE, Boolean.FALSE));
		assertEquals(null, PredicateUtil.binaryXor(Boolean.TRUE, null));
		assertEquals(Boolean.TRUE, PredicateUtil.binaryXor(Boolean.FALSE, Boolean.TRUE));
		assertEquals(Boolean.FALSE, PredicateUtil.binaryXor(Boolean.FALSE, Boolean.FALSE));
		assertEquals(null, PredicateUtil.binaryXor(Boolean.FALSE, null));
		assertEquals(null, PredicateUtil.binaryXor(null, Boolean.TRUE));
		assertEquals(null, PredicateUtil.binaryXor(null, Boolean.FALSE));
		assertEquals(null, PredicateUtil.binaryXor(null, null));
	}
}