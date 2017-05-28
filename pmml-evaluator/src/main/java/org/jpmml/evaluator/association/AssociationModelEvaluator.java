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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Target;
import org.dmg.pmml.Targets;
import org.dmg.pmml.association.AssociationModel;
import org.dmg.pmml.association.AssociationRule;
import org.dmg.pmml.association.Item;
import org.dmg.pmml.association.ItemRef;
import org.dmg.pmml.association.Itemset;
import org.jpmml.evaluator.CacheUtil;
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.HasGroupFields;
import org.jpmml.evaluator.IndexableUtil;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.InvalidFeatureException;
import org.jpmml.evaluator.InvalidResultException;
import org.jpmml.evaluator.MissingValueException;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TypeUtil;
import org.jpmml.evaluator.UnsupportedFeatureException;

public class AssociationModelEvaluator extends ModelEvaluator<AssociationModel> implements HasGroupFields, HasEntityRegistry<AssociationRule> {

	transient
	private List<InputField> groupInputFields = null;

	transient
	private BiMap<String, AssociationRule> entityRegistry = null;

	transient
	private Map<String, Item> items = null;

	transient
	private Map<String, Itemset> itemsets = null;

	transient
	private BiMap<String, String> itemValues = null;


	public AssociationModelEvaluator(PMML pmml){
		this(pmml, selectModel(pmml, AssociationModel.class));
	}

	public AssociationModelEvaluator(PMML pmml, AssociationModel associationModel){
		super(pmml, associationModel);

		Targets targets = associationModel.getTargets();
		if(targets != null){
			throw new InvalidFeatureException(targets);
		}
	}

	@Override
	public String getSummary(){
		return "Association rules";
	}

	@Override
	public List<InputField> getGroupFields(){

		if(this.groupInputFields == null){
			this.groupInputFields = createInputFields(MiningField.UsageType.GROUP);
		}

		return this.groupInputFields;
	}

	/**
	 * @return <code>null</code> Always.
	 */
	@Override
	public Target getTarget(FieldName name){
		return null;
	}

	@Override
	public FieldName getTargetFieldName(){
		return Evaluator.DEFAULT_TARGET_NAME;
	}

	@Override
	public BiMap<String, AssociationRule> getEntityRegistry(){

		if(this.entityRegistry == null){
			this.entityRegistry = getValue(AssociationModelEvaluator.entityCache);
		}

		return this.entityRegistry;
	}

