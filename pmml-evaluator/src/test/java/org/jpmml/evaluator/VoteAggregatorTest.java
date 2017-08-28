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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VoteAggregatorTest {

	private ValueFactory<Double> valueFactory = ValueFactoryFactory.DoubleValueFactory.INSTANCE;


	@Test
	public void sum(){
		VoteAggregator<String, Double> aggregator = new VoteAggregator<String, Double>(){

			@Override
			public ValueFactory<Double> getValueFactory(){
				return VoteAggregatorTest.this.valueFactory;
			}
		};
		aggregator.add("A", 1d);
		aggregator.add("B", 1d);
		aggregator.add("C", 1d);

		assertEquals(new LinkedHashSet<>(Arrays.asList("A", "B", "C")), aggregator.getWinners());

		aggregator.add("B", 0.5d);

		checkValues(1d, 1d + 0.5d, 1d, aggregator.sumMap());

		assertEquals(Collections.singleton("B"), aggregator.getWinners());

		aggregator.add("A", 0.5d);

		checkValues(1d + 0.5d, 1d + 0.5d, 1d, aggregator.sumMap());

		assertEquals(new LinkedHashSet<>(Arrays.asList("A", "B")), aggregator.getWinners());
	}

	static
	private void checkValues(double a, double b, double c, ValueMap<String, Double> values){
		assertEquals(new DoubleValue(a), values.get("A"));
		assertEquals(new DoubleValue(b), values.get("B"));
		assertEquals(new DoubleValue(c), values.get("C"));
	}
}