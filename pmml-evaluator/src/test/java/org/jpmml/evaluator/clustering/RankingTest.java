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

import java.util.Map;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RankingTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		Map<FieldName, ?> arguments = createArguments("input", 1d);

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		ClusterAffinityDistribution affinityDistribution = (ClusterAffinityDistribution)result.get(evaluator.getTargetFieldName());

		assertEquals("2", affinityDistribution.getResult());

		checkValue("2", 1d, result, "first");
		checkValue("3", 4d, result, "second");
		checkValue("1", 16d, result, "third");
	}

	static
	private void checkValue(String id, Double affinity, Map<FieldName, ?> result, String suffix){
		assertEquals(id, getOutput(result, "id_" + suffix));
		assertEquals(affinity, getOutput(result, "affinity_" + suffix));
	}
}