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

import static org.junit.Assert.assertTrue;

public class ClusteringTest {

	@Test
	public void evaluateHierarchicalClusteringIris() throws Exception {
		Batch batch = new RattleBatch("HierarchicalClustering", "Iris");

		assertTrue(BatchUtil.evaluate(batch));
	}

	@Test
	public void evaluateKMeansIris() throws Exception {
		Batch batch = new RattleBatch("KMeans", "Iris");

		assertTrue(BatchUtil.evaluate(batch));
	}
}