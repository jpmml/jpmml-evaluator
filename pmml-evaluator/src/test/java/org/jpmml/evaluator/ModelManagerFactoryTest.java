/*
 * Copyright (c) 2020 Villu Ruusmann
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

import java.util.Arrays;
import java.util.List;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.Header;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.dmg.pmml.tree.TreeModel;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ModelManagerFactoryTest {

	@Test
	public void newModelManager(){
		ModelManagerFactory<TreeModelManager> modelManagerFactory = new ModelManagerFactory<TreeModelManager>(TreeModelManager.class){

			@Override
			public List<Class<? extends TreeModelManager>> getServiceProviderClasses(Class<? extends Model> modelClazz){
				return Arrays.asList(RegressorManager.class, ClassifierManager.class);
			}
		};

		TreeModel treeModel = new TreeModel()
			.setMiningFunction(null)
			.setMiningSchema(new MiningSchema());

		PMML pmml = new PMML()
			.setHeader(new Header())
			.setDataDictionary(new DataDictionary())
			.addModels(treeModel);

		ModelManager<?> modelManager;

		try {
			modelManager = modelManagerFactory.newModelManager(pmml, treeModel);

			fail();
		} catch(InvalidMarkupException ime){
			// Ignored
		}

		treeModel.setMiningFunction(MiningFunction.REGRESSION);

		modelManager = modelManagerFactory.newModelManager(pmml, treeModel);

		assertTrue(modelManager instanceof RegressorManager);

		treeModel.setMiningFunction(MiningFunction.CLASSIFICATION);

		modelManager = modelManagerFactory.newModelManager(pmml, treeModel);

		assertTrue(modelManager instanceof ClassifierManager);
	}

	static
	abstract
	public class TreeModelManager extends ModelManager<TreeModel> {

		private TreeModelManager(){
		}

		public TreeModelManager(PMML pmml, TreeModel treeModel){
			super(pmml, treeModel);
		}
	}

	static
	public class RegressorManager extends TreeModelManager {

		private RegressorManager(){
		}

		public RegressorManager(PMML pmml, TreeModel treeModel){
			super(pmml, treeModel);

			MiningFunction miningFunction = treeModel.getMiningFunction();
			switch(miningFunction){
				case REGRESSION:
					break;
				default:
					throw new UnsupportedAttributeException(treeModel, miningFunction);
			}
		}
	}

	static
	public class ClassifierManager extends TreeModelManager {

		public ClassifierManager(PMML pmml, TreeModel treeModel){
			super(pmml, treeModel);

			MiningFunction miningFunction = treeModel.getMiningFunction();
			switch(miningFunction){
				case CLASSIFICATION:
					break;
				default:
					throw new UnsupportedAttributeException(treeModel, miningFunction);
			}
		}
	}
}