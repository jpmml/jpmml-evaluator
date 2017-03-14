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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SeparatorConverterTest {

	@Test
	public void unescape(){

		try {
			SeparatorConverter.unescape("");

			fail();
		} catch(IllegalArgumentException iae){
			// Ignored
		}

		assertEquals(" ", SeparatorConverter.unescape(" "));
		assertEquals(",", SeparatorConverter.unescape(","));
		assertEquals(";", SeparatorConverter.unescape(";"));

		assertEquals("\t", SeparatorConverter.unescape("\t"));
		assertEquals("\t", SeparatorConverter.unescape("\\t"));

		try {
			SeparatorConverter.unescape("\\");

			fail();
		} catch(IllegalArgumentException iae){
			// Ignored
		}

		assertEquals("\\", SeparatorConverter.unescape("\\\\"));

		try {
			SeparatorConverter.unescape("\\x");

			fail();
		} catch(IllegalArgumentException iae){
			//Ignored
		}

		assertEquals("\\x", SeparatorConverter.unescape("\\\\x"));
	}
}