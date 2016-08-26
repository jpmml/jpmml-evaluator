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

import java.io.InputStream;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.model.PMMLUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

abstract
public class ModelEvaluatorTest extends PMMLManagerTest {

	public ModelEvaluator<?> createModelEvaluator() throws Exception {
		return createModelEvaluator(getClass());
	}

	public ModelEvaluator<?> createModelEvaluator(ModelEvaluatorFactory modelEvaluatorFactory) throws Exception {
		return createModelEvaluator(getClass(), modelEvaluatorFactory);
	}

	static
	public ModelEvaluator<?> createModelEvaluator(Class<? extends ModelEvaluatorTest> clazz) throws Exception {
		return createModelEvaluator(clazz, ModelEvaluatorFactory.newInstance());
	}

	static
	public ModelEvaluator<?> createModelEvaluator(Class<? extends ModelEvaluatorTest> clazz, ModelEvaluatorFactory modelEvaluatorFactory) throws Exception {

		try(InputStream is = getInputStream(clazz)){
			return createModelEvaluator(is, modelEvaluatorFactory);
		}
	}

	static
	public ModelEvaluator<?> createModelEvaluator(InputStream is, ModelEvaluatorFactory modelEvaluatorFactory) throws Exception {
		PMML pmml = PMMLUtil.unmarshal(is);

		assertNull(pmml.getLocator());

		ModelEvaluator<?> modelEvaluator = modelEvaluatorFactory.newModelManager(pmml);

		return modelEvaluator;
	}

	static
	public void checkResultFields(List<?> targetNames, List<?> outputNames, Evaluator evaluator){
		Function<Object, FieldName> function = new Function<Object, FieldName>(){

			@Override
			public FieldName apply(Object object){
				return toFieldName(object);
			}
		};

		List<TargetField> targetFields = EvaluatorUtil.getTargetFields(evaluator);
		List<OutputField> outputFields = EvaluatorUtil.getOutputFields(evaluator);

		assertEquals(Lists.transform(targetNames, function), EvaluatorUtil.getNames(targetFields));
		assertEquals(Lists.transform(outputNames, function), EvaluatorUtil.getNames(outputFields));
	}
}