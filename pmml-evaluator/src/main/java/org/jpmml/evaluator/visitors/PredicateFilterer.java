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

import java.util.List;

import org.dmg.pmml.Attribute;
import org.dmg.pmml.CompoundPredicate;
import org.dmg.pmml.CompoundRule;
import org.dmg.pmml.Node;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.Segment;
import org.dmg.pmml.SimpleRule;
import org.dmg.pmml.VisitorAction;
import org.jpmml.model.visitors.AbstractVisitor;

/**
 * <p>
 * This class provides a skeletal implementation of {@link Predicate} filterers.
 * </p>
 */
abstract
public class PredicateFilterer extends AbstractVisitor {

	abstract
	public Predicate filter(Predicate predicate);

	@Override
	public VisitorAction visit(Attribute attribute){
		attribute.setPredicate(filter(attribute.getPredicate()));

		return super.visit(attribute);
	}

	@Override
	public VisitorAction visit(CompoundPredicate compoundPredicate){

		if(compoundPredicate.hasPredicates()){
			List<Predicate> predicates = compoundPredicate.getPredicates();

			for(int i = 0; i < predicates.size(); i++){
				predicates.set(i, filter(predicates.get(i)));
			}
		}

		return super.visit(compoundPredicate);
	}

	@Override
	public VisitorAction visit(CompoundRule compoundRule){
		compoundRule.setPredicate(filter(compoundRule.getPredicate()));

		return super.visit(compoundRule);
	}

	@Override
	public VisitorAction visit(Node node){
		node.setPredicate(filter(node.getPredicate()));

		return super.visit(node);
	}

	@Override
	public VisitorAction visit(Segment segment){
		segment.setPredicate(filter(segment.getPredicate()));

		return super.visit(segment);
	}

	@Override
	public VisitorAction visit(SimpleRule simpleRule){
		simpleRule.setPredicate(filter(simpleRule.getPredicate()));

		return super.visit(simpleRule);
	}
}