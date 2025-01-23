/*
 * Copyright (c) 2013 KNIME.com AG, Zurich, Switzerland
 * Copyright (c) 2014 Villu Ruusmann
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
package org.jpmml.evaluator.nearest_neighbor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.jpmml.evaluator.AffinityDistribution;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ClusteringNeighborhoodTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		Map<String, ?> arguments = createArguments("marital status", "d", "dependents", 0);

		Map<String, ?> results = evaluator.evaluate(arguments);

		AffinityDistribution<?> targetValue = (AffinityDistribution<?>)results.get(evaluator.getTargetName());

		assertThrows(EvaluationException.class, () -> targetValue.getResult());

		assertNull(targetValue.getPredictionReport());

		Collection<String> categories = targetValue.getCategories();

		assertEquals(5, categories.size());

		for(String category : categories){
			assertNotNull(targetValue.getAffinity(category));
			assertNull(targetValue.getAffinityReport(category));
		}

		assertEquals(Arrays.asList("3", "1", "4"), (targetValue.getEntityIdRanking()).subList(0, 3));

		assertEquals("3", results.get("neighbor1"));
		assertEquals("1", results.get("neighbor2"));
		assertEquals("4", results.get("neighbor3"));
	}
}