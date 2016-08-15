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

import org.dmg.pmml.Array;
import org.dmg.pmml.CompoundPredicate;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicate;
import org.dmg.pmml.tree.Node;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class PredicateTransformerTest {

	@Test
	public void transform(){
		SimpleSetPredicate simpleSetPredicate = new SimpleSetPredicate(FieldName.create("x"), SimpleSetPredicate.BooleanOperator.IS_IN, new Array(Array.Type.INT, "1 2 3"));

		assertSame(simpleSetPredicate, transform(simpleSetPredicate));

		CompoundPredicate compoundPredicate = new CompoundPredicate(CompoundPredicate.BooleanOperator.XOR)
			.addPredicates(simpleSetPredicate);

		assertSame(compoundPredicate, transform(compoundPredicate));

		compoundPredicate.setBooleanOperator(CompoundPredicate.BooleanOperator.SURROGATE);

		assertSame(simpleSetPredicate, transform(compoundPredicate));

		simpleSetPredicate.setArray(new Array(Array.Type.INT, "1"));

		SimplePredicate simplePredicate = (SimplePredicate)transform(simpleSetPredicate);

		assertEquals(simpleSetPredicate.getField(), simplePredicate.getField());
		assertEquals(SimplePredicate.Operator.EQUAL, simplePredicate.getOperator());
		assertEquals("1", simplePredicate.getValue());

		simpleSetPredicate.setBooleanOperator(SimpleSetPredicate.BooleanOperator.IS_NOT_IN);

		simplePredicate = (SimplePredicate)transform(compoundPredicate);

		assertEquals(simpleSetPredicate.getField(), simplePredicate.getField());
		assertEquals(SimplePredicate.Operator.NOT_EQUAL, simplePredicate.getOperator());
		assertEquals("1", simplePredicate.getValue());
	}

	static
	private Predicate transform(Predicate predicate){
		Node node = new Node()
			.setPredicate(predicate);

		PredicateTransformer transformer = new PredicateTransformer();
		transformer.applyTo(node);

		return node.getPredicate();
	}
}