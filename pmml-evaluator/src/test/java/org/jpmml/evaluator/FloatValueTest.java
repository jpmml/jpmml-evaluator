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

public class FloatValueTest {

	@Test
	public void exp(){
		assertTrue(Math.pow(2.7182817f, -7f) == Math.pow((float)Math.E, -7f));
		assertTrue(Math.pow(2.7182817f, -7f) == Math.pow((float)2.7182817d, -7f));

		assertFalse(Math.pow(2.7182817f, -7f) == Math.pow(2.7182817d, -7f));

		assertTrue(FloatValue.exp(-7f) > (float)Math.exp(-7f));
		assertTrue(FloatValue.exp(-7f) > (float)Math.pow((float)Math.E, -7f));

		assertTrue(FloatValue.exp(0f) == (float)Math.exp(0f));
		assertTrue(FloatValue.exp(0f) == (float)Math.pow((float)Math.E, 0f));

		assertTrue(FloatValue.exp(7f) < (float)Math.exp(7f));
		assertTrue(FloatValue.exp(7f) < (float)Math.pow((float)Math.E, 7f));
	}
}