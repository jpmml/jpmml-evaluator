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
package org.jpmml.evaluator.mining;

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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.EmbeddedModel;
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.True;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segment;
import org.dmg.pmml.mining.Segmentation;
import org.jpmml.evaluator.CacheUtil;
import org.jpmml.evaluator.Classification;
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.HasProbability;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.InvalidFeatureException;
import org.jpmml.evaluator.InvalidResultException;
import org.jpmml.evaluator.MiningFieldUtil;
import org.jpmml.evaluator.MissingValueException;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.PMMLException;
import org.jpmml.evaluator.PredicateUtil;
import org.jpmml.evaluator.ProbabilityAggregator;
import org.jpmml.evaluator.ProbabilityDistribution;
import org.jpmml.evaluator.RegressionAggregator;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.UnsupportedFeatureException;
import org.jpmml.evaluator.VoteAggregator;

public class MiningModelEvaluator extends ModelEvaluator<MiningModel> implements MiningModelConsumer, HasEntityRegistry<Segment> {

	private ModelEvaluatorFactory evaluatorFactory = null;

	private ConcurrentMap<String, SegmentHandler> segmentHandlers = new ConcurrentHashMap<>();

	transient
	private BiMap<String, Segment> entityRegistry = null;


	public MiningModelEvaluator(PMML pmml){
		this(pmml, selectModel(pmml, MiningModel.class));
	}

	public MiningModelEvaluator(PMML pmml, MiningModel miningModel){
		super(pmml, miningModel);

		Segmentation segmentation = miningModel.getSegmentation();
		if(segmentation == null){
			throw new InvalidFeatureException(miningModel);
		}
	}

	@Override
	public Segmentation.MultipleModelMethod getMultipleModelMethod(){
		MiningModel miningModel = getModel();

		Segmentation segmentation = miningModel.getSegmentation();

		return segmentation.getMultipleModelMethod();
	}

	@Override
	public String getSummary(){
		return "Ensemble model";
	}

	@Override
	protected DataField getDataField(){
		Segmentation.MultipleModelMethod multipleModelMethod = getMultipleModelMethod();

		switch(multipleModelMethod){
			case SELECT_ALL:
			case SELECT_FIRST:
			case MODEL_CHAIN:
				return null;
			default:
				return super.getDataField();
		}
	}

	@Override
	public BiMap<String, Segment> getEntityRegistry(){

		if(this.entityRegistry == null){
			this.entityRegistry = getValue(MiningModelEvaluator.entityCache);
		}

		return this.entityRegistry;
	}

	@Override
	protected List<OutputField> createOutputFields(){
		List<OutputField> outputFields = super.createOutputFields();

		List<OutputField> nestedOutputFields = createNestedOutputFields();
		if(nestedOutputFields.size() > 0){
			// Depth-first ordering
			return ImmutableList.copyOf(Iterables.concat(nestedOutputFields, outputFields));
		}

		return outputFields;
	}

