/*
 * Copyright (c) 2013 Villu Ruusmann
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

public class VerificationUtil {

	private VerificationUtil(){
	}

	static
	public boolean acceptable(Object expected, Object actual, double precision, double zeroThreshold){

		if(expected instanceof Number && actual instanceof Number){
			return acceptable((Number)expected, (Number)actual, precision, zeroThreshold);
		}

		return Objects.equals(expected, actual);
	}

	/**
	 * @param precision The acceptable range given <em>in proportion</em> of the expected value, including its boundaries.
	 * @param zeroThreshold The threshold for distinguishing between zero and non-zero values.
	 */
	static
	public boolean acceptable(Number expected, Number actual, double precision, double zeroThreshold){

		if(precision < 0d || zeroThreshold < 0d){
			throw new IllegalArgumentException();
		} // End if

		if(isZero(expected, zeroThreshold) && isZero(actual, zeroThreshold)){
			return true;
		}

		double zeroBoundary = expected.doubleValue() * (1d - precision); // Pointed towards zero
		double infinityBoundary = expected.doubleValue() * (1d + precision); // Pointed towards positive or negative infinity

		// Positive values
		if(expected.doubleValue() >= 0){
			return (actual.doubleValue() >= zeroBoundary) && (actual.doubleValue() <= infinityBoundary);
		} else

		// Negative values
		{
			return (actual.doubleValue() <= zeroBoundary) && (actual.doubleValue() >= infinityBoundary);
		}
	}

	static
	public boolean isZero(Number value, double zeroThreshold){
		return (value.doubleValue() >= -zeroThreshold) && (value.doubleValue() <= zeroThreshold);
	}
}