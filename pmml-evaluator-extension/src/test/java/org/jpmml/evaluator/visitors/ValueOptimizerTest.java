/*
 * Copyright (c) 2019 Villu Ruusmann
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
package org.jpmml.evaluator.visitors;

import com.google.common.collect.ImmutableSet;
import org.dmg.pmml.Array;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Header;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicate;
import org.dmg.pmml.TransformationDictionary;
import org.dmg.pmml.Value;
import org.dmg.pmml.Version;
import org.dmg.pmml.regression.CategoricalPredictor;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.regression.RegressionTable;
import org.dmg.pmml.tree.BranchNode;
import org.dmg.pmml.tree.LeafNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.RichComplexArray;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValueOptimizerTest {

	@Test
	public void parseRegressionModel(){
		DataField dataField = new DataField(FieldName.create("x1"), OpType.CATEGORICAL, DataType.STRING)
			.addValues(new Value("false"), new Value("true"));

		DataDictionary dataDictionary = new DataDictionary()
			.addDataFields(dataField);

		CategoricalPredictor falseTerm = new CategoricalPredictor(dataField.getName(), "false", -1d);
		CategoricalPredictor trueTerm = new CategoricalPredictor(dataField.getName(), "true", 1d);

		RegressionTable regressionTable = new RegressionTable()
			.addCategoricalPredictors(falseTerm, trueTerm);

		MiningField miningField = new MiningField(dataField.getName());

		MiningSchema miningSchema = new MiningSchema()
			.addMiningFields(miningField);

		RegressionModel regressionModel = new RegressionModel(MiningFunction.REGRESSION, miningSchema, null)
			.addRegressionTables(regressionTable);

		PMML pmml = new PMML(Version.PMML_4_3.getVersion(), new Header(), dataDictionary)
			.addModels(regressionModel);

		ValueOptimizer optimizer = new ValueOptimizer(ValueOptimizer.Mode.STRICT);
		optimizer.applyTo(pmml);

		assertEquals("false", falseTerm.getValue());
		assertEquals("true", trueTerm.getValue());

		dataField.setDataType(DataType.BOOLEAN);

		optimizer.applyTo(pmml);

		assertEquals(Boolean.FALSE, falseTerm.getValue());
		assertEquals(Boolean.TRUE, trueTerm.getValue());
	}

	@Test
	public void parseTreeModel(){
		DataField dataField = new DataField(FieldName.create("x1"), OpType.CATEGORICAL, DataType.STRING);

		DataDictionary dataDictionary = new DataDictionary()
			.addDataFields(dataField);

		NormDiscrete normDiscrete = new NormDiscrete(dataField.getName(), "1");

		DerivedField derivedField = new DerivedField(OpType.CATEGORICAL, DataType.STRING)
			.setName(FieldName.create("global(" + dataField.getName() + ")"))
			.setExpression(normDiscrete);

		TransformationDictionary transformationDictionary = new TransformationDictionary()
			.addDerivedFields(derivedField);

		SimpleSetPredicate simpleSetPredicate = new SimpleSetPredicate(dataField.getName(), SimpleSetPredicate.BooleanOperator.IS_IN, new Array(Array.Type.STRING, "0 1"));

		Node parent = new BranchNode()
			.setScore("0")
			.setPredicate(simpleSetPredicate);

		SimplePredicate simplePredicate = new SimplePredicate(derivedField.getName(), SimplePredicate.Operator.EQUAL)
			.setValue("1");

		Node child = new LeafNode()
			.setScore("1")
			.setPredicate(simplePredicate);

		parent.addNodes(child);

		MiningField miningField = new MiningField(dataField.getName());

		MiningSchema miningSchema = new MiningSchema()
			.addMiningFields(miningField);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, miningSchema, null)
			.setNode(parent);

		PMML pmml = new PMML(Version.PMML_4_3.getVersion(), new Header(), dataDictionary)
			.setTransformationDictionary(transformationDictionary)
			.addModels(treeModel);

		ValueOptimizer optimizer = new ValueOptimizer(ValueOptimizer.Mode.STRICT);
		optimizer.applyTo(pmml);

		assertEquals("1", normDiscrete.getValue());
		assertEquals("1", simplePredicate.getValue());

		Array array = simpleSetPredicate.getArray();

		assertEquals(ImmutableSet.of("0", "1"), array.getValue());

		dataField.setDataType(DataType.INTEGER);

		optimizer.applyTo(pmml);

		assertEquals(1, normDiscrete.getValue());
		assertEquals("1", simplePredicate.getValue());

		array = simpleSetPredicate.getArray();

		assertTrue(array instanceof RichComplexArray);
		assertEquals(ImmutableSet.of(0, 1), array.getValue());

		dataField.setDataType(DataType.DOUBLE);
		derivedField.setDataType(DataType.INTEGER);

		optimizer.applyTo(pmml);

		assertEquals(1.0d, normDiscrete.getValue());
		assertEquals(1, simplePredicate.getValue());

		array = simpleSetPredicate.getArray();

		assertEquals(ImmutableSet.of(0.0d, 1.0d), array.getValue());

		dataField.setDataType(DataType.BOOLEAN);
		derivedField.setDataType(DataType.DOUBLE);

		optimizer.applyTo(pmml);

		assertEquals(true, normDiscrete.getValue());
		assertEquals(1.0d, simplePredicate.getValue());

		array = simpleSetPredicate.getArray();

		assertEquals(ImmutableSet.of(false, true), array.getValue());

		derivedField.setDataType(DataType.BOOLEAN);

		optimizer.applyTo(pmml);

		assertEquals(true, normDiscrete.getValue());
		assertEquals(true, simplePredicate.getValue());
	}
}