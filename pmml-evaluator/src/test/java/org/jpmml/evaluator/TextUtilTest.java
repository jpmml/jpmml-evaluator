/*
 * Copyright (c) 2017 Villu Ruusmann
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TextUtilTest {

	@Test
	public void frequency(){
		List<String> text = Arrays.asList("x", "x", "x", "x");

		assertEquals(0, TextUtil.frequency(text, Arrays.asList("x", "x", "x", "x", "x"), 0, false, Integer.MAX_VALUE));
		assertEquals(1, TextUtil.frequency(text, Arrays.asList("x", "x", "x", "x"), 0, false, Integer.MAX_VALUE));
		assertEquals(2, TextUtil.frequency(text, Arrays.asList("x", "x", "x"), 0, false, Integer.MAX_VALUE));
		assertEquals(3, TextUtil.frequency(text, Arrays.asList("x", "x"), 0, false, Integer.MAX_VALUE));
		assertEquals(4, TextUtil.frequency(text, Arrays.asList("x"), 0, false, Integer.MAX_VALUE));

		assertEquals(0, TextUtil.frequency(text, Arrays.asList("x", "X", "x", "x"), 0, false, Integer.MAX_VALUE));
		assertEquals(1, TextUtil.frequency(text, Arrays.asList("x", "X", "x", "x"), 1, false, 1));
		assertEquals(1, TextUtil.frequency(text, Arrays.asList("x", "X", "x", "x"), 1, false, Integer.MAX_VALUE));

		assertEquals(0, TextUtil.frequency(text, Arrays.asList("x", "X", "x"), 0, false, Integer.MAX_VALUE));
		assertEquals(1, TextUtil.frequency(text, Arrays.asList("x", "X", "x"), 1, false, 1));
		assertEquals(2, TextUtil.frequency(text, Arrays.asList("x", "X", "x"), 1, false, Integer.MAX_VALUE));

		assertEquals(0, TextUtil.frequency(text, Arrays.asList("x", "X"), 0, false, Integer.MAX_VALUE));
		assertEquals(1, TextUtil.frequency(text, Arrays.asList("x", "X"), 1, false, 1));
		assertEquals(3, TextUtil.frequency(text, Arrays.asList("x", "X"), 1, false, Integer.MAX_VALUE));

		assertEquals(0, TextUtil.frequency(text, Arrays.asList("X"), 0, false, Integer.MAX_VALUE));
		assertEquals(1, TextUtil.frequency(text, Arrays.asList("X"), 1, false, 1));
		assertEquals(4, TextUtil.frequency(text, Arrays.asList("X"), 1, false, Integer.MAX_VALUE));

		assertEquals(0, TextUtil.frequency(text, Arrays.asList("x", "X", "X", "x"), 0, false, Integer.MAX_VALUE));
		assertEquals(0, TextUtil.frequency(text, Arrays.asList("x", "X", "X", "x"), 1, false, Integer.MAX_VALUE));
		assertEquals(1, TextUtil.frequency(text, Arrays.asList("x", "X", "X", "x"), 2, false, Integer.MAX_VALUE));

		assertEquals(0, TextUtil.frequency(text, Arrays.asList("x", "X", "X"), 0, false, Integer.MAX_VALUE));
		assertEquals(0, TextUtil.frequency(text, Arrays.asList("x", "X", "X"), 1, false, Integer.MAX_VALUE));
		assertEquals(2, TextUtil.frequency(text, Arrays.asList("x", "X", "X"), 2, false, Integer.MAX_VALUE));

		assertEquals(0, TextUtil.frequency(text, Arrays.asList("X", "X"), 0, false, Integer.MAX_VALUE));
		assertEquals(0, TextUtil.frequency(text, Arrays.asList("X", "X"), 1, false, Integer.MAX_VALUE));
		assertEquals(3, TextUtil.frequency(text, Arrays.asList("X", "X"), 2, false, Integer.MAX_VALUE));

		assertEquals(0, TextUtil.frequency(text, Arrays.asList("x", "X", "X", "X"), 0, false, Integer.MAX_VALUE));
		assertEquals(0, TextUtil.frequency(text, Arrays.asList("x", "X", "X", "X"), 1, false, Integer.MAX_VALUE));
		assertEquals(0, TextUtil.frequency(text, Arrays.asList("x", "X", "X", "X"), 2, false, Integer.MAX_VALUE));
		assertEquals(1, TextUtil.frequency(text, Arrays.asList("x", "X", "X", "X"), 3, false, Integer.MAX_VALUE));

		assertEquals(0, TextUtil.frequency(text, Arrays.asList("X", "X", "X"), 0, false, Integer.MAX_VALUE));
		assertEquals(0, TextUtil.frequency(text, Arrays.asList("X", "X", "X"), 1, false, Integer.MAX_VALUE));
		assertEquals(0, TextUtil.frequency(text, Arrays.asList("X", "X", "X"), 2, false, Integer.MAX_VALUE));
		assertEquals(2, TextUtil.frequency(text, Arrays.asList("X", "X", "X"), 3, false, Integer.MAX_VALUE));
	}
}