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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AttributeReasonCodeTest extends ReasonCodeTest {

	@Test
	public void evaluate() throws Exception {
		Map<FieldName, ?> results = evaluateExample();

		ReasonCodeRanking<?> targetValue = (ReasonCodeRanking<?>)results.get(FieldName.create("overallScore"));

		assertEquals(3, (targetValue.getPartialScores()).size());

		assertEquals(29d, getOutput(results, "Final Score"));

		assertEquals("RC2_3", getOutput(results, "Reason Code 1"));
		assertEquals("RC1", getOutput(results, "Reason Code 2"));
		assertEquals(null, getOutput(results, "Reason Code 3"));
	}
}