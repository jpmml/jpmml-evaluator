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

import java.util.List;

import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.tree.DecisionTree;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.UnsupportedElementException;
import org.jpmml.model.visitors.AbstractVisitor;

public class NodeResolver extends AbstractVisitor {

	@Override
	public VisitorAction visit(DecisionTree decisionTree){
		throw new UnsupportedElementException(decisionTree);
	}

	@Override
	public VisitorAction visit(Node node){
		Object defaultChild = node.getDefaultChild();

		if(node.hasNodes()){
			List<Node> children = node.getNodes();

			for(int i = 0, max = children.size(); i < max; i++){
				Node child = children.get(i);

				Object id = child.getId();
				if(id != null && (id).equals(defaultChild)){
					node.setDefaultChild(child);

					break;
				}
			}
		}

		return super.visit(node);
	}

	@Override
	public VisitorAction visit(TreeModel treeModel){
		TreeModel.MissingValueStrategy missingValueStrategy = treeModel.getMissingValueStrategy();

		switch(missingValueStrategy){
			case DEFAULT_CHILD:
				break;
			default:
				return VisitorAction.SKIP;
		}

		return super.visit(treeModel);
	}
}