	@Override
	protected List<TargetField> createTargetFields(){
		List<TargetField> targetFields = super.createTargetFields();

		if(targetFields.size() > 0){
			AssociationModel associationModel = getModel();

			MiningSchema miningSchema = associationModel.getMiningSchema();

			throw new InvalidFeatureException("Too many target fields", miningSchema);
		}

		return targetFields;
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		AssociationModel associationModel = getModel();
		if(!associationModel.isScorable()){
			throw new InvalidResultException(associationModel);
		}

		MathContext mathContext = associationModel.getMathContext();
		switch(mathContext){
			case DOUBLE:
				break;
			default:
				throw new UnsupportedFeatureException(associationModel, mathContext);
		}

		Map<FieldName, Association> predictions;

		MiningFunction miningFunction = associationModel.getMiningFunction();
		switch(miningFunction){
			case ASSOCIATION_RULES:
				predictions = evaluateAssociationRules(context);
				break;
			default:
				throw new UnsupportedFeatureException(associationModel, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private Map<FieldName, Association> evaluateAssociationRules(EvaluationContext context){
		AssociationModel associationModel = getModel();

		Set<String> activeItems = getActiveItems(context);

		Map<String, Boolean> flags = new HashMap<>();

		List<Itemset> itemsets = associationModel.getItemsets();
		for(Itemset itemset : itemsets){
			flags.put(itemset.getId(), isSubset(activeItems, itemset));
		}

		List<AssociationRule> associationRules = associationModel.getAssociationRules();

		BitSet antecedentFlags = new BitSet(associationRules.size());
		BitSet consequentFlags = new BitSet(associationRules.size());

		for(int i = 0; i < associationRules.size(); i++){
			AssociationRule associationRule = associationRules.get(i);

			Boolean antecedentFlag = flags.get(associationRule.getAntecedent());
			if(antecedentFlag == null){
				throw new InvalidFeatureException(associationRule);
			}

			antecedentFlags.set(i, antecedentFlag);

			Boolean consequentFlag = flags.get(associationRule.getConsequent());
			if(consequentFlag == null){
				throw new InvalidFeatureException(associationRule);
			}

			consequentFlags.set(i, consequentFlag);
		}

		Association association = new Association(associationRules, antecedentFlags, consequentFlags){

			@Override
			public Map<String, Item> getItems(){
				return AssociationModelEvaluator.this.getItems();
			}

			@Override
			public Map<String, Itemset> getItemsets(){
				return AssociationModelEvaluator.this.getItemsets();
			}

			@Override
			public BiMap<String, AssociationRule> getAssociationRuleRegistry(){
				return AssociationModelEvaluator.this.getEntityRegistry();
			}
		};

		return Collections.singletonMap(getTargetFieldName(), association);
	}

	/**
	 * @return A set of {@link Item#getId() Item identifiers}.
	 */
	Set<String> getActiveItems(EvaluationContext context){
		AssociationModel associationModel = getModel();

		List<InputField> activeFields = getActiveFields();
		List<InputField> groupFields = getGroupFields();

		MiningSchema miningSchema = associationModel.getMiningSchema();

		List<String> itemValues = new ArrayList<>();

		// Categorical data style: no group fields, one or more active fields
		if(groupFields.size() == 0){

			if(activeFields.size() < 1){
				throw new InvalidFeatureException("No active fields", miningSchema);
			}

			for(InputField activeField : activeFields){
				FieldName name = activeField.getName();
				FieldValue value = context.evaluate(name);

				if(value == null){
					continue;
				}

				OpType opType = value.getOpType();
				switch(opType){
					case CATEGORICAL:
						{
							if((AssociationModelEvaluator.TRUE).equalsValue(value) || value.equalsString("T")){
								// "The item values are based on field names when the field has only true/false values"
								itemValues.add(name.getValue());
							} else

							if((AssociationModelEvaluator.FALSE).equalsValue(value) || value.equalsString("F")){
								continue;
							} else

							{
								Object object = value.getValue();

								// "The item values are based on field name followed by "=" and field value (category) when the field is a regular categorical field"
								itemValues.add(name.getValue() + "=" + TypeUtil.format(object));
							}
						}
						break;
					default:
						throw new EvaluationException();
				}
			}
		} else

		// Transactional data style: one group field, one active field
		if(groupFields.size() == 1){

			if(activeFields.size() < 1){
				throw new InvalidFeatureException("No active fields", miningSchema);
			} else

			if(activeFields.size() > 1){
				throw new InvalidFeatureException("Too many active fields", miningSchema);
			}

			InputField activeField = activeFields.get(0);

			FieldName name = activeField.getName();

			FieldValue value = context.evaluate(name);
			if(value == null){
				throw new MissingValueException(name);
			}

			Collection<?> objects = FieldValueUtil.getValue(Collection.class, value);
			for(Object object : objects){
				itemValues.add(TypeUtil.format(object));
			}
		} else

		{
			throw new InvalidFeatureException(miningSchema);
		}

		Set<String> result = new HashSet<>();

		Map<String, String> valueItems = (getItemValues().inverse());

		for(String itemValue : itemValues){
			String itemId = valueItems.get(itemValue);

			if(itemId == null){
				context.addWarning("Unknown item value \"" + itemValue + "\"");

				continue;
			}

			result.add(itemId);
		}

		return result;
	}

	static
	private boolean isSubset(Set<String> items, Itemset itemset){
		boolean result = true;

		List<ItemRef> itemRefs = itemset.getItemRefs();
		for(ItemRef itemRef : itemRefs){
			result &= items.contains(itemRef.getItemRef());

			if(!result){
				return false;
			}
		}

		return result;
	}

	private Map<String, Item> getItems(){

		if(this.items == null){
			this.items = getValue(AssociationModelEvaluator.itemCache);
		}

		return this.items;
	}

	private Map<String, Itemset> getItemsets(){

		if(this.itemsets == null){
			this.itemsets = getValue(AssociationModelEvaluator.itemsetCache);
		}

		return this.itemsets;
	}

	/**
	 * @return A bidirectional map between {@link Item#getId() Item identifiers} and {@link Item#getValue() Item values}.
	 */
	private BiMap<String, String> getItemValues(){

		if(this.itemValues == null){
			this.itemValues = getValue(AssociationModelEvaluator.itemValueCache);
		}

		return this.itemValues;
	}

	static
	private BiMap<String, String> parseItemValues(AssociationModel associationModel){
		BiMap<String, String> result = HashBiMap.create();

		List<Item> items = associationModel.getItems();
		for(Item item : items){
			result.put(item.getId(), item.getValue());
		}

		return result;
	}

	private static final FieldValue TRUE = FieldValueUtil.create(DataType.BOOLEAN, OpType.CATEGORICAL, true);
	private static final FieldValue FALSE = FieldValueUtil.create(DataType.BOOLEAN, OpType.CATEGORICAL, false);

	private static final LoadingCache<AssociationModel, BiMap<String, AssociationRule>> entityCache = CacheUtil.buildLoadingCache(new CacheLoader<AssociationModel, BiMap<String, AssociationRule>>(){

		@Override
		public BiMap<String, AssociationRule> load(AssociationModel associationModel){
			return EntityUtil.buildBiMap(associationModel.getAssociationRules());
		}
	});

	private static final LoadingCache<AssociationModel, Map<String, Item>> itemCache = CacheUtil.buildLoadingCache(new CacheLoader<AssociationModel, Map<String, Item>>(){

		@Override
		public Map<String, Item> load(AssociationModel associationModel){
			return IndexableUtil.buildMap(associationModel.getItems());
		}
	});

	private static final LoadingCache<AssociationModel, Map<String, Itemset>> itemsetCache = CacheUtil.buildLoadingCache(new CacheLoader<AssociationModel, Map<String, Itemset>>(){

		@Override
		public Map<String, Itemset> load(AssociationModel associationModel){
			return IndexableUtil.buildMap(associationModel.getItemsets());
		}
	});

	private static final LoadingCache<AssociationModel, BiMap<String, String>> itemValueCache = CacheUtil.buildLoadingCache(new CacheLoader<AssociationModel, BiMap<String, String>>(){

		@Override
		public BiMap<String, String> load(AssociationModel associationModel){
			return ImmutableBiMap.copyOf(parseItemValues(associationModel));
		}
	});
}