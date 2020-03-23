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

import org.dmg.pmml.False;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.True;
import org.dmg.pmml.tree.BranchNode;
import org.dmg.pmml.tree.LeafNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.model.visitors.FloatInterner;
import org.jpmml.model.visitors.VisitorBattery;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class NodeScoreParserTest {

	@Test
	public void parseAndIntern(){
		Node node1a = new BranchNode("1", True.INSTANCE);

		Node node2a = new LeafNode("2", False.INSTANCE);
		Node node2b = new BranchNode("2.0", False.INSTANCE);
		Node node2c = new LeafNode(2.0f, True.INSTANCE);

		node1a.addNodes(node2a, node2b, node2c);

		Node node3a = new LeafNode("error", False.INSTANCE);

		node2b.addNodes(node3a);

		TreeModel treeModel = new TreeModel(MiningFunction.CLASSIFICATION, new MiningSchema(), node1a)
			.setMathContext(MathContext.FLOAT);

		VisitorBattery visitorBattery = new VisitorBattery();
		visitorBattery.add(NodeScoreParser.class);
		visitorBattery.add(FloatInterner.class);

		visitorBattery.applyTo(treeModel);

		assertEquals("1", node1a.getScore());

		assertEquals("2", node2a.getScore());
		assertEquals("2.0", node2b.getScore());
		assertEquals(2.0f, node2c.getScore());

		assertEquals("error", node3a.getScore());

		treeModel.setMiningFunction(MiningFunction.REGRESSION);

		visitorBattery.applyTo(treeModel);

		assertEquals(1.0f, node1a.getScore());

		assertEquals(2.0f, node2a.getScore());
		assertEquals(2.0f, node2b.getScore());
		assertEquals(2.0f, node2c.getScore());

		assertSame(node2a.getScore(), node2b.getScore());
		assertSame(node2a.getScore(), node2c.getScore());

		assertEquals("error", node3a.getScore());
	}
}