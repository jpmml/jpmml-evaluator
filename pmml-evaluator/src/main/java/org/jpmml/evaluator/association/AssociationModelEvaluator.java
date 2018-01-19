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
import java.util.concurrent.Callable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
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
import org.dmg.pmml.TypeDefinitionField;
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
	private List<ItemValue> itemValues = null;


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

		Set<String> activeItems = getActiveItemIds(context);

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
	Set<String> getActiveItemIds(EvaluationContext context){
		AssociationModel associationModel = getModel();

		MiningSchema miningSchema = associationModel.getMiningSchema();

		List<InputField> activeFields = getActiveFields();
		List<InputField> groupFields = getGroupFields();

		Set<String> result = new HashSet<>();

		Map<FieldName, Set<FieldValue>> explodedValues = null;

		List<ItemValue> itemValues = getItemValues();
		for(ItemValue itemValue : itemValues){
			String id = itemValue.getId();
			FieldName name = itemValue.getField();
			String category = itemValue.getCategory();

			FieldValue value = context.evaluate(name);

			if(groupFields.size() == 0){

				if(value == null){
					continue;
				} // End if

				// "The item values are based on field names when the field has only true/false values"
				if(category == null){

					if((AssociationModelEvaluator.TRUE).equalsValue(value) || value.equalsString("T")){
						result.add(id);
					} else

					if((AssociationModelEvaluator.FALSE).equalsValue(value) || value.equalsString("F")){
						// Ignored
					} else

					{
						throw new EvaluationException();
					}
				} else

				{
					if(value.equalsString(category)){
						result.add(id);
					}
				}
			} else

			if(groupFields.size() == 1){

				if(value == null){
					throw new MissingValueException(name);
				} // End if

				if(explodedValues == null){
					explodedValues = new HashMap<>();
				}

				Set<FieldValue> explodedValue = explodedValues.get(name);
				if(explodedValue == null){
					explodedValue = new HashSet<>();

					Collection<?> objects = FieldValueUtil.getValue(Collection.class, value);
					for(Object object : objects){
						explodedValue.add(FieldValueUtil.create(value.getDataType(), value.getOpType(), object));
					}
				} // End if

				if(category == null){
					throw new EvaluationException();
				} else

				{
					FieldValue categoryValue = FieldValueUtil.create(value.getDataType(), value.getOpType(), category);

					if(explodedValue.contains(categoryValue)){
						result.add(id);
					}
				}
			} else

			{
				throw new InvalidFeatureException(miningSchema);
			}
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

	private List<ItemValue> getItemValues(){

		if(this.itemValues == null){
			this.itemValues = getValue(AssociationModelEvaluator.itemValueCache, createItemValueLoader(this));
		}

		return this.itemValues;
	}

	static
	private Callable<List<ItemValue>> createItemValueLoader(final AssociationModelEvaluator modelEvaluator){
		return new Callable<List<ItemValue>>(){

			@Override
			public List<ItemValue> call(){
				return parseItemValues(modelEvaluator);
			}
		};
	}

	static
	private List<ItemValue> parseItemValues(AssociationModelEvaluator modelEvaluator){
		AssociationModel associationModel = modelEvaluator.getModel();

		MiningSchema miningSchema = associationModel.getMiningSchema();

		List<InputField> activeFields = modelEvaluator.getActiveFields();
		List<InputField> groupFields = modelEvaluator.getGroupFields();

		List<ItemValue> result = new ArrayList<>();

		List<Item> items = associationModel.getItems();
		for(Item item : items){
			String id = item.getId();
			String value = item.getValue();

			if(value == null){
				throw new InvalidFeatureException(item);
			}

			FieldName name = item.getField();
			String category = item.getCategory();

			parser:
			if(name == null){

				// Categorical data style: no group fields, one or more active fields
				if(groupFields.size() == 0){

					if(activeFields.size() < 1){
						throw new InvalidFeatureException("No active fields", miningSchema);
					}

					name = FieldName.create(value);
					category = null;

					TypeDefinitionField field = modelEvaluator.resolveField(name);
					if(field != null){
						break parser;
					}

					int index = value.indexOf('=');
					if(index > -1){
						name = FieldName.create(value.substring(0, index));
						category = value.substring(index + 1);

						field = modelEvaluator.resolveField(name);
						if(field != null){
							break parser;
						}
					}

					throw new InvalidFeatureException(item);
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

					name = activeField.getName();
					category = value;
				} else

				{
					throw new InvalidFeatureException(miningSchema);
				}
			}

			ItemValue itemValue = new ItemValue(id, name, category);

			result.add(itemValue);
		}

		return ImmutableList.copyOf(result);
	}

	static
	private class ItemValue {

		private String id = null;

		private FieldName field = null;

		private String category = null;


		private ItemValue(String id, FieldName field, String category){
			setId(id);
			setField(field);
			setCategory(category);
		}

		private String getId(){
			return this.id;
		}

		private void setId(String id){
			this.id = id;
		}

		private FieldName getField(){
			return this.field;
		}

		private void setField(FieldName field){
			this.field = field;
		}

		private String getCategory(){
			return this.category;
		}

		private void setCategory(String category){
			this.category = category;
		}
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

	private static final Cache<AssociationModel, List<ItemValue>> itemValueCache = CacheUtil.buildCache();
}