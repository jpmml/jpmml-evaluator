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

import java.util.*;

import org.jpmml.manager.*;

import org.dmg.pmml.*;

import com.google.common.cache.*;
import com.google.common.collect.*;

public class ClusteringModelEvaluator extends ModelEvaluator<ClusteringModel> implements HasEntityRegistry<Cluster> {

	public ClusteringModelEvaluator(PMML pmml){
		this(pmml, find(pmml.getModels(), ClusteringModel.class));
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

		Map<FieldName, ?> predictions;

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

	private Map<FieldName, ClusterClassificationMap> evaluateClustering(EvaluationContext context){
		ClusteringModel clusteringModel = getModel();

		ClusteringModel.ModelClass modelClass = clusteringModel.getModelClass();
		switch(modelClass){
			case CENTER_BASED:
				break;
			default:
				throw new UnsupportedFeatureException(clusteringModel, modelClass);
		}

		List<FieldValue> values = Lists.newArrayList();

		List<ClusteringField> clusteringFields = getCenterClusteringFields();
		for(ClusteringField clusteringField : clusteringFields){
			FieldValue value = ExpressionUtil.evaluate(clusteringField.getField(), context);

			values.add(value);
		}

		ClusterClassificationMap result;

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

		return Collections.singletonMap(getTargetField(), result);
	}

	private ClusterClassificationMap evaluateSimilarity(ComparisonMeasure comparisonMeasure, List<ClusteringField> clusteringFields, List<FieldValue> values){
		ClusteringModel clusteringModel = getModel();

		ClusterClassificationMap result = new ClusterClassificationMap(ClassificationMap.Type.SIMILARITY);

		BitSet flags = MeasureUtil.toBitSet(values);

		BiMap<Cluster, String> inverseEntities = (getEntityRegistry()).inverse();

		List<Cluster> clusters = clusteringModel.getClusters();
		for(Cluster cluster : clusters){
			BitSet clusterFlags = CacheUtil.getValue(cluster, ClusteringModelEvaluator.clusterFlagCache);

			if(flags.size() != clusterFlags.size()){
				throw new InvalidFeatureException(cluster);
			}

			String id = inverseEntities.get(cluster);

			Double similarity = MeasureUtil.evaluateSimilarity(comparisonMeasure, clusteringFields, flags, clusterFlags);

			result.put(cluster, id, similarity);
		}

		return result;
	}

	private ClusterClassificationMap evaluateDistance(ComparisonMeasure comparisonMeasure, List<ClusteringField> clusteringFields, List<FieldValue> values){
		ClusteringModel clusteringModel = getModel();

		ClusterClassificationMap result = new ClusterClassificationMap(ClassificationMap.Type.DISTANCE);

		Double adjustment;

		MissingValueWeights missingValueWeights = clusteringModel.getMissingValueWeights();
		if(missingValueWeights != null){
			Array array = missingValueWeights.getArray();

			List<Double> adjustmentValues = ArrayUtil.getRealContent(array);
			if(values.size() != adjustmentValues.size()){
				throw new InvalidFeatureException(missingValueWeights);
			}

			adjustment = MeasureUtil.calculateAdjustment(values, adjustmentValues);
		} else

		{
			adjustment = MeasureUtil.calculateAdjustment(values);
		}

		BiMap<Cluster, String> inverseEntities = (getEntityRegistry()).inverse();

		List<Cluster> clusters = clusteringModel.getClusters();
		for(Cluster cluster : clusters){
			List<FieldValue> clusterValues = CacheUtil.getValue(cluster, ClusteringModelEvaluator.clusterValueCache);

			if(values.size() != clusterValues.size()){
				throw new InvalidFeatureException(cluster);
			}

			String id = inverseEntities.get(cluster);

			Double distance = MeasureUtil.evaluateDistance(comparisonMeasure, clusteringFields, values, clusterValues, adjustment);

			result.put(cluster, id, distance);
		}

		return result;
	}

	private List<ClusteringField> getCenterClusteringFields(){
		ClusteringModel clusteringModel = getModel();

		List<ClusteringField> result = Lists.newArrayList();

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

	private static final LoadingCache<Cluster, List<FieldValue>> clusterValueCache = CacheBuilder.newBuilder()
		.weakKeys()
		.build(new CacheLoader<Cluster, List<FieldValue>>(){

			@Override
			public List<FieldValue> load(Cluster cluster){
				Array array = cluster.getArray();

				List<FieldValue> result = Lists.newArrayList();

				List<? extends Number> values = ArrayUtil.getNumberContent(array);
				for(Number value : values){
					result.add(FieldValueUtil.create(value));
				}

				return result;
			}
		});

	private static final LoadingCache<Cluster, BitSet> clusterFlagCache = CacheBuilder.newBuilder()
		.weakKeys()
		.build(new CacheLoader<Cluster, BitSet>(){

			@Override
			public BitSet load(Cluster cluster){
				List<FieldValue> values = CacheUtil.getValue(cluster, ClusteringModelEvaluator.clusterValueCache);

				return MeasureUtil.toBitSet(values);
			}
		});

	private static final LoadingCache<ClusteringModel, BiMap<String, Cluster>> entityCache = CacheBuilder.newBuilder()
		.weakKeys()
		.build(new CacheLoader<ClusteringModel, BiMap<String, Cluster>>(){

			@Override
			public BiMap<String, Cluster> load(ClusteringModel clusteringModel){
				BiMap<String, Cluster> result = HashBiMap.create();

				EntityUtil.putAll(clusteringModel.getClusters(), result);

				return result;
			}
		});
}