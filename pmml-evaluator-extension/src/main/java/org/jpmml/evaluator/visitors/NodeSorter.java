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
package org.jpmml.evaluator.visitors;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Ordering;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.tree.Node;
import org.jpmml.model.visitors.AbstractVisitor;

/**
 * <p>
 * A Visitor that orders {@link Node} elements by their "hit" probability.
 * </p>
 */
public class NodeSorter extends AbstractVisitor {

	@Override
	public VisitorAction visit(Node node){

		if(node.hasNodes()){
			List<Node> nodes = node.getNodes();

			Collections.sort(nodes, NodeSorter.COMPARATOR);
		}

		return super.visit(node);
	}

	private static final Comparator<Node> COMPARATOR = new Comparator<Node>(){

		private Ordering<Double> ordering = Ordering.natural()
			.reverse() // Higher record count (ie. more probable) nodes first, lower record count (ie. less probable) nodes last
			.nullsLast();

		@Override
		public int compare(Node left, Node right){
			return this.ordering.compare(left.getRecordCount(), right.getRecordCount());
		}
	};
}