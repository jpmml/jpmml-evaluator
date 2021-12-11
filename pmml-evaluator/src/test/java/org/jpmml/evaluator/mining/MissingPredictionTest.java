/*
 * Copyright (c) 2018 Villu Ruusmann
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
package org.jpmml.evaluator.mining;

import java.util.Map;

import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.tree.NoTrueChildStrategyTransformer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MissingPredictionTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		assertEquals(null, evaluate(evaluator, null));

		evaluator = createModelEvaluator(new MissingThresholdTransformer(0.25d));

		assertEquals(null, evaluate(evaluator, 0d));
		assertEquals(null, evaluate(evaluator, 1d));
		assertEquals(null, evaluate(evaluator, 2d));
		assertEquals((Integer)1, evaluate(evaluator, 3d));
		assertEquals((Integer)1, evaluate(evaluator, 4d));

		evaluator = createModelEvaluator(new MissingThresholdTransformer(0.5d));

		assertEquals(null, evaluate(evaluator, 1d));
		assertEquals((Integer)1, evaluate(evaluator, 2d));
		assertEquals((Integer)1, evaluate(evaluator, 3d));

		evaluator = createModelEvaluator(new MissingThresholdTransformer(0.75d));

		// Two votes for the missing pseudo-category vs. one vote for the "1" category
		assertEquals(null, evaluate(evaluator, 1d));

		assertEquals((Integer)1, evaluate(evaluator, 2d));
		assertEquals((Integer)1, evaluate(evaluator, 3d));

		evaluator = createModelEvaluator(new MissingThresholdTransformer(0.75d), new NoTrueChildStrategyTransformer(TreeModel.NoTrueChildStrategy.RETURN_LAST_PREDICTION));

		assertEquals(null, evaluate(evaluator, null));

		assertEquals((Integer)0, evaluate(evaluator, 1d));
		assertEquals((Integer)1, evaluate(evaluator, 2d));
		assertEquals((Integer)1, evaluate(evaluator, 3d));
	}

	static
	private Integer evaluate(ModelEvaluator<?> evaluator, Double x){
		Map<String, ?> arguments = createArguments("x", x);

		Map<String, ?> results = evaluator.evaluate(arguments);

		return (Integer)decode(results.get("y"));
	}
}