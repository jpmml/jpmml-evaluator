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
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProbabilityAggregatorTest {

	@Test
	public void max(){
		ProbabilityAggregator aggregator = new ProbabilityAggregator(5);

		assertEquals(Collections.emptyMap(), aggregator.maxMap(ProbabilityAggregatorTest.CATEGORIES));

		aggregator.add(createProbabilityDistribution(0.2d, 0.6d, 0.2d));
		aggregator.add(createProbabilityDistribution(0.6d, 0.3d, 0.1d));
		aggregator.add(createProbabilityDistribution(0.1d, 0.6d, 0.3d));

		checkValues(0.6d, 0.3d, 0.1d, aggregator.maxMap(ProbabilityAggregatorTest.CATEGORIES));
		checkValues((0.2d + 0.1d) / 2d, (0.6d + 0.6d) / 2d, (0.2d + 0.3d) / 2d, aggregator.maxMap(Arrays.asList("B", "A", "C")));

		aggregator.add(createProbabilityDistribution(0.1d, 0.1d, 0.8d));

		checkValues(0.1d, 0.1d, 0.8d, aggregator.maxMap(ProbabilityAggregatorTest.CATEGORIES));

		aggregator.add(createProbabilityDistribution(0.0d, 0.2d, 0.8d));

		checkValues((0.1d + 0.0d) / 2d, (0.1d + 0.2d) / 2d, (0.8d + 0.8d) / 2d, aggregator.maxMap(ProbabilityAggregatorTest.CATEGORIES));
	}

	@Test
	public void median(){
		ProbabilityAggregator aggregator = new ProbabilityAggregator(3);

		assertEquals(Collections.emptyMap(), aggregator.medianMap(ProbabilityAggregatorTest.CATEGORIES));

		aggregator.add(createProbabilityDistribution(0.3d, 0.4d, 0.3d));
		aggregator.add(createProbabilityDistribution(0.1d, 0.6d, 0.3d));

		checkValues((0.3d + 0.1d) / 2d, (0.4d + 0.6d) / 2d, (0.3d + 0.3d) / 2d, aggregator.medianMap(ProbabilityAggregatorTest.CATEGORIES));

		aggregator.add(createProbabilityDistribution(0.3d, 0.5d, 0.2d));

		checkValues(0.3d, 0.5d, 0.2d, aggregator.medianMap(ProbabilityAggregatorTest.CATEGORIES));
	}

	@Test
	public void weightedAverage(){
		ProbabilityAggregator aggregator = new ProbabilityAggregator();
		aggregator.add(createProbabilityDistribution(0.2d, 0.6d, 0.2d), 3d);
		aggregator.add(createProbabilityDistribution(0.6d, 0.1d, 0.3d), 1d);

		checkValues((0.2 * 3d + 0.6d) / 4d, (0.6d * 3d + 0.1d) / 4d, (0.2d * 3d + 0.3d) / 4d, aggregator.averageMap(4d));
	}

	static
	private void checkValues(Double a, Double b, Double c, Map<String, Double> values){
		assertEquals(a, values.get("A"));
		assertEquals(b, values.get("B"));
		assertEquals(c, values.get("C"));
	}

	static
	private HasProbability createProbabilityDistribution(Double a, Double b, Double c){
		ProbabilityDistribution result = new ProbabilityDistribution();
		result.put("A", a);
		result.put("B", b);
		result.put("C", c);

		return result;
	}

	private static final List<String> CATEGORIES = Arrays.asList("A", "B", "C");
}