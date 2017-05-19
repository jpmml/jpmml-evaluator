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
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.junit.Test;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class PredicateInternerTest {

	@Test
	public void internSimplePredicate(){
		FieldName name = FieldName.create("x");

		Predicate left = new SimplePredicate(name, SimplePredicate.Operator.EQUAL)
			.setValue("1");

		Predicate right = new SimplePredicate(name, SimplePredicate.Operator.EQUAL)
			.setValue("1");

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
	}

	@Test
	public void internFalse(){
		checkTree(new False(), new False());
	}

	static
	private void checkTree(Predicate left, Predicate right){
		checkTree(createNode(left), createNode(right));
	}

	static
	private void checkTree(Node leftChild, Node rightChild){
		Node root = new Node()
			.setPredicate(new True())
			.addNodes(leftChild, rightChild);

		TreeModel treeModel = new TreeModel()
			.setNode(root);

		assertNotSame(leftChild.getPredicate(), rightChild.getPredicate());

		intern(treeModel);

		assertSame(leftChild.getPredicate(), rightChild.getPredicate());
	}

	static
	private Node createNode(Predicate predicate){
		Node node = new Node()
			.setPredicate(predicate);

		return node;
	}

	static
	private void intern(TreeModel treeModel){
		PredicateInterner interner = new PredicateInterner();
		interner.applyTo(treeModel);
	}
}