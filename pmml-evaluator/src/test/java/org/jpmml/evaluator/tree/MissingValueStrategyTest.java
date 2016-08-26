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
package org.jpmml.evaluator.tree;

import java.util.Collections;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MissingValueStrategyTest extends ModelEvaluatorTest {

	@Test
	public void nullPrediction() throws Exception {
		Map<FieldName, ?> arguments = createArguments("outlook", "sunny", "temperature", null, "humidity", null);

		NodeScoreDistribution targetValue = evaluate(TreeModel.MissingValueStrategy.NULL_PREDICTION, arguments);

		assertNull(targetValue);
	}

	@Test
	public void nullPredictionDefault() throws Exception {
		NodeScoreDistribution targetValue = evaluate(TreeModel.MissingValueStrategy.NULL_PREDICTION, Collections.<FieldName, FieldValue>emptyMap());

		assertNull(targetValue);
	}

	@Test
	public void lastPrediction() throws Exception {
		Map<FieldName, ?> arguments = createArguments("outlook", "sunny", "temperature", null, "humidity", null);

		NodeScoreDistribution targetValue = evaluate(TreeModel.MissingValueStrategy.LAST_PREDICTION, arguments);

		assertEquals("2", targetValue.getEntityId());

		assertEquals((Double)0.8d, targetValue.getProbability("will play"));
		assertEquals((Double)0.04d, targetValue.getProbability("may play"));
		assertEquals((Double)0.16d, targetValue.getProbability("no play"));
	}

	@Test
	public void lastPredictionDefault() throws Exception {
		NodeScoreDistribution targetValue = evaluate(TreeModel.MissingValueStrategy.LAST_PREDICTION, Collections.<FieldName, FieldValue>emptyMap());

		assertEquals("1", targetValue.getEntityId());

		assertEquals((Double)(60d / 100d), targetValue.getProbability("will play"));
		assertEquals((Double)(30d / 100d), targetValue.getProbability("may play"));
		assertEquals((Double)(10d / 100d), targetValue.getProbability("no play"));
	}

	@Test
	public void defaultChildSinglePenalty() throws Exception {
		Map<FieldName, ?> arguments = createArguments("outlook", null, "temperature", 40d, "humidity", 70d);

		NodeScoreDistribution targetValue = evaluate(TreeModel.MissingValueStrategy.DEFAULT_CHILD, 0.8d, arguments);

		assertEquals("4", targetValue.getEntityId());

		assertEquals((Double)0.4d, targetValue.getProbability("will play"));
		assertEquals((Double)0d, targetValue.getProbability("may play"));
		assertEquals((Double)0.6d, targetValue.getProbability("no play"));

		double missingValuePenatly = 0.8d;

		assertEquals((Double)(0.4d * missingValuePenatly), targetValue.getConfidence("will play"));
		assertEquals((Double)(0d * missingValuePenatly), targetValue.getConfidence("may play"));
		assertEquals((Double)(0.6d * missingValuePenatly), targetValue.getConfidence("no play"));
	}

	@Test
	public void defaultChildMultiplePenalties() throws Exception {
		Map<FieldName, ?> arguments = createArguments("outlook", null, "temperature", null, "humidity", 70d);

		NodeScoreDistribution targetValue = evaluate(TreeModel.MissingValueStrategy.DEFAULT_CHILD, 0.8d, arguments);

		assertEquals("3", targetValue.getEntityId());

		assertEquals((Double)0.9d, targetValue.getProbability("will play"));
		assertEquals((Double)0.05d, targetValue.getProbability("may play"));
		assertEquals((Double)0.05d, targetValue.getProbability("no play"));

		double missingValuePenalty = (0.8d * 0.8d);

		assertEquals((Double)(0.9d * missingValuePenalty), targetValue.getConfidence("will play"));
		assertEquals((Double)(0.05d * missingValuePenalty), targetValue.getConfidence("may play"));
		assertEquals((Double)(0.05d * missingValuePenalty), targetValue.getConfidence("no play"));
	}

	private NodeScoreDistribution evaluate(TreeModel.MissingValueStrategy missingValueStrategy, Map<FieldName, ?> arguments) throws Exception {
		return evaluate(missingValueStrategy, null, arguments);
	}

	private NodeScoreDistribution evaluate(TreeModel.MissingValueStrategy missingValueStrategy, Double missingValuePenalty, Map<FieldName, ?> arguments) throws Exception {
		TreeModelEvaluator evaluator = (TreeModelEvaluator)createModelEvaluator();

		TreeModel treeModel = evaluator.getModel()
			.setMissingValueStrategy(missingValueStrategy)
			.setMissingValuePenalty(missingValuePenalty);

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		return (NodeScoreDistribution)result.get(evaluator.getTargetFieldName());
	}
}