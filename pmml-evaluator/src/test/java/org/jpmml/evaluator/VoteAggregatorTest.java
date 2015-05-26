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
import java.util.LinkedHashSet;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VoteAggregatorTest {

	@Test
	public void increment(){
		VoteAggregator<String> aggregator = new VoteAggregator<>();
		aggregator.add("A", 1d);
		aggregator.add("B", 1d);
		aggregator.add("C", 1d);

		assertEquals(new LinkedHashSet<>(Arrays.asList("A", "B", "C")), aggregator.getWinners());

		aggregator.add("B", 0.5d);

		assertEquals(Collections.singleton("B"), aggregator.getWinners());

		aggregator.add("A", 0.5d);

		assertEquals(new LinkedHashSet<>(Arrays.asList("A", "B")), aggregator.getWinners());

		Map<String, Double> sumMap = aggregator.sumMap();

		assertEquals((Double)1.5d, sumMap.get("A"));
		assertEquals((Double)1.5d, sumMap.get("B"));
		assertEquals((Double)1.0d, sumMap.get("C"));

		assertEquals(3, aggregator.size());
	}
}