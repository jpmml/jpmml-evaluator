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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FloatEquivalenceTest {

	@Test
	public void doEquivalent(){
		FloatEquivalence equivalence = new FloatEquivalence(0);

		assertFalse(equivalence.doEquivalent(Float.toString(FLOAT_PI), Double.toString(DOUBLE_PI)));
		assertFalse(equivalence.doEquivalent(Double.toString(FLOAT_PI), Double.toString(DOUBLE_PI)));

		assertTrue(equivalence.doEquivalent(FLOAT_PI, FLOAT_PI));
		assertTrue(equivalence.doEquivalent(Float.toString(FLOAT_PI), FLOAT_PI));
		assertTrue(equivalence.doEquivalent(Double.toString(FLOAT_PI), FLOAT_PI));
		assertTrue(equivalence.doEquivalent(Double.toString(DOUBLE_PI), FLOAT_PI));

		assertTrue(equivalence.doEquivalent(FLOAT_PI, DOUBLE_PI));
		assertTrue(equivalence.doEquivalent(Float.toString(FLOAT_PI), DOUBLE_PI));
		assertTrue(equivalence.doEquivalent(Double.toString(FLOAT_PI), DOUBLE_PI));
		assertTrue(equivalence.doEquivalent(Double.toString(DOUBLE_PI), DOUBLE_PI));
	}

	private static final float FLOAT_PI = (float)Math.PI;
	private static final double DOUBLE_PI = Math.PI;
}