/*
 * Copyright (c) 2022 Villu Ruusmann
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TokenizedStringTest {

	@Test
	public void slice(){
		TokenizedString tokens = new TokenizedString("one", "two", "three", "four", "five");

		assertSame(tokens, tokens.slice(0, 5));

		assertEquals(new TokenizedString("one"), tokens.slice(0, 1));
		assertEquals(new TokenizedString("one", "two"), tokens.slice(0, 2));

		assertEquals(new TokenizedString("two"), tokens.slice(1, 2));
		assertEquals(new TokenizedString("two", "three"), tokens.slice(1, 3));

		assertEquals(new TokenizedString("four"), tokens.slice(3, 4));
		assertEquals(new TokenizedString("four", "five"), tokens.slice(3, 5));
	}
}