	@Override
	public Map<FieldName, ?> evaluate(Map<FieldName, ?> arguments){
		MiningModelEvaluationContext context = new MiningModelEvaluationContext(this);
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

		Map<FieldName, ?> predictions;

		MiningFunction miningFunction = miningModel.getMiningFunction();
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

		ProbabilityDistribution result;

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case MAJORITY_VOTE:
			case WEIGHTED_MAJORITY_VOTE:
				{
					result = new ProbabilityDistribution(aggregateVotes(segmentation, segmentResults));

					// Convert from votes to probabilities
					result.normalizeValues();
				}
				break;
			case MAX:
			case MEDIAN:
				{
					TargetField targetField = getTargetField();

					DataField dataField = targetField.getDataField();

					List<String> categories = FieldValueUtil.getTargetCategories(dataField);

					result = new ProbabilityDistribution(aggregateProbabilities(segmentation, segmentResults, categories));
				}
				break;
			case AVERAGE:
			case WEIGHTED_AVERAGE:
				{
					result = new ProbabilityDistribution(aggregateProbabilities(segmentation, segmentResults, null));
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

		Classification result = new Classification(Classification.Type.VOTE, aggregateVotes(segmentation, segmentResults));

		result.computeResult(DataType.STRING);

		return Collections.singletonMap(getTargetFieldName(), result);
	}

	private Map<FieldName, ?> evaluateAny(MiningModelEvaluationContext context){
		List<SegmentResult> segmentResults = evaluateSegmentation(context);

		return getSegmentationResult(Collections.<Segmentation.MultipleModelMethod>emptySet(), segmentResults);
	}

	private List<SegmentResult> evaluateSegmentation(MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();

		Segmentation segmentation = miningModel.getSegmentation();

		LocalTransformations localTransformations = segmentation.getLocalTransformations();
		if(localTransformations != null){
			throw new UnsupportedFeatureException(localTransformations);
		}

		BiMap<String, Segment> entityRegistry = getEntityRegistry();

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();

		Model lastModel = null;

		MiningFunction miningFunction = miningModel.getMiningFunction();

		MiningModelEvaluationContext miningModelContext = null;

		ModelEvaluationContext modelContext = null;

		List<Segment> segments = segmentation.getSegments();

		List<SegmentResult> segmentResults = new ArrayList<>(segments.size());

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
					if(!(miningFunction).equals(model.getMiningFunction())){
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

				TargetField segmentTargetField = segmentEvaluator.getTargetField();

				segmentResult = new SegmentResult(segment, segmentId, result, segmentTargetField);
			} catch(PMMLException pe){
				pe.ensureContext(segment);

				throw pe;
			}

			context.putResult(segmentId, segmentResult);

			switch(multipleModelMethod){
				case MODEL_CHAIN:
					{
						List<OutputField> outputFields = segmentEvaluator.getOutputFields();
						for(OutputField outputField : outputFields){
							FieldName name = outputField.getName();

							int depth = outputField.getDepth();
							if(depth > 0){
								continue;
							}

							context.putOutputField(outputField.getOutputField());

							FieldValue value = segmentContext.getField(name);
							if(value == null){
								throw new MissingValueException(name, segment);
							}

							context.declare(name, value);
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
					segmentResults.add(segmentResult);
					break;
			}
		}

		// "The model element used inside the last Segment element executed must have the same MINING-FUNCTION"
		switch(multipleModelMethod){
			case MODEL_CHAIN:
				if(lastModel != null && !(miningFunction).equals(lastModel.getMiningFunction())){
					throw new InvalidFeatureException(lastModel);
				}
				break;
			default:
				break;
		}

		return segmentResults;
	}

	private Map<FieldName, ?> getSegmentationResult(Set<Segmentation.MultipleModelMethod> multipleModelMethods, List<SegmentResult> segmentResults){
		MiningModel miningModel = getModel();

		Segmentation segmentation = miningModel.getSegmentation();

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();
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
			return Collections.singletonMap(getTargetFieldName(), null);
		}

		return null;
	}

	private List<Segment> getActiveHead(List<Segment> segments){

		for(int i = 0, max = segments.size(); i < max; i++){
			Segment segment = segments.get(i);

			Predicate predicate = segment.getPredicate();
			if(predicate == null){
				throw new InvalidFeatureException(segment);
			} // End if

			if(predicate instanceof True){
				return segments.subList(0, i + 1);
			}
		}

		return segments;
	}

	private List<Segment> getActiveTail(List<Segment> segments){
		return Lists.reverse(getActiveHead(Lists.reverse(segments)));
	}

	private List<OutputField> createNestedOutputFields(){
		MiningModel miningModel = getModel();

		Segmentation segmentation = miningModel.getSegmentation();

		List<Segment> segments = segmentation.getSegments();

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case SELECT_ALL:
				// Ignored
				break;
			case SELECT_FIRST:
				return createNestedOutputFields(getActiveHead(segments));
			case MODEL_CHAIN:
				return createNestedOutputFields(getActiveTail(segments));
			default:
				break;
		}

		return Collections.emptyList();
	}

