/*
 * Copyright (c) 2013 Villu Ruusmann
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

import java.util.Collections;
import java.util.Map;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.False;
import org.dmg.pmml.Header;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Target;
import org.dmg.pmml.TargetValue;
import org.dmg.pmml.Targets;
import org.dmg.pmml.Version;
import org.dmg.pmml.tree.LeafNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.tree.TreeModelEvaluator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TargetUtilTest {

	@Test
	public void evaluateRegressionDefault(){
		TargetValue targetValue = new TargetValue()
			.setDefaultValue(Math.PI);

		Target target = new Target()
			.setTargetField(null)
			.addTargetValues(targetValue);

		ModelEvaluator<?> evaluator = createTreeModelEvaluator(MiningFunction.REGRESSION, MathContext.FLOAT, target);

		DataField dataField = evaluator.getDataField(target.getTargetField());

		assertEquals(OpType.CONTINUOUS, dataField.requireOpType());
		assertEquals(DataType.FLOAT, dataField.requireDataType());

		Map<String, ?> results = evaluator.evaluate(Collections.emptyMap());

		assertEquals((float)Math.PI, results.get(dataField.requireName()));

		evaluator = createTreeModelEvaluator(MiningFunction.REGRESSION, MathContext.DOUBLE, target);

		dataField = evaluator.getDefaultDataField();

		assertEquals(OpType.CONTINUOUS, dataField.requireOpType());
		assertEquals(DataType.DOUBLE, dataField.requireDataType());

		results = evaluator.evaluate(Collections.emptyMap());

		assertEquals(Math.PI, results.get(dataField.requireName()));
	}

	@Test
	public void evaluateClassificationDefault(){
		TargetValue yesValue = new TargetValue()
			.setValue("yes")
			.setPriorProbability(1d / 7d);

		TargetValue noValue = new TargetValue()
			.setValue("no")
			.setPriorProbability(1d - NumberUtil.asDouble(yesValue.requirePriorProbability()));

		Target target = new Target()
			.setTargetField(null)
			.addTargetValues(yesValue, noValue);

		ModelEvaluator<?> evaluator = createTreeModelEvaluator(MiningFunction.CLASSIFICATION, MathContext.FLOAT, target);

		DataField dataField = evaluator.getDataField(target.getTargetField());

		assertEquals(OpType.CATEGORICAL, dataField.requireOpType());
		assertEquals(DataType.STRING, dataField.requireDataType());

		Map<String, ?> results = evaluator.evaluate(Collections.emptyMap());

		HasProbability hasProbability = (HasProbability)results.get(dataField.requireName());

		assertEquals((Double)((double)(float)(1d / 7d)), hasProbability.getProbability("yes"));
		assertEquals((Double)((double)(float)(1d - (1d / 7d))), hasProbability.getProbability("no"));

		evaluator = createTreeModelEvaluator(MiningFunction.CLASSIFICATION, MathContext.DOUBLE, target);

		dataField = evaluator.getDataField(target.getTargetField());

		assertEquals(OpType.CATEGORICAL, dataField.requireOpType());
		assertEquals(DataType.STRING, dataField.requireDataType());

		results = evaluator.evaluate(Collections.emptyMap());

		hasProbability = (HasProbability)results.get(dataField.requireName());

		assertEquals(yesValue.requirePriorProbability(), hasProbability.getProbability("yes"));
		assertEquals(noValue.requirePriorProbability(), hasProbability.getProbability("no"));
	}

	@Test
	public void processValue(){
		Target target = new Target()
			.setTargetField("amount")
			.setRescaleFactor(3.14d)
			.setRescaleConstant(10d);

		FloatValue floatValue = new FloatValue((8f * 3.14f) + 10f);
		DoubleValue doubleValue = new DoubleValue((8d * 3.14d) + 10d);

		assertTrue(floatValue.floatValue() != doubleValue.floatValue());
		assertTrue(floatValue.doubleValue() != doubleValue.doubleValue());

		assertEquals(floatValue, TargetUtil.processValue(target, new FloatValue(8f)));
		assertEquals(doubleValue, TargetUtil.processValue(target, new DoubleValue(8d)));

		target.setCastInteger(Target.CastInteger.ROUND)
			.setMin(-10d)
			.setMax(10.5d);

		assertEquals(new FloatValue(35f), TargetUtil.processValue(target, new FloatValue(8f)));
		assertEquals(new DoubleValue(35d), TargetUtil.processValue(target, new DoubleValue(8d)));

		assertEquals(new FloatValue(43f), TargetUtil.processValue(target, new FloatValue(12.97f)));
		assertEquals(new DoubleValue(43d), TargetUtil.processValue(target, new DoubleValue(12.97d)));
	}

	static
	private TreeModelEvaluator createTreeModelEvaluator(MiningFunction miningFunction, MathContext mathContext, Target target){
		Node root = new LeafNode(null, False.INSTANCE);

		Targets targets = new Targets()
			.addTargets(target);

		TreeModel treeModel = new TreeModel(miningFunction, new MiningSchema(), root)
			.setSplitCharacteristic(TreeModel.SplitCharacteristic.BINARY_SPLIT)
			.setMathContext(mathContext)
			.setTargets(targets);

		PMML pmml = new PMML(Version.PMML_4_4.getVersion(), new Header(), new DataDictionary())
			.addModels(treeModel);

		ModelEvaluatorBuilder modelEvaluatorBuilder = new ModelEvaluatorBuilder(pmml);

		return (TreeModelEvaluator)modelEvaluatorBuilder.build();
	}
}