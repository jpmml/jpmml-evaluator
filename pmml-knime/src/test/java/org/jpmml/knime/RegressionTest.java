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
package org.jpmml.knime;

import org.jpmml.evaluator.IntegrationTest;
import org.jpmml.evaluator.PMMLEquivalence;
import org.junit.Test;

public class RegressionTest extends IntegrationTest {

	public RegressionTest(){
		super(new PMMLEquivalence(1e-15, 1e-15));
	}

	@Test
	public void evaluateDecisionTreeAuto() throws Exception {
		evaluate("DecisionTree", "Auto");
	}

	@Test
	public void evaluateGeneralRegressionAuto() throws Exception {
		evaluate("GeneralRegression", "Auto");
	}

	@Test
	public void evaluateModelEnsembleAuto() throws Exception {
		evaluate("ModelEnsemble", "Auto", new PMMLEquivalence(1e-14, 1e-14));
	}

	@Test
	public void evaluatePolynomialRegressionAuto() throws Exception {
		evaluate("PolynomialRegression", "Auto", new PMMLEquivalence(1e-14, 1e-14));
	}

	@Test
	public void evaluateRegressionEnsembleAuto() throws Exception {
		evaluate("RegressionEnsemble", "Auto");
	}
}