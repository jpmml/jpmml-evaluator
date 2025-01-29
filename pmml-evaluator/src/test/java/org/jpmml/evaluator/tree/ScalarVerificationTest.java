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

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

import org.dmg.pmml.PMML;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.PMMLTransformer;
import org.jpmml.model.MarkupException;
import org.jpmml.model.visitors.ActiveFieldFinder;
import org.jpmml.model.visitors.FieldRenamer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScalarVerificationTest extends ModelEvaluatorTest {

	@Test
	public void verify() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		evaluator.verify();

		Map<String, ?> arguments = createArguments("sepal length", 5.1, "sepal width", 3.5, "petal length", 1.4, "petal width", 0.2);

		Map<String, ?> results = evaluator.evaluate(arguments);

		assertEquals(1 + 4, results.size());

		assertEquals("setosa", decode(results.get("species")));

		assertEquals("setosa", results.get("predicted species"));

		assertEquals(1.0, results.get("probability setosa"));
		assertEquals(0.0, results.get("probability versicolor"));
		assertEquals(0.0, results.get("probability virginica"));
	}

	@Test
	public void verifyMapped() throws Exception {
		PMMLTransformer<?> fieldTransformer = new PMMLTransformer<MarkupException>(){

			@Override
			public PMML apply(PMML pmml){
				return updateFields(pmml);
			}
		};

		ModelEvaluator<?> evaluator = createModelEvaluator(fieldTransformer);

		evaluator.verify();

		Map<String, ?> arguments = createArguments("SEPAL.LENGTH", 5.1, "SEPAL.WIDTH", 3.5, "PETAL.LENGTH", 1.4, "PETAL.WIDTH", 0.2);

		Map<String, ?> results = evaluator.evaluate(arguments);

		assertEquals(1 + 4, results.size());

		assertEquals("setosa", decode(results.get("SPECIES")));

		assertEquals("setosa", results.get("PREDICTED(SPECIES)"));

		assertEquals(1.0, results.get("PROBABILITY(SETOSA)"));
		assertEquals(0.0, results.get("PROBABILITY(VERSICOLOR)"));
		assertEquals(0.0, results.get("PROBABILITY(VIRGINICA)"));
	}

	static
	private PMML updateFields(PMML pmml){
		Set<String> activeFieldNames = ActiveFieldFinder.getFieldNames(pmml);

		Map<String, String> mappings = new AbstractMap<>(){

			@Override
			public String get(Object key){
				String name = (String)key;

				if(activeFieldNames.contains(name)){
					return updateInputName(name);
				} else

				{
					return updateTargetName(name);
				}
			}

			@Override
			public Set<Map.Entry<String, String>> entrySet(){
				throw new UnsupportedOperationException();
			}
		};

		FieldRenamer fieldRenamer = new FieldRenamer(mappings);
		fieldRenamer.applyTo(pmml);

		return pmml;
	}

	static
	private String updateInputName(String name){
		String result = name.toUpperCase();

		return result.replace(' ', '.');
	}

	static
	private String updateTargetName(String name){
		String result = name.toUpperCase();

		int index = result.indexOf(' ');
		if(index > -1){
			result = result.substring(0, index) + "(" + result.substring(index + 1) +  ")";
		}

		return result;
	}
}