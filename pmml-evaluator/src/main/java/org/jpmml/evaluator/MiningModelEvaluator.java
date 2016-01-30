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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

	private ConcurrentMap<String, SegmentHandler> segmentHandlers = new ConcurrentHashMap<>();

	transient
	private BiMap<String, Segment> entityRegistry = null;


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

		if(this.entityRegistry == null){
			this.entityRegistry = getValue(MiningModelEvaluator.entityCache);
		}

		return this.entityRegistry;
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
	public Map<FieldName, ?> evaluate(Map<FieldName, ?> arguments){
		ModelEvaluationContext context = new MiningModelEvaluationContext(this);
		context.setArguments(arguments);

		return evaluate(context);
	}

	@Override
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		return evaluate((MiningModelEvaluationContext)context);
	}

	public Map<FieldName, ?> evaluate(MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();
		if(!miningModel.isScorable()){
			throw new InvalidResultException(miningModel);
		} // End if

		if(miningModel.hasEmbeddedModels()){
			EmbeddedModel embeddedModel = Iterables.get(miningModel.getEmbeddedModels(), 0);

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

		List<SegmentResult> segmentResults = evaluateSegmentation(context);

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

		List<SegmentResult> segmentResults = evaluateSegmentation(context);

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

		List<SegmentResult> segmentResults = evaluateSegmentation(context);

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
		List<SegmentResult> segmentResults = evaluateSegmentation(context);

		return getSegmentationResult(Collections.<MultipleModelMethodType>emptySet(), segmentResults);
	}

	private List<SegmentResult> evaluateSegmentation(MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();

		Segmentation segmentation = miningModel.getSegmentation();

		LocalTransformations localTransformations = segmentation.getLocalTransformations();
		if(localTransformations != null){
			throw new UnsupportedFeatureException(localTransformations);
		}

		BiMap<String, Segment> entityRegistry = getEntityRegistry();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();

		Model lastModel = null;

		MiningFunctionType miningFunction = miningModel.getFunctionName();

		MiningModelEvaluationContext miningModelContext = null;

		ModelEvaluationContext modelContext = null;

		List<Segment> segments = segmentation.getSegments();

		List<SegmentResult> results = new ArrayList<>(segments.size());

		for(int i = 0, max = segments.size(); i < max; i++){
			Segment segment = segments.get(i);

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

			String segmentId = EntityUtil.getId(segment, entityRegistry);

			SegmentHandler segmentHandler = this.segmentHandlers.get(segmentId);
			if(segmentHandler == null){
				segmentHandler = createSegmentHandler(model);

				this.segmentHandlers.putIfAbsent(segmentId, segmentHandler);
			}

			ModelEvaluator<?> segmentEvaluator = (ModelEvaluator<?>)segmentHandler.getEvaluator();

			ModelEvaluationContext segmentContext;

			if(segmentEvaluator instanceof MiningModelEvaluator){
				MiningModelEvaluator segmentMiningEvaluator = (MiningModelEvaluator)segmentEvaluator;

				if(miningModelContext == null){
					miningModelContext = new MiningModelEvaluationContext(context, segmentMiningEvaluator);
				} else

				{
					miningModelContext.reset(segmentMiningEvaluator);
				}

				segmentContext = miningModelContext;
			} else

			{
				if(modelContext == null){
					modelContext = new ModelEvaluationContext(context, segmentEvaluator);
				} else

				{
					modelContext.reset(segmentEvaluator);
				}

				segmentContext = modelContext;
			}

			segmentContext.setCompatible(segmentHandler.isCompatible());

			SegmentResult segmentResult;

			try {
				Map<FieldName, ?> result = segmentEvaluator.evaluate(segmentContext);

				FieldName segmentTargetField = segmentEvaluator.getTargetField();

				segmentResult = new SegmentResult(segment, segmentId, result, segmentTargetField);
			} catch(PMMLException pe){
				pe.ensureContext(segment);

				throw pe;
			}

			context.putResult(segmentId, segmentResult);

			switch(multipleModelMethod){
				case MODEL_CHAIN:
					{
						List<FieldName> outputFields = segmentEvaluator.getOutputFields();
						for(FieldName outputField : outputFields){
							OutputField segmentOutputField = segmentEvaluator.getOutputField(outputField);
							if(outputField == null){
								throw new MissingFieldException(outputField, segment);
							}

							context.putOutputField(segmentOutputField);

							FieldValue outputValue = segmentContext.getField(outputField);
							if(outputValue == null){
								throw new MissingValueException(outputField, segment);
							}

							context.declare(outputField, outputValue);
						}
					}
					break;
				default:
					break;
			}

			List<String> segmentWarnings = segmentContext.getWarnings();
			for(String segmentWarning : segmentWarnings){
				context.addWarning(segmentWarning);
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

	private Map<FieldName, ?> getSegmentationResult(Set<MultipleModelMethodType> multipleModelMethods, List<SegmentResult> segmentResults){
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

	private SegmentHandler createSegmentHandler(Model model){
		ModelEvaluatorFactory evaluatorFactory = getEvaluatorFactory();

		if(evaluatorFactory == null){
			evaluatorFactory = ModelEvaluatorFactory.newInstance();
		}

		ModelEvaluator<?> evaluator = evaluatorFactory.newModelManager(getPMML(), model);

		boolean compatible = true;

		List<FieldName> activeFields = evaluator.getActiveFields();
		for(FieldName activeField : activeFields){
			MiningField miningField = evaluator.getMiningField(activeField);

			DataField dataField = getDataField(activeField);
			if(dataField != null){
				compatible &= MiningFieldUtil.isDefault(miningField);
			}
		}

		SegmentHandler result = new SegmentHandler(evaluator, compatible);

		return result;
	}

	public ModelEvaluatorFactory getEvaluatorFactory(){
		return this.evaluatorFactory;
	}

	public void setEvaluatorFactory(ModelEvaluatorFactory evaluatorFactory){
		this.evaluatorFactory = evaluatorFactory;
	}

	static
	private Double aggregateValues(Segmentation segmentation, List<SegmentResult> segmentResults){
		RegressionAggregator aggregator = new RegressionAggregator();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();

		double denominator = 0d;

		for(SegmentResult segmentResult : segmentResults){
			Double value = (Double)segmentResult.getTargetValue(DataType.DOUBLE);

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
	private Map<String, Double> aggregateVotes(Segmentation segmentation, List<SegmentResult> segmentResults){
		VoteAggregator<String> aggregator = new VoteAggregator<>();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();

		for(SegmentResult segmentResult : segmentResults){
			String key = (String)segmentResult.getTargetValue(DataType.STRING);

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
	private Map<String, Double> aggregateProbabilities(Segmentation segmentation, List<SegmentResult> segmentResults){
		ProbabilityAggregator aggregator = new ProbabilityAggregator();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();

		double denominator = 0d;

		for(SegmentResult segmentResult : segmentResults){
			HasProbability hasProbability = segmentResult.getTargetValue(HasProbability.class);

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
	private Map<FieldName, ?> selectAll(List<SegmentResult> segmentResults){
		ListMultimap<FieldName, Object> result = ArrayListMultimap.create();

		Set<FieldName> keys = null;

		for(SegmentResult segmentResult : segmentResults){

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

	static
	private class SegmentHandler implements Serializable {

		private Evaluator evaluator = null;

		private boolean compatible = false;


		private SegmentHandler(Evaluator evaluator, boolean compatible){
			setEvaluator(evaluator);
			setCompatible(compatible);
		}

		public Evaluator getEvaluator(){
			return this.evaluator;
		}

		private void setEvaluator(Evaluator evaluator){
			this.evaluator = evaluator;
		}

		public boolean isCompatible(){
			return this.compatible;
		}

		private void setCompatible(boolean compatible){
			this.compatible = compatible;
		}
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