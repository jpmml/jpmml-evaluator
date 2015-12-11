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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.EmbeddedModel;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.MiningModel;
import org.dmg.pmml.Model;
import org.dmg.pmml.MultipleModelMethodType;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.Segment;
import org.dmg.pmml.Segmentation;

public class MiningModelEvaluator extends ModelEvaluator<MiningModel> implements HasEntityRegistry<Segment> {

	private ModelEvaluatorFactory evaluatorFactory = null;


	public MiningModelEvaluator(PMML pmml){
		super(pmml, MiningModel.class);
	}

	public MiningModelEvaluator(PMML pmml, MiningModel miningModel){
		super(pmml, miningModel);
	}

	@Override
	public String getSummary(){
		return "Ensemble model";
	}

	@Override
	public BiMap<String, Segment> getEntityRegistry(){
		return getValue(MiningModelEvaluator.entityCache);
	}

	@Override
	protected DataField getDataField(){
		MiningModel miningModel = getModel();

		Segmentation segmentation = miningModel.getSegmentation();
		if(segmentation == null){
			return null;
		}

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case SELECT_ALL:
				return null;
			default:
				return super.getDataField();
		}
	}

	@Override
	public MiningModelEvaluationContext createContext(ModelEvaluationContext parent){
		return new MiningModelEvaluationContext(parent, this);
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		return evaluate((MiningModelEvaluationContext)context);
	}

	public Map<FieldName, ?> evaluate(MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();
		if(!miningModel.isScorable()){
			throw new InvalidResultException(miningModel);
		}

		EmbeddedModel embeddedModel = Iterables.getFirst(miningModel.getEmbeddedModels(), null);
		if(embeddedModel != null){
			throw new UnsupportedFeatureException(embeddedModel);
		}

		Segmentation segmentation = miningModel.getSegmentation();
		if(segmentation == null){
			throw new InvalidFeatureException(miningModel);
		}

		Map<FieldName, ?> predictions;

		MiningFunctionType miningFunction = miningModel.getFunctionName();
		switch(miningFunction){
			case REGRESSION:
				predictions = evaluateRegression(context);
				break;
			case CLASSIFICATION:
				predictions = evaluateClassification(context);
				break;
			case CLUSTERING:
				predictions = evaluateClustering(context);
				break;
			default:
				predictions = evaluateAny(context);
				break;
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private Map<FieldName, ?> evaluateRegression(MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResultMap> segmentResults = evaluateSegmentation(context);

		Map<FieldName, ?> predictions = getSegmentationResult(REGRESSION_METHODS, segmentResults);
		if(predictions != null){
			return predictions;
		}

		Segmentation segmentation = miningModel.getSegmentation();

		Double result = aggregateValues(segmentation, segmentResults);

		return TargetUtil.evaluateRegression(result, context);
	}

	private Map<FieldName, ?> evaluateClassification(MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResultMap> segmentResults = evaluateSegmentation(context);

		Map<FieldName, ?> predictions = getSegmentationResult(CLASSIFICATION_METHODS, segmentResults);
		if(predictions != null){
			return predictions;
		}

		Segmentation segmentation = miningModel.getSegmentation();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();

		Classification result;

		switch(multipleModelMethod){
			case MAJORITY_VOTE:
			case WEIGHTED_MAJORITY_VOTE:
				{
					result = new ProbabilityDistribution();
					result.putAll(aggregateVotes(segmentation, segmentResults));

					// Convert from votes to probabilities
					result.normalizeValues();
				}
				break;
			case MAX:
			case MEDIAN:
				{
					// The max and median aggregation functions yield non-probability distributions
					result = new Classification(Classification.Type.VOTE);
					result.putAll(aggregateProbabilities(segmentation, segmentResults));
				}
				break;
			case AVERAGE:
			case WEIGHTED_AVERAGE:
				{
					// The average and weighted average (with weights summing to 1) aggregation functions yield probability distributions
					result = new ProbabilityDistribution();
					result.putAll(aggregateProbabilities(segmentation, segmentResults));
				}
				break;
			default:
				throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
		}

		return TargetUtil.evaluateClassification(result, context);
	}

	private Map<FieldName, ?> evaluateClustering(MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResultMap> segmentResults = evaluateSegmentation(context);

		Map<FieldName, ?> predictions = getSegmentationResult(CLUSTERING_METHODS, segmentResults);
		if(predictions != null){
			return predictions;
		}

		Segmentation segmentation = miningModel.getSegmentation();

		Classification result = new Classification(Classification.Type.VOTE);
		result.putAll(aggregateVotes(segmentation, segmentResults));

		result.computeResult(DataType.STRING);

		return Collections.singletonMap(getTargetField(), result);
	}

	private Map<FieldName, ?> evaluateAny(MiningModelEvaluationContext context){
		List<SegmentResultMap> segmentResults = evaluateSegmentation(context);

		return getSegmentationResult(Collections.<MultipleModelMethodType>emptySet(), segmentResults);
	}

	private List<SegmentResultMap> evaluateSegmentation(MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResultMap> results = new ArrayList<>();

		Segmentation segmentation = miningModel.getSegmentation();

		LocalTransformations localTransformations = segmentation.getLocalTransformations();
		if(localTransformations != null){
			throw new UnsupportedFeatureException(localTransformations);
		}

		ModelEvaluatorFactory evaluatorFactory = getEvaluatorFactory();
		if(evaluatorFactory == null){
			evaluatorFactory = ModelEvaluatorFactory.newInstance();
		}

		BiMap<String, Segment> entityRegistry = getEntityRegistry();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();

		Model lastModel = null;

		MiningFunctionType miningFunction = miningModel.getFunctionName();

		Map<FieldName, OutputField> segmentOutputFields = new HashMap<>();

		List<Segment> segments = segmentation.getSegments();
		for(Segment segment : segments){
			Predicate predicate = segment.getPredicate();
			if(predicate == null){
				throw new InvalidFeatureException(segment);
			}

			Boolean status = PredicateUtil.evaluate(predicate, context);
			if(status == null || !status.booleanValue()){
				continue;
			}

			Model model = segment.getModel();
			if(model == null){
				throw new InvalidFeatureException(segment);
			}

			// "With the exception of modelChain models, all model elements used inside Segment elements in one MiningModel must have the same MINING-FUNCTION"
			switch(multipleModelMethod){
				case MODEL_CHAIN:
					lastModel = model;
					break;
				default:
					if(!(miningFunction).equals(model.getFunctionName())){
						throw new InvalidFeatureException(model);
					}
					break;
			}

			ModelEvaluator<?> evaluator = evaluatorFactory.newModelManager(getPMML(), model);

			ModelEvaluationContext segmentContext = evaluator.createContext(context);

			boolean compatible = true;

			List<FieldName> activeFields = evaluator.getActiveFields();
			for(FieldName activeField : activeFields){
				MiningField miningField = evaluator.getMiningField(activeField);

				DataField dataField = getDataField(activeField);
				if(dataField != null){

					if(compatible){
						compatible &= MiningFieldUtil.isDefault(miningField);
					}

					continue;
				}

				OutputField outputField = null;

				switch(multipleModelMethod){
					case MODEL_CHAIN:
						outputField = segmentOutputFields.get(activeField);
						break;
					default:
						break;
				}

				if(outputField != null){
					FieldValue value = context.getField(activeField);

					if(value == null){
						value = FieldValueUtil.performMissingValueTreatment(outputField, miningField);
					} else

					{
						value = FieldValueUtil.performValidValueTreatment(outputField, miningField, FieldValueUtil.getValue(value));
					}

					segmentContext.declare(activeField, value);

					continue;
				}

				throw new InvalidFeatureException(miningField);
			}

			segmentContext.setCompatible(compatible);

			Map<FieldName, ?> result = evaluator.evaluate(segmentContext);

			FieldName targetField = evaluator.getTargetField();

			final
			String entityId = EntityUtil.getId(segment, entityRegistry);

			SegmentResultMap segmentResult = new SegmentResultMap(segment, targetField){

				@Override
				public String getEntityId(){
					return entityId;
				}
			};
			segmentResult.putAll(result);

			context.putResult(entityId, segmentResult);

			switch(multipleModelMethod){
				case MODEL_CHAIN:
					{
						List<FieldName> outputFields = evaluator.getOutputFields();
						for(FieldName outputField : outputFields){
							FieldValue outputValue = segmentContext.getField(outputField);
							if(outputValue == null){
								throw new MissingValueException(outputField, segment);
							}

							context.declare(outputField, outputValue);

							OutputField segmentOutputField = evaluator.getOutputField(outputField);

							segmentOutputFields.put(outputField, segmentOutputField);
						}
					}
					break;
				default:
					break;
			}

			List<String> warnings = segmentContext.getWarnings();
			for(String warning : warnings){
				context.addWarning(warning);
			}

			switch(multipleModelMethod){
				case SELECT_FIRST:
					return Collections.singletonList(segmentResult);
				default:
					results.add(segmentResult);
					break;
			}
		}

		// "The model element used inside the last Segment element executed must have the same MINING-FUNCTION"
		switch(multipleModelMethod){
			case MODEL_CHAIN:
				if(lastModel != null && !(miningFunction).equals(lastModel.getFunctionName())){
					throw new InvalidFeatureException(lastModel);
				}
				break;
			default:
				break;
		}

		return results;
	}

	private Map<FieldName, ?> getSegmentationResult(Set<MultipleModelMethodType> multipleModelMethods, List<SegmentResultMap> segmentResults){
		MiningModel miningModel = getModel();

		Segmentation segmentation = miningModel.getSegmentation();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case SELECT_ALL:
				return selectAll(segmentResults);
			case SELECT_FIRST:
				if(segmentResults.size() > 0){
					return segmentResults.get(0);
				}
				break;
			case MODEL_CHAIN:
				if(segmentResults.size() > 0){
					return segmentResults.get(segmentResults.size() - 1);
				}
				break;
			default:
				if(!(multipleModelMethods).contains(multipleModelMethod)){
					throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
				}
				break;
		}

		// "If no segments have predicates that evaluate to true, then the result is a missing value"
		if(segmentResults.size() == 0){
			return Collections.singletonMap(getTargetField(), null);
		}

		return null;
	}

	public ModelEvaluatorFactory getEvaluatorFactory(){
		return this.evaluatorFactory;
	}

	public void setEvaluatorFactory(ModelEvaluatorFactory evaluatorFactory){
		this.evaluatorFactory = evaluatorFactory;
	}

	static
	private Double aggregateValues(Segmentation segmentation, List<SegmentResultMap> segmentResults){
		RegressionAggregator aggregator = new RegressionAggregator();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();

		double denominator = 0d;

		for(SegmentResultMap segmentResult : segmentResults){
			Object targetValue = EvaluatorUtil.decode(segmentResult.getTargetValue());

			Double value = (Double)TypeUtil.parseOrCast(DataType.DOUBLE, targetValue);

			switch(multipleModelMethod){
				case SUM:
				case MEDIAN:
					aggregator.add(value);
					break;
				case AVERAGE:
					aggregator.add(value);
					denominator += 1d;
					break;
				case WEIGHTED_AVERAGE:
					double weight = segmentResult.getWeight();

					aggregator.add(value * weight);
					denominator += weight;
					break;
				default:
					throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
			}
		}

		switch(multipleModelMethod){
			case SUM:
				return aggregator.sum();
			case MEDIAN:
				return aggregator.median();
			case AVERAGE:
			case WEIGHTED_AVERAGE:
				return aggregator.average(denominator);
			default:
				throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
		}
	}

	static
	private Map<String, Double> aggregateVotes(Segmentation segmentation, List<SegmentResultMap> segmentResults){
		VoteAggregator<String> aggregator = new VoteAggregator<>();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();

		for(SegmentResultMap segmentResult : segmentResults){
			Object targetValue = EvaluatorUtil.decode(segmentResult.getTargetValue());

			String key = (String)targetValue;

			switch(multipleModelMethod){
				case MAJORITY_VOTE:
					aggregator.add(key, 1d);
					break;
				case WEIGHTED_MAJORITY_VOTE:
					aggregator.add(key, segmentResult.getWeight());
					break;
				default:
					throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
			}
		}

		return aggregator.sumMap();
	}

	static
	private Map<String, Double> aggregateProbabilities(Segmentation segmentation, List<SegmentResultMap> segmentResults){
		ProbabilityAggregator aggregator = new ProbabilityAggregator();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();

		double denominator = 0d;

		for(SegmentResultMap segmentResult : segmentResults){
			Object targetValue = segmentResult.getTargetValue();

			HasProbability hasProbability = TypeUtil.cast(HasProbability.class, targetValue);

			switch(multipleModelMethod){
				case MAX:
				case MEDIAN:
					aggregator.add(hasProbability);
					break;
				case AVERAGE:
					aggregator.add(hasProbability);
					denominator += 1d;
					break;
				case WEIGHTED_AVERAGE:
					double weight = segmentResult.getWeight();

					aggregator.add(hasProbability, weight);
					denominator += weight;
					break;
				default:
					throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
			}
		}

		switch(multipleModelMethod){
			case MAX:
				return aggregator.maxMap();
			case MEDIAN:
				return aggregator.medianMap();
			case AVERAGE:
			case WEIGHTED_AVERAGE:
				return aggregator.averageMap(denominator);
			default:
				throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
		}
	}

	static
	private Map<FieldName, ?> selectAll(List<SegmentResultMap> segmentResults){
		ListMultimap<FieldName, Object> result = ArrayListMultimap.create();

		Set<FieldName> keys = null;

		for(SegmentResultMap segmentResult : segmentResults){

			if(keys == null){
				keys = new LinkedHashSet<>(segmentResult.keySet());
			} // End if

			// Ensure that all List values in the ListMultimap contain the same number of elements
			if(!(keys).equals(segmentResult.keySet())){
				throw new EvaluationException();
			}

			for(FieldName key : keys){
				result.put(key, segmentResult.get(key));
			}
		}

		return result.asMap();
	}

	private static final Set<MultipleModelMethodType> REGRESSION_METHODS = EnumSet.of(MultipleModelMethodType.SUM, MultipleModelMethodType.MEDIAN, MultipleModelMethodType.AVERAGE, MultipleModelMethodType.WEIGHTED_AVERAGE);
	private static final Set<MultipleModelMethodType> CLASSIFICATION_METHODS = EnumSet.of(MultipleModelMethodType.MAJORITY_VOTE, MultipleModelMethodType.WEIGHTED_MAJORITY_VOTE, MultipleModelMethodType.SUM, MultipleModelMethodType.MEDIAN, MultipleModelMethodType.AVERAGE, MultipleModelMethodType.WEIGHTED_AVERAGE);
	private static final Set<MultipleModelMethodType> CLUSTERING_METHODS = EnumSet.of(MultipleModelMethodType.MAJORITY_VOTE, MultipleModelMethodType.WEIGHTED_MAJORITY_VOTE);

	private static final LoadingCache<MiningModel, BiMap<String, Segment>> entityCache = CacheUtil.buildLoadingCache(new CacheLoader<MiningModel, BiMap<String, Segment>>(){

		@Override
		public BiMap<String, Segment> load(MiningModel miningModel){
			Segmentation segmentation = miningModel.getSegmentation();

			return EntityUtil.buildBiMap(segmentation.getSegments());
		}
	});
}