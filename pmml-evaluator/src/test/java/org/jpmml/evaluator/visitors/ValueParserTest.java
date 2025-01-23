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

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.dmg.pmml.Array;
import org.dmg.pmml.Cell;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.FieldColumnPair;
import org.dmg.pmml.Header;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Row;
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
import org.jpmml.evaluator.InlineTableUtil;
import org.jpmml.evaluator.RichComplexArray;
import org.jpmml.model.cells.InputCell;
import org.jpmml.model.cells.OutputCell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValueParserTest {

	@Test
	public void parseRegressionModel(){
		Value falseValue = new Value("false");
		Value trueValue = new Value("true");
		Value invalidValue = new Value("N/A");

		DataField dataField = new DataField("x1", OpType.CATEGORICAL, DataType.STRING)
			.addValues(falseValue, trueValue, invalidValue);

		DataDictionary dataDictionary = new DataDictionary()
			.addDataFields(dataField);

		Cell inputCell = new InputCell()
			.setValue("true");

		Cell outputCell = new OutputCell()
			.setValue("1");

		InlineTable inlineTable = new InlineTable()
			.addRows(new Row(Arrays.asList(inputCell, outputCell)));

		FieldColumnPair fieldColumnPair = new FieldColumnPair()
			.setField(dataField)
			.setColumn(InlineTableUtil.parseColumn(inputCell.getName()));

		MapValues mapValues = new MapValues(InlineTableUtil.parseColumn(outputCell.getName()), inlineTable)
			.setDefaultValue("0")
			.addFieldColumnPairs(fieldColumnPair);

		DerivedField derivedField = new DerivedField("double(x1)", OpType.CATEGORICAL, DataType.DOUBLE, mapValues);

		TransformationDictionary transformationDictionary = new TransformationDictionary()
			.addDerivedFields(derivedField);

		CategoricalPredictor falseTerm = new CategoricalPredictor(dataField, "false", -1d);
		CategoricalPredictor trueTerm = new CategoricalPredictor(dataField, "true", 1d);

		RegressionTable regressionTable = new RegressionTable()
			.addCategoricalPredictors(falseTerm, trueTerm);

		MiningField miningField = new MiningField(dataField)
			.setMissingValueReplacement("false")
			.setInvalidValueReplacement("N/A");

		MiningSchema miningSchema = new MiningSchema()
			.addMiningFields(miningField);

		RegressionModel regressionModel = new RegressionModel(MiningFunction.REGRESSION, miningSchema, null)
			.addRegressionTables(regressionTable);

		PMML pmml = new PMML(Version.PMML_4_4.getVersion(), new Header(), dataDictionary)
			.setTransformationDictionary(transformationDictionary)
			.addModels(regressionModel);

		List<DataField> dataFields = dataDictionary.getDataFields();

		ValueParser parser = new ValueParser(ValueParser.Mode.STRICT);
		parser.applyTo(pmml);

		dataField = dataFields.get(0);

		assertEquals("false", falseValue.requireValue());
		assertEquals("true", trueValue.requireValue());
		assertEquals("N/A", invalidValue.requireValue());

		assertEquals("true", inputCell.getValue());
		assertEquals("1", outputCell.getValue());

		assertEquals("0", mapValues.getDefaultValue());

		assertEquals("false", falseTerm.requireValue());
		assertEquals("true", trueTerm.requireValue());

		assertEquals("false", miningField.getMissingValueReplacement());
		assertEquals("N/A", miningField.getInvalidValueReplacement());

		dataField.setDataType(DataType.BOOLEAN);

		mapValues.setDataType(DataType.FLOAT);

		parser.applyTo(pmml);

		assertEquals(Boolean.FALSE, falseValue.requireValue());
		assertEquals(Boolean.TRUE, trueValue.requireValue());
		assertEquals("N/A", invalidValue.requireValue());

		assertEquals(Boolean.TRUE, inputCell.getValue());
		assertEquals(1f, outputCell.getValue());

		assertEquals(0f, mapValues.getDefaultValue());

		assertEquals(Boolean.FALSE, falseTerm.requireValue());
		assertEquals(Boolean.TRUE, trueTerm.requireValue());

		assertEquals(Boolean.FALSE, miningField.getMissingValueReplacement());
		assertEquals("N/A", miningField.getInvalidValueReplacement());
	}

	@Test
	public void parseTreeModel(){
		DataField dataField = new DataField("x1", OpType.CATEGORICAL, DataType.STRING);

		DataDictionary dataDictionary = new DataDictionary()
			.addDataFields(dataField);

		NormDiscrete normDiscrete = new NormDiscrete(dataField, "1");

		DerivedField derivedField = new DerivedField("global(" + dataField.requireName() + ")", OpType.CATEGORICAL, DataType.STRING, normDiscrete);

		TransformationDictionary transformationDictionary = new TransformationDictionary()
			.addDerivedFields(derivedField);

		SimplePredicate simplePredicate = new SimplePredicate(derivedField, SimplePredicate.Operator.EQUAL, "1");

		Node child = new LeafNode("1", simplePredicate);

		SimpleSetPredicate simpleSetPredicate = new SimpleSetPredicate(dataField, SimpleSetPredicate.BooleanOperator.IS_IN, new Array(Array.Type.STRING, "0 1"));

		Node root = new BranchNode("0", simpleSetPredicate)
			.addNodes(child);

		MiningField miningField = new MiningField(dataField);

		MiningSchema miningSchema = new MiningSchema()
			.addMiningFields(miningField);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, miningSchema, null)
			.setNode(root);

		PMML pmml = new PMML(Version.PMML_4_4.getVersion(), new Header(), dataDictionary)
			.setTransformationDictionary(transformationDictionary)
			.addModels(treeModel);

		List<DataField> dataFields = dataDictionary.getDataFields();

		ValueParser parser = new ValueParser(ValueParser.Mode.STRICT);
		parser.applyTo(pmml);

		dataField = dataFields.get(0);

		assertEquals("1", normDiscrete.requireValue());
		assertEquals("1", simplePredicate.requireValue());

		Array array = simpleSetPredicate.requireArray();

		assertEquals(ImmutableSet.of("0", "1"), array.getValue());

		dataField.setDataType(DataType.INTEGER);

		parser.applyTo(pmml);

		dataField = dataFields.get(0);

		assertEquals(1, normDiscrete.requireValue());
		assertEquals("1", simplePredicate.requireValue());

		array = simpleSetPredicate.requireArray();

		assertTrue(array instanceof RichComplexArray);
		assertEquals(ImmutableSet.of(0, 1), array.getValue());

		dataField.setDataType(DataType.DOUBLE);
		derivedField.setDataType(DataType.INTEGER);

		parser.applyTo(pmml);

		dataField = dataFields.get(0);

		assertEquals(1.0d, normDiscrete.requireValue());
		assertEquals(1, simplePredicate.requireValue());

		array = simpleSetPredicate.requireArray();

		assertEquals(ImmutableSet.of(0.0d, 1.0d), array.getValue());

		dataField.setDataType(DataType.BOOLEAN);
		derivedField.setDataType(DataType.DOUBLE);

		parser.applyTo(pmml);

		dataField = dataFields.get(0);

		assertEquals(true, normDiscrete.requireValue());
		assertEquals(1.0d, simplePredicate.requireValue());

		array = simpleSetPredicate.requireArray();

		assertEquals(ImmutableSet.of(false, true), array.getValue());

		derivedField.setDataType(DataType.BOOLEAN);

		parser.applyTo(pmml);

		dataField = dataFields.get(0);

		assertEquals(true, normDiscrete.requireValue());
		assertEquals(true, simplePredicate.requireValue());
	}
}