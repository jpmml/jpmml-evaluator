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
package org.jpmml.evaluator.mining;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Computable;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.HasEntityId;
import org.jpmml.evaluator.HasProbability;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.tree.TreeModelEvaluator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SelectAllTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		CountingModelEvaluatorFactory evaluatorFactory = new CountingModelEvaluatorFactory();

		Evaluator evaluator = createModelEvaluator(evaluatorFactory);

		Map<FieldName, ?> arguments = createArguments("sepal_length", 5.1d, "sepal_width", 3.5d, "petal_length", 1.4d, "petal_width", 0.2d);

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		assertEquals(1, evaluatorFactory.getMiningModelCount());
		assertEquals(5, evaluatorFactory.getTreeModelCount());

		assertEquals(1, result.size());

		Collection<?> species = (Collection<?>)result.get(FieldName.create("species"));

		assertEquals(5, species.size());

		for(Object value : species){
			assertTrue((value instanceof Computable) & (value instanceof HasEntityId) & (value instanceof HasProbability));
		}

		assertEquals(Arrays.asList("setosa", "setosa", "setosa", "setosa", "versicolor"), EvaluatorUtil.decode(species));
	}

	static
	private class CountingModelEvaluatorFactory extends ModelEvaluatorFactory {

		private int miningModelCount = 0;

		private int treeModelCount = 0;


		@Override
		public ModelEvaluator<? extends Model> newModelEvaluator(PMML pmml, Model model){
			ModelEvaluator<?> modelEvaluator = super.newModelEvaluator(pmml, model);

			if(modelEvaluator instanceof MiningModelEvaluator){
				this.miningModelCount++;
			} else

			if(modelEvaluator instanceof TreeModelEvaluator){
				this.treeModelCount++;
			} else

			{
				throw new AssertionError();
			}

			return modelEvaluator;
		}

		public int getMiningModelCount(){
			return this.miningModelCount;
		}

		public int getTreeModelCount(){
			return this.treeModelCount;
		}
	}
}