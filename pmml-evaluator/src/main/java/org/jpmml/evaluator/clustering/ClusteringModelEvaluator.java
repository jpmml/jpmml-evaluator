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
package org.jpmml.evaluator.clustering;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import org.dmg.pmml.Array;
import org.dmg.pmml.ComparisonMeasure;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Distance;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.Measure;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Similarity;
import org.dmg.pmml.Target;
import org.dmg.pmml.Targets;
import org.dmg.pmml.clustering.CenterFields;
import org.dmg.pmml.clustering.Cluster;
import org.dmg.pmml.clustering.ClusteringField;
import org.dmg.pmml.clustering.ClusteringModel;
import org.dmg.pmml.clustering.MissingValueWeights;
import org.jpmml.evaluator.ArrayUtil;
import org.jpmml.evaluator.CacheUtil;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.InvalidAttributeException;
import org.jpmml.evaluator.InvalidElementException;
import org.jpmml.evaluator.MeasureUtil;
import org.jpmml.evaluator.MisplacedElementException;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.PMMLAttributes;
import org.jpmml.evaluator.PMMLElements;
import org.jpmml.evaluator.UnsupportedAttributeException;
import org.jpmml.evaluator.UnsupportedElementException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;

public class ClusteringModelEvaluator extends ModelEvaluator<ClusteringModel> implements HasEntityRegistry<Cluster> {

	transient
	private BiMap<String, Cluster> entityRegistry = null;


	public ClusteringModelEvaluator(PMML pmml){
		this(pmml, selectModel(pmml, ClusteringModel.class));
	}

	public ClusteringModelEvaluator(PMML pmml, ClusteringModel clusteringModel){
		super(pmml, clusteringModel);

		ComparisonMeasure comparisonMeasure = clusteringModel.getComparisonMeasure();
		if(comparisonMeasure == null){
			throw new MissingElementException(clusteringModel, PMMLElements.CLUSTERINGMODEL_COMPARISONMEASURE);
		}

		ClusteringModel.ModelClass modelClass = clusteringModel.getModelClass();
		switch(modelClass){
			case CENTER_BASED:
				break;
			default:
				throw new UnsupportedAttributeException(clusteringModel, modelClass);
		}

		CenterFields centerFields = clusteringModel.getCenterFields();
		if(centerFields != null){
			throw new UnsupportedElementException(centerFields);
		}

		if(!clusteringModel.hasClusteringFields()){
			throw new MissingElementException(clusteringModel, PMMLElements.CLUSTERINGMODEL_CLUSTERINGFIELDS);
		} // End if

		if(!clusteringModel.hasClusters()){
			throw new MissingElementException(clusteringModel, PMMLElements.CLUSTERINGMODEL_CLUSTERS);
		}

		Targets targets = clusteringModel.getTargets();
		if(targets != null){
			throw new MisplacedElementException(targets);
		}
	}

	@Override
	public String getSummary(){
		return "Clustering model";
	}

	/**
	 * @return <code>null</code> Always.
	 */
	@Override
	public Target getTarget(FieldName name){
		return null;
	}

