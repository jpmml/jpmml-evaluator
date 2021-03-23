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

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.HasReasonCodeRanking;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AttributeReasonCodeTest extends ReasonCodeTest {

	@Test
	public <T extends HasPartialScores & HasReasonCodeRanking> void evaluateComplex() throws Exception {
		Map<FieldName, ?> results = evaluateExample();

		T targetValue = (T)results.get(FieldName.create("overallScore"));

		assertEquals(3, (targetValue.getPartialScores()).size());
		assertEquals(2, (targetValue.getReasonCodeRanking()).size());

		assertEquals(29d, getOutput(results, "Final Score"));

		assertEquals("RC2_3", getOutput(results, "Reason Code 1"));
		assertEquals("RC1", getOutput(results, "Reason Code 2"));
		assertEquals(null, getOutput(results, "Reason Code 3"));
	}

	@Test
	public void evaluateSimple() throws Exception {
		Map<FieldName, ?> results = evaluateExample(new DisableReasonCodesTransformer());

		HasPartialScores targetValue = (HasPartialScores)results.get(FieldName.create("overallScore"));

		assertFalse(targetValue instanceof HasReasonCodeRanking);

		assertEquals(29d, EvaluatorUtil.decode(targetValue));
	}
}