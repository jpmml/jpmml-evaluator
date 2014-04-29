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
package org.jpmml.evaluator;

import org.jpmml.manager.*;

abstract
public class ModelEvaluatorTest extends PMMLManagerTest {

	public ModelEvaluator<?> createModelEvaluator() throws Exception {
		return createModelEvaluator(getClass());
	}

	static
	public ModelEvaluator<?> createModelEvaluator(Class<? extends ModelEvaluatorTest> clazz) throws Exception {
		PMMLManager manager = createPMMLManager(clazz);

		return (ModelEvaluator<?>)manager.getModelManager(null, ModelEvaluatorFactory.getInstance());
	}

	static
	public String getEntityId(Object object){

		if(object instanceof HasEntityId){
			HasEntityId hasEntityId = (HasEntityId)object;

			return hasEntityId.getEntityId();
		}

		return null;
	}
}