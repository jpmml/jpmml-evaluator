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
package org.jpmml.rapidminer;

import org.jpmml.evaluator.IntegrationTest;
import org.jpmml.evaluator.PMMLEquivalence;
import org.junit.Test;

public class ClassificationTest extends IntegrationTest {

	public ClassificationTest(){
		super(new PMMLEquivalence(1e-13, 1e-13));
	}

	@Test
	public void evaluateDecisionTreeIris() throws Exception {
		evaluate("DecisionTree", "Iris");
	}

	@Test
	public void evaluateNeuralNetworkIris() throws Exception {
		evaluate("NeuralNetwork", "Iris");
	}

	@Test
	public void evaluateRuleSetIris() throws Exception {
		evaluate("RuleSet", "Iris");
	}

	@Test
	public void evaluateDecisionTreeAudit() throws Exception {
		evaluate("DecisionTree", "Audit");
	}

	@Test
	public void evaluateRuleSetAudit() throws Exception {
		evaluate("RuleSet", "Audit");
	}
}