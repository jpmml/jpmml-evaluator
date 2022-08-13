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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import org.dmg.pmml.DataType;
import org.dmg.pmml.EmbeddedModel;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Model;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Output;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.True;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.PMMLAttributes;
import org.dmg.pmml.mining.Segment;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.mining.VariableWeight;
import org.jpmml.evaluator.Configuration;
import org.jpmml.evaluator.DefaultDataField;
import org.jpmml.evaluator.DuplicateFieldValueException;
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.MissingFieldValueException;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.PredicateUtil;
import org.jpmml.evaluator.ProbabilityDistribution;
import org.jpmml.evaluator.Regression;
import org.jpmml.evaluator.SyntheticTargetField;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.evaluator.ValueUtil;
import org.jpmml.model.InvalidAttributeException;
import org.jpmml.model.InvalidElementException;
import org.jpmml.model.PMMLException;
import org.jpmml.model.UnsupportedAttributeException;
import org.jpmml.model.UnsupportedElementException;
import org.jpmml.model.UnsupportedElementListException;
import org.jpmml.model.XPathUtil;

public class MiningModelEvaluator extends ModelEvaluator<MiningModel> implements HasEntityRegistry<Segment> {

	private BiMap<String, Segment> entityRegistry = ImmutableBiMap.of();

	private Map<String, Set<ResultFeature>> segmentResultFeatures = Collections.emptyMap();

	private ConcurrentMap<String, ModelEvaluator<?>> segmentModelEvaluators = new ConcurrentHashMap<>();


	private MiningModelEvaluator(){
	}

