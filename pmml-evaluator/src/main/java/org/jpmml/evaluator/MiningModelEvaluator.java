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

import com.google.common.collect.*;

public class MiningModelEvaluator extends ModelEvaluator<MiningModel> {

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
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context){
		MiningModel miningModel = getModel();
		if(!miningModel.isScorable()){
			throw new InvalidResultException(miningModel);
		}

		EmbeddedModel embeddedModel = Iterables.getFirst(miningModel.getEmbeddedModels(), null);
		if(embeddedModel != null){
			throw new UnsupportedFeatureException(embeddedModel);
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

	private Map<FieldName, ?> evaluateRegression(ModelEvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResult> segmentResults = evaluateSegmentation(context);

		Segmentation segmentation = miningModel.getSegmentation();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case SELECT_FIRST:
			case MODEL_CHAIN:
				return dispatchSingleResult(segmentation, segmentResults);
			case SELECT_ALL:
				throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
			default:
				break;
		}

		double sum = 0d;

		for(SegmentResult segmentResult : segmentResults){
			Object targetValue = EvaluatorUtil.decode(segmentResult.getTargetValue());

			Number number = (Number)TypeUtil.parseOrCast(DataType.DOUBLE, targetValue);

			switch(multipleModelMethod){
				case SUM:
				case AVERAGE:
					sum += number.doubleValue();
					break;
				case WEIGHTED_AVERAGE:
					sum += segmentResult.getWeight() * number.doubleValue();
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
				result = (sum / segmentResults.size());
				break;
			default:
				throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
		}

		return TargetUtil.evaluateRegression(result, context);
	}

	private Map<FieldName, ?> evaluateClassification(ModelEvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResult> segmentResults = evaluateSegmentation(context);

		Segmentation segmentation = miningModel.getSegmentation();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case SELECT_FIRST:
			case MODEL_CHAIN:
				return dispatchSingleResult(segmentation, segmentResults);
			case SELECT_ALL:
				throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
			default:
				break;
		}

		DefaultClassificationMap<Object> result = new DefaultClassificationMap<Object>();
		result.putAll(countVotes(segmentation, segmentResults));

		// Convert from votes to probabilities
		result.normalizeValues();

		return TargetUtil.evaluateClassification(result, context);
	}

	private Map<FieldName, ?> evaluateClustering(ModelEvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResult> segmentResults = evaluateSegmentation(context);

		Segmentation segmentation = miningModel.getSegmentation();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case SELECT_FIRST:
			case MODEL_CHAIN:
				return dispatchSingleResult(segmentation, segmentResults);
			case SELECT_ALL:
				throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
			default:
				break;
		}

		ClassificationMap<Object> result = new ClassificationMap<Object>(ClassificationMap.Type.VOTE);
		result.putAll(countVotes(segmentation, segmentResults));

		return Collections.singletonMap(getTargetField(), result);
	}

	private Map<FieldName, ?> evaluateAny(ModelEvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResult> segmentResults = evaluateSegmentation(context);

		Segmentation segmentation = miningModel.getSegmentation();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case SELECT_FIRST:
			case MODEL_CHAIN:
				return dispatchSingleResult(segmentation, segmentResults);
			case SELECT_ALL:
				throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
			default:
				break;
		}

		throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
	}

	private Map<FieldName, ?> dispatchSingleResult(Segmentation segmentation, List<SegmentResult> results){

		if(results.size() < 1 || results.size() > 1){
			throw new MissingResultException(segmentation);
		}

		SegmentResult result = results.get(0);

		return result.getResult();
	}

	static
	private Map<Object, Double> countVotes(Segmentation segmentation, List<SegmentResult> segmentResults){
		VoteCounter<Object> counter = new VoteCounter<Object>();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();

		for(SegmentResult segmentResult : segmentResults){
			Object targetValue = EvaluatorUtil.decode(segmentResult.getTargetValue());

			switch(multipleModelMethod){
				case MAJORITY_VOTE:
					counter.increment(targetValue);
					break;
				case WEIGHTED_MAJORITY_VOTE:
					counter.increment(targetValue, segmentResult.getWeight());
					break;
				default:
					throw new UnsupportedFeatureException(segmentation, multipleModelMethod);
			}
		}

		return counter;
	}

	@SuppressWarnings (
		value = "fallthrough"
	)
	private List<SegmentResult> evaluateSegmentation(ModelEvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResult> results = Lists.newArrayList();

		Segmentation segmentation = miningModel.getSegmentation();

		MultipleModelMethodType multipleModelMethod = segmentation.getMultipleModelMethod();

		Model lastModel = null;

		MiningFunctionType miningFunction = miningModel.getFunctionName();

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

			ModelEvaluator<?> evaluator = MiningModelEvaluator.evaluatorFactory.getModelManager(getPMML(), model);

			ModelEvaluationContext segmentContext = new ModelEvaluationContext(evaluator, context);

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

			switch(multipleModelMethod){
				case SELECT_FIRST:
					return Collections.singletonList(new SegmentResult(segment, targetField, result));
				case MODEL_CHAIN:
					results.clear();
					// Falls through
				default:
					results.add(new SegmentResult(segment, targetField, result));
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

	private static final ModelEvaluatorFactory evaluatorFactory = ModelEvaluatorFactory.getInstance();

	static
	private class SegmentResult {

		private Segment segment = null;

		private FieldName targetField = null;

		private Map<FieldName, ?> result = null;


		public SegmentResult(Segment segment, FieldName targetField, Map<FieldName, ?> result){
			setSegment(segment);
			setTargetField(targetField);
			setResult(result);
		}

		public double getWeight(){
			Segment segment = getSegment();

			return segment.getWeight();
		}

		public Object getTargetValue(){
			Map<FieldName, ?> result = getResult();

			return result.get(getTargetField());
		}

		public Segment getSegment(){
			return this.segment;
		}

		private void setSegment(Segment segment){
			this.segment = segment;
		}

		public FieldName getTargetField(){
			return this.targetField;
		}

		private void setTargetField(FieldName targetField){
			this.targetField = targetField;
		}

		public Map<FieldName, ?> getResult(){
			return this.result;
		}

		private void setResult(Map<FieldName, ?> result){
			this.result = result;
		}
	}
}