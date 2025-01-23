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
package org.jpmml.evaluator.scorecard;

import java.util.Map;

import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.HasReasonCodeRanking;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class CharacteristicReasonCodeTest extends ReasonCodeTest {

	@Test
	public <T extends HasPartialScores & HasReasonCodeRanking> void evaluateComplex() throws Exception {
		Map<String, ?> results = evaluateExample();

		T targetValue = (T)results.get("overallScore");

		assertEquals(3, (targetValue.getPartialScores()).size());
		assertEquals(2, (targetValue.getReasonCodeRanking()).size());

		assertEquals(29d, results.get("Final Score"));

		assertEquals("RC2", results.get("Reason Code 1"));
		assertEquals("RC1", results.get("Reason Code 2"));
		assertEquals(null, results.get("Reason Code 3"));
	}

	@Test
	public void evaluateSimple() throws Exception {
		Map<String, ?> results = evaluateExample(new DisableReasonCodesTransformer());

		HasPartialScores targetValue = (HasPartialScores)results.get("overallScore");

		assertFalse(targetValue instanceof HasReasonCodeRanking);

		assertEquals(29d, EvaluatorUtil.decode(targetValue));
	}
}