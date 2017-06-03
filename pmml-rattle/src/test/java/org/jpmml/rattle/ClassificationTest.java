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

import com.google.common.base.Predicates;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.Batch;
import org.jpmml.evaluator.IntegrationTest;
import org.jpmml.evaluator.PMMLEquivalence;
import org.junit.Test;

public class ClassificationTest extends IntegrationTest {

	@Test
	public void evaluateDecisionTreeIris() throws Exception {
		evaluate("DecisionTree", "Iris");
	}

	@Test
	public void evaluateDecisionTreeXformIris() throws Exception {
		evaluate("DecisionTreeXform", "Iris");
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
		evaluate("LogisticRegression", "Iris");
	}

	@Test
	public void evaluateNaiveBayesIris() throws Exception {
		evaluate("NaiveBayes", "Iris");
	}

	@Test
	public void evaluateNeuralNetworkIris() throws Exception {

		try(Batch batch = createBatch("NeuralNetwork", "Iris", Predicates.<FieldName>alwaysTrue())){
			evaluate(batch, new PMMLEquivalence(1e-6, 1e-6));
		}
	}

	@Test
	public void evaluateRandomForestIris() throws Exception {
		evaluate("RandomForest", "Iris");
	}

	@Test
	public void evaluateRandomForestXformIris() throws Exception {
		evaluate("RandomForestXform", "Iris");
	}

	@Test
	public void evaluateGeneralRegressionVersicolor() throws Exception {
		evaluate("GeneralRegression", "Versicolor");
	}

	@Test
	public void evaluateDecisionTreeAudit() throws Exception {
		evaluate("DecisionTree", "Audit");
	}

	@Test
	public void evaluateGeneralRegressionAudit() throws Exception {
		evaluate("GeneralRegression", "Audit");
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

		try(Batch batch = createBatch("NeuralNetwork", "Audit", Predicates.<FieldName>alwaysTrue())){
			evaluate(batch, new PMMLEquivalence(1e-5, 1e-5));
		}
	}

	@Test
	public void evaluateRandomForestAudit() throws Exception {
		evaluate("RandomForest", "Audit");
	}
}