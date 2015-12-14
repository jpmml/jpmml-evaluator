/*
 * Copyright (c) 2012 University of Tartu
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

import java.util.Arrays;
import java.util.List;

import org.dmg.pmml.Array;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ArrayUtilTest {

	@Test
	public void parseIntArray(){
		assertEquals(Arrays.asList("1", "2", "3"), parseIntArray("1 2 3"));
	}

	@Test
	public void parseStringArray(){
		assertEquals(Arrays.asList("a"), parseStringArray("\"a\""));

		assertEquals(Arrays.asList("a", "b", "c"), parseStringArray("a b c"));
		assertEquals(Arrays.asList("a", "b", "c"), parseStringArray("\"a\" \"b\" \"c\""));

		assertEquals(Arrays.asList("a b c"), parseStringArray("\"a b c\""));

		assertEquals(Arrays.asList("\"a b c"), parseStringArray("\"a b c"));
		assertEquals(Arrays.asList("\\a", "\\b\\", "c\\"), parseStringArray("\\a \\b\\ c\\"));

		assertEquals(Arrays.asList("a \"b\" c"), parseStringArray("\"a \\\"b\\\" c\""));
		assertEquals(Arrays.asList("\"a b c\""), parseStringArray("\"\\\"a b c\\\"\""));
	}

	@Test
	public void intern(){
		List<String> left = parseStringArray("a b c");
		List<String> right = parseStringArray("\"a\" \"b\" \"c\"");

		for(int i = 0; i < 3; i++){
			assertSame(left.get(i), right.get(i));
		}
	}

	static
	private List<String> parseIntArray(String content){
		return ArrayUtil.parse(new Array(Array.Type.INT, content));
	}

	static
	private List<String> parseStringArray(String content){
		return ArrayUtil.parse(new Array(Array.Type.STRING, content));
	}
}