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
package org.jpmml.evaluator.rule_set;

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
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.True;
import org.dmg.pmml.rule_set.CompoundRule;
import org.dmg.pmml.rule_set.PMMLAttributes;
import org.dmg.pmml.rule_set.PMMLElements;
import org.dmg.pmml.rule_set.Rule;
import org.dmg.pmml.rule_set.RuleSelectionMethod;
import org.dmg.pmml.rule_set.RuleSet;
import org.dmg.pmml.rule_set.RuleSetModel;
import org.dmg.pmml.rule_set.SimpleRule;
import org.jpmml.evaluator.CacheUtil;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.NumberUtil;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.PredicateUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.UndefinedResultException;
import org.jpmml.evaluator.UnsupportedAttributeException;
import org.jpmml.evaluator.UnsupportedElementException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;

public class RuleSetModelEvaluator extends ModelEvaluator<RuleSetModel> implements HasEntityRegistry<SimpleRule> {

	private BiMap<String, SimpleRule> entityRegistry = null;


	private RuleSetModelEvaluator(){
	}

	public RuleSetModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, RuleSetModel.class));
	}

	public RuleSetModelEvaluator(PMML pmml, RuleSetModel ruleSetModel){
		super(pmml, ruleSetModel);

		RuleSet ruleSet = ruleSetModel.getRuleSet();
		if(ruleSet == null){
			throw new MissingElementException(ruleSetModel, PMMLElements.RULESETMODEL_RULESET);
		} // End if

		if(!ruleSet.hasRuleSelectionMethods()){
			throw new MissingElementException(ruleSet, PMMLElements.RULESET_RULESELECTIONMETHODS);
		}
	}

	@Override
	public String getSummary(){
		return "Ruleset model";
	}

	@Override
	public BiMap<String, SimpleRule> getEntityRegistry(){

		if(this.entityRegistry == null){
			this.entityRegistry = getValue(RuleSetModelEvaluator.entityCache);
		}

		return this.entityRegistry;
	}

	@Override
	protected <V extends Number> Map<FieldName, ? extends Classification<?, V>> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		RuleSetModel ruleSetModel = getModel();

		RuleSet ruleSet = ruleSetModel.getRuleSet();

		TargetField targetField = getTargetField();

		List<RuleSelectionMethod> ruleSelectionMethods = ruleSet.getRuleSelectionMethods();

		// "If more than one method is included, then the first method is used as the default method for scoring"
		RuleSelectionMethod ruleSelectionMethod = ruleSelectionMethods.get(0);

		// Both the ordering of keys and values is significant
		ListMultimap<Object, SimpleRule> firedRules = LinkedListMultimap.create();

		evaluateRules(ruleSet.getRules(), firedRules, context);

		SimpleRuleScoreDistribution<V> result = new SimpleRuleScoreDistribution<V>(new ValueMap<Object, V>(2 * firedRules.size())){

			@Override
			public BiMap<String, SimpleRule> getEntityRegistry(){
				return RuleSetModelEvaluator.this.getEntityRegistry();
			}
		};

		// Return the default prediction when no rules in the ruleset fire
		if(firedRules.isEmpty()){
			Object defaultScore = ruleSet.getDefaultScore();
			if(defaultScore == null){
				throw new MissingAttributeException(ruleSet, PMMLAttributes.RULESET_DEFAULTSCORE);
			}

			Number defaultConfidence = ruleSet.getDefaultConfidence();
			if(defaultConfidence == null){
				throw new MissingAttributeException(ruleSet, PMMLAttributes.RULESET_DEFAULTCONFIDENCE);
			}

			Value<V> value = valueFactory.newValue(defaultConfidence);

			result.put(new SimpleRule(defaultScore, True.INSTANCE), defaultScore, value);

			return TargetUtil.evaluateClassification(targetField, result);
		}

		RuleSelectionMethod.Criterion criterion = ruleSelectionMethod.getCriterion();
		if(criterion == null){
			throw new MissingAttributeException(ruleSelectionMethod, PMMLAttributes.RULESELECTIONMETHOD_CRITERION);
		}

		Set<?> keys = firedRules.keySet();
		for(Object key : keys){
			List<SimpleRule> keyRules = firedRules.get(key);

			switch(criterion){
				case FIRST_HIT:
					{
						SimpleRule winner = keyRules.get(0);

						// The first value of the first key
						if(result.getEntity() == null){
							result.setEntity(winner);
						}

						Value<V> value = valueFactory.newValue(winner.getConfidence());

						result.put(key, value);
					}
					break;
				case WEIGHTED_SUM:
					{
						SimpleRule winner = null;

						Value<V> totalWeight = valueFactory.newValue();

						for(SimpleRule keyRule : keyRules){
							Number weight = keyRule.getWeight();

							if(winner == null || NumberUtil.compare(winner.getWeight(), weight) < 0){
								winner = keyRule;
							}

							totalWeight.add(weight);
						}

						int size = firedRules.size();
						if(size == 0){
							throw new UndefinedResultException();
						}

						Value<V> value = totalWeight.divide(size);

						result.put(winner, key, value);
					}
					break;
				case WEIGHTED_MAX:
					{
						SimpleRule winner = null;

						for(SimpleRule keyRule : keyRules){

							if(winner == null || NumberUtil.compare(winner.getWeight(), keyRule.getWeight()) < 0){
								winner = keyRule;
							}
						}

						Value<V> value = valueFactory.newValue(winner.getConfidence());

						result.put(winner, key, value);
					}
					break;
				default:
					throw new UnsupportedAttributeException(ruleSelectionMethod, criterion);
			}
		}

		return TargetUtil.evaluateClassification(targetField, result);
	}

	static
	private void evaluateRules(List<Rule> rules, ListMultimap<Object, SimpleRule> firedRules, EvaluationContext context){

		for(Rule rule : rules){
			evaluateRule(rule, firedRules, context);
		}
	}

	static
	private void evaluateRule(Rule rule, ListMultimap<Object, SimpleRule> firedRules, EvaluationContext context){
		Boolean status = PredicateUtil.evaluatePredicateContainer(rule, context);

		if(status == null || !status.booleanValue()){
			return;
		} // End if

		if(rule instanceof SimpleRule){
			SimpleRule simpleRule = (SimpleRule)rule;

			Object score = simpleRule.getScore();
			if(score == null){
				throw new MissingAttributeException(simpleRule, PMMLAttributes.SIMPLERULE_SCORE);
			}

			firedRules.put(score, simpleRule);
		} else

		if(rule instanceof CompoundRule){
			CompoundRule compoundRule = (CompoundRule)rule;

			evaluateRules(compoundRule.getRules(), firedRules, context);
		} else

		{
			throw new UnsupportedElementException(rule);
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
				throw new UnsupportedElementException(rule);
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