/*
 * Copyright (c) 2018 Villu Ruusmann
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
package org.jpmml.evaluator;

import org.dmg.pmml.False;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.PMML;
import org.dmg.pmml.tree.LeafNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.model.SerializationUtil;
import org.junit.Test;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class ModelEvaluatorBuilderTest {

	@Test
	public void nativeClone(){
		ModelEvaluatorBuilder modelEvaluatorBuilder = createModelEvaluatorBuilder();

		ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
		ValueFactoryFactory valueFactoryFactory = ValueFactoryFactory.newInstance();

		modelEvaluatorBuilder
			.setModelEvaluatorFactory(modelEvaluatorFactory)
			.setValueFactoryFactory(valueFactoryFactory);

		ModelEvaluatorBuilder clonedModelEvaluatorBuilder = modelEvaluatorBuilder.clone();

		assertNotSame(modelEvaluatorBuilder, clonedModelEvaluatorBuilder);
		assertNotSame(modelEvaluatorBuilder.getConfigurationBuilder(), clonedModelEvaluatorBuilder.getConfigurationBuilder());

		assertSame(modelEvaluatorFactory, clonedModelEvaluatorBuilder.getModelEvaluatorFactory());
		assertSame(valueFactoryFactory, clonedModelEvaluatorBuilder.getValueFactoryFactory());
	}

	@Test
	public void serializationClone() throws Exception {
		ModelEvaluatorBuilder modelEvaluatorBuilder = createModelEvaluatorBuilder();

		ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
		ValueFactoryFactory valueFactoryFactory = ValueFactoryFactory.newInstance();

		modelEvaluatorBuilder
			.setModelEvaluatorFactory(modelEvaluatorFactory)
			.setValueFactoryFactory(valueFactoryFactory);

		ModelEvaluatorBuilder clonedModelEvaluatorBuilder = SerializationUtil.clone(modelEvaluatorBuilder);

		assertNotSame(modelEvaluatorBuilder, clonedModelEvaluatorBuilder);

		assertNotSame(modelEvaluatorFactory, clonedModelEvaluatorBuilder.getModelEvaluatorFactory());
		assertNotSame(valueFactoryFactory, clonedModelEvaluatorBuilder.getValueFactoryFactory());
	}

	static
	private ModelEvaluatorBuilder createModelEvaluatorBuilder(){
		Node root = new LeafNode(null, False.INSTANCE);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, new MiningSchema(), root);

		PMML pmml = new PMML()
			.addModels(treeModel);

		return new ModelEvaluatorBuilder(pmml, treeModel);
	}
}