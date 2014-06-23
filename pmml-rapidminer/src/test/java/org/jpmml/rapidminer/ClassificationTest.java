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

import org.jpmml.evaluator.Batch;
import org.jpmml.evaluator.BatchUtil;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ClassificationTest {

	@Test
	public void evaluateDecisionTreeIris() throws Exception {
		Batch batch = new RapidMinerBatch("DecisionTree", "Iris");

		assertTrue(BatchUtil.evaluate(batch));
	}

	@Test
	public void evaluateNeuralNetworkIris() throws Exception {
		Batch batch = new RapidMinerBatch("NeuralNetwork", "Iris");

		assertTrue(BatchUtil.evaluate(batch));
	}

	@Test
	public void evaluateRuleSetIris() throws Exception {
		Batch batch = new RapidMinerBatch("RuleSet", "Iris");

		assertTrue(BatchUtil.evaluate(batch));
	}

	@Test
	public void evaluateDecisionTreeAudit() throws Exception {
		Batch batch = new RapidMinerBatch("DecisionTree", "Audit");

		assertTrue(BatchUtil.evaluate(batch));
	}

	@Test
	public void evaluateRuleSetAudit() throws Exception {
		Batch batch = new RapidMinerBatch("RuleSet", "Audit");

		assertTrue(BatchUtil.evaluate(batch));
	}
}