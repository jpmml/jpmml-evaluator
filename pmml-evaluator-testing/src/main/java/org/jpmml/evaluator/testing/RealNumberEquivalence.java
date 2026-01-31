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

import java.util.Objects;

import com.google.common.base.Equivalence;
import org.jpmml.evaluator.Computable;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.TypeUtil;

/**
 * <p>
 * A strategy for determining the equivalence of floating-point numbers based on the Unit in the Last Place (ULP) distance.
 * </p>
 *
 * Two values are considered equivalent when the ULP distance is within the specified tolerance.
 * If the two values have different signs, then the ULP distance is calculated by summing their ULP distances to the zero value.
 */
public class RealNumberEquivalence extends Equivalence<Object> {

	private int tolerance = 0;


	public RealNumberEquivalence(int tolerance){
		setTolerance(tolerance);
	}

	@Override
	public boolean doEquivalent(Object expected, Object actual){
		int tolerance = getTolerance();

		if(actual instanceof Computable){
			actual = EvaluatorUtil.decode(actual);
		}

		expected = TypeUtil.parseOrCast(TypeUtil.getDataType(actual), expected);

		if(expected instanceof Float && actual instanceof Float){
			float expectedValue = (Float)expected;
			float actualValue = (Float)actual;

			// Float#floatToIntBits normalizes zero and NaN values
			int expectedBits = Float.floatToIntBits(expectedValue);
			int actualBits = Float.floatToIntBits(actualValue);

			int ulpDifference;

			// On different sides of zero
			if((expectedBits ^ actualBits) < 0){
				ulpDifference = distanceFromZero(expectedBits) + distanceFromZero(actualBits);
			} else

			// On the same side
			{
				ulpDifference = Math.abs(expectedBits - actualBits);
			}

			return ulpDifference <= tolerance;
		} else

		if(expected instanceof Double && actual instanceof Double){
			double expectedValue = (Double)expected;
			double actualValue = (Double)actual;

			// Double#doubleToLongBits normalizes zero and NaN values
			long expectedBits = Double.doubleToLongBits(expectedValue);
			long actualBits = Double.doubleToLongBits(actualValue);

			long ulpDifference;

			// On different sides of zero
			if((expectedBits ^ actualBits) < 0){
				ulpDifference = distanceFromZero(expectedBits) + distanceFromZero(actualBits);
			} else

			// On the same side
			{
				ulpDifference = Math.abs(expectedBits - actualBits);
			}

			return ulpDifference <= tolerance;
		}

		return Objects.equals(expected, actual);
	}

	@Override
	public int doHash(Object object){
		throw new UnsupportedOperationException();
	}

	public int getTolerance(){
		return this.tolerance;
	}

	private void setTolerance(int tolerance){

		if(tolerance < 0){
			throw new IllegalArgumentException();
		}

		this.tolerance = tolerance;
	}

	static
	private int distanceFromZero(int floatBits){

		if(floatBits < 0){
			return (floatBits - Integer.MIN_VALUE);
		} else

		{
			return floatBits;
		}
	}

	static
	private long distanceFromZero(long doubleBits){

		if(doubleBits < 0){
			return (doubleBits - Long.MIN_VALUE);
		} else

		{
			return doubleBits;
		}
	}
}