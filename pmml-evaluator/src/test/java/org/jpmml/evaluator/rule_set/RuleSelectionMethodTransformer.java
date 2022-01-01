/*
 * Copyright (c) 2021 Villu Ruusmann
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
package org.jpmml.evaluator.rule_set;

import java.util.List;

import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.rule_set.RuleSelectionMethod;
import org.dmg.pmml.rule_set.RuleSet;
import org.jpmml.evaluator.PMMLTransformer;
import org.jpmml.model.visitors.AbstractVisitor;

public class RuleSelectionMethodTransformer implements PMMLTransformer<RuntimeException> {

	private RuleSelectionMethod ruleSelectionMethod;


	public RuleSelectionMethodTransformer(RuleSelectionMethod ruleSelectionMethod){
		setRuleSelectionMethod(ruleSelectionMethod);
	}

	@Override
	public PMML apply(PMML pmml){
		Visitor visitor = new AbstractVisitor(){

			@Override
			public VisitorAction visit(RuleSet ruleSet){
				List<RuleSelectionMethod> ruleSelectionMethods = ruleSet.getRuleSelectionMethods();

				if(!ruleSelectionMethods.isEmpty()){
					ruleSelectionMethods.clear();
				}

				ruleSelectionMethods.add(getRuleSelectionMethod());

				return super.visit(ruleSet);
			}
		};

		visitor.applyTo(pmml);

		return pmml;
	}

	public RuleSelectionMethod getRuleSelectionMethod(){
		return this.ruleSelectionMethod;
	}

	private void setRuleSelectionMethod(RuleSelectionMethod ruleSelectionMethod){
		this.ruleSelectionMethod = ruleSelectionMethod;
	}
}