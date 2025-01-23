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
package org.jpmml.evaluator.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SeparatorConverterTest {

	@Test
	public void unescape(){
		assertThrows(IllegalArgumentException.class, () -> SeparatorConverter.unescape(""));

		assertEquals(" ", SeparatorConverter.unescape(" "));
		assertEquals(",", SeparatorConverter.unescape(","));
		assertEquals(";", SeparatorConverter.unescape(";"));

		assertEquals("\t", SeparatorConverter.unescape("\t"));
		assertEquals("\t", SeparatorConverter.unescape("\\t"));

		assertThrows(IllegalArgumentException.class, () -> SeparatorConverter.unescape("\\"));

		assertEquals("\\", SeparatorConverter.unescape("\\\\"));

		assertThrows(IllegalArgumentException.class, () -> SeparatorConverter.unescape("\\x"));

		assertEquals("\\x", SeparatorConverter.unescape("\\\\x"));
	}
}