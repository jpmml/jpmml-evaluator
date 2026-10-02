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
import java.util.Collections;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.False;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.True;
import org.dmg.pmml.tree.BranchNode;
import org.dmg.pmml.tree.LeafNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.model.visitors.FloatInterner;
import org.jpmml.model.visitors.VisitorBattery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class NodeScoreParserTest extends ParserTest {

	@Test
	public void parseTreeModel(){
		DataField dataField = new DataField("y", OpType.CATEGORICAL, DataType.STRING);

		DataDictionary dataDictionary = createDataDictionary(dataField);

		Node node1a = new BranchNode("1", True.INSTANCE);

		Node node2a = new LeafNode("2", False.INSTANCE);
		Node node2b = new BranchNode("2.0", False.INSTANCE);
		Node node2c = new LeafNode(2.0f, True.INSTANCE);

		node1a.addNodes(node2a, node2b, node2c);

		Node node3a = new LeafNode("error", False.INSTANCE);

		node2b.addNodes(node3a);

		MiningSchema miningSchema = createMiningSchema(Collections.emptyList(), Arrays.asList(dataField));

		TreeModel treeModel = new TreeModel(MiningFunction.CLASSIFICATION, miningSchema, node1a)
			.setMathContext(MathContext.FLOAT);

		PMML pmml = createPMML(dataDictionary, treeModel);

		VisitorBattery visitorBattery = new VisitorBattery();
		visitorBattery.add(NodeScoreParser.class);
		visitorBattery.add(FloatInterner.class);

		visitorBattery.applyTo(pmml);

		assertEquals("1", node1a.requireScore());

		assertEquals("2", node2a.requireScore());
		assertEquals("2.0", node2b.requireScore());
		assertEquals(2.0f, node2c.requireScore());

		assertEquals("error", node3a.requireScore());

		dataField
			.setOpType(OpType.CONTINUOUS)
			.setDataType(DataType.FLOAT);

		treeModel.setMiningFunction(MiningFunction.REGRESSION);

		visitorBattery.applyTo(pmml);

		assertEquals(1.0f, node1a.requireScore());

		assertEquals(2.0f, node2a.requireScore());
		assertEquals(2.0f, node2b.requireScore());
		assertEquals(2.0f, node2c.requireScore());

		assertSame(node2a.requireScore(), node2b.requireScore());
		assertSame(node2a.requireScore(), node2c.requireScore());

		assertEquals("error", node3a.requireScore());
	}
}