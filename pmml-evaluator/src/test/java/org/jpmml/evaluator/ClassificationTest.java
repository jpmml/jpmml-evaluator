/*
 * Copyright (c) 2014 Villu Ruusmann
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

public class ClassificationTest {

	@Test
	public void measureSimilarity(){
		Classification.Type type = Classification.Type.SIMILARITY;

		DoubleValue doubleZero = new DoubleValue(Numbers.DOUBLE_ZERO);
		DoubleValue doubleOneHalf = new DoubleValue(Numbers.DOUBLE_ONE_HALF);
		DoubleValue doubleOne = new DoubleValue(Numbers.DOUBLE_ONE);

		assertTrue(type.compareValues(doubleOneHalf, doubleZero) > 0);
		assertTrue(type.compareValues(doubleZero, doubleOneHalf) < 0);

		assertTrue(type.compareValues(doubleOneHalf, doubleOneHalf) == 0);

		assertTrue(type.compareValues(doubleOne, doubleOneHalf) > 0);
		assertTrue(type.compareValues(doubleOneHalf, doubleOne) < 0);

		assertFalse(type.isValidValue(new DoubleValue(-0d)));

		assertTrue(type.isValidValue(new DoubleValue(0d)));
		assertTrue(type.isValidValue(new DoubleValue(Double.POSITIVE_INFINITY)));
	}

	@Test
	public void measureDistance(){
		Classification.Type type = Classification.Type.DISTANCE;

		FloatValue floatZero = new FloatValue(Numbers.FLOAT_ZERO);
		FloatValue floatOneHalf = new FloatValue(0.5f);
		FloatValue floatOne = new FloatValue(Numbers.FLOAT_ONE);

		assertTrue(type.compareValues(floatOneHalf, floatZero) < 0);
		assertTrue(type.compareValues(floatZero, floatOneHalf) > 0);

		assertTrue(type.compareValues(floatOneHalf, floatOneHalf) == 0);

		assertTrue(type.compareValues(floatOne, floatOneHalf) < 0);
		assertTrue(type.compareValues(floatOneHalf, floatOne) > 0);

		assertFalse(type.isValidValue(new FloatValue(-0f)));

		assertTrue(type.isValidValue(new FloatValue(0f)));
		assertTrue(type.isValidValue(new FloatValue(Float.POSITIVE_INFINITY)));
	}
}