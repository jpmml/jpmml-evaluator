/*
 * Copyright (c) 2025 Villu Ruusmann
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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class SimpleInternerTest {

	@Test
	public void internIntegers(){
		SimpleInterner<Integer> interner = SimpleInterner.newInstance();

		Integer left = new Integer(0);
		Integer right = new Integer(0);

		assertNotSame(left, right);
		assertEquals(left, right);

		Integer internedLeft = interner.intern(left);

		assertSame(left, internedLeft);

		Integer internedRight = interner.intern(right);

		assertNotSame(right, internedRight);

		assertSame(internedLeft, internedRight);
		assertEquals(internedLeft, internedRight);

		interner.clear();

		internedRight = interner.intern(right);

		assertSame(right, internedRight);

		internedLeft = interner.intern(left);

		assertNotSame(left, internedLeft);

		assertSame(internedLeft, internedRight);
		assertEquals(internedLeft, internedRight);
	}

	@Test
	public void internDoubles(){
		SimpleInterner<Double> interner = SimpleInterner.newInstance();

		Double left = new Double(1d);
		Double right = new Double(1d);

		assertNotSame(left, right);
		assertEquals(left, right);

		Double internedLeft = interner.intern(left);
		Double internedRight = interner.intern(right);

		assertSame(internedLeft, internedRight);
		assertEquals(internedLeft, internedRight);
	}
}