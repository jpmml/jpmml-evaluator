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

import org.dmg.pmml.Interval;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiscretizationUtilTest {

	@Test
	public void contains(){
		Double min = Double.valueOf(Integer.MIN_VALUE);
		Double max = Double.valueOf(Integer.MAX_VALUE);

		Interval negative = createInterval(Interval.Closure.OPEN_OPEN, min, 0d);
		assertTrue(DiscretizationUtil.contains(negative, -1d));
		assertFalse(DiscretizationUtil.contains(negative, 0d));

		Interval negativeNull = createInterval(Interval.Closure.OPEN_OPEN, null, 0d);
		assertTrue(DiscretizationUtil.contains(negativeNull, -1d));
		assertFalse(DiscretizationUtil.contains(negativeNull, 0d));

		Interval positive = createInterval(Interval.Closure.OPEN_OPEN, 0d, max);
		assertFalse(DiscretizationUtil.contains(positive, 0d));
		assertTrue(DiscretizationUtil.contains(positive, 1d));

		Interval positiveNull = createInterval(Interval.Closure.OPEN_OPEN, 0d, null);
		assertFalse(DiscretizationUtil.contains(positiveNull, 0d));
		assertTrue(DiscretizationUtil.contains(positiveNull, 1d));

		Interval negativeAndZero = createInterval(Interval.Closure.OPEN_CLOSED, min, 0d);
		assertTrue(DiscretizationUtil.contains(negativeAndZero, -1d));
		assertTrue(DiscretizationUtil.contains(negativeAndZero, 0d));

		Interval zeroAndPositive = createInterval(Interval.Closure.CLOSED_OPEN, 0d, max);
		assertTrue(DiscretizationUtil.contains(zeroAndPositive, 0d));
		assertTrue(DiscretizationUtil.contains(zeroAndPositive, 1d));
	}

	static
	private Interval createInterval(Interval.Closure closure, Double left, Double right){
		Interval result = new Interval(closure)
			.withLeftMargin(left)
			.withRightMargin(right);

		return result;
	}
}