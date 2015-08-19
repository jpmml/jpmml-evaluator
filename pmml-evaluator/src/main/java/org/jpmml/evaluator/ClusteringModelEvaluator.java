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
import org.dmg.pmml.CenterFields;
import org.dmg.pmml.Cluster;
import org.dmg.pmml.ClusteringField;
import org.dmg.pmml.ClusteringModel;
import org.dmg.pmml.ComparisonMeasure;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Measure;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.MissingValueWeights;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Target;

public class ClusteringModelEvaluator extends ModelEvaluator<ClusteringModel> implements HasEntityRegistry<Cluster> {

	public ClusteringModelEvaluator(PMML pmml){
		super(pmml, ClusteringModel.class);
	}

	public ClusteringModelEvaluator(PMML pmml, ClusteringModel clusteringModel){
		super(pmml, clusteringModel);
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
		return getValue(ClusteringModelEvaluator.entityCache);
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		ClusteringModel clusteringModel = getModel();
		if(!clusteringModel.isScorable()){
			throw new InvalidResultException(clusteringModel);
		}

		Map<FieldName, ClusterAffinityDistribution> predictions;

		MiningFunctionType miningFunction = clusteringModel.getFunctionName();
		switch(miningFunction){
			case CLUSTERING:
				predictions = evaluateClustering(context);
				break;
			default:
				throw new UnsupportedFeatureException(clusteringModel, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private Map<FieldName, ClusterAffinityDistribution> evaluateClustering(EvaluationContext context){
		ClusteringModel clusteringModel = getModel();

		ClusteringModel.ModelClass modelClass = clusteringModel.getModelClass();
		switch(modelClass){
			case CENTER_BASED:
				break;
			default:
				throw new UnsupportedFeatureException(clusteringModel, modelClass);
		}

		CenterFields centerFields = clusteringModel.getCenterFields();
		if(centerFields != null){
			throw new UnsupportedFeatureException(centerFields);
		}

		List<FieldValue> values = new ArrayList<>();

		List<ClusteringField> clusteringFields = getCenterClusteringFields();
		for(ClusteringField clusteringField : clusteringFields){
			FieldValue value = context.evaluate(clusteringField.getField());

			values.add(value);
		}

		ClusterAffinityDistribution result;

		ComparisonMeasure comparisonMeasure = clusteringModel.getComparisonMeasure();

		Measure measure = comparisonMeasure.getMeasure();

		if(MeasureUtil.isSimilarity(measure)){
			result = evaluateSimilarity(comparisonMeasure, clusteringFields, values);
		} else

		if(MeasureUtil.isDistance(measure)){
			result = evaluateDistance(comparisonMeasure, clusteringFields, values);
		} else

		{
			throw new UnsupportedFeatureException(measure);
		}

		// "For clustering models, the identifier of the winning cluster is returned as the predictedValue"
		result.computeResult(DataType.STRING);

		return Collections.singletonMap(getTargetField(), result);
	}

	private ClusterAffinityDistribution evaluateSimilarity(ComparisonMeasure comparisonMeasure, List<ClusteringField> clusteringFields, List<FieldValue> values){
		ClusteringModel clusteringModel = getModel();

		BiMap<String, Cluster> entityRegistry = getEntityRegistry();

		ClusterAffinityDistribution result = new ClusterAffinityDistribution(Classification.Type.SIMILARITY, entityRegistry);

		BitSet flags = MeasureUtil.toBitSet(values);

		List<Cluster> clusters = clusteringModel.getClusters();
		for(Cluster cluster : clusters){
			BitSet clusterFlags = CacheUtil.getValue(cluster, ClusteringModelEvaluator.clusterFlagCache);

			if(flags.size() != clusterFlags.size()){
				throw new InvalidFeatureException(cluster);
			}

			String id = EntityUtil.getId(cluster, entityRegistry);

			Double similarity = MeasureUtil.evaluateSimilarity(comparisonMeasure, clusteringFields, flags, clusterFlags);

			result.put(cluster, id, similarity);
		}

		return result;
	}

	private ClusterAffinityDistribution evaluateDistance(ComparisonMeasure comparisonMeasure, List<ClusteringField> clusteringFields, List<FieldValue> values){
		ClusteringModel clusteringModel = getModel();

		BiMap<String, Cluster> entityRegistry = getEntityRegistry();

		ClusterAffinityDistribution result = new ClusterAffinityDistribution(Classification.Type.DISTANCE, entityRegistry);

		Double adjustment;

		MissingValueWeights missingValueWeights = clusteringModel.getMissingValueWeights();
		if(missingValueWeights != null){
			Array array = missingValueWeights.getArray();

			List<? extends Number> adjustmentValues = ArrayUtil.asNumberList(array);
			if(values.size() != adjustmentValues.size()){
				throw new InvalidFeatureException(missingValueWeights);
			}

			adjustment = MeasureUtil.calculateAdjustment(values, adjustmentValues);
		} else

		{
			adjustment = MeasureUtil.calculateAdjustment(values);
		}

		List<Cluster> clusters = clusteringModel.getClusters();
		for(Cluster cluster : clusters){
			List<FieldValue> clusterValues = CacheUtil.getValue(cluster, ClusteringModelEvaluator.clusterValueCache);

			if(values.size() != clusterValues.size()){
				throw new InvalidFeatureException(cluster);
			}

			String id = EntityUtil.getId(cluster, entityRegistry);

			Double distance = MeasureUtil.evaluateDistance(comparisonMeasure, clusteringFields, values, clusterValues, adjustment);

			result.put(cluster, id, distance);
		}

		return result;
	}

	private List<ClusteringField> getCenterClusteringFields(){
		ClusteringModel clusteringModel = getModel();

		List<ClusteringField> result = new ArrayList<>();

		List<ClusteringField> clusteringFields = clusteringModel.getClusteringFields();
		for(ClusteringField clusteringField : clusteringFields){
			ClusteringField.CenterField centerField = clusteringField.getCenterField();

			switch(centerField){
				case TRUE:
					result.add(clusteringField);
					break;
				case FALSE:
					break;
				default:
					throw new UnsupportedFeatureException(clusteringField, centerField);
			}
		}

		return result;
	}

	private static final LoadingCache<Cluster, List<FieldValue>> clusterValueCache = CacheUtil.buildLoadingCache(new CacheLoader<Cluster, List<FieldValue>>(){

		@Override
		public List<FieldValue> load(Cluster cluster){
			Array array = cluster.getArray();

			List<? extends Number> values = ArrayUtil.asNumberList(array);

			return ImmutableList.copyOf(FieldValueUtil.createAll(values));
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