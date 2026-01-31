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
package org.jpmml.evaluator.testing;

import com.google.common.base.Equivalence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RealNumberEquivalenceTest {

	@Test
	public void doEquivalenceFloat(){
		checkEquivalence(true, -0f, 0f, 0);
		checkEquivalence(true, Float.NaN, Float.NaN, 0);
		checkEquivalence(true, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, 0);
		checkEquivalence(true, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 0);

		float expectedValue = Float.MIN_VALUE;
		float actualValue = -Float.MIN_VALUE;

		checkEquivalence(false, expectedValue, actualValue, 1);
		checkEquivalence(true, expectedValue, actualValue, 2);

		expectedValue = (float)Math.PI;
		actualValue = Float.intBitsToFloat(Float.floatToIntBits(expectedValue) + 2);

		checkEquivalence(false, expectedValue, actualValue, 0);
		checkEquivalence(false, expectedValue, actualValue, 1);
		checkEquivalence(true, expectedValue, actualValue, 2);
		checkEquivalence(true, expectedValue, actualValue, 3);
	}

	@Test
	public void doEquivalenceDouble(){
		checkEquivalence(true, -0d, 0d, 0);
		checkEquivalence(true, Double.NaN, Double.NaN, 0);
		checkEquivalence(true, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 0);
		checkEquivalence(true, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0);

		double expectedValue = Double.MIN_VALUE;
		double actualValue = -Double.MIN_VALUE;

		checkEquivalence(false, expectedValue, actualValue, 1);
		checkEquivalence(true, expectedValue, actualValue, 2);

		expectedValue = Math.PI;
		actualValue = Double.longBitsToDouble(Double.doubleToLongBits(expectedValue) + 2);

		checkEquivalence(false, expectedValue, actualValue, 0);
		checkEquivalence(false, expectedValue, actualValue, 1);
		checkEquivalence(true, expectedValue, actualValue, 2);
		checkEquivalence(true, expectedValue, actualValue, 3);
	}

	static
	private void checkEquivalence(boolean result, float expectedValue, float actualValue, int tolerance){
		Equivalence<Object> equivalence = new RealNumberEquivalence(tolerance);

		assertEquals(result, equivalence.equivalent(expectedValue, actualValue));
		assertEquals(result, equivalence.equivalent((double)expectedValue, actualValue));
		assertEquals(result, equivalence.equivalent(Float.toString(expectedValue), actualValue));
		assertEquals(result, equivalence.equivalent(Double.toString(expectedValue), actualValue));
	}

	static
	private void checkEquivalence(boolean result, double expectedValue, double actualValue, int tolerance){
		Equivalence<Object> equivalence = new RealNumberEquivalence(tolerance);

		assertEquals(result, equivalence.equivalent(expectedValue, actualValue));
		assertEquals(result, equivalence.equivalent(Double.toString(expectedValue), actualValue));
	}
}