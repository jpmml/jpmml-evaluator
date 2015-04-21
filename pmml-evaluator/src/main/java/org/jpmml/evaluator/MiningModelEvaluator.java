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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.EmbeddedModel;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.MiningModel;
import org.dmg.pmml.Model;
import org.dmg.pmml.MultipleModelMethodType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.Segment;
import org.dmg.pmml.Segmentation;
import org.dmg.pmml.TreeModel;
import org.jpmml.manager.InvalidFeatureException;
import org.jpmml.manager.UnsupportedFeatureException;

public class MiningModelEvaluator extends ModelEvaluator<MiningModel> implements HasEntityRegistry<Segment> {

	private ModelEvaluatorFactory evaluatorFactory = null;


	public MiningModelEvaluator(PMML pmml){
		this(pmml, find(pmml.getModels(), MiningModel.class));
	}

	public MiningModelEvaluator(PMML pmml, MiningModel miningModel){
		super(pmml, miningModel);
	}

	@Override
	public String getSummary(){
		MiningModel miningModel = getModel();

		if(isRandomForest(miningModel)){
			return "Random forest";
		}

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

		Map<FieldName, ?> predictions = getRegressionResult(segmentResults);
		if(predictions != null){
			return predictions;
		}

		Segmentation segmentation = miningModel.getSegmentation();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();

		double sum = 0d;

		double denominator = 0d;

		for(SegmentResultMap segmentResult : segmentResults){
			Object targetValue = EvaluatorUtil.decode(segmentResult.getTargetValue());

			Number number = (Number)TypeUtil.parseOrCast(DataType.DOUBLE, targetValue);

			switch(multipleModelMethod){
				case SUM:
					sum += number.doubleValue();
					break;
				case AVERAGE:
					sum += number.doubleValue();
					denominator += 1d;
					break;
				case WEIGHTED_AVERAGE:
					double weight = segmentResult.getWeight();

					sum += (weight * number.doubleValue());
					denominator += weight;
					break;
				default:
					throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
			}
		}

		Double result;

		switch(multipleModelMethod){
			case SUM:
				result = sum;
				break;
			case AVERAGE:
			case WEIGHTED_AVERAGE:
				result = (sum / denominator);
				break;
			default:
				throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
		}

		return TargetUtil.evaluateRegression(result, context);
	}

	@SuppressWarnings (
		value = {"fallthrough"}
	)
	private Map<FieldName, ?> getRegressionResult(List<SegmentResultMap> segmentResults){
		MiningModel miningModel = getModel();

		Segmentation segmentation = miningModel.getSegmentation();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case SELECT_ALL:
				return selectAll(segmentResults);
			case SELECT_FIRST:
				if(segmentResults.size() > 0){
					return getFirst(segmentResults);
				}
				// Falls through
			case MODEL_CHAIN:
				if(segmentResults.size() > 0){
					return getLast(segmentResults);
				}
				// Falls through
			case SUM:
			case AVERAGE:
			case WEIGHTED_AVERAGE:
				if(segmentResults.size() == 0){
					return Collections.singletonMap(getTargetField(), null);
				}
				break;
			default:
				break;
		}

