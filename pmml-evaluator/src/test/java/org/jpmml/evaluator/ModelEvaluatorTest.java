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
import org.jpmml.evaluator.visitors.ModelEvaluatorVisitorBattery;
import org.jpmml.model.SerializationUtil;
import org.jpmml.model.visitors.ArrayListTransformer;
import org.jpmml.model.visitors.VisitorBattery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

abstract
public class ModelEvaluatorTest {

	public ModelEvaluator<?> createModelEvaluator(PMMLTransformer<?>... transformers) throws Exception {
		return createModelEvaluator(getClass(), transformers);
	}

	public ModelEvaluator<?> createModelEvaluator(Class<? extends ModelEvaluatorTest> clazz, PMMLTransformer<?>... transformers) throws Exception {
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

		Configuration configuration = configurationBuilder.build();

		return createModelEvaluator(clazz, configuration, transformers);
	}

	public ModelEvaluator<?> createModelEvaluator(Configuration configuration, PMMLTransformer<?>... transformers) throws Exception {
		return createModelEvaluator(getClass(), configuration, transformers);
	}

	public ModelEvaluator<?> createModelEvaluator(Class<? extends ModelEvaluatorTest> clazz, Configuration configuration, PMMLTransformer<?>... transformers) throws Exception {

		try(InputStream is = getInputStream(clazz)){
			return createModelEvaluator(is, configuration, transformers);
		}
	}

	public ModelEvaluator<?> createModelEvaluator(InputStream is, Configuration configuration, PMMLTransformer<?>... transformers) throws Exception {
		LoadingModelEvaluatorBuilder modelEvaluatorBuilder = createLoadingModelEvaluatorBuilder(configuration)
			.load(is);

		for(PMMLTransformer<?> transformer : transformers){
			modelEvaluatorBuilder = modelEvaluatorBuilder.transform(transformer);
		}

		modelEvaluatorBuilder = SerializationUtil.clone(modelEvaluatorBuilder);

		ModelEvaluator<?> modelEvaluator = modelEvaluatorBuilder.build();

		modelEvaluator = SerializationUtil.clone(modelEvaluator);

		return modelEvaluator;
	}

	static
	private LoadingModelEvaluatorBuilder createLoadingModelEvaluatorBuilder(Configuration configuration){
		VisitorBattery visitorBattery = new ModelEvaluatorVisitorBattery(){

			{
				// Keep element lists mutable
				remove(ArrayListTransformer.class);
			}
		};

		ModelEvaluatorBuilder modelEvaluatorBuilder = new LoadingModelEvaluatorBuilder()
			.setVisitors(visitorBattery)
			.setModelEvaluatorFactory(configuration.getModelEvaluatorFactory())
			.setValueFactoryFactory(configuration.getValueFactoryFactory())
			.setOutputFilter(configuration.getOutputFilter())
			.setCheckSchema(false);

		return (LoadingModelEvaluatorBuilder)modelEvaluatorBuilder;
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
	public Map<String, ?> createArguments(Object... objects){
		Map<String, Object> result = new HashMap<>();

		if(objects.length % 2 != 0){
			throw new IllegalArgumentException();
		}

		for(int i = 0; i < objects.length / 2; i++){
			Object key = objects[i * 2];
			Object value = objects[i * 2 + 1];

			result.put((String)key, value);
		}

		return result;
	}

	static
	public Object decode(Object value){

		if(value != null){
			assertTrue(value instanceof Computable);
		}

		return EvaluatorUtil.decode(value);
	}

	static
	public void checkTargetFields(List<String> targetNames, Evaluator evaluator){
		List<TargetField> targetFields = evaluator.getTargetFields();

		assertEquals(targetNames, Lists.transform(targetFields, TargetField::getName));
	}

	static
	public void checkResultFields(List<String> targetNames, List<String> outputNames, Evaluator evaluator){
		List<TargetField> targetFields = evaluator.getTargetFields();
		List<OutputField> outputFields = evaluator.getOutputFields();

		assertEquals(targetNames, Lists.transform(targetFields, TargetField::getName));
		assertEquals(outputNames, Lists.transform(outputFields, OutputField::getName));
	}
}