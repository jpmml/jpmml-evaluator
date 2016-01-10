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
package org.jpmml.evaluator;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.dmg.pmml.CategoricalScoringMethodType;
import org.dmg.pmml.ComparisonMeasure;
import org.dmg.pmml.ContinuousScoringMethodType;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.InstanceField;
import org.dmg.pmml.InstanceFields;
import org.dmg.pmml.KNNInput;
import org.dmg.pmml.KNNInputs;
import org.dmg.pmml.Measure;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.NearestNeighborModel;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.TableLocator;
import org.dmg.pmml.TrainingInstances;
import org.dmg.pmml.TypeDefinitionField;

public class NearestNeighborModelEvaluator extends ModelEvaluator<NearestNeighborModel> {

	public NearestNeighborModelEvaluator(PMML pmml){
		super(pmml, NearestNeighborModel.class);
	}

	public NearestNeighborModelEvaluator(PMML pmml, NearestNeighborModel nearestNeighborModel){
		super(pmml, nearestNeighborModel);
	}

	@Override
	public String getSummary(){
		return "k-Nearest neighbors model";
	}

	/**
	 * @return <code>null</code> Always.
	 */
	@Override
	protected DataField getDataField(){
		return null;
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		NearestNeighborModel nearestNeighborModel = getModel();
		if(!nearestNeighborModel.isScorable()){
			throw new InvalidResultException(nearestNeighborModel);
		}

		Map<FieldName, AffinityDistribution> predictions;

		MiningFunctionType miningFunction = nearestNeighborModel.getFunctionName();
		switch(miningFunction){
			// The model contains one or more continuous and/or categorical target(s)
			case REGRESSION:
			case CLASSIFICATION:
			case MIXED:
				predictions = evaluateMixed(context);
				break;
			// The model does not contain targets
			case CLUSTERING:
				predictions = evaluateClustering(context);
				break;
			default:
				throw new UnsupportedFeatureException(nearestNeighborModel, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private Map<FieldName, AffinityDistribution> evaluateMixed(ModelEvaluationContext context){
		NearestNeighborModel nearestNeighborModel = getModel();

		Table<Integer, FieldName, FieldValue> table = getTrainingInstances();

		List<InstanceResult> instanceResults = evaluateInstanceRows(context);

		Ordering<InstanceResult> ordering = (Ordering.natural()).reverse();

		List<InstanceResult> nearestInstanceResults = ordering.sortedCopy(instanceResults);

		nearestInstanceResults = nearestInstanceResults.subList(0, nearestNeighborModel.getNumberOfNeighbors());

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

		Map<FieldName, AffinityDistribution> result = new LinkedHashMap<>();

		List<FieldName> targetFields = getTargetFields();
		for(FieldName targetField : targetFields){
			DataField dataField = getDataField(targetField);
			if(dataField == null){
				throw new MissingFieldException(targetField, nearestNeighborModel);
			}

			Object value;

			OpType opType = dataField.getOpType();
			switch(opType){
				case CONTINUOUS:
					value = calculateContinuousTarget(targetField, nearestInstanceResults, table);
					break;
				case CATEGORICAL:
					value = calculateCategoricalTarget(targetField, nearestInstanceResults, table);
					break;
				default:
					throw new UnsupportedFeatureException(dataField, opType);
			}

			value = TypeUtil.parseOrCast(dataField.getDataType(), value);

			result.put(targetField, createAffinityDistribution(instanceResults, function, value));
		}

		return result;
	}

	private Map<FieldName, AffinityDistribution> evaluateClustering(ModelEvaluationContext context){
		NearestNeighborModel nearestNeighborModel = getModel();

		Table<Integer, FieldName, FieldValue> table = getTrainingInstances();

		List<InstanceResult> instanceResults = evaluateInstanceRows(context);

		FieldName instanceIdVariable = nearestNeighborModel.getInstanceIdVariable();
		if(instanceIdVariable == null){
			throw new InvalidFeatureException(nearestNeighborModel);
		}

		Function<Integer, String> function = createIdentifierResolver(instanceIdVariable, table);

		return Collections.singletonMap(getTargetField(), createAffinityDistribution(instanceResults, function, null));
	}

	private List<InstanceResult> evaluateInstanceRows(ModelEvaluationContext context){
		NearestNeighborModel nearestNeighborModel = getModel();

		List<FieldValue> values = new ArrayList<>();

		KNNInputs knnInputs = nearestNeighborModel.getKNNInputs();
		for(KNNInput knnInput : knnInputs){
			FieldValue value = context.evaluate(knnInput.getField());

			values.add(value);
		}

		ComparisonMeasure comparisonMeasure = nearestNeighborModel.getComparisonMeasure();

		Measure measure = comparisonMeasure.getMeasure();

		if(MeasureUtil.isSimilarity(measure)){
			return evaluateSimilarity(comparisonMeasure, knnInputs.getKNNInputs(), values);
		} else

		if(MeasureUtil.isDistance(measure)){
			return evaluateDistance(comparisonMeasure, knnInputs.getKNNInputs(), values);
		} else

		{
			throw new UnsupportedFeatureException(measure);
		}
	}

	private List<InstanceResult> evaluateSimilarity(ComparisonMeasure comparisonMeasure, List<KNNInput> knnInputs, List<FieldValue> values){
		List<InstanceResult> result = new ArrayList<>();

		BitSet flags = MeasureUtil.toBitSet(values);

		Map<Integer, BitSet> flagMap = getInstanceFlags();

		Set<Integer> rowKeys = flagMap.keySet();
		for(Integer rowKey : rowKeys){
			BitSet instanceFlags = flagMap.get(rowKey);

			Double similarity = MeasureUtil.evaluateSimilarity(comparisonMeasure, knnInputs, flags, instanceFlags);

			result.add(new InstanceResult.Similarity(rowKey, similarity));
		}

		return result;
	}

	private List<InstanceResult> evaluateDistance(ComparisonMeasure comparisonMeasure, List<KNNInput> knnInputs, List<FieldValue> values){
		List<InstanceResult> result = new ArrayList<>();

		Double adjustment = MeasureUtil.calculateAdjustment(values);

		Map<Integer, List<FieldValue>> valueMap = getInstanceValues();

		Set<Integer> rowKeys = valueMap.keySet();
		for(Integer rowKey : rowKeys){
			List<FieldValue> instanceValues = valueMap.get(rowKey);

			Double distance = MeasureUtil.evaluateDistance(comparisonMeasure, knnInputs, values, instanceValues, adjustment);

			result.add(new InstanceResult.Distance(rowKey, distance));
		}

		return result;
	}

	private Double calculateContinuousTarget(FieldName name, List<InstanceResult> instanceResults, Table<Integer, FieldName, FieldValue> table){
		NearestNeighborModel nearestNeighborModel = getModel();

		RegressionAggregator aggregator = new RegressionAggregator();

		double denominator = 0d;

		ContinuousScoringMethodType continuousScoringMethod = nearestNeighborModel.getContinuousScoringMethod();

		for(InstanceResult instanceResult : instanceResults){
			FieldValue value = table.get(instanceResult.getId(), name);
			if(value == null){
				throw new MissingValueException(name);
			}

			Number number = value.asNumber();

			switch(continuousScoringMethod){
				case MEDIAN:
					aggregator.add(number.doubleValue());
					break;
				case AVERAGE:
					aggregator.add(number.doubleValue());
					denominator += 1d;
					break;
				case WEIGHTED_AVERAGE:
					double weight = instanceResult.getWeight(nearestNeighborModel.getThreshold());

					aggregator.add(number.doubleValue() * weight);
					denominator += weight;
					break;
				default:
					throw new UnsupportedFeatureException(nearestNeighborModel, continuousScoringMethod);
			}
		}

		switch(continuousScoringMethod){
			case MEDIAN:
				return aggregator.median();
			case AVERAGE:
			case WEIGHTED_AVERAGE:
				return aggregator.average(denominator);
			default:
				throw new UnsupportedFeatureException(nearestNeighborModel, continuousScoringMethod);
		}
	}

	@SuppressWarnings (
		value = {"rawtypes", "unchecked"}
	)
	private Object calculateCategoricalTarget(FieldName name, List<InstanceResult> instanceResults, Table<Integer, FieldName, FieldValue> table){
		NearestNeighborModel nearestNeighborModel = getModel();

		VoteAggregator<Object> aggregator = new VoteAggregator<>();

		CategoricalScoringMethodType categoricalScoringMethod = nearestNeighborModel.getCategoricalScoringMethod();

		for(InstanceResult instanceResult : instanceResults){
			FieldValue value = table.get(instanceResult.getId(), name);
			if(value == null){
				throw new MissingValueException(name);
			}

			Object object = value.getValue();

			switch(categoricalScoringMethod){
				case MAJORITY_VOTE:
					aggregator.add(object, 1d);
					break;
				case WEIGHTED_MAJORITY_VOTE:
					aggregator.add(object, instanceResult.getWeight(nearestNeighborModel.getThreshold()));
					break;
				default:
					throw new UnsupportedFeatureException(nearestNeighborModel, categoricalScoringMethod);
			}
		}

		Set<Object> winners = aggregator.getWinners();

		// "In case of a tie, the category with the largest number of cases in the training data is the winner"
		if(winners.size() > 1){
			Multiset<Object> multiset = LinkedHashMultiset.create();

			Map<Integer, FieldValue> column = table.column(name);

			Function<FieldValue, Object> function = new Function<FieldValue, Object>(){

				@Override
				public Object apply(FieldValue value){
					return value.getValue();
				}
			};
			multiset.addAll(Collections2.transform(column.values(), function));

			aggregator.clear();

			for(Object winner : winners){
				aggregator.add(winner, (double)multiset.count(winner));
			}

			winners = aggregator.getWinners();

			// "If multiple categories are tied on the largest number of cases in the training data, then the category with the smallest data value (in lexical order) among the tied categories is the winner"
			if(winners.size() > 1){
				return Collections.min((Collection)winners);
			}
		}

		return Iterables.getFirst(winners, null);
	}

	private Function<Integer, String> createIdentifierResolver(final FieldName name, final Table<Integer, FieldName, FieldValue> table){
		Function<Integer, String> function = new Function<Integer, String>(){

			@Override
			public String apply(Integer row){
				FieldValue value = table.get(row, name);
				if(value == null){
					throw new MissingValueException(name);
				}

				return value.asString();
			}
		};

		return function;
	}

	private AffinityDistribution createAffinityDistribution(List<InstanceResult> instanceResults, Function<Integer, String> function, Object value){
		NearestNeighborModel nearestNeighborModel = getModel();

		AffinityDistribution result;

		ComparisonMeasure comparisonMeasure = nearestNeighborModel.getComparisonMeasure();

		Measure measure = comparisonMeasure.getMeasure();

		if(MeasureUtil.isSimilarity(measure)){
			result = new AffinityDistribution(Classification.Type.SIMILARITY, value);
		} else

		if(MeasureUtil.isDistance(measure)){
			result = new AffinityDistribution(Classification.Type.DISTANCE, value);
		} else

		{
			throw new UnsupportedFeatureException(measure);
		}

		for(InstanceResult instanceResult : instanceResults){
			result.put(function.apply(instanceResult.getId()), instanceResult.getValue());
		}

		return result;
	}

	private Table<Integer, FieldName, FieldValue> getTrainingInstances(){
		return getValue(createTrainingInstanceLoader(this), NearestNeighborModelEvaluator.trainingInstanceCache);
	}

	static
	private Callable<Table<Integer, FieldName, FieldValue>> createTrainingInstanceLoader(final NearestNeighborModelEvaluator modelEvaluator){
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

		TrainingInstances trainingInstances = nearestNeighborModel.getTrainingInstances();

		TableLocator tableLocator = trainingInstances.getTableLocator();
		if(tableLocator != null){
			throw new UnsupportedFeatureException(tableLocator);
		}

		FieldName instanceIdVariable = nearestNeighborModel.getInstanceIdVariable();

		List<FieldLoader> fieldLoaders = new ArrayList<>();

		InstanceFields instanceFields = trainingInstances.getInstanceFields();
		for(InstanceField instanceField : instanceFields){
			FieldName name = instanceField.getField();
			String column = instanceField.getColumn();

			if(instanceIdVariable != null && (instanceIdVariable).equals(name)){
				fieldLoaders.add(new IdentifierLoader(name, column));

				continue;
			}

			TypeDefinitionField field = modelEvaluator.resolveField(name);
			if(field == null){
				throw new MissingFieldException(name, instanceField);
			} // End if

			if(field instanceof DataField){
				DataField dataField = (DataField)field;
				MiningField miningField = modelEvaluator.getMiningField(name);

				fieldLoaders.add(new DataFieldLoader(name, column, dataField, miningField));
			} else

			if(field instanceof DerivedField){
				DerivedField derivedField = (DerivedField)field;

				fieldLoaders.add(new DerivedFieldLoader(name, column, derivedField));
			} else

			{
				throw new InvalidFeatureException(instanceField);
			}
		}

		Table<Integer, FieldName, FieldValue> result = HashBasedTable.create();

		InlineTable inlineTable = trainingInstances.getInlineTable();
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

				ModelEvaluationContext context = new ModelEvaluationContext(null, modelEvaluator);
				context.declareAll(rowValues);

				result.put(rowKey, name, ExpressionUtil.evaluate(derivedField, context));
			}
		}

		return result;
	}

	private Map<Integer, BitSet> getInstanceFlags(){
		return getValue(createInstanceFlagLoader(this), NearestNeighborModelEvaluator.instanceFlagCache);
	}

	static
	private Callable<Map<Integer, BitSet>> createInstanceFlagLoader(final NearestNeighborModelEvaluator modelEvaluator){
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

		Map<Integer, List<FieldValue>> valueMap = modelEvaluator.getValue(createInstanceValueLoader(modelEvaluator), NearestNeighborModelEvaluator.instanceValueCache);

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
		return getValue(createInstanceValueLoader(this), NearestNeighborModelEvaluator.instanceValueCache);
	}

	static
	private Callable<Map<Integer, List<FieldValue>>> createInstanceValueLoader(final NearestNeighborModelEvaluator modelEvaluator){
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

		Table<Integer, FieldName, FieldValue> table = modelEvaluator.getValue(createTrainingInstanceLoader(modelEvaluator), NearestNeighborModelEvaluator.trainingInstanceCache);

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
			return FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, value);
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
			return FieldValueUtil.prepare(getDataField(), getMiningField(), value);
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
			return FieldValueUtil.create(getDerivedField(), value);
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
	private class InstanceResult implements Comparable<InstanceResult> {

		private Integer id = null;

		private Double value = null;


		private InstanceResult(Integer id, Double value){
			setId(id);
			setValue(value);
		}

		abstract
		public double getWeight(double threshold);

		public Integer getId(){
			return this.id;
		}

		private void setId(Integer id){
			this.id = id;
		}

		public Double getValue(){
			return this.value;
		}

		private void setValue(Double value){
			this.value = value;
		}

		static
		private class Similarity extends InstanceResult {

			private Similarity(Integer id, Double value){
				super(id, value);
			}

			@Override
			public int compareTo(InstanceResult that){

				if(that instanceof Similarity){
					return Classification.Type.SIMILARITY.compare(this.getValue(), that.getValue());
				}

				throw new ClassCastException();
			}

			@Override
			public double getWeight(double threshold){
				throw new EvaluationException();
			}
		}

		static
		private class Distance extends InstanceResult {

			private Distance(Integer id, Double value){
				super(id, value);
			}

			@Override
			public int compareTo(InstanceResult that){

				if(that instanceof Distance){
					return Classification.Type.DISTANCE.compare(this.getValue(), that.getValue());
				}

				throw new ClassCastException();
			}

			@Override
			public double getWeight(double threshold){
				return 1d / (getValue() + threshold);
			}
		}
	}

	private static final Cache<NearestNeighborModel, Table<Integer, FieldName, FieldValue>> trainingInstanceCache = CacheUtil.buildCache();

	private static final Cache<NearestNeighborModel, Map<Integer, BitSet>> instanceFlagCache = CacheUtil.buildCache();

	private static final Cache<NearestNeighborModel, Map<Integer, List<FieldValue>>> instanceValueCache = CacheUtil.buildCache();
}