		return null;
	}

	private Map<FieldName, ?> evaluateClassification(MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResultMap> segmentResults = evaluateSegmentation(context);

		Map<FieldName, ?> predictions = getClassificationResult(segmentResults);
		if(predictions != null){
			return predictions;
		}

		Segmentation segmentation = miningModel.getSegmentation();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();

		ClassificationMap<String> result;

		switch(multipleModelMethod){
			case MAJORITY_VOTE:
			case WEIGHTED_MAJORITY_VOTE:
				{
					result = new ProbabilityClassificationMap();
					result.putAll(countVotes(segmentation, segmentResults));

					// Convert from votes to probabilities
					result.normalizeValues();
				}
				break;
			case AVERAGE:
			case WEIGHTED_AVERAGE:
				{
					// Averages and weighted averages of probabilities are probabilities
					result = new ProbabilityClassificationMap();
					result.putAll(aggregateProbabilities(segmentation, segmentResults));
				}
				break;
			case MAX:
				{
					// The aggregation operation implicitly converts from probabilities to votes
					result = new ClassificationMap<String>(ClassificationMap.Type.VOTE);
					result.putAll(aggregateProbabilities(segmentation, segmentResults));
				}
				break;
			default:
				throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
		}

		return TargetUtil.evaluateClassification(result, context);
	}

	@SuppressWarnings (
		value = {"fallthrough"}
	)
	private Map<FieldName, ?> getClassificationResult(List<SegmentResultMap> segmentResults){
		MiningModel miningModel = getModel();

		Segmentation segmentation = miningModel.getSegmentation();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case SELECT_ALL:
				return selectAll(segmentResults);
			case SELECT_FIRST:
				if(segmentResults.size() > 0){
					return getFirst(segmentResults);
				}
				// Falls through
			case MODEL_CHAIN:
				if(segmentResults.size() > 0){
					return getLast(segmentResults);
				}
				// Falls through
			case MAJORITY_VOTE:
			case WEIGHTED_MAJORITY_VOTE:
				if(segmentResults.size() == 0){
					return Collections.singletonMap(getTargetField(), null);
				}
				break;
			default:
				break;
		}

		return null;
	}

	private Map<FieldName, ?> evaluateClustering(MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResultMap> segmentResults = evaluateSegmentation(context);

		Map<FieldName, ?> predictions = getClusteringResult(segmentResults);
		if(predictions != null){
			return predictions;
		}

		Segmentation segmentation = miningModel.getSegmentation();

		ClassificationMap<String> result = new ClassificationMap<String>(ClassificationMap.Type.VOTE);
		result.putAll(countVotes(segmentation, segmentResults));

		return Collections.singletonMap(getTargetField(), result);
	}

	@SuppressWarnings (
		value = {"fallthrough"}
	)
	private Map<FieldName, ?> getClusteringResult(List<SegmentResultMap> segmentResults){
		MiningModel miningModel = getModel();

		Segmentation segmentation = miningModel.getSegmentation();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case SELECT_ALL:
				return selectAll(segmentResults);
			case SELECT_FIRST:
				if(segmentResults.size() > 0){
					return getFirst(segmentResults);
				}
				// Falls through
			case MODEL_CHAIN:
				if(segmentResults.size() > 0){
					return getLast(segmentResults);
				}
				// Falls through
			case MAJORITY_VOTE:
			case WEIGHTED_MAJORITY_VOTE:
				if(segmentResults.size() == 0){
					return Collections.singletonMap(getTargetField(), null);
				}
				break;
			default:
				break;
		}

		return null;
	}

	@SuppressWarnings (
		value = {"fallthrough"}
	)
	private Map<FieldName, ?> evaluateAny(MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResultMap> segmentResults = evaluateSegmentation(context);

		Segmentation segmentation = miningModel.getSegmentation();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case SELECT_ALL:
				return selectAll(segmentResults);
			case SELECT_FIRST:
				if(segmentResults.size() > 0){
					return getFirst(segmentResults);
				}
				// Falls through
			case MODEL_CHAIN:
				if(segmentResults.size() > 0){
					return getLast(segmentResults);
				}
				return Collections.singletonMap(getTargetField(), null);
			default:
				break;
		}

		throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
	}

	private List<SegmentResultMap> evaluateSegmentation(MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResultMap> results = Lists.newArrayList();

		Segmentation segmentation = miningModel.getSegmentation();

		LocalTransformations localTransformations = segmentation.getLocalTransformations();
		if(localTransformations != null){
			throw new UnsupportedFeatureException(localTransformations);
		}

		BiMap<Segment, String> inverseEntities = (getEntityRegistry()).inverse();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();

		Model lastModel = null;

		MiningFunctionType miningFunction = miningModel.getFunctionName();

		ModelEvaluatorFactory evaluatorFactory = getEvaluatorFactory();
		if(evaluatorFactory == null){
			evaluatorFactory = ModelEvaluatorFactory.getInstance();
		}

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

			String id = inverseEntities.get(segment);

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

			ModelEvaluator<?> evaluator = evaluatorFactory.getModelManager(getPMML(), model);

			ModelEvaluationContext segmentContext = evaluator.createContext(context);

			Map<FieldName, ?> result = evaluator.evaluate(segmentContext);

			FieldName targetField = evaluator.getTargetField();

			List<FieldName> outputFields = evaluator.getOutputFields();
			for(FieldName outputField : outputFields){
				FieldValue outputValue = segmentContext.getField(outputField);
				if(outputValue == null){
					throw new MissingFieldException(outputField, segment);
				}

				// "The OutputFields from one model element can be passed as input to the MiningSchema of subsequent models"
				context.declare(outputField, outputValue);
			}

			List<String> warnings = segmentContext.getWarnings();
			for(String warning : warnings){
				context.addWarning(warning);
			}

			SegmentResultMap segmentResult = new SegmentResultMap(segment, targetField);
			segmentResult.putAll(result);

			context.putResult(id, segmentResult);

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

	private Map<FieldName, ?> selectAll(List<SegmentResultMap> segmentResults){
		ListMultimap<FieldName, Object> result = ArrayListMultimap.create();

		Set<FieldName> keys = null;

		for(SegmentResultMap segmentResult : segmentResults){

			if(keys == null){
				keys = Sets.newLinkedHashSet(segmentResult.keySet());
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

	public ModelEvaluatorFactory getEvaluatorFactory(){
		return this.evaluatorFactory;
	}

	public void setEvaluatorFactory(ModelEvaluatorFactory evaluatorFactory){
		this.evaluatorFactory = evaluatorFactory;
	}

	static
	private <E> E getFirst(List<E> list){
		return list.get(0);
	}

	static
	private <E> E getLast(List<E> list){
		return list.get(list.size() - 1);
	}

	static
	private Map<String, Double> countVotes(Segmentation segmentation, List<SegmentResultMap> segmentResults){
		VoteCounter<String> counter = new VoteCounter<String>();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();

		for(SegmentResultMap segmentResult : segmentResults){
			String targetCategory = (String)EvaluatorUtil.decode(segmentResult.getTargetValue());

			switch(multipleModelMethod){
				case MAJORITY_VOTE:
					counter.increment(targetCategory);
					break;
				case WEIGHTED_MAJORITY_VOTE:
					counter.increment(targetCategory, segmentResult.getWeight());
					break;
				default:
					throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
			}
		}

		return counter;
	}

	static
	private Map<String, Double> aggregateProbabilities(Segmentation segmentation, List<SegmentResultMap> segmentResults){
		ProbabilityAggregator aggregator = new ProbabilityAggregator();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();

		double denominator = 0d;

		for(SegmentResultMap segmentResult : segmentResults){
			Object targetValue = segmentResult.getTargetValue();

			if(!(targetValue instanceof HasProbability)){
				throw new TypeCheckException(HasProbability.class, targetValue);
			}

			HasProbability hasProbability = (HasProbability)targetValue;

			switch(multipleModelMethod){
				case MAX:
					aggregator.max(hasProbability);
					break;
				case AVERAGE:
					aggregator.sum(hasProbability);
					denominator += 1d;
					break;
				case WEIGHTED_AVERAGE:
					double weight = segmentResult.getWeight();

					aggregator.sum(hasProbability, weight);
					denominator += weight;
					break;
				default:
					throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
			}
		}

		switch(multipleModelMethod){
			case MAX:
				break;
			case AVERAGE:
			case WEIGHTED_AVERAGE:
				aggregator.divide(denominator);
				break;
			default:
				throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
		}

		return aggregator;
	}

	static
	private boolean isRandomForest(MiningModel miningModel){
		Segmentation segmentation = miningModel.getSegmentation();

		if(segmentation == null){
			return false;
		}

		List<Segment> segments = segmentation.getSegments();

		// How many trees does it take to make a forest?
		boolean result = (segments.size() > 3);

		for(Segment segment : segments){
			Model model = segment.getModel();

			result &= (model instanceof TreeModel);
		}

		return result;
	}

	private static final LoadingCache<MiningModel, BiMap<String, Segment>> entityCache = CacheBuilder.newBuilder()
		.weakKeys()
		.build(new CacheLoader<MiningModel, BiMap<String, Segment>>(){

			@Override
			public BiMap<String, Segment> load(MiningModel miningModel){
				Segmentation segmentation = miningModel.getSegmentation();

				return EntityUtil.buildBiMap(segmentation.getSegments());
			}
		});
}