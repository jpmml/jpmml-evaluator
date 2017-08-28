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
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProbabilityAggregatorTest {

	private ValueFactory<Double> valueFactory = ValueFactoryFactory.DoubleValueFactory.INSTANCE;


	@Test
	public void average(){
		ProbabilityAggregator<Double> aggregator = new ProbabilityAggregator<Double>(0){

			@Override
			public ValueFactory<Double> getValueFactory(){
				return ProbabilityAggregatorTest.this.valueFactory;
			}
		};

		aggregator.add(createProbabilityDistribution(0.2d, null, 0.8d));
		aggregator.add(createProbabilityDistribution(null, 1.0d, null));

		checkValues(0.2d / 2d, 1.0d / 2d, 0.8d / 2d, aggregator.averageMap());
	}

	@Test
	public void weightedAverage(){
		ProbabilityAggregator<Double> aggregator = new ProbabilityAggregator<Double>(0, this.valueFactory.newVector(0)){

			@Override
			public ValueFactory<Double> getValueFactory(){
				return ProbabilityAggregatorTest.this.valueFactory;
			}
		};

		aggregator.add(createProbabilityDistribution(0.2d, 0.6d, 0.2d), 3d);
		aggregator.add(createProbabilityDistribution(0.6d, 0.1d, 0.3d), 1d);

		checkValues((0.2 * 3d + 0.6d) / 4d, (0.6d * 3d + 0.1d) / 4d, (0.2d * 3d + 0.3d) / 4d, aggregator.weightedAverageMap());
	}

	@Test
	public void median(){
		ProbabilityAggregator<Double> aggregator = new ProbabilityAggregator<Double>(3){

			@Override
			public ValueFactory<Double> getValueFactory(){
				return ProbabilityAggregatorTest.this.valueFactory;
			}
		};

		assertEquals(new ValueMap<>(), aggregator.medianMap(ProbabilityAggregatorTest.CATEGORIES));

		aggregator.add(createProbabilityDistribution(0.3d, 0.4d, 0.3d));
		aggregator.add(createProbabilityDistribution(0.1d, 0.6d, 0.3d));

		checkValues((0.3d + 0.1d) / 2d, (0.4d + 0.6d) / 2d, (0.3d + 0.3d) / 2d, aggregator.medianMap(ProbabilityAggregatorTest.CATEGORIES));

		aggregator.add(createProbabilityDistribution(0.3d, 0.5d, 0.2d));

		checkValues(0.3d, 0.5d, 0.2d, aggregator.medianMap(ProbabilityAggregatorTest.CATEGORIES));
	}

	@Test
	public void max(){
		ProbabilityAggregator<Double> aggregator = new ProbabilityAggregator<Double>(5){

			@Override
			public ValueFactory<Double> getValueFactory(){
				return ProbabilityAggregatorTest.this.valueFactory;
			}
		};

		assertEquals(new ValueMap<>(), aggregator.maxMap(ProbabilityAggregatorTest.CATEGORIES));

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

	static
	private void checkValues(double a, double b, double c, ValueMap<String, Double> values){
		assertEquals(new DoubleValue(a), values.get("A"));
		assertEquals(new DoubleValue(b), values.get("B"));
		assertEquals(new DoubleValue(c), values.get("C"));
	}

	static
	private HasProbability createProbabilityDistribution(Double a, Double b, Double c){
		ProbabilityDistribution result = new ProbabilityDistribution();

		if(a != null){
			result.put("A", a);
		} // End if

		if(b != null){
			result.put("B", b);
		} // End if

		if(c != null){
			result.put("C", c);
		}

		return result;
	}

	private static final List<String> CATEGORIES = Arrays.asList("A", "B", "C");
}