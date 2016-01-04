/*
 * Copyright (c) 2016 Villu Ruusmann
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
package org.jpmml.rattle;

import java.util.Arrays;
import java.util.List;

import org.dmg.pmml.Node;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NodeSorterTest {

	@Test
	public void sort(){
		Node first = new Node();
		Node second = new Node();
		Node third = new Node();

		Node root = new Node()
			.addNodes(first, second, third);

		assertEquals(Arrays.asList(first, second, third), sort(root));

		first.setRecordCount(100d);
		third.setRecordCount(300d);

		assertEquals(Arrays.asList(third, first, second), sort(root));

		second.setRecordCount(200d);

		assertEquals(Arrays.asList(third, second, first), sort(root));

		first.setRecordCount(null);
		second.setRecordCount(null);
		third.setRecordCount(null);

		assertEquals(Arrays.asList(third, second, first), sort(root));
	}

	static
	public List<Node> sort(Node node){
		NodeSorter sorter = new NodeSorter();
		sorter.applyTo(node);

		return node.getNodes();
	}
}