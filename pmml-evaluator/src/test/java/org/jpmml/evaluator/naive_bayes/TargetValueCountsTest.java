/*
 * Copyright (c) 2013 KNIME.com AG, Zurich, Switzerland
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
package org.jpmml.evaluator.naive_bayes;

import java.util.Map;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TargetValueCountsTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		NaiveBayesModelEvaluator evaluator = (NaiveBayesModelEvaluator)createModelEvaluator();

		Map<FieldName, ?> arguments = createArguments("age of individual", 24, "gender", "male", "no of claims", "2", "domicile", null, "age of car", 1d);

		Map<FieldName, Map<String, Double>> fieldCountSums = evaluator.getFieldCountSums();

		Map<String, Double> countSums = fieldCountSums.get(FieldName.create("gender"));

		assertEquals((Double)8598d, countSums.get("100"));
		assertEquals((Double)2533d, countSums.get("500"));
		assertEquals((Double)1522d, countSums.get("1000"));
		assertEquals((Double)697d, countSums.get("5000"));
		assertEquals((Double)90d, countSums.get("10000"));

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		Classification targetValue = (Classification)result.get(evaluator.getTargetFieldName());

		double l0 = 8723d * 0.001d * 4273d / 8598d * 225d / 8561d * 830d / 8008d;
		double l1 = 2557d * probability(24.936, 0.516, 24) * 1321d / 2533d * 10d / 2436d * 182d / 2266d;
		double l2 = 1530d * probability(24.588, 0.635, 24) * 780d / 1522d * 9d / 1496d * 51d / 1191d;
		double l3 = 709d * probability(24.428, 0.379, 24) * 405d / 697d * 0.001d * 26d / 699d;
		double l4 = 100d * probability(24.770, 0.314, 24) * 42d / 90d * 10d / 98d * 6d / 87d;

		double denominator = (l0 + l1 + l2 + l3 + l4);

		assertEquals(l0 / denominator, targetValue.get("100"), 1e-8);
		assertEquals(l1 / denominator, targetValue.get("500"), 1e-8);
		assertEquals(l2 / denominator, targetValue.get("1000"), 1e-8);
		assertEquals(l3 / denominator, targetValue.get("5000"), 1e-8);
		assertEquals(l4 / denominator, targetValue.get("10000"), 1e-8);
	}

	static
	private double probability(double mean, double variance, double x){
		NormalDistribution distribution = new NormalDistribution(mean, Math.sqrt(variance));

		return distribution.density(x);
	}
}