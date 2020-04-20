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

public class PMMLEquivalence extends Equivalence<Object> {

	private double precision = 0d;

	private double zeroThreshold = 0d;


	public PMMLEquivalence(double precision, double zeroThreshold){
		setPrecision(precision);
		setZeroThreshold(zeroThreshold);
	}

	@Override
	public boolean doEquivalent(Object expected, Object actual){
		double precision = getPrecision();
		double zeroThreshold = getZeroThreshold();

		if(actual instanceof Computable){
			actual = EvaluatorUtil.decode(actual);
		}

		expected = TypeUtil.parseOrCast(TypeUtil.getDataType(actual), expected);

		return VerificationUtil.acceptable(expected, actual, precision, zeroThreshold);
	}

	@Override
	public int doHash(Object object){
		throw new UnsupportedOperationException();
	}

	public double getPrecision(){
		return this.precision;
	}

	private void setPrecision(double precision){

		if(precision < 0d){
			throw new IllegalArgumentException();
		}

		this.precision = precision;
	}

	public double getZeroThreshold(){
		return this.zeroThreshold;
	}

	private void setZeroThreshold(double zeroThreshold){

		if(zeroThreshold < 0d){
			throw new IllegalArgumentException();
		}

		this.zeroThreshold = zeroThreshold;
	}
}