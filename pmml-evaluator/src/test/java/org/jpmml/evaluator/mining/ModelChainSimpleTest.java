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
package org.jpmml.evaluator.mining;

import java.util.Arrays;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.Evaluator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ModelChainSimpleTest extends ModelChainTest {

	@Test
	public void getResultFields() throws Exception {
		Evaluator evaluator = createModelEvaluator();

		checkResultFields(Arrays.asList("Class", "PollenIndex"), Arrays.asList("Pollen Index"), evaluator);
	}

	@Test
	public void evaluateSetosa() throws Exception {
		Map<FieldName, ?> result = evaluateExample(1.4, 0.2);

		assertEquals(0.8d + 0.3d, getTarget(result, "PollenIndex"));
	}
}