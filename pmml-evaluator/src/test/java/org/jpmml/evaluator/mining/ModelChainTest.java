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

import java.util.Map;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.Configuration;
import org.jpmml.evaluator.ConfigurationBuilder;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.OutputFilters;

abstract
public class ModelChainTest extends ModelEvaluatorTest {

	public Map<FieldName, ?> evaluateExample(double petalLength, double petalWidth) throws Exception {
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
			.setOutputFilter(OutputFilters.KEEP_FINAL_RESULTS);

		Configuration configuration = configurationBuilder.build();

		Evaluator evaluator = createModelEvaluator(configuration);

		Map<FieldName, ?> arguments = createArguments("petal_length", petalLength, "petal_width", petalWidth, "temperature", 0d, "cloudiness", 0d);

		return evaluator.evaluate(arguments);
	}
}