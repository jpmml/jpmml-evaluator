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

import java.util.List;

import org.dmg.pmml.Array;
import org.dmg.pmml.CompoundPredicate;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicate;
import org.jpmml.evaluator.ArrayUtil;
import org.jpmml.evaluator.ExtensionUtil;
import org.jpmml.model.visitors.PredicateFilterer;

public class PredicateTransformer extends PredicateFilterer {

	@Override
	public Predicate filter(Predicate predicate){

		if(predicate == null || ExtensionUtil.hasExtensions(predicate)){
			return predicate;
		}

		return transform(predicate);
	}

	public Predicate transform(Predicate predicate){

		if(predicate instanceof SimpleSetPredicate){
			return transform((SimpleSetPredicate)predicate);
		} else

		if(predicate instanceof CompoundPredicate){
			return transform((CompoundPredicate)predicate);
		}

		return predicate;
	}

	private Predicate transform(SimpleSetPredicate simpleSetPredicate){
		Array array = simpleSetPredicate.getArray();

		List<String> content = ArrayUtil.getContent(array);
		if(content.size() != 1){
			return simpleSetPredicate;
		}

		String value = content.get(0);

		SimpleSetPredicate.BooleanOperator booleanOperator = simpleSetPredicate.getBooleanOperator();
		switch(booleanOperator){
			case IS_IN:
				return createSimplePredicate(simpleSetPredicate.getField(), SimplePredicate.Operator.EQUAL, value);
			case IS_NOT_IN:
				return createSimplePredicate(simpleSetPredicate.getField(), SimplePredicate.Operator.NOT_EQUAL, value);
			default:
				break;
		}

		return simpleSetPredicate;
	}

	private Predicate transform(CompoundPredicate compoundPredicate){
		List<Predicate> predicates = compoundPredicate.getPredicates();

		CompoundPredicate.BooleanOperator booleanOperator = compoundPredicate.getBooleanOperator();
		switch(booleanOperator){
			case SURROGATE:
				return transform(predicates.get(0));
			default:
				break;
		}

		return compoundPredicate;
	}

	static
	private SimplePredicate createSimplePredicate(FieldName field, SimplePredicate.Operator operator, String value){
		SimplePredicate simplePredicate = new SimplePredicate(field, operator)
			.setValue(value);

		return simplePredicate;
	}
}