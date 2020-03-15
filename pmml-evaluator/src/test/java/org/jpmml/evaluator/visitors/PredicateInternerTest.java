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
package org.jpmml.evaluator.visitors;

import org.dmg.pmml.Array;
import org.dmg.pmml.False;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicate;
import org.dmg.pmml.True;
import org.dmg.pmml.tree.BranchNode;
import org.dmg.pmml.tree.LeafNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.junit.Test;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class PredicateInternerTest {

	@Test
	public void internSimplePredicate(){
		FieldName name = FieldName.create("x");

		Predicate left = new SimplePredicate(name, SimplePredicate.Operator.EQUAL, "1");
		Predicate right = new SimplePredicate(name, SimplePredicate.Operator.EQUAL, "1");

		checkTree(left, right);
	}

	@Test
	public void internSimpleSetPredicate(){
		FieldName name = FieldName.create("x");

		Predicate left = new SimpleSetPredicate(name, SimpleSetPredicate.BooleanOperator.IS_IN, new Array(Array.Type.STRING, "1"));
		Predicate right = new SimpleSetPredicate(name, SimpleSetPredicate.BooleanOperator.IS_IN, new Array(Array.Type.STRING, "\"1\""));

		checkTree(left, right);
	}

	@Test
	public void internTrue(){
		checkTree(new True(), new True());
		checkTree(new True(), True.INSTANCE);
	}

	@Test
	public void internFalse(){
		checkTree(new False(), new False());
		checkTree(new False(), False.INSTANCE);
	}

	static
	private void checkTree(Predicate left, Predicate right){
		checkTree(new LeafNode(null, left), new LeafNode(null, right));
	}

	static
	private void checkTree(Node leftChild, Node rightChild){
		Node root = new BranchNode(null, True.INSTANCE)
			.addNodes(leftChild, rightChild);

		TreeModel treeModel = new TreeModel()
			.setNode(root);

		assertNotSame(leftChild.getPredicate(), rightChild.getPredicate());

		PredicateInterner interner = new PredicateInterner();
		interner.applyTo(treeModel);

		assertSame(leftChild.getPredicate(), rightChild.getPredicate());
	}
}