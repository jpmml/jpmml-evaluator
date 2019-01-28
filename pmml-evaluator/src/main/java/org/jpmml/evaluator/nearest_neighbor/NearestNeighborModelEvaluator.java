/*
 * Copyright (c) 2013 KNIME.com AG, Zurich, Switzerland
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jpmml.evaluator.nearest_neighbor;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import org.dmg.pmml.ComparisonMeasure;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Distance;
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.Measure;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Similarity;
import org.dmg.pmml.nearest_neighbor.InstanceField;
import org.dmg.pmml.nearest_neighbor.InstanceFields;
import org.dmg.pmml.nearest_neighbor.KNNInput;
import org.dmg.pmml.nearest_neighbor.KNNInputs;
import org.dmg.pmml.nearest_neighbor.NearestNeighborModel;
import org.dmg.pmml.nearest_neighbor.TrainingInstances;
import org.jpmml.evaluator.AffinityDistribution;
import org.jpmml.evaluator.CacheUtil;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.ExpressionUtil;
import org.jpmml.evaluator.FieldUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.FieldValues;
import org.jpmml.evaluator.InlineTableUtil;
import org.jpmml.evaluator.InputFieldUtil;
import org.jpmml.evaluator.InvalidAttributeException;
import org.jpmml.evaluator.InvalidElementException;
import org.jpmml.evaluator.InvisibleFieldException;
import org.jpmml.evaluator.MeasureUtil;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.MissingFieldException;
import org.jpmml.evaluator.MissingValueException;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.PMMLAttributes;
import org.jpmml.evaluator.PMMLElements;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TypeInfo;
import org.jpmml.evaluator.TypeInfos;
import org.jpmml.evaluator.TypeUtil;
import org.jpmml.evaluator.UnsupportedAttributeException;
import org.jpmml.evaluator.UnsupportedElementException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueAggregator;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.evaluator.VoteAggregator;
import org.jpmml.model.visitors.FieldReferenceFinder;

public class NearestNeighborModelEvaluator extends ModelEvaluator<NearestNeighborModel> {

	transient
	private Table<Integer, FieldName, FieldValue> trainingInstances = null;

	transient
	private Map<Integer, BitSet> instanceFlags = null;

	transient
	private Map<Integer, List<FieldValue>> instanceValues = null;


	public NearestNeighborModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, NearestNeighborModel.class));
	}

	public NearestNeighborModelEvaluator(PMML pmml, NearestNeighborModel nearestNeighborModel){
		super(pmml, nearestNeighborModel);

		ComparisonMeasure comparisoonMeasure = nearestNeighborModel.getComparisonMeasure();
		if(comparisoonMeasure == null){
			throw new MissingElementException(nearestNeighborModel, PMMLElements.NEARESTNEIGHBORMODEL_COMPARISONMEASURE);
		}

		TrainingInstances trainingInstances = nearestNeighborModel.getTrainingInstances();
		if(trainingInstances == null){
			throw new MissingElementException(nearestNeighborModel, PMMLElements.NEARESTNEIGHBORMODEL_TRAININGINSTANCES);
		}

		InstanceFields instanceFields = trainingInstances.getInstanceFields();
		if(instanceFields == null){
			throw new MissingElementException(trainingInstances, PMMLElements.TRAININGINSTANCES_INSTANCEFIELDS);
		} // End if

		if(!instanceFields.hasInstanceFields()){
			throw new MissingElementException(instanceFields, PMMLElements.INSTANCEFIELDS_INSTANCEFIELDS);
		}

		KNNInputs knnInputs = nearestNeighborModel.getKNNInputs();
		if(knnInputs == null){
			throw new MissingElementException(nearestNeighborModel, PMMLElements.NEARESTNEIGHBORMODEL_KNNINPUTS);
		} // End if

		if(!knnInputs.hasKNNInputs()){
			throw new MissingElementException(knnInputs, PMMLElements.KNNINPUTS_KNNINPUTS);
		}
	}

	@Override
	public String getSummary(){
		return "k-Nearest neighbors model";
	}

	@Override
	protected DataField getDataField(){
		MiningFunction miningFunction = getMiningFunction();

		switch(miningFunction){
			case REGRESSION:
			case CLASSIFICATION:
			case MIXED:
				return null;
			default:
				return super.getDataField();
		}
	}

	@Override
	protected <V extends Number> Map<FieldName, AffinityDistribution<V>> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateMixed(valueFactory, context);
	}

	@Override
	protected <V extends Number> Map<FieldName, AffinityDistribution<V>> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateMixed(valueFactory, context);
	}

	@Override
	protected <V extends Number> Map<FieldName, AffinityDistribution<V>> evaluateMixed(ValueFactory<V> valueFactory, EvaluationContext context){
		NearestNeighborModel nearestNeighborModel = getModel();

		Table<Integer, FieldName, FieldValue> table = getTrainingInstances();

		List<InstanceResult<V>> instanceResults = evaluateInstanceRows(valueFactory, context);

		Ordering<InstanceResult<V>> ordering = (Ordering.natural()).reverse();

		List<InstanceResult<V>> nearestInstanceResults = ordering.sortedCopy(instanceResults);

		int numberOfNeighbors = nearestNeighborModel.getNumberOfNeighbors();

		nearestInstanceResults = nearestInstanceResults.subList(0, numberOfNeighbors);

		Function<Integer, String> function = new Function<Integer, String>(){

			@Override
			public String apply(Integer row){
				return row.toString();
			}
		};

		FieldName instanceIdVariable = nearestNeighborModel.getInstanceIdVariable();
		if(instanceIdVariable != null){
			function = createIdentifierResolver(instanceIdVariable, table);
		}

		Map<FieldName, AffinityDistribution<V>> results = new LinkedHashMap<>();

		List<TargetField> targetFields = getTargetFields();
		for(TargetField targetField : targetFields){
			FieldName name = targetField.getName();

			Object value;

			OpType opType = targetField.getOpType();
			switch(opType){
				case CONTINUOUS:
					value = calculateContinuousTarget(valueFactory, name, nearestInstanceResults, table);
					break;
				case CATEGORICAL:
					value = calculateCategoricalTarget(valueFactory, name, nearestInstanceResults, table);
					break;
				default:
					throw new InvalidElementException(nearestNeighborModel);
			}

			value = TypeUtil.parseOrCast(targetField.getDataType(), value);

			AffinityDistribution<V> result = createAffinityDistribution(instanceResults, function, value);

			results.put(name, result);
		}

		return results;
	}

	@Override
	protected <V extends Number> Map<FieldName, AffinityDistribution<V>> evaluateClustering(ValueFactory<V> valueFactory, EvaluationContext context){
		NearestNeighborModel nearestNeighborModel = getModel();

		Table<Integer, FieldName, FieldValue> table = getTrainingInstances();

		List<InstanceResult<V>> instanceResults = evaluateInstanceRows(valueFactory, context);

		FieldName instanceIdVariable = nearestNeighborModel.getInstanceIdVariable();
		if(instanceIdVariable == null){
			throw new MissingAttributeException(nearestNeighborModel, PMMLAttributes.NEARESTNEIGHBORMODEL_INSTANCEIDVARIABLE);
		}

		Function<Integer, String> function = createIdentifierResolver(instanceIdVariable, table);

		AffinityDistribution<V> result = createAffinityDistribution(instanceResults, function, null);

		return Collections.singletonMap(getTargetName(), result);
	}

	private <V extends Number> List<InstanceResult<V>> evaluateInstanceRows(ValueFactory<V> valueFactory, EvaluationContext context){
		NearestNeighborModel nearestNeighborModel = getModel();

		ComparisonMeasure comparisonMeasure = nearestNeighborModel.getComparisonMeasure();

		List<FieldValue> values = new ArrayList<>();

		KNNInputs knnInputs = nearestNeighborModel.getKNNInputs();
		for(KNNInput knnInput : knnInputs){
			FieldName name = knnInput.getField();
			if(name == null){
				throw new MissingAttributeException(knnInput, PMMLAttributes.KNNINPUT_FIELD);
			}

			FieldValue value = context.evaluate(name);

			values.add(value);
		}

		Measure measure = MeasureUtil.ensureMeasure(comparisonMeasure);

		if(measure instanceof Similarity){
			return evaluateSimilarity(valueFactory, comparisonMeasure, knnInputs.getKNNInputs(), values);
		} else

		if(measure instanceof Distance){
			return evaluateDistance(valueFactory, comparisonMeasure, knnInputs.getKNNInputs(), values);
		} else

		{
			throw new UnsupportedElementException(measure);
		}
	}

	private <V extends Number> List<InstanceResult<V>> evaluateSimilarity(ValueFactory<V> valueFactory, ComparisonMeasure comparisonMeasure, List<KNNInput> knnInputs, List<FieldValue> values){
		BitSet flags = MeasureUtil.toBitSet(values);

		Map<Integer, BitSet> flagMap = getInstanceFlags();

		List<InstanceResult<V>> result = new ArrayList<>(flagMap.size());

		Set<Integer> rowKeys = flagMap.keySet();
		for(Integer rowKey : rowKeys){
			BitSet instanceFlags = flagMap.get(rowKey);

			Value<V> similarity = MeasureUtil.evaluateSimilarity(valueFactory, comparisonMeasure, knnInputs, flags, instanceFlags);

			result.add(new InstanceResult.Similarity<>(rowKey, similarity));
		}

		return result;
	}

	private <V extends Number> List<InstanceResult<V>> evaluateDistance(ValueFactory<V> valueFactory, ComparisonMeasure comparisonMeasure, List<KNNInput> knnInputs, List<FieldValue> values){
		Map<Integer, List<FieldValue>> valueMap = getInstanceValues();

		List<InstanceResult<V>> result = new ArrayList<>(valueMap.size());

		Value<V> adjustment = MeasureUtil.calculateAdjustment(valueFactory, values);

		Set<Integer> rowKeys = valueMap.keySet();
		for(Integer rowKey : rowKeys){
			List<FieldValue> instanceValues = valueMap.get(rowKey);

			Value<V> distance = MeasureUtil.evaluateDistance(valueFactory, comparisonMeasure, knnInputs, values, instanceValues, adjustment);

			result.add(new InstanceResult.Distance<>(rowKey, distance));
		}

		return result;
	}

	private <V extends Number> V calculateContinuousTarget(ValueFactory<V> valueFactory, FieldName name, List<InstanceResult<V>> instanceResults, Table<Integer, FieldName, FieldValue> table){
		NearestNeighborModel nearestNeighborModel = getModel();

		NearestNeighborModel.ContinuousScoringMethod continuousScoringMethod = nearestNeighborModel.getContinuousScoringMethod();

		ValueAggregator<V> aggregator;

		switch(continuousScoringMethod){
			case AVERAGE:
				aggregator = new ValueAggregator<>(valueFactory.newVector(0));
				break;
			case WEIGHTED_AVERAGE:
				aggregator = new ValueAggregator<>(valueFactory.newVector(0), valueFactory.newVector(0), valueFactory.newVector(0));
				break;
			case MEDIAN:
				aggregator = new ValueAggregator<>(valueFactory.newVector(instanceResults.size()));
				break;
			default:
				throw new UnsupportedAttributeException(nearestNeighborModel, continuousScoringMethod);
		}

		for(InstanceResult<V> instanceResult : instanceResults){
			FieldValue value = table.get(instanceResult.getId(), name);
			if(Objects.equals(FieldValues.MISSING_VALUE, value)){
				throw new MissingValueException(name);
			}

			Number number = value.asNumber();

			switch(continuousScoringMethod){
				case AVERAGE:
				case MEDIAN:
					aggregator.add(number);
					break;
				case WEIGHTED_AVERAGE:
					InstanceResult.Distance distance = TypeUtil.cast(InstanceResult.Distance.class, instanceResult);

					Value<V> weight = distance.getWeight(nearestNeighborModel.getThreshold());

					aggregator.add(number, weight.doubleValue());
					break;
				default:
					throw new UnsupportedAttributeException(nearestNeighborModel, continuousScoringMethod);
			}
		}

		switch(continuousScoringMethod){
			case AVERAGE:
				return (aggregator.average()).getValue();
			case WEIGHTED_AVERAGE:
				return (aggregator.weightedAverage()).getValue();
			case MEDIAN:
				return (aggregator.median()).getValue();
			default:
				throw new UnsupportedAttributeException(nearestNeighborModel, continuousScoringMethod);
		}
	}

	@SuppressWarnings (
		value = {"rawtypes", "unchecked"}
	)
	private <V extends Number> Object calculateCategoricalTarget(ValueFactory<V> valueFactory, FieldName name, List<InstanceResult<V>> instanceResults, Table<Integer, FieldName, FieldValue> table){
		NearestNeighborModel nearestNeighborModel = getModel();

		VoteAggregator<Object, V> aggregator = new VoteAggregator<Object, V>(){

			@Override
			public ValueFactory<V> getValueFactory(){
				return valueFactory;
			}
		};

		NearestNeighborModel.CategoricalScoringMethod categoricalScoringMethod = nearestNeighborModel.getCategoricalScoringMethod();

		for(InstanceResult<V> instanceResult : instanceResults){
			FieldValue value = table.get(instanceResult.getId(), name);
			if(Objects.equals(FieldValues.MISSING_VALUE, value)){
				throw new MissingValueException(name);
			}

			Object object = value.getValue();

			switch(categoricalScoringMethod){
				case MAJORITY_VOTE:
					aggregator.add(object);
					break;
				case WEIGHTED_MAJORITY_VOTE:
					InstanceResult.Distance distance = TypeUtil.cast(InstanceResult.Distance.class, instanceResult);

					Value<V> weight = distance.getWeight(nearestNeighborModel.getThreshold());

					aggregator.add(object, weight.doubleValue());
					break;
				default:
					throw new UnsupportedAttributeException(nearestNeighborModel, categoricalScoringMethod);
			}
		}

		Set<Object> winners = aggregator.getWinners();

		// "In case of a tie, the category with the largest number of cases in the training data is the winner"
		if(winners.size() > 1){
			Multiset<Object> multiset = LinkedHashMultiset.create();

			Map<Integer, FieldValue> column = table.column(name);

			multiset.addAll(Collections2.transform(column.values(), FieldValue::getValue));

			aggregator.clear();

			for(Object winner : winners){
				aggregator.add(winner, multiset.count(winner));
			}

			winners = aggregator.getWinners();

			// "If multiple categories are tied on the largest number of cases in the training data, then the category with the smallest data value (in lexical order) among the tied categories is the winner"
			if(winners.size() > 1){
				return Collections.min((Collection)winners);
			}
		}

		return Iterables.getFirst(winners, null);
	}

	private Function<Integer, String> createIdentifierResolver(FieldName name, Table<Integer, FieldName, FieldValue> table){
		Function<Integer, String> function = new Function<Integer, String>(){

			@Override
			public String apply(Integer row){
				FieldValue value = table.get(row, name);
				if(Objects.equals(FieldValues.MISSING_VALUE, value)){
					throw new MissingValueException(name);
				}

				return value.asString();
			}
		};

		return function;
	}

	private <V extends Number> AffinityDistribution<V> createAffinityDistribution(List<InstanceResult<V>> instanceResults, Function<Integer, String> function, Object result){
		NearestNeighborModel nearestNeighborModel = getModel();

		ComparisonMeasure comparisonMeasure = nearestNeighborModel.getComparisonMeasure();

		ValueMap<String, V> values = new ValueMap<>(2 * instanceResults.size());

		for(InstanceResult<V> instanceResult : instanceResults){
			values.put(function.apply(instanceResult.getId()), instanceResult.getValue());
		}

		Measure measure = MeasureUtil.ensureMeasure(comparisonMeasure);

		if(measure instanceof Similarity){
			return new AffinityDistribution<>(Classification.Type.SIMILARITY, values, result);
		} else

		if(measure instanceof Distance){
			return new AffinityDistribution<>(Classification.Type.DISTANCE, values, result);
		} else

		{
			throw new UnsupportedElementException(measure);
		}
	}

	private Table<Integer, FieldName, FieldValue> getTrainingInstances(){

		if(this.trainingInstances == null){
			this.trainingInstances = getValue(NearestNeighborModelEvaluator.trainingInstanceCache, createTrainingInstanceLoader(this));
		}

		return this.trainingInstances;
	}

	static
	private Callable<Table<Integer, FieldName, FieldValue>> createTrainingInstanceLoader(NearestNeighborModelEvaluator modelEvaluator){
		return new Callable<Table<Integer, FieldName, FieldValue>>(){

			@Override
			public Table<Integer, FieldName, FieldValue> call(){
				return parseTrainingInstances(modelEvaluator);
			}
		};
	}

	static
	private Table<Integer, FieldName, FieldValue> parseTrainingInstances(NearestNeighborModelEvaluator modelEvaluator){
		NearestNeighborModel nearestNeighborModel = modelEvaluator.getModel();

		FieldName instanceIdVariable = nearestNeighborModel.getInstanceIdVariable();

		Set<FieldName> fieldNames = new HashSet<>();

		FieldReferenceFinder variableFinder = new FieldReferenceFinder();
		variableFinder.applyTo(nearestNeighborModel);

		fieldNames.addAll(variableFinder.getFieldNames());

		List<TargetField> targetFields = modelEvaluator.getTargetFields();
		for(TargetField targetField : targetFields){
			fieldNames.add(targetField.getName());
		}

		TrainingInstances trainingInstances = nearestNeighborModel.getTrainingInstances();

		List<FieldLoader> fieldLoaders = new ArrayList<>();

		InstanceFields instanceFields = trainingInstances.getInstanceFields();
		for(InstanceField instanceField : instanceFields){
			FieldName name = instanceField.getField();
			if(name == null){
				throw new MissingAttributeException(instanceField, PMMLAttributes.INSTANCEFIELD_FIELD);
			}

			String column = instanceField.getColumn();

			if(instanceIdVariable != null && (instanceIdVariable).equals(name)){
				fieldLoaders.add(new IdentifierLoader(name, column));

				continue;
			} // End if

			if(!fieldNames.contains(name)){
				continue;
			}

			Field<?> field = modelEvaluator.resolveField(name);
			if(field == null){
				throw new MissingFieldException(name, instanceField);
			} // End if

			if(field instanceof DataField){
				DataField dataField = (DataField)field;

				MiningField miningField = modelEvaluator.getMiningField(name);
				if(miningField == null){
					throw new InvisibleFieldException(name, instanceField);
				}

				fieldLoaders.add(new DataFieldLoader(name, column, dataField, miningField));
			} else

			if(field instanceof DerivedField){
				DerivedField derivedField = (DerivedField)field;

				fieldLoaders.add(new DerivedFieldLoader(name, column, derivedField));
			} else

			{
				throw new InvalidAttributeException(instanceField, PMMLAttributes.INSTANCEFIELD_FIELD, name);
			}
		}

		Table<Integer, FieldName, FieldValue> result = HashBasedTable.create();

		InlineTable inlineTable = InlineTableUtil.getInlineTable(trainingInstances);
		if(inlineTable != null){
			Table<Integer, String, String> table = InlineTableUtil.getContent(inlineTable);

			Set<Integer> rowKeys = table.rowKeySet();
			for(Integer rowKey : rowKeys){
				Map<String, String> rowValues = table.row(rowKey);

				for(FieldLoader fieldLoader : fieldLoaders){
					result.put(rowKey, fieldLoader.getName(), fieldLoader.load(rowValues));
				}
			}
		}

		KNNInputs knnInputs = nearestNeighborModel.getKNNInputs();
		for(KNNInput knnInput : knnInputs){
			FieldName name = knnInput.getField();

			DerivedField derivedField = modelEvaluator.resolveDerivedField(name);
			if(derivedField == null){
				continue;
			}

			Set<Integer> rowKeys = result.rowKeySet();
			for(Integer rowKey : rowKeys){
				Map<FieldName, FieldValue> rowValues = result.row(rowKey);

				if(rowValues.containsKey(name)){
					continue;
				}

				ModelEvaluationContext context = new ModelEvaluationContext(modelEvaluator);
				context.declareAll(rowValues);

				FieldValue value = ExpressionUtil.evaluateTypedExpressionContainer(derivedField, context);

				result.put(rowKey, name, value);
			}
		}

		int numberOfNeighbors = nearestNeighborModel.getNumberOfNeighbors();
		if(numberOfNeighbors < 0 || result.size() < numberOfNeighbors){
			throw new InvalidAttributeException(nearestNeighborModel, PMMLAttributes.NEARESTNEIGHBORMODEL_NUMBEROFNEIGHBORS, numberOfNeighbors);
		}

		return result;
	}

	private Map<Integer, BitSet> getInstanceFlags(){

		if(this.instanceFlags == null){
			this.instanceFlags = getValue(NearestNeighborModelEvaluator.instanceFlagCache, createInstanceFlagLoader(this));
		}

		return this.instanceFlags;
	}

	static
	private Callable<Map<Integer, BitSet>> createInstanceFlagLoader(NearestNeighborModelEvaluator modelEvaluator){
		return new Callable<Map<Integer, BitSet>>(){

			@Override
			public Map<Integer, BitSet> call(){
				return loadInstanceFlags(modelEvaluator);
			}
		};
	}

	static
	private Map<Integer, BitSet> loadInstanceFlags(NearestNeighborModelEvaluator modelEvaluator){
		Map<Integer, BitSet> result = new LinkedHashMap<>();

		Map<Integer, List<FieldValue>> valueMap = modelEvaluator.getValue(NearestNeighborModelEvaluator.instanceValueCache, createInstanceValueLoader(modelEvaluator));

		Maps.EntryTransformer<Integer, List<FieldValue>, BitSet> transformer = new Maps.EntryTransformer<Integer, List<FieldValue>, BitSet>(){

			@Override
			public BitSet transformEntry(Integer key, List<FieldValue> value){
				return MeasureUtil.toBitSet(value);
			}
		};
		result.putAll(Maps.transformEntries(valueMap, transformer));

		return result;
	}

	private Map<Integer, List<FieldValue>> getInstanceValues(){

		if(this.instanceValues == null){
			this.instanceValues = getValue(NearestNeighborModelEvaluator.instanceValueCache, createInstanceValueLoader(this));
		}

		return this.instanceValues;
	}

	static
	private Callable<Map<Integer, List<FieldValue>>> createInstanceValueLoader(NearestNeighborModelEvaluator modelEvaluator){
		return new Callable<Map<Integer, List<FieldValue>>>(){

			@Override
			public Map<Integer, List<FieldValue>> call(){
				return loadInstanceValues(modelEvaluator);
			}
		};
	}

	static
	private Map<Integer, List<FieldValue>> loadInstanceValues(NearestNeighborModelEvaluator modelEvaluator){
		NearestNeighborModel nearestNeighborModel = modelEvaluator.getModel();

		Map<Integer, List<FieldValue>> result = new LinkedHashMap<>();

		Table<Integer, FieldName, FieldValue> table = modelEvaluator.getValue(NearestNeighborModelEvaluator.trainingInstanceCache, createTrainingInstanceLoader(modelEvaluator));

		KNNInputs knnInputs = nearestNeighborModel.getKNNInputs();

		Set<Integer> rowKeys = ImmutableSortedSet.copyOf(table.rowKeySet());
		for(Integer rowKey : rowKeys){
			List<FieldValue> values = new ArrayList<>();

			Map<FieldName, FieldValue> rowValues = table.row(rowKey);

			for(KNNInput knnInput : knnInputs){
				FieldValue value = rowValues.get(knnInput.getField());

				values.add(value);
			}

			result.put(rowKey, values);
		}

		return result;
	}

	static
	abstract
	private class FieldLoader {

		private FieldName name = null;

		private String column = null;


		private FieldLoader(FieldName name, String column){
			setName(name);
			setColumn(column);
		}

		abstract
		public FieldValue prepare(String value);

		public FieldValue load(Map<String, String> values){
			String value = values.get(getColumn());

			return prepare(value);
		}

		public FieldName getName(){
			return this.name;
		}

		private void setName(FieldName name){
			this.name = name;
		}

		public String getColumn(){
			return this.column;
		}

		private void setColumn(String column){
			this.column = column;
		}
	}

	static
	private class IdentifierLoader extends FieldLoader {

		private IdentifierLoader(FieldName name, String column){
			super(name, column);
		}

		@Override
		public FieldValue prepare(String value){
			return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, value);
		}
	}

	static
	private class DataFieldLoader extends FieldLoader {

		private DataField dataField = null;

		private MiningField miningField = null;


		private DataFieldLoader(FieldName name, String column, DataField dataField, MiningField miningField){
			super(name, column);

			setDataField(dataField);
			setMiningField(miningField);
		}

		@Override
		public FieldValue prepare(String value){
			return InputFieldUtil.prepareInputValue(getDataField(), getMiningField(), value);
		}

		public DataField getDataField(){
			return this.dataField;
		}

		private void setDataField(DataField dataField){
			this.dataField = dataField;
		}

		public MiningField getMiningField(){
			return this.miningField;
		}

		private void setMiningField(MiningField miningField){
			this.miningField = miningField;
		}
	}

	static
	private class DerivedFieldLoader extends FieldLoader {

		private DerivedField derivedField = null;


		private DerivedFieldLoader(FieldName name, String column, DerivedField derivedField){
			super(name, column);

			setDerivedField(derivedField);
		}

		@Override
		public FieldValue prepare(String value){
			DerivedField derivedField = getDerivedField();

			TypeInfo typeInfo = new TypeInfo(){

				@Override
				public DataType getDataType(){
					DataType dataType = derivedField.getDataType();
					if(dataType == null){
						throw new MissingAttributeException(derivedField, PMMLAttributes.DERIVEDFIELD_DATATYPE);
					}

					return dataType;
				}

				@Override
				public OpType getOpType(){
					OpType opType = derivedField.getOpType();
					if(opType == null){
						throw new MissingAttributeException(derivedField, PMMLAttributes.DERIVEDFIELD_OPTYPE);
					}

					return opType;
				}

				@Override
				public List<?> getOrdering(){
					List<?> ordering = FieldUtil.getValidValues(derivedField);

					return ordering;
				}
			};

			return FieldValueUtil.create(typeInfo, value);
		}

		public DerivedField getDerivedField(){
			return this.derivedField;
		}

		private void setDerivedField(DerivedField derivedField){
			this.derivedField = derivedField;
		}
	}

	static
	abstract
	private class InstanceResult<V extends Number> implements Comparable<InstanceResult<V>> {

		private Integer id = null;

		private Value<V> value = null;


		private InstanceResult(Integer id, Value<V> value){
			setId(id);
			setValue(value);
		}

		public Integer getId(){
			return this.id;
		}

		private void setId(Integer id){
			this.id = id;
		}

		public Value<V> getValue(){
			return this.value;
		}

		private void setValue(Value<V> value){
			this.value = value;
		}

		static
		private class Similarity<V extends Number> extends InstanceResult<V> {

			private Similarity(Integer id, Value<V> value){
				super(id, value);
			}

			@Override
			public int compareTo(InstanceResult<V> that){

				if(that instanceof Similarity){
					return Classification.Type.SIMILARITY.compareValues(this.getValue(), that.getValue());
				}

				throw new ClassCastException();
			}
		}

		static
		private class Distance<V extends Number> extends InstanceResult<V> {

			private Distance(Integer id, Value<V> value){
				super(id, value);
			}

			@Override
			public int compareTo(InstanceResult<V> that){

				if(that instanceof Distance){
					return Classification.Type.DISTANCE.compareValues(this.getValue(), that.getValue());
				}

				throw new ClassCastException();
			}

			public Value<V> getWeight(double threshold){
				Value<V> value = getValue();

				value = value.copy();

				if(threshold != 0d){
					value.add(threshold);
				}

				return value.reciprocal();
			}
		}
	}

	private static final Cache<NearestNeighborModel, Table<Integer, FieldName, FieldValue>> trainingInstanceCache = CacheUtil.buildCache();

	private static final Cache<NearestNeighborModel, Map<Integer, BitSet>> instanceFlagCache = CacheUtil.buildCache();

	private static final Cache<NearestNeighborModel, Map<Integer, List<FieldValue>>> instanceValueCache = CacheUtil.buildCache();
}
