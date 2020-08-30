/*
 * Copyright (c) 2018 Villu Ruusmann
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
package org.jpmml.evaluator.tree;

import java.util.Arrays;

import com.google.common.collect.BiMap;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.Header;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Version;
import org.dmg.pmml.tree.BranchNode;
import org.dmg.pmml.tree.LeafNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HasNodeRegistryTest {

	@Test
	public void getPath(){
		Node node1a = new BranchNode();

		Node node2a = new BranchNode();
		Node node2b = new BranchNode();

		node1a.addNodes(node2a, node2b);

		Node node3a = new BranchNode();
		Node node3b = new BranchNode();

		node2a.addNodes(node3a, node3b);

		Node node3c = new LeafNode();
		Node node3d = new LeafNode();

		node2b.addNodes(node3c, node3d);

		PMML pmml = new PMML(Version.PMML_4_4.getVersion(), new Header(), new DataDictionary())
			.addModels(new TreeModel(MiningFunction.REGRESSION, new MiningSchema(), node1a));

		HasNodeRegistry hasNodeRegistry = new ComplexTreeModelEvaluator(pmml);

		BiMap<Node, String> nodeRegistry = (hasNodeRegistry.getEntityRegistry()).inverse();

		String id1a = nodeRegistry.get(node1a);

		String id2a = nodeRegistry.get(node2a);
		String id2b = nodeRegistry.get(node2b);

		String id3a = nodeRegistry.get(node3a);
		String id3b = nodeRegistry.get(node3b);
		String id3c = nodeRegistry.get(node3c);
		String id3d = nodeRegistry.get(node3d);

		assertEquals(Arrays.asList(node1a), hasNodeRegistry.getPath(id1a));
		assertEquals(Arrays.asList(node1a, node2a), hasNodeRegistry.getPath(id2a));
		assertEquals(Arrays.asList(node1a, node2a, node3a), hasNodeRegistry.getPath(id3a));

		assertEquals(Arrays.asList(node1a), hasNodeRegistry.getPathBetween(id1a, id1a));
		assertEquals(Arrays.asList(node1a, node2a), hasNodeRegistry.getPathBetween(id1a, id2a));
		assertNull(hasNodeRegistry.getPathBetween(id2a, id1a));
		assertEquals(Arrays.asList(node2a, node3a), hasNodeRegistry.getPathBetween(id2a, id3a));
		assertEquals(Arrays.asList(node2a, node3b), hasNodeRegistry.getPathBetween(id2a, id3b));

		assertNull(hasNodeRegistry.getPathBetween(id2a, id2b));
		assertNull(hasNodeRegistry.getPathBetween(id2a, id3c));
		assertNull(hasNodeRegistry.getPathBetween(id2a, id3d));
	}
}