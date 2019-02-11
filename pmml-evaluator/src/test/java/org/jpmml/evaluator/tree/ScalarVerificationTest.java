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

import java.lang.reflect.Method;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.InputMapper;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.ResultMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScalarVerificationTest extends ModelEvaluatorTest {

	@Test
	public void verify() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		evaluator.verify();

		Map<FieldName, ?> arguments = createArguments("sepal length", 5.1, "sepal width", 3.5, "petal length", 1.4, "petal width", 0.2);

		Map<FieldName, ?> results = evaluator.evaluate(arguments);

		assertEquals(1 + 4, results.size());

		assertEquals("setosa", getTarget(results, "species"));

		assertEquals("setosa", getOutput(results, "predicted species"));

		assertEquals(1.0, getOutput(results, "probability setosa"));
		assertEquals(0.0, getOutput(results, "probability versicolor"));
		assertEquals(0.0, getOutput(results, "probability virginica"));
	}

	@Test
	public void verifyMapped() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		InputMapper inputMapper = new InputMapper(){

			@Override
			public FieldName apply(FieldName name){
				String value = name.getValue();

				value = (value.toUpperCase()).replace(' ', '.');

				return FieldName.create(value);
			}
		};

		Method inputMapperSetter = ModelEvaluator.class.getDeclaredMethod("setInputMapper", InputMapper.class);
		if(!inputMapperSetter.isAccessible()){
			inputMapperSetter.setAccessible(true);
		}

		inputMapperSetter.invoke(evaluator, inputMapper);

		ResultMapper resultMapper = new ResultMapper(){

			@Override
			public FieldName apply(FieldName name){
				String value = name.getValue();

				value = value.toUpperCase();

				int index = value.indexOf(' ');
				if(index > -1){
					value = value.substring(0, index) + "(" + value.substring(index + 1) +  ")";
				}

				return FieldName.create(value);
			}
		};

		Method resultMapperSetter = ModelEvaluator.class.getDeclaredMethod("setResultMapper", ResultMapper.class);
		if(!resultMapperSetter.isAccessible()){
			resultMapperSetter.setAccessible(true);
		}

		resultMapperSetter.invoke(evaluator, resultMapper);

		evaluator.verify();

		Map<FieldName, ?> arguments = createArguments("SEPAL.LENGTH", 5.1, "SEPAL.WIDTH", 3.5, "PETAL.LENGTH", 1.4, "PETAL.WIDTH", 0.2);

		Map<FieldName, ?> results = evaluator.evaluate(arguments);

		assertEquals(1 + 4, results.size());

		assertEquals("setosa", getTarget(results, "SPECIES"));

		assertEquals("setosa", getOutput(results, "PREDICTED(SPECIES)"));

		assertEquals(1.0, getOutput(results, "PROBABILITY(SETOSA)"));
		assertEquals(0.0, getOutput(results, "PROBABILITY(VERSICOLOR)"));
		assertEquals(0.0, getOutput(results, "PROBABILITY(VIRGINICA)"));
	}
}