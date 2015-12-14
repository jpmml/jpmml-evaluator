/*
 * Copyright (c) 2013 Villu Ruusmann
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
package org.jpmml.rattle;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.ArchiveBatchTest;
import org.jpmml.evaluator.Batch;
import org.jpmml.evaluator.BatchUtil;
import org.jpmml.evaluator.NodeScoreDistribution;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClassificationTest extends ArchiveBatchTest {

	@Test
	public void evaluateDecisionTreeIris() throws Exception {

		try(Batch batch = createBatch("DecisionTree", "Iris")){
			NodeScoreDistribution targetValue = (NodeScoreDistribution)BatchUtil.evaluateDefault(batch);

			assertEquals("7", targetValue.getEntityId());

			evaluate(batch);
		}
	}

	@Test
	public void evaluateKernlabSVMIris() throws Exception {
		evaluate("KernlabSVM", "Iris");
	}

	@Test
	public void evaluateLibSVMIris() throws Exception {
		evaluate("LibSVM", "Iris");
	}

	@Test
	public void evaluateLogisticRegressionIris() throws Exception {

		try(Batch batch = createBatch("LogisticRegression", "Iris")){
			assertEquals(null, BatchUtil.evaluateDefault(batch));

			evaluate(batch);
		}
	}

	@Test
	public void evaluateNaiveBayesIris() throws Exception {
		evaluate("NaiveBayes", "Iris");
	}

	@Test
	public void evaluateNeuralNetworkIris() throws Exception {

		try(Batch batch = createBatch("NeuralNetwork", "Iris")){
			assertEquals(null, BatchUtil.evaluateDefault(batch));

			evaluate(batch, null, 1e-6, 1e-6);
		}
	}

	@Test
	public void evaluateRandomForestIris() throws Exception {
		evaluate("RandomForest", "Iris");
	}

	@Test
	public void evaluateGeneralRegressionVersicolor() throws Exception {

		try(Batch batch = createBatch("GeneralRegression", "Versicolor")){
			assertEquals(null, BatchUtil.evaluateDefault(batch));

			evaluate(batch);
		}
	}

	@Test
	public void evaluateDecisionTreeAudit() throws Exception {

		try(Batch batch = createBatch("DecisionTree", "Audit")){
			NodeScoreDistribution targetValue = (NodeScoreDistribution)BatchUtil.evaluateDefault(batch);

			assertEquals("2", targetValue.getEntityId());

			evaluate(batch);
		}
	}

	@Test
	public void evaluateGeneralRegressionAudit() throws Exception {

		try(Batch batch = createBatch("GeneralRegression", "Audit")){
			assertEquals(null, BatchUtil.evaluateDefault(batch));

			evaluate(batch);
		}
	}

	@Test
	public void evaluateKernlabSVMAudit() throws Exception {
		evaluate("KernlabSVM", "Audit");
	}

	@Test
	public void evaluateLibSVMAudit() throws Exception {
		evaluate("LibSVM", "Audit");
	}

	@Test
	public void evaluateLogisticRegressionAudit() throws Exception {
		evaluate("LogisticRegression", "Audit");
	}

	@Test
	public void evaluateNaiveBayesAudit() throws Exception {
		evaluate("NaiveBayes", "Audit");
	}

	@Test
	public void evaluateNeuralNetworkAudit() throws Exception {

		try(Batch batch = createBatch("NeuralNetwork", "Audit")){
			assertEquals(null, BatchUtil.evaluateDefault(batch));

			Set<FieldName> ignoredFields = ImmutableSet.of(FieldName.create("Probability_0"), FieldName.create("Probability_1"));

			evaluate(batch, ignoredFields);
		}
	}

	@Test
	public void evaluateRandomForestAudit() throws Exception {
		evaluate("RandomForest", "Audit");
	}
}