	private List<OutputField> createNestedOutputFields(List<Segment> segments){
		List<OutputField> result = new ArrayList<>();

		BiMap<String, Segment> entityRegistry = getEntityRegistry();

		for(int i = 0, max = segments.size(); i < max; i++){
			Segment segment = segments.get(i);

			Model model = segment.getModel();
			if(model == null){
				throw new InvalidFeatureException(segment);
			}

			String segmentId = EntityUtil.getId(segment, entityRegistry);

			SegmentHandler segmentHandler = this.segmentHandlers.get(segmentId);
			if(segmentHandler == null){
				segmentHandler = createSegmentHandler(model);

				this.segmentHandlers.putIfAbsent(segmentId, segmentHandler);
			}

			Evaluator evaluator = segmentHandler.getEvaluator();

			List<OutputField> outputFields = evaluator.getOutputFields();
			for(OutputField outputField : outputFields){
				OutputField nestedOutputField = new OutputField(outputField);

				result.add(nestedOutputField);
			}
		}

		return ImmutableList.copyOf(result);
	}

	private SegmentHandler createSegmentHandler(Model model){
		ModelEvaluatorFactory evaluatorFactory = getEvaluatorFactory();

		if(evaluatorFactory == null){
			evaluatorFactory = ModelEvaluatorFactory.newInstance();
		}

		ModelEvaluator<?> evaluator = evaluatorFactory.newModelManager(getPMML(), model);

		boolean compatible = true;

		List<InputField> activeFields = evaluator.getActiveFields();
		for(InputField activeField : activeFields){
			Field field = activeField.getField();
			MiningField miningField = activeField.getMiningField();

			if(field instanceof DataField){
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
		RegressionAggregator aggregator;

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case MEDIAN:
				aggregator = new RegressionAggregator(segmentResults.size());
				break;
			default:
				aggregator = new RegressionAggregator();
				break;
		}

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

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();

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
	private Map<String, Double> aggregateProbabilities(Segmentation segmentation, List<SegmentResult> segmentResults, List<String> categories){
		ProbabilityAggregator aggregator;

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case MAX:
			case MEDIAN:
				aggregator = new ProbabilityAggregator(segmentResults.size());
				break;
			default:
				aggregator = new ProbabilityAggregator();
				break;
		}

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
				return aggregator.maxMap(categories);
			case MEDIAN:
				return aggregator.medianMap(categories);
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

	private static final Set<Segmentation.MultipleModelMethod> REGRESSION_METHODS = EnumSet.of(Segmentation.MultipleModelMethod.SUM, Segmentation.MultipleModelMethod.MEDIAN, Segmentation.MultipleModelMethod.AVERAGE, Segmentation.MultipleModelMethod.WEIGHTED_AVERAGE);
	private static final Set<Segmentation.MultipleModelMethod> CLASSIFICATION_METHODS = EnumSet.of(Segmentation.MultipleModelMethod.MAJORITY_VOTE, Segmentation.MultipleModelMethod.WEIGHTED_MAJORITY_VOTE, Segmentation.MultipleModelMethod.SUM, Segmentation.MultipleModelMethod.MEDIAN, Segmentation.MultipleModelMethod.AVERAGE, Segmentation.MultipleModelMethod.WEIGHTED_AVERAGE);
	private static final Set<Segmentation.MultipleModelMethod> CLUSTERING_METHODS = EnumSet.of(Segmentation.MultipleModelMethod.MAJORITY_VOTE, Segmentation.MultipleModelMethod.WEIGHTED_MAJORITY_VOTE);

	private static final LoadingCache<MiningModel, BiMap<String, Segment>> entityCache = CacheUtil.buildLoadingCache(new CacheLoader<MiningModel, BiMap<String, Segment>>(){

		@Override
		public BiMap<String, Segment> load(MiningModel miningModel){
			Segmentation segmentation = miningModel.getSegmentation();

			return EntityUtil.buildBiMap(segmentation.getSegments());
		}
	});
}