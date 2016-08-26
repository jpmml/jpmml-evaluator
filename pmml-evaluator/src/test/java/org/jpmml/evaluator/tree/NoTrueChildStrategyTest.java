/*
 * Copyright (c) 2011 University of Tartu
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jpmml.evaluator.tree;

import java.util.Map;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NoTrueChildStrategyTest extends ModelEvaluatorTest {

	@Test
	public void returnNullPrediction() throws Exception {
		NodeScoreDistribution targetValue = evaluate(TreeModel.NoTrueChildStrategy.RETURN_NULL_PREDICTION, 0d);

		assertNull(targetValue);

		targetValue = evaluate(TreeModel.NoTrueChildStrategy.RETURN_NULL_PREDICTION, 0.5d);

		assertNull(targetValue);

		targetValue = evaluate(TreeModel.NoTrueChildStrategy.RETURN_NULL_PREDICTION, 1d);

		assertEquals("3", targetValue.getEntityId());
	}

	@Test
	public void returnLastPrediction() throws Exception {
		NodeScoreDistribution targetValue = evaluate(TreeModel.NoTrueChildStrategy.RETURN_LAST_PREDICTION, 0d);

		// The root Node evaluates to true, but it cannot be returned as a result, because it does not specify a score attribute
		assertNull(targetValue);

		targetValue = evaluate(TreeModel.NoTrueChildStrategy.RETURN_LAST_PREDICTION, 0.5d);

		assertEquals("2", targetValue.getEntityId());

		targetValue = evaluate(TreeModel.NoTrueChildStrategy.RETURN_LAST_PREDICTION, 1d);

		assertEquals("3", targetValue.getEntityId());
	}

	private NodeScoreDistribution evaluate(TreeModel.NoTrueChildStrategy noTrueChildStrategy, Double value) throws Exception {
		TreeModelEvaluator evaluator = (TreeModelEvaluator)createModelEvaluator();

		TreeModel treeModel = evaluator.getModel()
			.setNoTrueChildStrategy(noTrueChildStrategy);

		Map<FieldName, ?> arguments = createArguments("probability", value);

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		return (NodeScoreDistribution)result.get(evaluator.getTargetFieldName());
	}
}