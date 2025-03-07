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
package org.jpmml.evaluator.association;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

abstract
public class AssociationSchemaTest extends ModelEvaluatorTest {

	abstract
	public Map<String, ?> createItemArguments(List<String> items);

	@Test
	public void evaluate() throws Exception {
		evaluate(Arrays.asList("Cracker", "Coke"), Arrays.asList("1", "3"), Arrays.asList("1"), Arrays.asList("3"));
		evaluate(Arrays.asList("Cracker", "Water"), Arrays.asList("1", "2", "3", "4", "5"), Arrays.asList("3", "4", "5"), Arrays.asList("1", "2"));
		evaluate(Arrays.asList("Water", "Coke"), Arrays.asList("2", "5"), Arrays.asList("2", "5"), Arrays.<String>asList());
		evaluate(Arrays.asList("Cracker", "Water", "Coke"), Arrays.asList("1", "2", "3", "4", "5"), Arrays.asList("4", "5"), Arrays.asList("1", "2", "3"));
		evaluate(Arrays.asList("Cracker", "Water", "Banana", "Apple"), Arrays.asList("1", "2", "3", "4", "5"), Arrays.asList("3", "4", "5"), Arrays.asList("1", "2"));
	}

	private void evaluate(List<String> items, List<String> recommendations, List<String> exclusiveRecommendations, List<String> ruleAssociations) throws Exception {
		Evaluator evaluator = createModelEvaluator();

		checkTargetFields(Collections.emptyList(), evaluator);

		Map<String, ?> arguments = createItemArguments(items);

		Map<String, ?> results = evaluator.evaluate(arguments);

		assertEquals(recommendations, results.get("Recommendation"));
		assertEquals(exclusiveRecommendations, results.get("Exclusive_Recommendation"));
		assertEquals(ruleAssociations, results.get("Rule_Association"));

		assertEquals(Iterables.getFirst(recommendations, null), results.get("Top Recommendation"));
		assertEquals(Iterables.getFirst(exclusiveRecommendations, null), results.get("Top Exclusive_Recommendation"));
		assertEquals(Iterables.getFirst(ruleAssociations, null), results.get("Top Rule_Association"));
	}
}