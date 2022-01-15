/*
 * Copyright (c) 2020 Villu Ruusmann
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

import java.util.List;

import org.dmg.pmml.PMML;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.PMMLAttributes;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.model.InvalidAttributeException;

abstract
public class TreeModelEvaluator extends ModelEvaluator<TreeModel> {

	protected TreeModelEvaluator(){
	}

	public TreeModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, TreeModel.class));
	}

	public TreeModelEvaluator(PMML pmml, TreeModel treeModel){
		super(pmml, treeModel);

		@SuppressWarnings("unused")
		Node root = treeModel.requireNode();
	}

	@Override
	public String getSummary(){
		return "Tree model";
	}

	static
	protected Node findDefaultChild(Node node){
		Object defaultChild = node.requireDefaultChild();

		if(defaultChild instanceof Node){
			return (Node)defaultChild;
		}

		List<Node> children = node.getNodes();
		for(int i = 0, max = children.size(); i < max; i++){
			Node child = children.get(i);

			Object id = child.getId();
			if(id != null && (id).equals(defaultChild)){
				return child;
			}
		}

		// "Only Nodes which are immediate children of the respective Node can be referenced"
		throw new InvalidAttributeException(node, PMMLAttributes.COMPLEXNODE_DEFAULTCHILD, defaultChild);
	}
}