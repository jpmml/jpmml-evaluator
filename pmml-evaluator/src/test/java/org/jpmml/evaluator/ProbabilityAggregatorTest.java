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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProbabilityAggregatorTest {

	@Test
	public void max(){
		ProbabilityAggregator aggregator = new ProbabilityAggregator();

		aggregator.max(createClassificationMap(1d, 3d));

		assertEquals((Double)1d, aggregator.get("A"));
		assertEquals((Double)3d, aggregator.get("B"));

		aggregator.max(createClassificationMap(3d, 1d));

		assertEquals((Double)3d, aggregator.get("A"));
		assertEquals((Double)3d, aggregator.get("B"));
	}

	@Test
	public void sum(){
		ProbabilityAggregator aggregator = new ProbabilityAggregator();

		aggregator.sum(createClassificationMap(1d, 3d));

		assertEquals((Double)1d, aggregator.get("A"));
		assertEquals((Double)3d, aggregator.get("B"));

		aggregator.sum(createClassificationMap(3d, 1d), 0.5d);

		double sumA = (1d + (3d * 0.5d));
		double sumB = (3d + (1d * 0.5d));

		assertEquals((Double)sumA, aggregator.get("A"));
		assertEquals((Double)sumB, aggregator.get("B"));

		aggregator.divide(1.5d);

		double denominator = (1d + 0.5d);

		assertEquals((Double)(sumA / denominator), aggregator.get("A"));
		assertEquals((Double)(sumB / denominator), aggregator.get("B"));
	}

	static
	private ProbabilityClassificationMap createClassificationMap(Double a, Double b){
		ProbabilityClassificationMap result = new ProbabilityClassificationMap();
		result.put("A", a);
		result.put("B", b);

		return result;
	}
}