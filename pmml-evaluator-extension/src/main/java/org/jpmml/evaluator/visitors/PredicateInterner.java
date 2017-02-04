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
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicate;
import org.dmg.pmml.True;
import org.dmg.pmml.Visitable;
import org.jpmml.evaluator.ArrayUtil;
import org.jpmml.evaluator.ExtensionUtil;
import org.jpmml.model.visitors.PredicateFilterer;

/**
 * <p>
 * A Visitor that interns {@link Predicate} elements.
 * </p>
 */
public class PredicateInterner extends PredicateFilterer {

	private ElementHashMap<SimplePredicate> simplePredicateCache = new ElementHashMap<SimplePredicate>(){

		@Override
		public ElementKey createKey(SimplePredicate simplePredicate){
			Object[] content = {simplePredicate.getField(), simplePredicate.getOperator(), simplePredicate.getValue()};

			return new ElementKey(content);
		}
	};

	private ElementHashMap<SimpleSetPredicate> simpleSetPredicateCache = new ElementHashMap<SimpleSetPredicate>(){

		@Override
		public ElementKey createKey(SimpleSetPredicate simpleSetPredicate){
			Array array = simpleSetPredicate.getArray();

			Object[] content = {simpleSetPredicate.getField(), simpleSetPredicate.getBooleanOperator(), ArrayUtil.getContent(array)};

			return new ElementKey(content);
		}
	};

	private ElementHashMap<True> truePredicateCache = new ElementHashMap<True>(){

		@Override
		public ElementKey createKey(True truePredicate){
			return ElementKey.EMPTY;
		}
	};

	private ElementHashMap<False> falsePredicateCache = new ElementHashMap<False>(){

		@Override
		public ElementKey createKey(False falsePredicate){
			return ElementKey.EMPTY;
		}
	};


	@Override
	public void applyTo(Visitable visitable){
		reset();

		super.applyTo(visitable);
	}

	public void reset(){
		this.simplePredicateCache.clear();
		this.simpleSetPredicateCache.clear();
		this.truePredicateCache.clear();
		this.falsePredicateCache.clear();
	}

	@Override
	public Predicate filter(Predicate predicate){

		if(predicate == null || ExtensionUtil.hasExtensions(predicate)){
			return predicate;
		}

		return intern(predicate);
	}

	public Predicate intern(Predicate predicate){

		if(predicate instanceof SimplePredicate){
			return intern((SimplePredicate)predicate);
		} else

		if(predicate instanceof SimpleSetPredicate){
			return intern((SimpleSetPredicate)predicate);
		} else

		if(predicate instanceof True){
			return intern((True)predicate);
		} else

		if(predicate instanceof False){
			return intern((False)predicate);
		}

		return predicate;
	}

	private SimplePredicate intern(SimplePredicate simplePredicate){
		return this.simplePredicateCache.intern(simplePredicate);
	}

	private SimpleSetPredicate intern(SimpleSetPredicate simpleSetPredicate){
		return this.simpleSetPredicateCache.intern(simpleSetPredicate);
	}

	private True intern(True truePredicate){
		return this.truePredicateCache.intern(truePredicate);
	}

	private False intern(False falsePredicate){
		return this.falsePredicateCache.intern(falsePredicate);
	}
}