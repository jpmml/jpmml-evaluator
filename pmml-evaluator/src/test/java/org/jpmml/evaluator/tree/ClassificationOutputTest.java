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
package org.jpmml.evaluator.tree;

import java.util.Arrays;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.Configuration;
import org.jpmml.evaluator.ConfigurationBuilder;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.OutputFilters;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClassificationOutputTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

		Configuration configuration = configurationBuilder.build();

		ModelEvaluator<?> evaluator = createModelEvaluator(configuration);

		checkResultFields(Arrays.asList("result"), Arrays.asList("output_predictedValue", "output_predictedDisplayValue", "output_probability"), evaluator);

		Map<FieldName, ?> arguments = createArguments("flag", false);

		Map<FieldName, ?> results = evaluator.evaluate(arguments);

		assertEquals(1 + 3, results.size());

		assertEquals("0", getTarget(results, "result"));

		assertEquals("0", getOutput(results, "output_predictedValue"));
		assertEquals("zero", getOutput(results, "output_predictedDisplayValue"));
		assertEquals(1d, getOutput(results, "output_probability"));

		configurationBuilder.setOutputFilter(OutputFilters.KEEP_FINAL_RESULTS);

		configuration = configurationBuilder.build();

		evaluator.configure(configuration);

		checkResultFields(Arrays.asList("result"), Arrays.asList("output_predictedDisplayValue", "output_probability"), evaluator);

		results = evaluator.evaluate(arguments);

		assertEquals(1 + 2, results.size());
	}
}