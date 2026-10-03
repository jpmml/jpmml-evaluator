/*
 * Copyright (c) 2026 Villu Ruusmann
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segment;
import org.dmg.pmml.mining.Segmentation;
import org.jpmml.model.InvalidAttributeException;
import org.jpmml.model.InvalidElementException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalDataFieldTest extends ModelEvaluatorTest {

	@Test
	public void evaluateTopLevel() throws Exception {
		PMMLTransformer<RuntimeException> modelTransformer = new PMMLTransformer<>(){

			@Override
			public PMML apply(PMML pmml){
				List<Model> models = pmml.getModels();

				MiningModel miningModel = (MiningModel)models.get(0);

				Segmentation segmentation = miningModel.requireSegmentation();

				List<Segment> segments = segmentation.requireSegments();

				Segment segment = segments.get(0);

				models.set(0, segment.requireModel());

				return pmml;
			}
		};

		ModelEvaluator<?> evaluator = createModelEvaluator(modelTransformer);

		List<InputField> inputFields = evaluator.getInputFields();

		assertTrue(inputFields.isEmpty());

		checkResultFields(Arrays.asList("y1", "y2"), Arrays.asList("margin(y1)", "margin(y2)"), evaluator);

		List<TargetField> targetFields = evaluator.getTargetFields();

		for(TargetField targetField : targetFields){
			DataField dataField = targetField.getField();

			assertTrue(dataField instanceof LocalDataField);
			assertFalse(targetField.isSynthetic());

			assertEquals(OpType.CONTINUOUS, targetField.getOpType());
			assertEquals(DataType.DOUBLE, targetField.getDataType());
		}

		Map<String, ?> results = evaluator.evaluate(Collections.emptyMap());

		assertEquals(1d + 10d, decode(results.get("y1")));
		assertEquals(2d + 20d, decode(results.get("y2")));

		PMMLTransformer<RuntimeException> singleTargetTransformer = new PMMLTransformer<>(){

			@Override
			public PMML apply(PMML pmml){
				List<Model> models = pmml.getModels();

				Model model = models.get(0);

				MiningSchema miningSchema = model.requireMiningSchema();

				List<MiningField> miningFields = miningSchema.getMiningFields();

				miningFields.remove(1);

				return pmml;
			}
		};

		evaluator = createModelEvaluator(modelTransformer, singleTargetTransformer);

		assertThrows(InvalidElementException.class, evaluator::getTargetFields);

		PMMLTransformer<RuntimeException> mixedTargetTransformer = new PMMLTransformer<>(){

			@Override
			public PMML apply(PMML pmml){
				DataField globalDataField = new DataField("y1", OpType.CONTINUOUS, DataType.DOUBLE);

				DataDictionary dataDictionary = pmml.requireDataDictionary();

				dataDictionary.addDataFields(globalDataField);

				return pmml;
			}
		};

		evaluator = createModelEvaluator(modelTransformer, mixedTargetTransformer);

		assertThrows(InvalidElementException.class, evaluator::getTargetFields);

		PMMLTransformer<RuntimeException> residualTransformer = new PMMLTransformer<>(){

			@Override
			public PMML apply(PMML pmml){
				List<Model> models = pmml.getModels();

				Model model = models.get(0);

				OutputField residualField = new OutputField("residual", OpType.CONTINUOUS, DataType.DOUBLE)
					.setResultFeature(ResultFeature.RESIDUAL)
					.setTargetField("y1");

				Output output = model.getOutput();

				output.addOutputFields(residualField);

				return pmml;
			}
		};

		evaluator = createModelEvaluator(modelTransformer, residualTransformer);

		assertThrows(InvalidAttributeException.class, evaluator::getInputFields);
	}

	@Test
	public void evaluateNested() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		List<InputField> inputFields = evaluator.getInputFields();

		assertTrue(inputFields.isEmpty());

		checkResultFields(Arrays.asList("y"), Collections.emptyList(), evaluator);

		Map<String, ?> results = evaluator.evaluate(Collections.emptyMap());

		assertEquals(Collections.singleton("y"), results.keySet());

		assertEquals((1d + 10d) * 1d + (2d + 20d) * 10d, results.get("y"));

		PMMLTransformer<RuntimeException> localTargetTransformer = new PMMLTransformer<>(){

			@Override
			public PMML apply(PMML pmml){
				List<Model> models = pmml.getModels();

				Model model = models.get(0);

				MiningField firstMiningField = new MiningField("y1")
					.setUsageType(MiningField.UsageType.TARGET);

				MiningField secondMiningField = new MiningField("y2")
					.setUsageType(MiningField.UsageType.TARGET);

				MiningSchema miningSchema = new MiningSchema()
					.addMiningFields(firstMiningField, secondMiningField);

				model.setMiningSchema(miningSchema);

				return pmml;
			}
		};

		evaluator = createModelEvaluator(localTargetTransformer);

		assertThrows(MissingFieldException.class, evaluator::getTargetFields);
	}
}