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

import com.google.common.base.Equivalence;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RealNumberEquivalenceTest {

	@Test
	public void doEquivalenceFloat(){
		float expectedValue = (float)Math.PI;
		float actualValue = expectedValue;

		checkEquivalence(true, expectedValue, actualValue, 0);

		for(int i = 1; i <= 3; i++){
			actualValue = expectedValue - i * Math.ulp(expectedValue);

			checkEquivalence(i <= 2, expectedValue, actualValue, 2);

			actualValue = expectedValue + i * Math.ulp(expectedValue);

			checkEquivalence(i <= 2, expectedValue, actualValue, 2);
		}
	}

	@Test
	public void doEquivalenceDouble(){
		double expectedValue = Math.PI;
		double actualValue = expectedValue;

		checkEquivalence(true, expectedValue, actualValue, 0);

		for(int i = 1; i <= 3; i++){
			actualValue = expectedValue - i * Math.ulp(expectedValue);

			checkEquivalence(i <= 2, expectedValue, actualValue, 2);

			actualValue = expectedValue + i * Math.ulp(expectedValue);

			checkEquivalence(i <= 2, expectedValue, actualValue, 2);
		}
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