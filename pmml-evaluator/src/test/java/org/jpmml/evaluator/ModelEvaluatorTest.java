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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.model.PMMLUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

abstract
public class ModelEvaluatorTest {

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

		return modelEvaluatorFactory.newModelEvaluator(pmml);
	}

	static
	public InputStream getInputStream(Class<? extends ModelEvaluatorTest> clazz){
		String name = clazz.getName();

		if(name.startsWith("org.jpmml.evaluator.")){
			name = name.substring("org.jpmml.evaluator.".length());
		}

		return clazz.getResourceAsStream("/pmml/" + name.replace('.', '/') + ".pmml");
	}

	static
	public Map<FieldName, ?> createArguments(Object... objects){
		Map<FieldName, Object> result = new HashMap<>();

		if(objects.length % 2 != 0){
			throw new IllegalArgumentException();
		}

		for(int i = 0; i < objects.length / 2; i++){
			Object key = objects[i * 2];
			Object value = objects[i * 2 + 1];

			result.put(toFieldName(key), value);
		}

		return result;
	}

	static
	public FieldName toFieldName(Object object){

		if(object instanceof String){
			String string = (String)object;

			return FieldName.create(string);
		}

		return (FieldName)object;
	}

	static
	public Object getTarget(Map<FieldName, ?> result, Object name){
		Object value = result.get(toFieldName(name));

		return EvaluatorUtil.decode(value);
	}

	static
	public Object getOutput(Map<FieldName, ?> result, Object name){
		Object value = result.get(toFieldName(name));

		return value;
	}

	static
	public void checkResultFields(List<?> targetNames, List<?> outputNames, Evaluator evaluator){
		Function<Object, FieldName> function = new Function<Object, FieldName>(){

			@Override
			public FieldName apply(Object object){
				return toFieldName(object);
			}
		};

		List<TargetField> targetFields = evaluator.getTargetFields();
		List<OutputField> outputFields = evaluator.getOutputFields();

		assertEquals(Lists.transform(targetNames, function), EvaluatorUtil.getNames(targetFields));
		assertEquals(Lists.transform(outputNames, function), EvaluatorUtil.getNames(outputFields));
	}
}