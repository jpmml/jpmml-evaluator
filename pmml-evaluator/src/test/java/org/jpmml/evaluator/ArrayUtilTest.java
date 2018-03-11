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
import java.util.List;

import org.dmg.pmml.Array;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ArrayUtilTest {

	@Test
	public void parseIntArray(){
		List<?> first = parseIntArray("1 2 3");
		List<?> second = parseIntArray("1 2 3");

		checkSame(Arrays.asList(1, 2, 3), first, second);
	}

	@Test
	public void parseRealArray(){
		List<?> first = parseRealArray("1 2 3");
		List<?> second = parseRealArray("1.0 2.0 3.0");

		checkSame(Arrays.asList(1d, 2d, 3d), first, second);
	}

	@Test
	public void parseStringArray(){
		List<?> first = parseStringArray("a b c");
		List<?> second = parseStringArray("\"a\" \"b\" \"c\"");

		checkSame(Arrays.asList("a", "b", "c"), first, second);
	}

	static
	private void checkSame(List<?> expected, List<?> actualFirst, List<?> actualSecond){
		// Assert element equality
		assertEquals(expected, actualFirst);
		assertEquals(expected, actualSecond);

		// Assert element identity (after interning) between actual List instances
		for(int i = 0; i < expected.size(); i++){
			assertSame(actualFirst.get(i), actualSecond.get(i));
		}
	}

	static
	private List<?> parseIntArray(String value){
		return ArrayUtil.parse(new Array(Array.Type.INT, value));
	}

	static
	private List<?> parseRealArray(String value){
		return ArrayUtil.parse(new Array(Array.Type.REAL, value));
	}

	static
	private List<?> parseStringArray(String value){
		return ArrayUtil.parse(new Array(Array.Type.STRING, value));
	}
}