	public MiningModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, MiningModel.class));
	}

	public MiningModelEvaluator(PMML pmml, MiningModel miningModel){
		super(pmml, miningModel);

		if(miningModel.hasEmbeddedModels()){
			List<EmbeddedModel> embeddedModels = miningModel.getEmbeddedModels();

			throw new UnsupportedElementListException(embeddedModels);
		}

		Segmentation segmentation = miningModel.requireSegmentation();

		@SuppressWarnings("unused")
		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.requireMultipleModelMethod();

		List<Segment> segments = segmentation.requireSegments();

		this.entityRegistry = ImmutableBiMap.copyOf(EntityUtil.buildBiMap(segments));

		for(Segment segment : segments){
			VariableWeight variableWeight = segment.getVariableWeight();

			if(variableWeight != null){
				throw new UnsupportedElementException(variableWeight);
			}
		}

		LocalTransformations localTransformations = segmentation.getLocalTransformations();
		if(localTransformations != null){
			throw new UnsupportedElementException(localTransformations);
		}

		Output output = miningModel.getOutput();
		if(output != null && output.hasOutputFields()){
			this.segmentResultFeatures = ImmutableMap.copyOf(toImmutableSetMap(collectSegmentResultFeatures(output)));
		}
	}

	protected Set<ResultFeature> getSegmentResultFeatures(String segmentId){
		return this.segmentResultFeatures.get(segmentId);
	}

	@Override
	public void configure(Configuration configuration){
		super.configure(configuration);

		this.segmentModelEvaluators.clear();
	}

	@Override
	public String getSummary(){
		return "Ensemble model";
	}

	@Override
	public DefaultDataField getDefaultDataField(){
		MiningModel miningModel = getModel();

		Segmentation segmentation = miningModel.requireSegmentation();

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.requireMultipleModelMethod();
		switch(multipleModelMethod){
			case SELECT_FIRST:
			case SELECT_ALL:
			case MODEL_CHAIN:
			case MULTI_MODEL_CHAIN:
				return null;
			default:
				return super.getDefaultDataField();
		}
	}

	@Override
	public boolean isPure(){
		return false;
	}

	@Override
	public String getTargetName(){
		List<TargetField> targetFields = super.getTargetFields();

		if(targetFields.isEmpty()){
			return Evaluator.DEFAULT_TARGET_NAME;
		}

		return super.getTargetName();
	}

	@Override
	public BiMap<String, Segment> getEntityRegistry(){
		return this.entityRegistry;
	}

	@Override
	protected List<OutputField> createOutputFields(){
		List<OutputField> outputFields = super.createOutputFields();

		List<OutputField> nestedOutputFields = createNestedOutputFields();
		if(!nestedOutputFields.isEmpty()){
			// Depth-first ordering
			outputFields.addAll(0, nestedOutputFields);
		}

		return outputFields;
	}

	@Override
	protected int getNumberOfVisibleFields(){
		return -1;
	}

	@Override
	public ModelEvaluationContext createEvaluationContext(){
		return new MiningModelEvaluationContext(this);
	}

	@Override
	public Map<String, ?> evaluateInternal(ModelEvaluationContext context){
		return super.evaluateInternal((MiningModelEvaluationContext)context);
	}

	@Override
	protected <V extends Number> Map<String, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResult> segmentResults = evaluateSegmentation((MiningModelEvaluationContext)context);

		// "If no segments have predicates that evaluate to true, then the result is a missing value"
		if(isEmpty(segmentResults)){
			return TargetUtil.evaluateRegressionDefault(valueFactory, getTargetFields());
		}

		Segmentation segmentation = miningModel.requireSegmentation();

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.requireMultipleModelMethod();
		Segmentation.MissingPredictionTreatment missingPredictionTreatment = segmentation.getMissingPredictionTreatment();
		Number missingThreshold = segmentation.getMissingThreshold();
		if(missingThreshold.doubleValue() < 0d || missingThreshold.doubleValue() > 1d){
			throw new InvalidAttributeException(segmentation, PMMLAttributes.SEGMENTATION_MISSINGTHRESHOLD, missingThreshold);
		}

		switch(multipleModelMethod){
			case SELECT_FIRST:
				return selectFirst(segmentResults);
			case SELECT_ALL:
				return selectAll(segmentResults);
			case MODEL_CHAIN:
				return modelChain(segmentResults);
			case MULTI_MODEL_CHAIN:
				return multiModelChain(segmentResults);
			default:
				break;
		}

		TargetField targetField = getTargetField();

		Regression<V> result;

		switch(multipleModelMethod){
			case AVERAGE:
			case WEIGHTED_AVERAGE:
			case MEDIAN:
			case WEIGHTED_MEDIAN:
			case SUM:
			case WEIGHTED_SUM:
				{
					Value<V> value = MiningModelUtil.aggregateValues(valueFactory, multipleModelMethod, missingPredictionTreatment, missingThreshold, segmentResults);
					if(value == null){
						return TargetUtil.evaluateRegressionDefault(valueFactory, targetField);
					}

					value = TargetUtil.evaluateRegressionInternal(targetField, value);

					result = new AggregateScore<V>(value){

						@Override
						public Collection<? extends SegmentResult> getSegmentResults(){
							return segmentResults;
						}
					};
				}
				break;
			case MAJORITY_VOTE:
			case WEIGHTED_MAJORITY_VOTE:
			case MAX:
				throw new InvalidAttributeException(segmentation, multipleModelMethod);
			default:
				throw new UnsupportedAttributeException(segmentation, multipleModelMethod);
		}

		return TargetUtil.evaluateRegression(targetField, result);
	}

	@Override
	protected <V extends Number> Map<String, ?> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResult> segmentResults = evaluateSegmentation((MiningModelEvaluationContext)context);

		// "If no segments have predicates that evaluate to true, then the result is a missing value"
		if(isEmpty(segmentResults)){
			return TargetUtil.evaluateClassificationDefault(valueFactory, getTargetFields());
		}

		Segmentation segmentation = miningModel.requireSegmentation();

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.requireMultipleModelMethod();
		Segmentation.MissingPredictionTreatment missingPredictionTreatment = segmentation.getMissingPredictionTreatment();
		Number missingThreshold = segmentation.getMissingThreshold();
		if(missingThreshold.doubleValue() < 0d || missingThreshold.doubleValue() > 1d){
			throw new InvalidAttributeException(segmentation, PMMLAttributes.SEGMENTATION_MISSINGTHRESHOLD, missingThreshold);
		}

		switch(multipleModelMethod){
			case SELECT_FIRST:
				return selectFirst(segmentResults);
			case SELECT_ALL:
				return selectAll(segmentResults);
			case MODEL_CHAIN:
				return modelChain(segmentResults);
			case MULTI_MODEL_CHAIN:
				return multiModelChain(segmentResults);
			default:
				break;
		}

		TargetField targetField = getTargetField();

		ProbabilityDistribution<V> result;

		switch(multipleModelMethod){
			case MAJORITY_VOTE:
			case WEIGHTED_MAJORITY_VOTE:
				{
					ValueMap<Object, V> values = MiningModelUtil.aggregateVotes(valueFactory, multipleModelMethod, missingPredictionTreatment, missingThreshold, segmentResults);
					if(values == null){
						return TargetUtil.evaluateClassificationDefault(valueFactory, targetField);
					}

					// Convert from votes to probabilities
					ValueUtil.normalizeSimpleMax(values);

					result = new AggregateProbabilityDistribution<V>(values){

						@Override
						public Collection<? extends SegmentResult> getSegmentResults(){
							return segmentResults;
						}
					};
				}
				break;
			case AVERAGE:
			case WEIGHTED_AVERAGE:
			case MEDIAN:
			case MAX:
				{
					List<?> targetCategories = targetField.getCategories();
					if(targetCategories != null && targetCategories.size() < 2){
						throw new InvalidElementException(miningModel);
					}

					ValueMap<Object, V> values = MiningModelUtil.aggregateProbabilities(valueFactory, multipleModelMethod, missingPredictionTreatment, missingThreshold, targetCategories, segmentResults);
					if(values == null){
						return TargetUtil.evaluateClassificationDefault(valueFactory, targetField);
					}

					result = new AggregateProbabilityDistribution<V>(values){

						@Override
						public Collection<? extends SegmentResult> getSegmentResults(){
							return segmentResults;
						}
					};
				}
				break;
			case WEIGHTED_MEDIAN:
			case SUM:
			case WEIGHTED_SUM:
				throw new InvalidAttributeException(segmentation, multipleModelMethod);
			default:
				throw new UnsupportedAttributeException(segmentation, multipleModelMethod);
		}

		return TargetUtil.evaluateClassification(targetField, result);
	}

	@Override
	protected <V extends Number> Map<String, ?> evaluateClustering(ValueFactory<V> valueFactory, EvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResult> segmentResults = evaluateSegmentation((MiningModelEvaluationContext)context);

		if(isEmpty(segmentResults)){
			return TargetUtil.evaluateDefault(getTargetFields());
		}

		Segmentation segmentation = miningModel.requireSegmentation();

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.requireMultipleModelMethod();
		Segmentation.MissingPredictionTreatment missingPredictionTreatment = segmentation.getMissingPredictionTreatment();
		Number missingThreshold = segmentation.getMissingThreshold();
		if(missingThreshold.doubleValue() < 0d || missingThreshold.doubleValue() > 1d){
			throw new InvalidAttributeException(segmentation, PMMLAttributes.SEGMENTATION_MISSINGTHRESHOLD, missingThreshold);
		}

		switch(multipleModelMethod){
			case SELECT_FIRST:
				return selectFirst(segmentResults);
			case SELECT_ALL:
				return selectAll(segmentResults);
			case MODEL_CHAIN:
				return modelChain(segmentResults);
			case MULTI_MODEL_CHAIN:
				return multiModelChain(segmentResults);
			default:
				break;
		}

		AggregateVoteDistribution<V> result;

		switch(multipleModelMethod){
			case MAJORITY_VOTE:
			case WEIGHTED_MAJORITY_VOTE:
				{
					ValueMap<Object, V> values = MiningModelUtil.aggregateVotes(valueFactory, multipleModelMethod, missingPredictionTreatment, missingThreshold, segmentResults);
					if(values == null){
						return Collections.singletonMap(getTargetName(), null);
					}

					result = new AggregateVoteDistribution<V>(values){

						@Override
						public Collection<? extends SegmentResult> getSegmentResults(){
							return segmentResults;
						}
					};
				}
				break;
			case AVERAGE:
			case WEIGHTED_AVERAGE:
			case MEDIAN:
			case WEIGHTED_MEDIAN:
			case MAX:
			case SUM:
			case WEIGHTED_SUM:
				throw new InvalidAttributeException(segmentation, multipleModelMethod);
			case SELECT_FIRST:
			case SELECT_ALL:
			case MODEL_CHAIN:
			case MULTI_MODEL_CHAIN:
				throw new InvalidAttributeException(segmentation, multipleModelMethod);
			default:
				throw new UnsupportedAttributeException(segmentation, multipleModelMethod);
		}

		result.computeResult(DataType.STRING);

		return Collections.singletonMap(getTargetName(), result);
	}

	@Override
	protected <V extends Number> Map<String, ?> evaluateAssociationRules(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateAny(valueFactory, context);
	}

	@Override
	protected <V extends Number> Map<String, ?> evaluateMixed(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateAny(valueFactory, context);
	}

	private <V extends Number> Map<String, ?> evaluateAny(ValueFactory<V> valueFactory, EvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResult> segmentResults = evaluateSegmentation((MiningModelEvaluationContext)context);

		if(isEmpty(segmentResults)){
			return TargetUtil.evaluateDefault(getTargetFields());
		}

		Segmentation segmentation = miningModel.requireSegmentation();

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.requireMultipleModelMethod();
		Segmentation.MissingPredictionTreatment missingPredictionTreatment = segmentation.getMissingPredictionTreatment();

		switch(multipleModelMethod){
			case SELECT_FIRST:
				return selectFirst(segmentResults);
			case SELECT_ALL:
				return selectAll(segmentResults);
			case MODEL_CHAIN:
				return modelChain(segmentResults);
			case MULTI_MODEL_CHAIN:
				return multiModelChain(segmentResults);
			case MAJORITY_VOTE:
			case WEIGHTED_MAJORITY_VOTE:
			case AVERAGE:
			case WEIGHTED_AVERAGE:
			case MEDIAN:
			case WEIGHTED_MEDIAN:
			case MAX:
			case SUM:
			case WEIGHTED_SUM:
				throw new InvalidAttributeException(segmentation, multipleModelMethod);
			default:
				throw new UnsupportedAttributeException(segmentation, multipleModelMethod);
		}
	}

	private List<SegmentResult> evaluateSegmentation(MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();

		BiMap<String, Segment> entityRegistry = getEntityRegistry();

		MiningFunction miningFunction = miningModel.requireMiningFunction();

		Segmentation segmentation = miningModel.requireSegmentation();

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.requireMultipleModelMethod();
		Segmentation.MissingPredictionTreatment missingPredictionTreatment = segmentation.getMissingPredictionTreatment();

		Set<String> resultNames = null;

		Model lastModel = null;

		MiningModelEvaluationContext miningModelContext = null;

		ModelEvaluationContext modelContext = null;

		List<Segment> segments = segmentation.requireSegments();

		List<SegmentResult> segmentResults = new ArrayList<>(segments.size());

		for(int i = 0, max = segments.size(); i < max; i++){
			Segment segment = segments.get(i);

			Boolean status = PredicateUtil.evaluatePredicateContainer(segment, context);
			if(status == null || !status.booleanValue()){
				continue;
			}

			Model model = segment.requireModel();

			// "With the exception of modelChain models, all model elements used inside Segment elements in one MiningModel must have the same MINING-FUNCTION"
			switch(multipleModelMethod){
				case MODEL_CHAIN:
					break;
				case MULTI_MODEL_CHAIN:
					// Falls through
				default:
					checkMiningFunction(model, miningFunction);
					break;
			}

			String segmentId = EntityUtil.getId(segment, entityRegistry);

			ModelEvaluator<?> segmentModelEvaluator = ensureSegmentModelEvaluator(segmentId, model);

			ModelEvaluationContext segmentContext;

			if(segmentModelEvaluator instanceof MiningModelEvaluator){

				if(miningModelContext == null){
					miningModelContext = (MiningModelEvaluationContext)segmentModelEvaluator.createEvaluationContext();
					miningModelContext.setParent(context);
				} else

				{
					miningModelContext.setModelEvaluator(segmentModelEvaluator);
				}

				segmentContext = miningModelContext;
			} else

			{
				if(modelContext == null){
					modelContext = segmentModelEvaluator.createEvaluationContext();
					modelContext.setParent(context);
				} else

				{
					modelContext.setModelEvaluator(segmentModelEvaluator);
				}

				segmentContext = modelContext;
			}

			Map<String, ?> results;

			try {
				results = segmentModelEvaluator.evaluateInternal(segmentContext);
			} catch(PMMLException pe){
				throw pe.ensureContext(segment);
			}

			SegmentResult segmentResult = new SegmentResult(segment, results){

				@Override
				public String getEntityId(){
					return segmentId;
				}

				@Override
				protected ModelEvaluator<?> getModelEvaluator(){
					return segmentModelEvaluator;
				}
			};

			context.putResult(segmentId, segmentResult);

			List<String> segmentWarnings = segmentContext.getWarnings();
			if(!segmentWarnings.isEmpty()){

				for(String segmentWarning : segmentWarnings){
					context.addWarning(segmentWarning);
				}
			}

			boolean skipSegment = false;

			switch(missingPredictionTreatment){
				case RETURN_MISSING:
					{
						boolean hasMissingTargetValues = segmentResult.hasMissingTargetValues();

						if(hasMissingTargetValues){
							return null;
						}
					}
					break;
				case SKIP_SEGMENT:
					{
						switch(multipleModelMethod){
							case SELECT_FIRST:
								skipSegment = segmentResult.hasMissingTargetValues();
								break;
							case SELECT_ALL:
								throw new UnsupportedAttributeException(segmentation, missingPredictionTreatment);
							// "skipSegment should not be used with modelChain combination method"
							case MODEL_CHAIN:
							case MULTI_MODEL_CHAIN:
								throw new InvalidAttributeException(segmentation, missingPredictionTreatment);
							default:
								skipSegment = true;
								break;
						}
					}
					break;
				case CONTINUE:
					break;
				default:
					throw new UnsupportedAttributeException(segmentation, missingPredictionTreatment);
			}

			if(!skipSegment){

				switch(multipleModelMethod){
					case SELECT_FIRST:
						return Collections.singletonList(segmentResult);
					case SELECT_ALL:
						{
							Set<String> names = segmentResult.keySet();

							if(resultNames == null){
								resultNames = new LinkedHashSet<>(names);
							} else

							{
								if(!(names).equals(resultNames)){
									Function<String, String> function = new Function<String, String>(){

										@Override
										public String apply(String name){
											return EvaluationException.formatName(name);
										}
									};

									throw new EvaluationException("Field sets " + Iterables.transform(names, function) + " and " + Iterables.transform(segmentResult.keySet(), function) + " do not match");
								}
							}
						}
						break;
					case MODEL_CHAIN:
						break;
					case MULTI_MODEL_CHAIN:
						{
							Set<String> names = segmentResult.keySet();

							if(resultNames == null){
								resultNames = new LinkedHashSet<>(segments.size() * names.size());
								resultNames.addAll(names);
							} else

							{
								for(String name : names){
									boolean unique = resultNames.add(name);

									if(!unique){
										throw new DuplicateFieldValueException(name);
									}
								}
							}
						}
						break;
					default:
						break;
				} // End switch

				switch(multipleModelMethod){
					case MODEL_CHAIN:
					case MULTI_MODEL_CHAIN:
						{
							Model segmentModel = segmentModelEvaluator.getModel();

							Output segmentOutput = segmentModel.getOutput();
							if(segmentOutput != null && segmentOutput.hasOutputFields()){
								List<org.dmg.pmml.OutputField> pmmlSegmentOutputFields = segmentOutput.getOutputFields();

								for(org.dmg.pmml.OutputField pmmlSegmentOutputField : pmmlSegmentOutputFields){
									String name = pmmlSegmentOutputField.requireName();

									context.putOutputField(name, pmmlSegmentOutputField);

									FieldValue value;

									try {
										value = segmentContext.lookup(name);
									} catch(MissingFieldValueException mfve){
										throw mfve.ensureContext(segment);
									}

									context.declare(name, value);
								}
							}
						}
						break;
					default:
						break;
				}
			}

			boolean clearValues = !segmentModelEvaluator.isPure();

			segmentContext.reset(clearValues);

			switch(multipleModelMethod){
				case MODEL_CHAIN:
					lastModel = model;
					break;
				default:
					break;
			}

			segmentResults.add(segmentResult);
		}

		// "The model element used inside the last Segment element executed must have the same MINING-FUNCTION"
		switch(multipleModelMethod){
			case MODEL_CHAIN:
				if(lastModel != null){
					checkMiningFunction(lastModel, miningFunction);
				}
				break;
			default:
				break;
		}

		return segmentResults;
	}

	private List<Segment> getActiveHead(List<Segment> segments){

		for(int i = 0, max = segments.size(); i < max; i++){
			Segment segment = segments.get(i);

			Predicate predicate = segment.requirePredicate();

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

		Segmentation segmentation = miningModel.requireSegmentation();

		List<Segment> segments = segmentation.requireSegments();

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.requireMultipleModelMethod();
		switch(multipleModelMethod){
			case SELECT_FIRST:
				return createNestedOutputFields(getActiveHead(segments));
			case SELECT_ALL:
				// Ignored
				break;
			case MODEL_CHAIN:
				return createNestedOutputFields(getActiveTail(segments));
			case MULTI_MODEL_CHAIN:
				return createNestedOutputFields(segments);
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

			Model model = segment.requireModel();

			String segmentId = EntityUtil.getId(segment, entityRegistry);

			ModelEvaluator<?> segmentModelEvaluator = ensureSegmentModelEvaluator(segmentId, model);

			List<OutputField> outputFields = segmentModelEvaluator.getOutputFields();
			for(OutputField outputField : outputFields){
				OutputField nestedOutputField = new OutputField(outputField.getField(), outputField.getDepth() + 1);

				result.add(nestedOutputField);
			}
		}

		return result;
	}

	private ModelEvaluator<?> ensureSegmentModelEvaluator(String segmentId, Model model){
		ModelEvaluator<?> segmentModelEvaluator = this.segmentModelEvaluators.get(segmentId);

		if(segmentModelEvaluator == null){
			segmentModelEvaluator = createSegmentModelEvaluator(segmentId, model);

			this.segmentModelEvaluators.putIfAbsent(segmentId, segmentModelEvaluator);
		}

		return segmentModelEvaluator;
	}

	private ModelEvaluator<?> createSegmentModelEvaluator(String segmentId, Model model){
		MiningModel miningModel = getModel();

		MiningFunction miningFunction = miningModel.requireMiningFunction();

		Segmentation segmentation = miningModel.requireSegmentation();

		Set<ResultFeature> extraResultFeatures = EnumSet.noneOf(ResultFeature.class);

		Set<ResultFeature> resultFeatures = getResultFeatures();
		if(!resultFeatures.isEmpty()){
			extraResultFeatures.addAll(resultFeatures);
		}

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.requireMultipleModelMethod();
		switch(multipleModelMethod){
			case AVERAGE:
			case WEIGHTED_AVERAGE:
			case MEDIAN:
			case MAX:
				{
					switch(miningFunction){
						case CLASSIFICATION:
							extraResultFeatures.add(ResultFeature.PROBABILITY);
							break;
						default:
							break;
					}
				}
				break;
			default:
				break;
		}

		Set<ResultFeature> segmentResultFeatures = getSegmentResultFeatures(segmentId);
		if(segmentResultFeatures != null && !segmentResultFeatures.isEmpty()){
			extraResultFeatures.addAll(segmentResultFeatures);
		}

		Configuration configuration = ensureConfiguration();

		ModelEvaluatorFactory modelEvaluatorFactory = configuration.getModelEvaluatorFactory();

		ModelEvaluator<?> modelEvaluator = modelEvaluatorFactory.newModelEvaluator(getPMML(), model, extraResultFeatures);

		MiningFunction segmentMiningFunction = model.requireMiningFunction();

		if((miningFunction == MiningFunction.CLASSIFICATION) && (segmentMiningFunction == MiningFunction.CLASSIFICATION)){
			List<TargetField> targetFields = getTargetFields();
			List<TargetField> segmentTargetFields = modelEvaluator.getTargetFields();

			if(targetFields.size() == 1 && segmentTargetFields.size() == 1){
				TargetField targetField = targetFields.get(0);
				TargetField segmentTargetField = segmentTargetFields.get(0);

				if(segmentTargetField instanceof SyntheticTargetField){
					SyntheticTargetField syntheticTargetField = (SyntheticTargetField)segmentTargetField;

					modelEvaluator.setDefaultDataField(new DefaultDataField(OpType.CATEGORICAL, targetField.getDataType()));
				}
			}
		}

		modelEvaluator.configure(configuration);

		return modelEvaluator;
	}

	private Map<String, ?> selectFirst(List<SegmentResult> segmentResults){
		return segmentResults.get(0);
	}

	private Map<String, ?> selectAll(List<SegmentResult> segmentResults){
		ListMultimap<String, Object> result = ArrayListMultimap.create();

		for(SegmentResult segmentResult : segmentResults){
			Set<String> names = segmentResult.keySet();

			for(String name : names){
				result.put(name, segmentResult.get(name));
			}
		}

		return Multimaps.asMap(result);
	}

	private Map<String, ?> modelChain(List<SegmentResult> segmentResults){
		return segmentResults.get(segmentResults.size() - 1);
	}

	private Map<String, ?> multiModelChain(List<SegmentResult> segmentResults){
		Map<String, Object> result = new LinkedHashMap<>();

		for(SegmentResult segmentResult : segmentResults){
			result.putAll(segmentResult);
		}

		return result;
	}

	static
	private boolean isEmpty(List<SegmentResult> segmentResults){
		return (segmentResults == null) || segmentResults.isEmpty();
	}

	static
	private void checkMiningFunction(Model model, MiningFunction parentMiningFunction){
		MiningFunction miningFunction = model.requireMiningFunction();

		switch(parentMiningFunction){
			case MIXED:
				break;
			default:
				if(miningFunction != parentMiningFunction){
					throw new InvalidAttributeException(InvalidAttributeException.formatMessage(XPathUtil.formatElement(model.getClass()) + "@functionName=" + miningFunction.value()), model);
				}
				break;
		}
	}
}
