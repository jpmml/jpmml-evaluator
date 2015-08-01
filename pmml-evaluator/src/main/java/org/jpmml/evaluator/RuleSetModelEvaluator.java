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
package org.jpmml.evaluator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import org.dmg.pmml.CompoundRule;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.Rule;
import org.dmg.pmml.RuleSelectionMethod;
import org.dmg.pmml.RuleSet;
import org.dmg.pmml.RuleSetModel;
import org.dmg.pmml.SimpleRule;

public class RuleSetModelEvaluator extends ModelEvaluator<RuleSetModel> implements HasEntityRegistry<SimpleRule> {

	public RuleSetModelEvaluator(PMML pmml){
		super(pmml, RuleSetModel.class);
	}

	public RuleSetModelEvaluator(PMML pmml, RuleSetModel ruleSetModel){
		super(pmml, ruleSetModel);
	}

	@Override
	public String getSummary(){
		return "Ruleset model";
	}

	@Override
	public BiMap<String, SimpleRule> getEntityRegistry(){
		return getValue(RuleSetModelEvaluator.entityCache);
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		RuleSetModel ruleSetModel = getModel();
		if(!ruleSetModel.isScorable()){
			throw new InvalidResultException(ruleSetModel);
		}

		Map<FieldName, ? extends Classification> predictions;

		MiningFunctionType miningFunction = ruleSetModel.getFunctionName();
		switch(miningFunction){
			case CLASSIFICATION:
				predictions = evaluateClassification(context);
				break;
			default:
				throw new UnsupportedFeatureException(ruleSetModel, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private Map<FieldName, ? extends Classification> evaluateClassification(ModelEvaluationContext context){
		RuleSetModel ruleSetModel = getModel();

		RuleSet ruleSet = ruleSetModel.getRuleSet();

		List<RuleSelectionMethod> ruleSelectionMethods = ruleSet.getRuleSelectionMethods();
		if(ruleSelectionMethods.size() < 1){
			throw new InvalidFeatureException(ruleSet);
		}

		// "If more than one method is included, the first method is used as the default method for scoring"
		RuleSelectionMethod ruleSelectionMethod = ruleSelectionMethods.get(0);

		// Both the ordering of keys and values is significant
		ListMultimap<String, SimpleRule> firedRules = LinkedListMultimap.create();

		evaluateRules(ruleSet.getRules(), firedRules, context);

		BiMap<String, SimpleRule> entityRegistry = getEntityRegistry();

		SimpleRuleScoreDistribution result = new SimpleRuleScoreDistribution(entityRegistry);

		// Return the default prediction when no rules in the ruleset fire
		if(firedRules.size() == 0){
			String score = ruleSet.getDefaultScore();

			result.put(new SimpleRule(score), score, ruleSet.getDefaultConfidence());

			return TargetUtil.evaluateClassification(result, context);
		}

		RuleSelectionMethod.Criterion criterion = ruleSelectionMethod.getCriterion();

		Set<String> keys = firedRules.keySet();
		for(String key : keys){
			List<SimpleRule> keyRules = firedRules.get(key);

			switch(criterion){
				case FIRST_HIT:
					{
						SimpleRule winner = keyRules.get(0);

						// The first value of the first key
						if(result.getEntity() == null){
							result.setEntity(winner);
						}

						result.put(key, winner.getConfidence());
					}
					break;
				case WEIGHTED_SUM:
					{
						SimpleRule winner = null;

						double totalWeight = 0;

						for(SimpleRule keyRule : keyRules){

							if(winner == null || (winner.getWeight() < keyRule.getWeight())){
								winner = keyRule;
							}

							totalWeight += keyRule.getWeight();
						}

						result.put(winner, key, totalWeight / firedRules.size());
					}
					break;
				case WEIGHTED_MAX:
					{
						SimpleRule winner = null;

						for(SimpleRule keyRule : keyRules){

							if(winner == null || (winner.getWeight() < keyRule.getWeight())){
								winner = keyRule;
							}
						}

						result.put(winner, key, winner.getConfidence());
					}
					break;
				default:
					throw new UnsupportedFeatureException(ruleSelectionMethod, criterion);
			}
		}

		return TargetUtil.evaluateClassification(result, context);
	}

	static
	private void evaluateRule(Rule rule, ListMultimap<String, SimpleRule> firedRules, EvaluationContext context){
		Predicate predicate = rule.getPredicate();
		if(predicate == null){
			throw new InvalidFeatureException(rule);
		}

		Boolean status = PredicateUtil.evaluate(predicate, context);
		if(status == null || !status.booleanValue()){
			return;
		} // End if

		if(rule instanceof SimpleRule){
			SimpleRule simpleRule = (SimpleRule)rule;

			firedRules.put(simpleRule.getScore(), simpleRule);
		} else

		if(rule instanceof CompoundRule){
			CompoundRule compoundRule = (CompoundRule)rule;

			evaluateRules(compoundRule.getRules(), firedRules, context);
		} else

		{
			throw new UnsupportedFeatureException(rule);
		}
	}

	static
	private void evaluateRules(List<Rule> rules, ListMultimap<String, SimpleRule> firedRules, EvaluationContext context){

		for(Rule rule : rules){
			evaluateRule(rule, firedRules, context);
		}
	}

	private static final LoadingCache<RuleSetModel, BiMap<String, SimpleRule>> entityCache = CacheUtil.buildLoadingCache(new CacheLoader<RuleSetModel, BiMap<String, SimpleRule>>(){

		@Override
		public BiMap<String, SimpleRule> load(RuleSetModel ruleSetModel){
			ImmutableBiMap.Builder<String, SimpleRule> builder = new ImmutableBiMap.Builder<>();

			RuleSet ruleSet = ruleSetModel.getRuleSet();

			builder = collectRules(ruleSet.getRules(), new AtomicInteger(1), builder);

			return builder.build();
		}

		private ImmutableBiMap.Builder<String, SimpleRule> collectRule(Rule rule, AtomicInteger index, ImmutableBiMap.Builder<String, SimpleRule> builder){

			if(rule instanceof SimpleRule){
				SimpleRule simpleRule = (SimpleRule)rule;

				builder = EntityUtil.put(simpleRule, index, builder);
			} else

			if(rule instanceof CompoundRule){
				CompoundRule compoundRule = (CompoundRule)rule;

				builder = collectRules(compoundRule.getRules(), index, builder);
			} else

			{
				throw new UnsupportedFeatureException(rule);
			}

			return builder;
		}

		private ImmutableBiMap.Builder<String, SimpleRule> collectRules(List<Rule> rules, AtomicInteger index, ImmutableBiMap.Builder<String, SimpleRule> builder){

			for(Rule rule : rules){
				builder = collectRule(rule, index, builder);
			}

			return builder;
		}
	});
}