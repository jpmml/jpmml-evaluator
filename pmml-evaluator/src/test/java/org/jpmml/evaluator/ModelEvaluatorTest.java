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

import com.google.common.collect.Lists;
import org.dmg.pmml.FieldName;
import org.jpmml.model.SerializationUtil;

import static org.junit.Assert.assertEquals;

abstract
public class ModelEvaluatorTest {

	public ModelEvaluator<?> createModelEvaluator() throws Exception {
		return createModelEvaluator(getClass());
	}

	public ModelEvaluator<?> createModelEvaluator(Class<? extends ModelEvaluatorTest> clazz) throws Exception {
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

		Configuration configuration = configurationBuilder.build();

		return createModelEvaluator(clazz, configuration);
	}

	public ModelEvaluator<?> createModelEvaluator(Configuration configuration) throws Exception {
		return createModelEvaluator(getClass(), configuration);
	}

	public ModelEvaluator<?> createModelEvaluator(Class<? extends ModelEvaluatorTest> clazz, Configuration configuration) throws Exception {

		try(InputStream is = getInputStream(clazz)){
			return createModelEvaluator(is, configuration);
		}
	}

	public ModelEvaluator<?> createModelEvaluator(InputStream is, Configuration configuration) throws Exception {
		ModelEvaluatorBuilder modelEvaluatorBuilder = createLoadingModelEvaluatorBuilder(configuration)
			.load(is);

		modelEvaluatorBuilder = SerializationUtil.clone(modelEvaluatorBuilder);

		ModelEvaluator<?> modelEvaluator = modelEvaluatorBuilder.build();

		modelEvaluator = SerializationUtil.clone(modelEvaluator);

		return modelEvaluator;
	}

	static
	private LoadingModelEvaluatorBuilder createLoadingModelEvaluatorBuilder(Configuration configuration){
		LoadingModelEvaluatorBuilder modelEvaluatorBuilder = new LoadingModelEvaluatorBuilder(){

			{
				setVisitors(new TestModelEvaluatorBattery());
				setModelEvaluatorFactory(configuration.getModelEvaluatorFactory());
				setValueFactoryFactory(configuration.getValueFactoryFactory());
				setOutputFilter(configuration.getOutputFilter());
			}

			@Override
			protected void checkSchema(ModelEvaluator<?> modelEvaluator){
			}
		};

		return modelEvaluatorBuilder;
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
	public Object getTarget(Map<FieldName, ?> results, Object name){
		Object value = results.get(toFieldName(name));

		return EvaluatorUtil.decode(value);
	}

	static
	public Object getOutput(Map<FieldName, ?> results, Object name){
		Object value = results.get(toFieldName(name));

		return value;
	}

	static
	public void checkTargetFields(List<?> targetNames, Evaluator evaluator){
		List<TargetField> targetFields = evaluator.getTargetFields();

		assertEquals(Lists.transform(targetNames, ModelEvaluatorTest::toFieldName), Lists.transform(targetFields, TargetField::getName));
	}

	static
	public void checkResultFields(List<?> targetNames, List<?> outputNames, Evaluator evaluator){
		List<TargetField> targetFields = evaluator.getTargetFields();
		List<OutputField> outputFields = evaluator.getOutputFields();

		assertEquals(Lists.transform(targetNames, ModelEvaluatorTest::toFieldName), Lists.transform(targetFields, TargetField::getName));
		assertEquals(Lists.transform(outputNames, ModelEvaluatorTest::toFieldName), Lists.transform(outputFields, OutputField::getName));
	}
}