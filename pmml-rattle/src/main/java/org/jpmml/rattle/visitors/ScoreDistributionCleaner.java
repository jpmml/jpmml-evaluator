/*
 * Copyright (c) 2015 Villu Ruusmann
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
package org.jpmml.rattle.visitors;

import java.util.Deque;
import java.util.List;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.ScoreDistribution;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.model.visitors.AbstractVisitor;

public class ScoreDistributionCleaner extends AbstractVisitor {

	@Override
	public VisitorAction visit(Node node){
		TreeModel treeModel = getTreeModel();

		MiningFunction miningFunction = treeModel.getMiningFunction();
		switch(miningFunction){
			case REGRESSION:
				if(node.hasScoreDistributions()){
					List<ScoreDistribution> scoreDistributions = node.getScoreDistributions();

					scoreDistributions.clear();
				}
				break;
			default:
				break;
		}

		return super.visit(node);
	}

	private TreeModel getTreeModel(){
		Deque<PMMLObject> parents = getParents();

		return (TreeModel)Iterables.find(parents, Predicates.instanceOf(TreeModel.class));
	}
}