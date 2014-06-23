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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VerificationUtilTest {

	@Test
	public void acceptable(){
		double precision = 0.001;
		double zeroThreshold = (precision * precision);

		assertTrue(VerificationUtil.acceptable(1.0, 1.0, precision, zeroThreshold));

		assertTrue(VerificationUtil.acceptable(1.0, 0.999, precision, zeroThreshold));
		assertFalse(VerificationUtil.acceptable(1.0, 0.99895, precision, zeroThreshold));

		assertTrue(VerificationUtil.acceptable(1.0, 1.001, precision, zeroThreshold));
		assertFalse(VerificationUtil.acceptable(1.0, 1.00105, precision, zeroThreshold));

		assertTrue(VerificationUtil.acceptable(-1.0, -1.0, precision, zeroThreshold));

		assertTrue(VerificationUtil.acceptable(-1.0, -1.001, precision, zeroThreshold));
		assertFalse(VerificationUtil.acceptable(-1.0, -1.00105, precision, zeroThreshold));

		assertTrue(VerificationUtil.acceptable(-1.0, -0.999, precision, zeroThreshold));
		assertFalse(VerificationUtil.acceptable(-1.0, -0.99895, precision, zeroThreshold));
	}

	@Test
	public void isZero(){
		double zeroThreshold = 0.001;

		assertTrue(VerificationUtil.isZero(0.0005, zeroThreshold));
		assertTrue(VerificationUtil.isZero(0, zeroThreshold));
		assertTrue(VerificationUtil.isZero(-0.0005, zeroThreshold));

		assertFalse(VerificationUtil.isZero(0.0015, zeroThreshold));
		assertFalse(VerificationUtil.isZero(-0.0015, zeroThreshold));
	}
}