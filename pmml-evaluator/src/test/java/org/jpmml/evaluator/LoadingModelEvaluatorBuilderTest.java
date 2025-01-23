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

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import jakarta.xml.bind.JAXBException;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.Header;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.True;
import org.dmg.pmml.Version;
import org.dmg.pmml.tree.ComplexNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.java.JavaModel;
import org.jpmml.model.DirectByteArrayOutputStream;
import org.jpmml.model.PMMLUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class LoadingModelEvaluatorBuilderTest {

	@Test
	public void build(){
		LoadingModelEvaluatorBuilder modelEvaluatorBuilder = new LoadingModelEvaluatorBuilder();

		try {
			modelEvaluatorBuilder.load(createResource(null));
		} catch(Exception e){
			throw new AssertionError(e);
		}

		// Identity transformation
		PMMLTransformer<RuntimeException> transformer = new PMMLTransformer<RuntimeException>(){

			@Override
			public PMML apply(PMML pmml){
				return pmml;
			}
		};

		modelEvaluatorBuilder.transform(transformer);

		ModelEvaluator<?> modelEvaluator = modelEvaluatorBuilder.build();

		// Custom transformation, with the possibility of a checked exception to be thrown
		PMMLTransformer<TranslationException> javaTransformer = new PMMLTransformer<TranslationException>(){

			@Override
			public PMML apply(PMML pmml) throws TranslationException {
				List<Model> models = pmml.getModels();

				Number score;

				try {
					TreeModel treeModel = (TreeModel)Iterables.getOnlyElement(models);

					MiningFunction miningFunction = treeModel.requireMiningFunction();
					switch(miningFunction){
						case REGRESSION:
							break;
						default:
							throw new IllegalArgumentException();
					}

					Node root = treeModel.getNode();
					if(root.hasNodes()){
						throw new IllegalArgumentException();
					}

					Predicate predicate = root.requirePredicate();
					if(!(predicate instanceof True)){
						throw new IllegalArgumentException();
					}

					score = (Number)root.requireScore();
				} catch(Exception e){
					throw new TranslationException(e);
				}

				JavaModel javaModel = new JavaModel(){

					{
						setMiningFunction(MiningFunction.REGRESSION);
						setMiningSchema(new MiningSchema());
					}

					@Override
					public <V extends Number> Map<String, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
						Value<V> value = valueFactory.newValue(score);

						return Collections.singletonMap(Evaluator.DEFAULT_TARGET_NAME, value.getValue());
					}
				};

				PMML javaPMML = new PMML(pmml.requireVersion(), pmml.requireHeader(), pmml.requireDataDictionary())
					.addModels(javaModel);

				return javaPMML;
			}
		};

		try {
			modelEvaluatorBuilder.transform(javaTransformer);
		} catch(TranslationException te){
			throw new AssertionError(te);
		}

		ModelEvaluator<?> javaModelEvaluator = modelEvaluatorBuilder.build();

		assertNotEquals(modelEvaluator.getClass(), javaModelEvaluator.getClass());

		Map<String, ?> results = modelEvaluator.evaluate(Collections.emptyMap());
		Map<String, ?> javaResults = javaModelEvaluator.evaluate(Collections.emptyMap());

		assertEquals(results, javaResults);
	}

	static
	private InputStream createResource(String modelName) throws JAXBException {
		Node root = new ComplexNode()
			.setPredicate(True.INSTANCE)
			.setScore(1d);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, new MiningSchema(), root)
			.setModelName(modelName);

		PMML pmml = new PMML(Version.PMML_4_4.getVersion(), new Header(), new DataDictionary())
			.addModels(treeModel);

		DirectByteArrayOutputStream buffer = new DirectByteArrayOutputStream(1024);

		PMMLUtil.marshal(pmml, buffer);

		return buffer.getInputStream();
	}

	static
	private class TranslationException  extends Exception {

		private TranslationException(Throwable cause){
			super(cause);
		}
	}
}