	@Override
	public BiMap<String, Cluster> getEntityRegistry(){

		if(this.entityRegistry == null){
			this.entityRegistry = getValue(ClusteringModelEvaluator.entityCache);
		}

		return this.entityRegistry;
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		ClusteringModel clusteringModel = ensureScorableModel();

		ValueFactory<?> valueFactory;

		MathContext mathContext = clusteringModel.getMathContext();
		switch(mathContext){
			case FLOAT:
			case DOUBLE:
				valueFactory = ensureValueFactory();
				break;
			default:
				throw new UnsupportedAttributeException(clusteringModel, mathContext);
		}

		Map<FieldName, ? extends ClusterAffinityDistribution<?>> predictions;

		MiningFunction miningFunction = clusteringModel.getMiningFunction();
		switch(miningFunction){
			case CLUSTERING:
				predictions = evaluateClustering(valueFactory, context);
				break;
			case ASSOCIATION_RULES:
			case SEQUENCES:
			case CLASSIFICATION:
			case REGRESSION:
			case TIME_SERIES:
			case MIXED:
				throw new InvalidAttributeException(clusteringModel, miningFunction);
			default:
				throw new UnsupportedAttributeException(clusteringModel, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private <V extends Number> Map<FieldName, ClusterAffinityDistribution<V>> evaluateClustering(ValueFactory<V> valueFactory, EvaluationContext context){
		ClusteringModel clusteringModel = getModel();

		ComparisonMeasure comparisonMeasure = clusteringModel.getComparisonMeasure();

		List<ClusteringField> clusteringFields = getCenterClusteringFields();

		List<FieldValue> values = new ArrayList<>(clusteringFields.size());

		for(int i = 0, max = clusteringFields.size(); i < max; i++){
			ClusteringField clusteringField = clusteringFields.get(i);

			FieldName name = clusteringField.getField();
			if(name == null){
				throw new MissingAttributeException(clusteringField, PMMLAttributes.CLUSTERINGFIELD_FIELD);
			}

			FieldValue value = context.evaluate(name);

			values.add(value);
		}

		ClusterAffinityDistribution<V> result;

		Measure measure = MeasureUtil.ensureMeasure(comparisonMeasure);

		if(measure instanceof Similarity){
			result = evaluateSimilarity(valueFactory, comparisonMeasure, clusteringFields, values);
		} else

		if(measure instanceof Distance){
			result = evaluateDistance(valueFactory, comparisonMeasure, clusteringFields, values);
		} else

		{
			throw new UnsupportedElementException(measure);
		}

		// "For clustering models, the identifier of the winning cluster is returned as the predictedValue"
		result.computeResult(DataType.STRING);

		return Collections.singletonMap(getTargetFieldName(), result);
	}

	private <V extends Number> ClusterAffinityDistribution<V> evaluateSimilarity(ValueFactory<V> valueFactory, ComparisonMeasure comparisonMeasure, List<ClusteringField> clusteringFields, List<FieldValue> values){
		ClusteringModel clusteringModel = getModel();

		List<Cluster> clusters = clusteringModel.getClusters();

		ClusterAffinityDistribution<V> result = createClusterAffinityDistribution(Classification.Type.SIMILARITY, clusters);

		BitSet flags = MeasureUtil.toBitSet(values);

		for(Cluster cluster : clusters){
			BitSet clusterFlags = CacheUtil.getValue(cluster, ClusteringModelEvaluator.clusterFlagCache);

			if(flags.size() != clusterFlags.size()){
				throw new InvalidElementException(cluster);
			}

			Value<V> similarity = MeasureUtil.evaluateSimilarity(valueFactory, comparisonMeasure, clusteringFields, flags, clusterFlags);

			result.put(cluster, similarity);
		}

		return result;
	}

	private <V extends Number> ClusterAffinityDistribution<V> evaluateDistance(ValueFactory<V> valueFactory, ComparisonMeasure comparisonMeasure, List<ClusteringField> clusteringFields, List<FieldValue> values){
		ClusteringModel clusteringModel = getModel();

		List<Cluster> clusters = clusteringModel.getClusters();

		Value<V> adjustment;

		MissingValueWeights missingValueWeights = clusteringModel.getMissingValueWeights();
		if(missingValueWeights != null){
			Array array = missingValueWeights.getArray();

			List<? extends Number> adjustmentValues = ArrayUtil.asNumberList(array);
			if(values.size() != adjustmentValues.size()){
				throw new InvalidElementException(missingValueWeights);
			}

			adjustment = MeasureUtil.calculateAdjustment(valueFactory, values, adjustmentValues);
		} else

		{
			adjustment = MeasureUtil.calculateAdjustment(valueFactory, values);
		}

		ClusterAffinityDistribution<V> result = createClusterAffinityDistribution(Classification.Type.DISTANCE, clusters);

		for(Cluster cluster : clusters){
			List<FieldValue> clusterValues = CacheUtil.getValue(cluster, ClusteringModelEvaluator.clusterValueCache);

			if(values.size() != clusterValues.size()){
				throw new InvalidElementException(cluster);
			}

			Value<V> distance = MeasureUtil.evaluateDistance(valueFactory, comparisonMeasure, clusteringFields, values, clusterValues, adjustment);

			result.put(cluster, distance);
		}

		return result;
	}

	private List<ClusteringField> getCenterClusteringFields(){
		ClusteringModel clusteringModel = getModel();

		List<ClusteringField> clusteringFields = clusteringModel.getClusteringFields();

		List<ClusteringField> result = new ArrayList<>(clusteringFields.size());

		for(int i = 0, max = clusteringFields.size(); i < max; i++){
			ClusteringField clusteringField = clusteringFields.get(i);

			ClusteringField.CenterField centerField = clusteringField.getCenterField();
			switch(centerField){
				case TRUE:
					result.add(clusteringField);
					break;
				case FALSE:
					break;
				default:
					throw new UnsupportedAttributeException(clusteringField, centerField);
			}
		}

		return result;
	}

	private <V extends Number> ClusterAffinityDistribution<V> createClusterAffinityDistribution(Classification.Type type, List<Cluster> clusters){
		ClusterAffinityDistribution<V> result = new ClusterAffinityDistribution<V>(type, new ValueMap<String, V>(2 * clusters.size())){

			@Override
			public BiMap<String, Cluster> getEntityRegistry(){
				return ClusteringModelEvaluator.this.getEntityRegistry();
			}
		};

		return result;
	}

	private static final LoadingCache<Cluster, List<FieldValue>> clusterValueCache = CacheUtil.buildLoadingCache(new CacheLoader<Cluster, List<FieldValue>>(){

		@Override
		public List<FieldValue> load(Cluster cluster){
			Array array = cluster.getArray();

			List<? extends Number> values = ArrayUtil.asNumberList(array);

			return ImmutableList.copyOf(FieldValueUtil.createAll(DataType.DOUBLE, OpType.CONTINUOUS, values));
		}
	});

	private static final LoadingCache<Cluster, BitSet> clusterFlagCache = CacheUtil.buildLoadingCache(new CacheLoader<Cluster, BitSet>(){

		@Override
		public BitSet load(Cluster cluster){
			List<FieldValue> values = CacheUtil.getValue(cluster, ClusteringModelEvaluator.clusterValueCache);

			return MeasureUtil.toBitSet(values);
		}
	});

	private static final LoadingCache<ClusteringModel, BiMap<String, Cluster>> entityCache = CacheUtil.buildLoadingCache(new CacheLoader<ClusteringModel, BiMap<String, Cluster>>(){

		@Override
		public BiMap<String, Cluster> load(ClusteringModel clusteringModel){
			return EntityUtil.buildBiMap(clusteringModel.getClusters());
		}
	});
}