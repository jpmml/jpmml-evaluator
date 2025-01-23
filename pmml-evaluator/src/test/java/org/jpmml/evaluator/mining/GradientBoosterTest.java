/*
 * Copyright (c) 2022 Villu Ruusmann
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
import java.util.Collections;
import java.util.Map;

import org.dmg.pmml.mining.Segmentation;
import org.jpmml.evaluator.MissingFieldValueException;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class GradientBoosterTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		checkResultFields(Arrays.asList("y"), Arrays.asList("probability(event)", "probability(no event)"), evaluator);

		Map<String, ?> arguments = Collections.emptyMap();

		SegmentationResult results;

		try {
			results = (SegmentationResult)evaluator.evaluate(arguments);

			fail();
		} catch(MissingFieldValueException mfve){
			// Ignored
		}

		evaluator = createModelEvaluator(new MissingPredictionTreatmentTransformer(Segmentation.MissingPredictionTreatment.RETURN_MISSING));

		results = (SegmentationResult)evaluator.evaluate(arguments);

		assertTrue(results.containsKey("y"));
		assertNull(results.get("y"));

		assertNull(results.getSegmentResults());

		arguments = createArguments("x", 1d);

		results = (SegmentationResult)evaluator.evaluate(arguments);

		assertNotNull(results.get("y"));

		try {
			results.getResults(Arrays.asList("0"));

			fail();
		} catch(IllegalArgumentException iae){
			// Ignored
		}

		SegmentationResult regressorResults = (SegmentationResult)results.getResults(Arrays.asList("1"));

		assertSame(results.getResults(Arrays.asList("1", "1")), regressorResults.getResults(Arrays.asList("1")));
		assertSame(results.getResults(Arrays.asList("1", "2")), regressorResults.getResults(Arrays.asList("2")));
		assertSame(results.getResults(Arrays.asList("1", "3")), regressorResults.getResults(Arrays.asList("3")));

		Map<String, ?> classifierResults = results.getResults(Arrays.asList("2"));

		assertEquals(results.getResults(), classifierResults);
		assertNotSame(results.getResults(), classifierResults);
	}
}