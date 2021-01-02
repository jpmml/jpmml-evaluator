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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Target;
import org.dmg.pmml.Targets;
import org.dmg.pmml.association.AssociationModel;
import org.dmg.pmml.association.AssociationRule;
import org.dmg.pmml.association.Item;
import org.dmg.pmml.association.ItemRef;
import org.dmg.pmml.association.Itemset;
import org.dmg.pmml.association.PMMLAttributes;
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.FieldValues;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.HasGroupFields;
import org.jpmml.evaluator.IndexableUtil;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.InvalidAttributeException;
import org.jpmml.evaluator.MisplacedElementException;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingValueException;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.PMMLException;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TypeInfos;
import org.jpmml.evaluator.ValueFactory;

public class AssociationModelEvaluator extends ModelEvaluator<AssociationModel> implements HasGroupFields, HasEntityRegistry<AssociationRule> {

	private BiMap<String, AssociationRule> entityRegistry = ImmutableBiMap.of();

	private Map<String, Item> items = Collections.emptyMap();

	private Map<String, Itemset> itemsets = Collections.emptyMap();

	private List<InputField> groupInputFields = null;

	private List<ItemValue> itemValues = null;


	private AssociationModelEvaluator(){
	}

	public AssociationModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, AssociationModel.class));
	}

	public AssociationModelEvaluator(PMML pmml, AssociationModel associationModel){
		super(pmml, associationModel);

		Targets targets = associationModel.getTargets();
		if(targets != null){
			throw new MisplacedElementException(targets);
		} // End if

		if(associationModel.hasAssociationRules()){
			this.entityRegistry = ImmutableBiMap.copyOf(EntityUtil.buildBiMap(associationModel.getAssociationRules()));
		} // End if

		if(associationModel.hasItems()){
			this.items = ImmutableMap.copyOf(IndexableUtil.buildMap(associationModel.getItems(), PMMLAttributes.ITEM_ID));
		} // End if

		if(associationModel.hasItemsets()){
			this.itemsets = ImmutableMap.copyOf(IndexableUtil.buildMap(associationModel.getItemsets(), PMMLAttributes.ITEMSET_ID));
		}
	}

	@Override
	public String getSummary(){
		return "Association rules";
	}

	@Override
	public List<InputField> getGroupFields(){

		if(this.groupInputFields == null){
			List<InputField> groupInputFields = filterInputFields(createInputFields(MiningField.UsageType.GROUP));

			this.groupInputFields = ImmutableList.copyOf(groupInputFields);
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
	public FieldName getTargetName(){
		return Evaluator.DEFAULT_TARGET_NAME;
	}

	@Override
	public BiMap<String, AssociationRule> getEntityRegistry(){
		return this.entityRegistry;
	}

	@Override
	protected List<TargetField> createTargetFields(){
		List<TargetField> targetFields = super.createTargetFields();

		if(!targetFields.isEmpty()){
			throw createMiningSchemaException("Expected 0 target fields, got " + targetFields.size() + " target fields");
		}

		return targetFields;
	}

	@Override
	protected <V extends Number> Map<FieldName, Association> evaluateAssociationRules(ValueFactory<V> valueFactory, EvaluationContext context){
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

			String antecedent = associationRule.getAntecedent();
			if(antecedent == null){
				throw new MissingAttributeException(associationRule, PMMLAttributes.ASSOCIATIONRULE_ANTECEDENT);
			}

			Boolean antecedentFlag = flags.get(antecedent);
			if(antecedentFlag == null){
				throw new InvalidAttributeException(associationRule, PMMLAttributes.ASSOCIATIONRULE_ANTECEDENT, antecedent);
			}

			antecedentFlags.set(i, antecedentFlag);

			String consequent = associationRule.getConsequent();
			if(consequent == null){
				throw new MissingAttributeException(associationRule, PMMLAttributes.ASSOCIATIONRULE_CONSEQUENT);
			}

			Boolean consequentFlag = flags.get(consequent);
			if(consequentFlag == null){
				throw new InvalidAttributeException(associationRule, PMMLAttributes.ASSOCIATIONRULE_CONSEQUENT, consequent);
			}

			consequentFlags.set(i, consequentFlag);
		}

		Association association = new Association(associationRules, antecedentFlags, consequentFlags){

			@Override
			public BiMap<String, AssociationRule> getEntityRegistry(){
				return AssociationModelEvaluator.this.getEntityRegistry();
			}

			@Override
			public Map<String, Item> getItems(){
				return AssociationModelEvaluator.this.getItems();
			}

			@Override
			public Map<String, Itemset> getItemsets(){
				return AssociationModelEvaluator.this.getItemsets();
			}
		};

		return Collections.singletonMap(getTargetName(), association);
	}

	/**
	 * @return A set of {@link Item#getId() Item identifiers}.
	 */
	Set<String> getActiveItemIds(EvaluationContext context){
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

			if(groupFields.isEmpty()){

				if(FieldValueUtil.isMissing(value)){
					continue;
				} // End if

				// "The item values are based on field names when the field has only true/false values"
				if(category == null){
					DataType dataType = value.getDataType();

					switch(dataType){
						case STRING:
							if((AssociationModelEvaluator.STRING_TRUE).equalsValue(value)){
								result.add(id);

								break;
							} else

							if((AssociationModelEvaluator.STRING_FALSE).equalsValue(value)){
								break;
							}
							// Falls through
						default:
							if((AssociationModelEvaluator.BOOLEAN_TRUE).equalsValue(value)){
								result.add(id);

								break;
							} else

							if((AssociationModelEvaluator.BOOLEAN_FALSE).equalsValue(value)){
								break;
							}

							throw new EvaluationException("Expected " + PMMLException.formatValue(AssociationModelEvaluator.BOOLEAN_FALSE) + " or " + PMMLException.formatValue(AssociationModelEvaluator.BOOLEAN_TRUE) + ", got " + PMMLException.formatValue(value));
					}
				} else

				{
					if(value.equalsValue(category)){
						result.add(id);
					}
				}
			} else

			if(groupFields.size() == 1){

				if(FieldValueUtil.isMissing(value)){
					throw new MissingValueException(name);
				} // End if

				if(explodedValues == null){
					explodedValues = new HashMap<>();
				}

				Set<FieldValue> explodedValue = explodedValues.get(name);
				if(explodedValue == null){
					explodedValue = new HashSet<>();

					Collection<?> objects = value.asCollection();
					for(Object object : objects){
						explodedValue.add(FieldValueUtil.create(value, object));
					}
				} // End if

				if(category == null){
					throw new IllegalStateException();
				} else

				{
					FieldValue categoryValue = FieldValueUtil.create(value, category);

					if(explodedValue.contains(categoryValue)){
						result.add(id);
					}
				}
			} else

			{
				throw createMiningSchemaException("Expected 0 or 1 group field(s), got " + groupFields.size() + " group fields");
			}
		}

		return result;
	}

	private Map<String, Item> getItems(){
		return this.items;
	}

	private Map<String, Itemset> getItemsets(){
		return this.itemsets;
	}

	private List<ItemValue> getItemValues(){

		if(this.itemValues == null){
			this.itemValues = ImmutableList.copyOf(parseItemValues(this));
		}

		return this.itemValues;
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

	static
	private List<ItemValue> parseItemValues(AssociationModelEvaluator modelEvaluator){
		AssociationModel associationModel = modelEvaluator.getModel();

		List<InputField> activeFields = modelEvaluator.getActiveFields();
		List<InputField> groupFields = modelEvaluator.getGroupFields();

		List<ItemValue> result = new ArrayList<>();

		List<Item> items = associationModel.getItems();
		for(Item item : items){
			String id = item.getId();
			if(id == null){
				throw new MissingAttributeException(item, PMMLAttributes.ITEM_ID);
			}

			String value = item.getValue();
			if(value == null){
				throw new MissingAttributeException(item, PMMLAttributes.ITEM_VALUE);
			}

			FieldName name = item.getField();
			String category = item.getCategory();

			parser:
			if(name == null){

				// Categorical data style: no group fields, one or more active fields
				if(groupFields.isEmpty()){

					if(activeFields.isEmpty()){
						throw modelEvaluator.createMiningSchemaException("Expected 1 or more active field(s), got " + activeFields.size() + " active fields");
					}

					name = FieldName.create(value);
					category = null;

					Field<?> field = modelEvaluator.resolveField(name);
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

					throw new InvalidAttributeException(item, PMMLAttributes.ITEM_VALUE, value);
				} else

				// Transactional data style: one group field, one active field
				if(groupFields.size() == 1){

					if(activeFields.size() != 1){
						throw modelEvaluator.createMiningSchemaException("Expected 1 active field, got " + activeFields.size() + " active fields");
					}

					InputField activeField = activeFields.get(0);

					name = activeField.getFieldName();
					category = value;
				} else

				{
					throw modelEvaluator.createMiningSchemaException("Expected 0 or 1 group field(s), got " + groupFields.size() + " group fields");
				}
			} else

			{
				if(groupFields.size() == 1){

					if(category != null){
						break parser;
					}
				} // End if

				if(category == null){
					throw new MissingAttributeException(item, PMMLAttributes.ITEM_CATEGORY);
				}
			}

			ItemValue itemValue = new ItemValue(id, name, category);

			result.add(itemValue);
		}

		return result;
	}

	static
	private class ItemValue implements Serializable {

		private String id = null;

		private FieldName field = null;

		private String category = null;


		private ItemValue(){
		}

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

	// IBM SPSS-style schema
	private static final FieldValue STRING_TRUE = FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, "T");
	private static final FieldValue STRING_FALSE = FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, "F");

	private static final FieldValue BOOLEAN_TRUE = FieldValues.CATEGORICAL_BOOLEAN_TRUE;
	private static final FieldValue BOOLEAN_FALSE = FieldValues.CATEGORICAL_BOOLEAN_FALSE;
}