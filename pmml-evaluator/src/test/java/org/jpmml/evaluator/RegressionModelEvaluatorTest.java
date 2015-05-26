/*
 * Copyright (c) 2014 Villu Ruusmann
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
package org.jpmml.evaluator;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RegressionModelEvaluatorTest {

	@Test
	public void calculateCategoryProbabilities(){
		Map<String, Double> values = new LinkedHashMap<>();
		values.put("loud", 0.2d);
		values.put("louder", 0.7d);
		values.put("insane", 1d);

		RegressionModelEvaluator.calculateCategoryProbabilities(values, Arrays.asList("loud", "louder", "insane"));

		assertEquals((Double)(0.2d - 0d), values.get("loud"));
		assertEquals((Double)(0.7d - 0.2d), values.get("louder"));
		assertEquals((Double)(1d - 0.7d), values.get("insane"));
	}
}