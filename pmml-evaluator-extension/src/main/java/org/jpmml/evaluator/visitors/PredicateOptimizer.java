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

import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicate;
import org.jpmml.evaluator.ExtensionUtil;
import org.jpmml.evaluator.RichSimplePredicate;
import org.jpmml.evaluator.RichSimpleSetPredicate;
import org.jpmml.model.visitors.PredicateFilterer;

public class PredicateOptimizer extends PredicateFilterer {

	@Override
	public Predicate filter(Predicate predicate){

		if(predicate == null || ExtensionUtil.hasExtensions(predicate)){
			return predicate;
		}

		return optimize(predicate);
	}

	public Predicate optimize(Predicate predicate){

		if(predicate instanceof SimplePredicate){
			SimplePredicate simplePredicate = (SimplePredicate)predicate;

			return new RichSimplePredicate(simplePredicate);
		} else

		if(predicate instanceof SimpleSetPredicate){
			SimpleSetPredicate simpleSetPredicate = (SimpleSetPredicate)predicate;

			return new RichSimpleSetPredicate(simpleSetPredicate);
		}

		return predicate;
	}
}