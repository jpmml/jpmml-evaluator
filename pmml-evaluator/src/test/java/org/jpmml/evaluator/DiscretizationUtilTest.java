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

		Interval negative = createInterval(Interval.Closure.OPEN_OPEN, min, 0.0);
		assertTrue(DiscretizationUtil.contains(negative, -1.0));
		assertFalse(DiscretizationUtil.contains(negative, 0.0));

		Interval negativeNull = createInterval(Interval.Closure.OPEN_OPEN, null, 0.0);
		assertTrue(DiscretizationUtil.contains(negativeNull, -1.0));
		assertFalse(DiscretizationUtil.contains(negativeNull, 0.0));

		Interval positive = createInterval(Interval.Closure.OPEN_OPEN, 0.0, max);
		assertFalse(DiscretizationUtil.contains(positive, 0.0));
		assertTrue(DiscretizationUtil.contains(positive, 1.0));

		Interval positiveNull = createInterval(Interval.Closure.OPEN_OPEN, 0.0, null);
		assertFalse(DiscretizationUtil.contains(positiveNull, 0.0));
		assertTrue(DiscretizationUtil.contains(positiveNull, 1.0));

		Interval negativeAndZero = createInterval(Interval.Closure.OPEN_CLOSED, min, 0.0);
		assertTrue(DiscretizationUtil.contains(negativeAndZero, -1.0));
		assertTrue(DiscretizationUtil.contains(negativeAndZero, 0.0));

		Interval zeroAndPositive = createInterval(Interval.Closure.CLOSED_OPEN, 0.0, max);
		assertTrue(DiscretizationUtil.contains(zeroAndPositive, 0.0));
		assertTrue(DiscretizationUtil.contains(zeroAndPositive, 1.0));
	}

	static
	private Interval createInterval(Interval.Closure closure, Double left, Double right){
		Interval interval = new Interval(closure);
		interval.setLeftMargin(left);
		interval.setRightMargin(right);

		return interval;
	}
}