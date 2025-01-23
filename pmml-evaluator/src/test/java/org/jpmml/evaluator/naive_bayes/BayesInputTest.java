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
package org.jpmml.evaluator.naive_bayes;

import java.util.List;

import org.dmg.pmml.naive_bayes.BayesInput;
import org.dmg.pmml.naive_bayes.NaiveBayesModel;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BayesInputTest extends ModelEvaluatorTest {

	@Test
	public void getBayesInputs() throws Exception {
		NaiveBayesModelEvaluator evaluator = (NaiveBayesModelEvaluator)createModelEvaluator();

		checkBayesInputs(1, evaluator);

		evaluator = (NaiveBayesModelEvaluator)createModelEvaluator(new ExtractBayesInputsTransformer());

		checkBayesInputs(2, evaluator);
	}

	static
	private void checkBayesInputs(int expectedSize, NaiveBayesModelEvaluator evaluator){
		NaiveBayesModel naiveBayesModel = evaluator.getModel();

		List<BayesInput> bayesInputs = (naiveBayesModel.requireBayesInputs()).requireBayesInputs();

		assertEquals(expectedSize, bayesInputs.size());
	}
}