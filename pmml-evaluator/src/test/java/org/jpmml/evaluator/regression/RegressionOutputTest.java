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
package org.jpmml.evaluator.regression;

import java.util.Arrays;
import java.util.Map;

import org.jpmml.evaluator.Configuration;
import org.jpmml.evaluator.ConfigurationBuilder;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.OutputFilters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegressionOutputTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

		Configuration configuration = configurationBuilder.build();

		ModelEvaluator<?> evaluator = createModelEvaluator(configuration);

		checkResultFields(Arrays.asList("result"), Arrays.asList("RawResult", "RawIntegerResult", "FinalResult", "FinalIntegerResult", "BusinessDecision"), evaluator);

		Map<String, ?> arguments = createArguments("input", 4d);

		Map<String, ?> results = evaluator.evaluate(arguments);

		assertEquals(1 + 5, results.size());

		assertEquals(8d, results.get("result"));

		assertEquals(8d, results.get("RawResult"));
		assertEquals(8, results.get("RawIntegerResult"));
		assertEquals(35d, results.get("FinalResult"));
		assertEquals(35, results.get("FinalIntegerResult"));
		assertEquals("waive", results.get("BusinessDecision"));

		configurationBuilder.setOutputFilter(OutputFilters.KEEP_FINAL_RESULTS);

		configuration = configurationBuilder.build();

		evaluator.configure(configuration);

		checkResultFields(Arrays.asList("result"), Arrays.asList("FinalResult", "FinalIntegerResult", "BusinessDecision"), evaluator);

		results = evaluator.evaluate(arguments);

		assertEquals(1 + 3, results.size());
	}
}