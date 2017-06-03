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

import com.google.common.base.Predicate;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.IntegrationTest;
import org.jpmml.evaluator.PMMLEquivalence;
import org.junit.Test;

public class RegressionTest extends IntegrationTest {

	public RegressionTest(){
		super(new PMMLEquivalence(1e-13, 1e-13));
	}

	@Test
	public void evaluateDecisionTreeAuto() throws Exception {
		evaluate("DecisionTree", "Auto");
	}

	@Test
	public void evaluateDecisionTreeXFormAuto() throws Exception {
		evaluate("DecisionTreeXform", "Auto");
	}

	@Test
	public void evaluateGeneralRegressionAuto() throws Exception {
		evaluate("GeneralRegression", "Auto");
	}

	@Test
	public void evaluateGeneralRegressionXformAuto() throws Exception {
		evaluate("GeneralRegressionXform", "Auto");
	}

	@Test
	public void evaluateKernlabSVMAuto() throws Exception {
		evaluate("KernlabSVM", "Auto");
	}

	@Test
	public void evaluateLibSVMAuto() throws Exception {
		Predicate<FieldName> predicate = excludeFields(FieldName.create("mpg"), FieldName.create("predictedValue"));

		evaluate("LibSVM", "Auto", predicate, new PMMLEquivalence(1e-7, 1e-7));
	}

	@Test
	public void evaluateNeuralNetworkAuto() throws Exception {
		evaluate("NeuralNetwork", "Auto", new PMMLEquivalence(1e-6, 1e-6));
	}

	@Test
	public void evaluateRandomForestAuto() throws Exception {
		evaluate("RandomForest", "Auto");
	}

	@Test
	public void evaluateRegressionAuto() throws Exception {
		evaluate("Regression", "Auto");
	}

	@Test
	public void evaluateRegressionXformAuto() throws Exception {
		evaluate("RegressionXform", "Auto");
	}
}