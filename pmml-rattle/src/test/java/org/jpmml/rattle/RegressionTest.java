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

import org.jpmml.evaluator.Batch;
import org.jpmml.evaluator.BatchUtil;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RegressionTest {

	@Test
	public void evaluateGeneralRegressionOzone() throws Exception {
		Batch batch = new RattleBatch("GeneralRegression", "Ozone");

		assertTrue(BatchUtil.evaluate(batch));

		assertNull(BatchUtil.evaluateDefault(batch));
	}

	@Test
	public void evaluateNeuralNetworkOzone() throws Exception {
		Batch batch = new RattleBatch("NeuralNetwork", "Ozone");

		assertTrue(BatchUtil.evaluate(batch));
	}

	@Test
	public void evaluateRandomForestOzone() throws Exception {
		Batch batch = new RattleBatch("RandomForest", "Ozone");

		assertTrue(BatchUtil.evaluate(batch));
	}

	@Test
	public void evaluateRegressionOzone() throws Exception {
		Batch batch = new RattleBatch("Regression", "Ozone");

		assertTrue(BatchUtil.evaluate(batch));

		assertNull(BatchUtil.evaluateDefault(batch));
	}

	@Test
	public void evaluateSupportVectorMachineOzone() throws Exception {
		Batch batch = new RattleBatch("SupportVectorMachine", "Ozone");

		assertTrue(BatchUtil.evaluate(batch));
	}
}