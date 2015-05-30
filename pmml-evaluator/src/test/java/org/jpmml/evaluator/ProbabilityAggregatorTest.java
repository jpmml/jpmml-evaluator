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

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProbabilityAggregatorTest {

	@Test
	public void add(){
		ProbabilityAggregator aggregator = new ProbabilityAggregator();

		aggregator.add(createProbabilityDistribution(1d, 3d));

		Map<String, Double> maxMap = aggregator.maxMap();

		assertEquals((Double)1d, maxMap.get("A"));
		assertEquals((Double)3d, maxMap.get("B"));

		aggregator.add(createProbabilityDistribution(3d, 1d), 0.5d);

		maxMap = aggregator.maxMap();

		assertEquals((Double)(3d * 0.5d), maxMap.get("A"));
		assertEquals((Double)3d, maxMap.get("B"));

		double denominator = (1d + 0.5d);

		Map<String, Double> averageMap = aggregator.averageMap(denominator);

		double sumA = (1d + (3d * 0.5d));
		double sumB = (3d + (1d * 0.5d));

		assertEquals((Double)(sumA / denominator), averageMap.get("A"));
		assertEquals((Double)(sumB / denominator), averageMap.get("B"));

		assertEquals(2, aggregator.size());
	}

	static
	private HasProbability createProbabilityDistribution(Double a, Double b){
		ProbabilityDistribution result = new ProbabilityDistribution();
		result.put("A", a);
		result.put("B", b);

		return result;
	}
}