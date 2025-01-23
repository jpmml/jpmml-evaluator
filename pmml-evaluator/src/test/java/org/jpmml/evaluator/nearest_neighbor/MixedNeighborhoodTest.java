/*
 * Copyright (c) 2014 Villu Ruusmann
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
package org.jpmml.evaluator.nearest_neighbor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jpmml.evaluator.AffinityDistribution;
import org.jpmml.evaluator.Deltas;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MixedNeighborhoodTest extends ModelEvaluatorTest implements Deltas {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator(new NumberOfNeighborsTransformer(1));

		checkTargetFields(Arrays.asList("species", "species_class"), evaluator);

		Map<String, ?> arguments = createArguments("sepal length", 7d, "sepal width", 3.2d, "petal length", 4.7d, "petal width", 1.4d);

		Map<String, ?> results = evaluator.evaluate(arguments);

		AffinityDistribution<?> species = (AffinityDistribution<?>)results.get("species");
		assertEquals(20d, species.getResult());
		assertEquals("51", (species.getEntityIdRanking()).get(0));

		AffinityDistribution<?> speciesClass = (AffinityDistribution<?>)results.get("species_class");
		assertEquals("Iris-versicolor", speciesClass.getResult());
		assertEquals("51", (speciesClass.getEntityIdRanking()).get(0));

		assertEquals(20d, results.get("output_1"));
		assertEquals("Iris-versicolor", results.get("output_2"));
	}

	@Test
	public void evaluateFirstVsRest() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator(new RemoveTrainingInstanceTransformer(0));

		Map<String, ?> arguments = createArguments("sepal length", 5.1d, "sepal width", 3.5d, "petal length", 1.4d, "petal width", 0.2d);

		Map<String, ?> results = evaluator.evaluate(arguments);

		AffinityDistribution<?> species = (AffinityDistribution<?>)results.get("species");

		assertEquals(10d, species.getResult());

		List<Double> speciesAffinityRanking = species.getAffinityRanking();

		assertEquals((Double)0.01d, speciesAffinityRanking.get(0), DOUBLE_EXACT);
		assertEquals((Double)0.02d, speciesAffinityRanking.get(1), DOUBLE_EXACT);
		assertEquals((Double)0.02d, speciesAffinityRanking.get(2), DOUBLE_EXACT);

		AffinityDistribution<?> speciesClass = (AffinityDistribution<?>)results.get("species_class");

		assertEquals("Iris-setosa", speciesClass.getResult());

		List<Double> speciesClassAffinityRanking = speciesClass.getAffinityRanking();

		assertEquals(speciesAffinityRanking, speciesClassAffinityRanking);
	}

	@Test
	public void evaluateLastVsRest() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator(new RemoveTrainingInstanceTransformer(149));

		Map<String, ?> arguments = createArguments("sepal length", 5.9d, "sepal width", 3.0d, "petal length", 5.1d, "petal width", 1.8d);

		Map<String, ?> results = evaluator.evaluate(arguments);

		AffinityDistribution<?> species = (AffinityDistribution<?>)results.get("species");

		assertEquals(30d, species.getResult());

		List<Double> speciesAffinityRanking = species.getAffinityRanking();

		assertEquals((Double)0.08d, speciesAffinityRanking.get(0), DOUBLE_EXACT);
		assertEquals((Double)0.10d, speciesAffinityRanking.get(1), DOUBLE_EXACT);
		assertEquals((Double)0.11d, speciesAffinityRanking.get(2), DOUBLE_EXACT);

		AffinityDistribution<?> speciesClass = (AffinityDistribution<?>)results.get("species_class");

		assertEquals("Iris-virginica", speciesClass.getResult());
	}
}