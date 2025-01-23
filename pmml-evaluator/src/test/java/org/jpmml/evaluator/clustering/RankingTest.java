/*
 * Copyright (c) 2015 Villu Ruusmann
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
package org.jpmml.evaluator.clustering;

import java.util.Collections;
import java.util.Map;

import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RankingTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		checkTargetFields(Collections.singletonList(null), evaluator);

		Map<String, ?> arguments = createArguments("input", 1d);

		Map<String, ?> results = evaluator.evaluate(arguments);

		ClusterAffinityDistribution<?> affinityDistribution = (ClusterAffinityDistribution<?>)results.get(evaluator.getTargetName());

		assertEquals("2", affinityDistribution.getResult());

		checkValue("2", 1d, results, "first");
		checkValue("3", 4d, results, "second");
		checkValue("1", 16d, results, "third");
	}

	static
	private void checkValue(String id, Double affinity, Map<String, ?> results, String suffix){
		assertEquals(id, results.get("id_" + suffix));
		assertEquals(affinity, results.get("affinity_" + suffix));
	}
}