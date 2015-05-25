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

import com.google.common.collect.Range;
import org.dmg.pmml.Interval;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiscretizationUtilTest {

	@Test
	public void boundedRange(){
		Range<Double> open = toRange(Interval.Closure.OPEN_OPEN, -1d, 1d);
		assertFalse(open.contains(-Double.MAX_VALUE));
		assertFalse(open.contains(-1d));
		assertTrue(open.contains(0d));
		assertFalse(open.contains(1d));
		assertFalse(open.contains(Double.MAX_VALUE));

		Range<Double> openClosed = toRange(Interval.Closure.OPEN_CLOSED, -1d, 1d);
		assertFalse(openClosed.contains(-Double.MAX_VALUE));
		assertFalse(openClosed.contains(-1d));
		assertTrue(openClosed.contains(0d));
		assertTrue(openClosed.contains(1d));
		assertFalse(openClosed.contains(Double.MAX_VALUE));

		Range<Double> closedOpen = toRange(Interval.Closure.CLOSED_OPEN, -1d, 1d);
		assertFalse(closedOpen.contains(-Double.MAX_VALUE));
		assertTrue(closedOpen.contains(-1d));
		assertTrue(closedOpen.contains(0d));
		assertFalse(closedOpen.contains(1d));
		assertFalse(closedOpen.contains(Double.MAX_VALUE));

		Range<Double> closed = toRange(Interval.Closure.CLOSED_CLOSED, -1d, 1d);
		assertFalse(closed.contains(-Double.MAX_VALUE));
		assertTrue(closed.contains(-1d));
		assertTrue(closed.contains(0d));
		assertTrue(closed.contains(1d));
		assertFalse(closed.contains(Double.MAX_VALUE));
	}

	@Test
	public void unboundedRange(){
		Range<Double> lessThan = toRange(Interval.Closure.OPEN_OPEN, null, 0d);
		assertTrue(lessThan.contains(-Double.MAX_VALUE));
		assertFalse(lessThan.contains(0d));
		assertFalse(lessThan.contains(Double.MAX_VALUE));

		Range<Double> atMost = toRange(Interval.Closure.OPEN_CLOSED, null, 0d);
		assertTrue(atMost.contains(-Double.MAX_VALUE));
		assertTrue(atMost.contains(0d));
		assertFalse(atMost.contains(Double.MAX_VALUE));

		Range<Double> greaterThan = toRange(Interval.Closure.OPEN_OPEN, 0d, null);
		assertFalse(greaterThan.contains(-Double.MAX_VALUE));
		assertFalse(greaterThan.contains(0d));
		assertTrue(greaterThan.contains(Double.MAX_VALUE));

		Range<Double> atLeast = toRange(Interval.Closure.CLOSED_OPEN, 0d, null);
		assertFalse(atLeast.contains(-Double.MAX_VALUE));
		assertTrue(atLeast.contains(0d));
		assertTrue(atLeast.contains(Double.MAX_VALUE));
	}

	static
	private Range<Double> toRange(Interval.Closure closure, Double leftMargin, Double rightMargin){
		return DiscretizationUtil.toRange(createInterval(closure, leftMargin, rightMargin));
	}

	static
	private Interval createInterval(Interval.Closure closure, Double leftMargin, Double rightMargin){
		Interval result = new Interval(closure)
			.setLeftMargin(leftMargin)
			.setRightMargin(rightMargin);

		return result;
	}
}