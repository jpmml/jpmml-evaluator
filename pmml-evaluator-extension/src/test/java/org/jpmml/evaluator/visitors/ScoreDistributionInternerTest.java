/*
 * Copyright (c) 2017 Villu Ruusmann
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

import org.dmg.pmml.ScoreDistribution;
import org.dmg.pmml.True;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.junit.Test;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class ScoreDistributionInternerTest {

	@Test
	public void intern(){
		ScoreDistribution left = new ScoreDistribution("event", 0.33d);
		ScoreDistribution right = new ScoreDistribution("event", 0.33d);

		Node leftChild = createNode(left);
		Node rightChild = createNode(right);

		Node root = new Node()
			.setPredicate(new True())
			.addNodes(leftChild, rightChild);

		TreeModel treeModel = new TreeModel()
			.setNode(root);

		for(int i = 0; i < 2; i++){
			assertNotSame((leftChild.getScoreDistributions()).get(i), (rightChild.getScoreDistributions()).get(i));
		}

		intern(treeModel);

		for(int i = 0; i < 2; i++){
			assertSame((leftChild.getScoreDistributions()).get(i), (rightChild.getScoreDistributions()).get(i));
		}
	}

	static
	private Node createNode(ScoreDistribution event){
		ScoreDistribution noEvent = new ScoreDistribution("no-event", 1d - event.getRecordCount());

		Node node = new Node()
			.addScoreDistributions(event, noEvent);

		return node;
	}

	static
	private void intern(TreeModel treeModel){
		ScoreDistributionInterner interner = new ScoreDistributionInterner();
		interner.applyTo(treeModel);
	}
}