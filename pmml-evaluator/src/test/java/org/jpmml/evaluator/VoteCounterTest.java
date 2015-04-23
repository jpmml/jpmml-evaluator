/*
 * Copyright (c) 2015 Villu Ruusmann
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

import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.Sets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VoteCounterTest {

	@Test
	public void increment(){
		VoteCounter<String> counter = new VoteCounter<String>();
		counter.increment("A");
		counter.increment("B");
		counter.increment("C");

		assertEquals(Sets.newLinkedHashSet(Arrays.asList("A", "B", "C")), counter.getWinners());

		counter.increment("B", 0.5d);

		assertEquals(Collections.singleton("B"), counter.getWinners());

		counter.increment("A", 0.5d);

		assertEquals(Sets.newLinkedHashSet(Arrays.asList("A", "B")), counter.getWinners());

		assertEquals(3, counter.size());
	}
}