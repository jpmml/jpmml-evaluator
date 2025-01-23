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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValueAggregatorTest {

	private ValueFactory<Double> valueFactory = ValueFactoryFactory.DoubleValueFactory.INSTANCE;


	@Test
	public void sumAndAverage(){
		ValueAggregator<Double> aggregator = new ValueAggregator.UnivariateStatistic<>(this.valueFactory);

		assertThrows(UndefinedResultException.class, () -> aggregator.average());

		aggregator.add(2d);
		aggregator.add(3d);

		Value<Double> sumValue = new DoubleValue(2d + 3d);

		assertEquals(sumValue, aggregator.sum());
		assertEquals(sumValue.divide(2), aggregator.average());
	}

	@Test
	public void weightedSumAndAverage(){
		ValueAggregator<Double> aggregator = new ValueAggregator.WeightedUnivariateStatistic<>(this.valueFactory);

		assertThrows(UndefinedResultException.class, () -> aggregator.weightedAverage());

		aggregator.add(2d, 1d / 3d);
		aggregator.add(3d, 2d / 3d);

		Value<Double> weightedSumValue = new DoubleValue((2d * (1d / 3d)) + (3d * (2d / 3d)));

		assertEquals(weightedSumValue, aggregator.weightedSum());
		assertEquals(weightedSumValue.divide((1d / 3d) + (2d / 3d)), aggregator.weightedAverage());
	}

	@Test
	public void median(){
		ValueAggregator<Double> aggregator = new ValueAggregator.Median<>(this.valueFactory, 3);

		assertThrows(UndefinedResultException.class, () -> aggregator.median());

		aggregator.add(4d);
		aggregator.add(5d);

		assertEquals(new DoubleValue((4d + 5d) / 2d), aggregator.median());

		aggregator.add(6d);

		assertEquals(new DoubleValue(5d), aggregator.median());
	}

	@Test
	public void weightedMedian(){
		ValueAggregator<Double> aggregator = new ValueAggregator.WeightedMedian<>(this.valueFactory, 3);

		assertThrows(UndefinedResultException.class, () -> aggregator.weightedMedian());

		aggregator.add(1d, 3d);
		aggregator.add(5d, 7d);

		assertEquals(new DoubleValue(5d), aggregator.weightedMedian());

		aggregator.add(7d, 11d);

		assertEquals(new DoubleValue(7d), aggregator.weightedMedian());
	}
}