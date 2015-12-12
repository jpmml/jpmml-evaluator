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
import org.dmg.pmml.Node;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicate;
import org.dmg.pmml.True;
import org.junit.Test;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class PredicateInternerTest {

	@Test
	public void internSimplePredicate(){
		FieldName name = FieldName.create("x");

		Node left = new Node()
			.setPredicate(new SimplePredicate(name, SimplePredicate.Operator.EQUAL)
				.setValue("1"));

		Node right = new Node()
			.setPredicate(new SimplePredicate(name, SimplePredicate.Operator.EQUAL)
				.setValue("1"));

		checkTree(left, right);
	}

	@Test
	public void internSimpleSetPredicate(){
		FieldName name = FieldName.create("x");

		Node left = new Node()
			.setPredicate(new SimpleSetPredicate(name, SimpleSetPredicate.BooleanOperator.IS_IN, new Array(Array.Type.STRING, "1")));

		Node right = new Node()
			.setPredicate(new SimpleSetPredicate(name, SimpleSetPredicate.BooleanOperator.IS_IN, new Array(Array.Type.STRING, "\"1\"")));

		checkTree(left, right);
	}

	@Test
	public void internTrue(){
		Node left = new Node()
			.setPredicate(new True());

		Node right = new Node()
			.setPredicate(new True());

		checkTree(left, right);
	}

	@Test
	public void internFalse(){
		Node left = new Node()
			.setPredicate(new False());

		Node right = new Node()
			.setPredicate(new False());

		checkTree(left, right);
	}

	static
	private void checkTree(Node left, Node right){
		Node root = new Node()
			.addNodes(left, right);

		assertNotSame(left.getPredicate(), right.getPredicate());

		PredicateInterner interner = new PredicateInterner();
		interner.applyTo(root);

		assertSame(left.getPredicate(), right.getPredicate());
	}
}