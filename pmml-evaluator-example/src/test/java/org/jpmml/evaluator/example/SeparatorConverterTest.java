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

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SeparatorConverterTest {

	@Test
	public void unescape(){
		IStringConverter<Character> converter = new SeparatorConverter(null);

		assertThrows(ParameterException.class, () -> converter.convert(""));

		assertEquals(' ', converter.convert(" "));
		assertEquals(',', converter.convert(","));
		assertEquals(';', converter.convert(";"));

		assertEquals('\t', converter.convert("\t"));
		assertEquals('\t', converter.convert("\\t"));

		assertThrows(ParameterException.class, () -> converter.convert("\\"));

		assertEquals('\\', converter.convert("\\\\"));

		assertThrows(ParameterException.class, () -> converter.convert("\\x"));
	}
}