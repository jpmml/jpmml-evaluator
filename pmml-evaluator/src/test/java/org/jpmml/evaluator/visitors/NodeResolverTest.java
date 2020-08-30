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

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.Header;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.PMML;
import org.dmg.pmml.True;
import org.dmg.pmml.Version;
import org.dmg.pmml.tree.BranchNode;
import org.dmg.pmml.tree.LeafNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class NodeResolverTest {

	@Test
	public void resolve(){
		Node leftChild = new LeafNode()
			.setId("1");

		Node rightChild = new LeafNode()
			.setId("2");

		Node root = new BranchNode(null, True.INSTANCE)
			.setId("0")
			.setDefaultChild(rightChild.getId())
			.addNodes(leftChild, rightChild);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, new MiningSchema(), null)
			.setNode(root);

		PMML pmml = new PMML(Version.PMML_4_4.getVersion(), new Header(), new DataDictionary())
			.addModels(treeModel);

		NodeResolver resolver = new NodeResolver();
		resolver.applyTo(pmml);

		assertEquals(rightChild.getId(), root.getDefaultChild());

		treeModel.setMissingValueStrategy(TreeModel.MissingValueStrategy.DEFAULT_CHILD);

		resolver.applyTo(pmml);

		assertSame(rightChild, root.getDefaultChild());
	}
}