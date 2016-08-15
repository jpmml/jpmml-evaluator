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

import org.dmg.pmml.Array;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicate;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.HasParsedValue;
import org.jpmml.evaluator.HasParsedValueSet;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PredicateOptimizerTest {

	@Test
	public void optimizeSimplePredicate(){
		Predicate predicate = new SimplePredicate(FieldName.create("x"), SimplePredicate.Operator.EQUAL)
			.setValue("1");

		checkTree(predicate, HasParsedValue.class);
	}

	@Test
	public void optimizeSimpleSetPredicate(){
		Predicate predicate = new SimpleSetPredicate(FieldName.create("x"), SimpleSetPredicate.BooleanOperator.IS_IN, new Array(Array.Type.STRING, "1"));

		checkTree(predicate, HasParsedValueSet.class);
	}

	static
	private void checkTree(Predicate predicate, Class<?> clazz){
		Node root = new Node()
			.setPredicate(predicate);

		TreeModel treeModel = new TreeModel()
			.setNode(root);

		assertFalse(clazz.isInstance(root.getPredicate()));

		optimize(treeModel);

		assertTrue(clazz.isInstance(root.getPredicate()));
	}

	static
	private void optimize(TreeModel treeModel){
		PredicateOptimizer optimizer = new PredicateOptimizer();
		optimizer.applyTo(treeModel);
	}
}