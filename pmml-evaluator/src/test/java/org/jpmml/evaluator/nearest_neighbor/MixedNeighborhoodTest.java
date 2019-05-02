/*
 * Copyright (c) 2013 KNIME.com AG, Zurich, Switzerland
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jpmml.evaluator.nearest_neighbor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.Row;
import org.dmg.pmml.nearest_neighbor.NearestNeighborModel;
import org.dmg.pmml.nearest_neighbor.TrainingInstances;
import org.jpmml.evaluator.AffinityDistribution;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MixedNeighborhoodTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		NearestNeighborModelEvaluator evaluator = (NearestNeighborModelEvaluator)createModelEvaluator();

		checkTargetFields(Arrays.asList("species", "species_class"), evaluator);

		NearestNeighborModel nearestNeighborModel = evaluator.getModel()
			.setNumberOfNeighbors(1); // XXX

		Map<FieldName, ?> arguments = createArguments("sepal length", 7d, "sepal width", 3.2d, "petal length", 4.7d, "petal width", 1.4d);

		Map<FieldName, ?> results = evaluator.evaluate(arguments);

		AffinityDistribution<?> species = (AffinityDistribution<?>)results.get(FieldName.create("species"));
		assertEquals(20d, species.getResult());
		assertEquals("51", (species.getEntityIdRanking()).get(0));

		AffinityDistribution<?> speciesClass = (AffinityDistribution<?>)results.get(FieldName.create("species_class"));
		assertEquals("Iris-versicolor", speciesClass.getResult());
		assertEquals("51", (speciesClass.getEntityIdRanking()).get(0));

		assertEquals(20d, getOutput(results, "output_1"));
		assertEquals("Iris-versicolor", getOutput(results, "output_2"));
	}

	@Test
	public void evaluateFirstVsRest() throws Exception {
		NearestNeighborModelEvaluator evaluator = (NearestNeighborModelEvaluator)createModelEvaluator();

		removeRow(evaluator, 0);

		Map<FieldName, ?> arguments = createArguments("sepal length", 5.1d, "sepal width", 3.5d, "petal length", 1.4d, "petal width", 0.2d);

		Map<FieldName, ?> results = evaluator.evaluate(arguments);

		AffinityDistribution<?> species = (AffinityDistribution<?>)results.get(FieldName.create("species"));

		assertEquals(10d, species.getResult());

		List<Double> speciesAffinityRanking = species.getAffinityRanking();

		assertEquals((Double)0.01d, speciesAffinityRanking.get(0), 1e-13);
		assertEquals((Double)0.02d, speciesAffinityRanking.get(1), 1e-13);
		assertEquals((Double)0.02d, speciesAffinityRanking.get(2), 1e-13);

		AffinityDistribution<?> speciesClass = (AffinityDistribution<?>)results.get(FieldName.create("species_class"));

		assertEquals("Iris-setosa", speciesClass.getResult());

		List<Double> speciesClassAffinityRanking = speciesClass.getAffinityRanking();

		assertEquals(speciesAffinityRanking, speciesClassAffinityRanking);
	}

	@Test
	public void evaluateLastVsRest() throws Exception {
		NearestNeighborModelEvaluator evaluator = (NearestNeighborModelEvaluator)createModelEvaluator();

		removeRow(evaluator, 149);

		Map<FieldName, ?> arguments = createArguments("sepal length", 5.9d, "sepal width", 3.0d, "petal length", 5.1d, "petal width", 1.8d);

		Map<FieldName, ?> results = evaluator.evaluate(arguments);

		AffinityDistribution<?> species = (AffinityDistribution<?>)results.get(FieldName.create("species"));

		assertEquals(30d, species.getResult());

		List<Double> speciesAffinityRanking = species.getAffinityRanking();

		assertEquals((Double)0.08d, speciesAffinityRanking.get(0), 1e-13);
		assertEquals((Double)0.10d, speciesAffinityRanking.get(1), 1e-13);
		assertEquals((Double)0.11d, speciesAffinityRanking.get(2), 1e-13);

		AffinityDistribution<?> speciesClass = (AffinityDistribution<?>)results.get(FieldName.create("species_class"));

		assertEquals("Iris-virginica", speciesClass.getResult());
	}

	static
	private void removeRow(NearestNeighborModelEvaluator evaluator, int index){
		NearestNeighborModel nearestNeighborModel = evaluator.getModel();

		TrainingInstances trainingInstances = nearestNeighborModel.getTrainingInstances();

		InlineTable inlineTable = trainingInstances.getInlineTable();

		List<Row> rows = inlineTable.getRows();

		assertEquals(150, rows.size());

		trainingInstances.setRecordCount(149);

		rows.remove(index);
	}
}