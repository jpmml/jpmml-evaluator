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

import java.util.Objects;

import com.google.common.base.Equivalence;

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

			if(expectedValue == actualValue){
				return true;
			}

			float leftMargin = expectedValue - (tolerance * Math.ulp(expectedValue));
			float rightMargin = expectedValue + (tolerance * Math.ulp(expectedValue));

			if(actualValue >= leftMargin && actualValue <= rightMargin){
				return true;
			}

			return false;
		} else

		if(expected instanceof Double && actual instanceof Double){
			double expectedValue = (Double)expected;
			double actualValue = (Double)actual;

			if(expectedValue == actualValue){
				return true;
			}

			double leftMargin = expectedValue - (tolerance * Math.ulp(expectedValue));
			double rightMargin = expectedValue + (tolerance * Math.ulp(expectedValue));

			if(actualValue >= leftMargin && actualValue <= rightMargin){
				return true;
			}

			return false;
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
}