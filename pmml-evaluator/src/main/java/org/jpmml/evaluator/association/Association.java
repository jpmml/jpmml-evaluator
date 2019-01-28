/*
 * Copyright (c) 2013 Villu Ruusmann
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
package org.jpmml.evaluator.association;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.association.AssociationRule;
import org.jpmml.evaluator.AbstractComputable;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.HasRuleValues;
import org.jpmml.model.ToStringHelper;

/**
 * @see MiningFunction#ASSOCIATION_RULES
 */
abstract
public class Association extends AbstractComputable implements HasRuleValues, HasEntityRegistry<AssociationRule> {

	private List<AssociationRule> associationRules = null;

	private BitSet antecedentFlags = null;

	private BitSet consequentFlags = null;


	protected Association(List<AssociationRule> associationRules, BitSet antecedentFlags, BitSet consequentFlags){
		setAssociationRules(associationRules);

		setAntecedentFlags(antecedentFlags);
		setConsequentFlags(consequentFlags);
	}

	/**
	 * @throws UnsupportedOperationException Always.
	 */
	@Override
	public Object getResult(){
		throw new UnsupportedOperationException();
	}

	@Override
	protected ToStringHelper toStringHelper(){
		ToStringHelper helper = super.toStringHelper()
			.add("antecedentFlags", getAntecedentFlags())
			.add("consequentFlags", getConsequentFlags());

		return helper;
	}

	@Override
	public List<AssociationRule> getRuleValues(OutputField.Algorithm algorithm){
		List<AssociationRule> associationRules = getAssociationRules();

		BitSet flags;

		switch(algorithm){
			// "A rule is selected if its antecedent itemset is a subset of the input itemset"
			case RECOMMENDATION:
				flags = getAntecedentFlags();
				break;
			// "A rule is selected if its antecedent itemset is a subset of the input itemset, and its consequent itemset is not a subset of the input itemset"
			case EXCLUSIVE_RECOMMENDATION:
				flags = (BitSet)getAntecedentFlags().clone();
				flags.andNot(getConsequentFlags());
				break;
			// "A rule is selected if its antecedent and consequent itemsets are included in the input itemset"
			case RULE_ASSOCIATION:
				flags = (BitSet)getAntecedentFlags().clone();
				flags.and(getConsequentFlags());
				break;
			default:
				throw new IllegalArgumentException();
		}

		int cardinality = flags.cardinality();
		if(cardinality == 0){
			return Collections.emptyList();
		}

		List<AssociationRule> result = new ArrayList<>(cardinality);

		for(int i = flags.nextSetBit(0); i > -1; i = flags.nextSetBit(i + 1)){
			AssociationRule associationRule = associationRules.get(i);

			result.add(associationRule);
		}

		return result;
	}

	public List<AssociationRule> getAssociationRules(){
		return this.associationRules;
	}

	private void setAssociationRules(List<AssociationRule> associationRules){

		if(associationRules == null){
			throw new IllegalArgumentException();
		}

		this.associationRules = associationRules;
	}

	public BitSet getAntecedentFlags(){
		return this.antecedentFlags;
	}

	private void setAntecedentFlags(BitSet antecedentFlags){

		if(antecedentFlags == null){
			throw new IllegalArgumentException();
		}

		this.antecedentFlags = antecedentFlags;
	}

	public BitSet getConsequentFlags(){
		return this.consequentFlags;
	}

	private void setConsequentFlags(BitSet consequentFlags){

		if(consequentFlags == null){
			throw new IllegalArgumentException();
		}

		this.consequentFlags = consequentFlags;
	}
}