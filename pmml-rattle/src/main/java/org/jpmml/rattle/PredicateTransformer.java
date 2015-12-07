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
package org.jpmml.rattle;

import java.util.List;

import org.dmg.pmml.Array;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicate;
import org.jpmml.evaluator.ArrayUtil;
import org.jpmml.evaluator.UnsupportedFeatureException;
import org.jpmml.model.visitors.PredicateFilterer;

public class PredicateTransformer extends PredicateFilterer {

	@Override
	public Predicate filter(Predicate predicate){
		return transform(predicate);
	}

	public Predicate transform(Predicate predicate){

		if(predicate != null && predicate.hasExtensions()){
			return predicate;
		} // End if

		if(predicate instanceof SimpleSetPredicate){
			return transform((SimpleSetPredicate)predicate);
		}

		return predicate;
	}

	private Predicate transform(SimpleSetPredicate simpleSetPredicate){
		Array array = simpleSetPredicate.getArray();

		List<String> content = ArrayUtil.getContent(array);
		if(content.size() != 1){
			return simpleSetPredicate;
		}

		SimplePredicate.Operator operator;

		SimpleSetPredicate.BooleanOperator booleanOperator = simpleSetPredicate.getBooleanOperator();
		switch(booleanOperator){
			case IS_IN:
				operator = SimplePredicate.Operator.EQUAL;
				break;
			case IS_NOT_IN:
				operator = SimplePredicate.Operator.NOT_EQUAL;
				break;
			default:
				throw new UnsupportedFeatureException(simpleSetPredicate, booleanOperator);
		}

		SimplePredicate simplePredicate = new SimplePredicate(simpleSetPredicate.getField(), operator)
			.setValue(content.get(0));

		return simplePredicate;
	}
}