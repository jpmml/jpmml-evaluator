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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Function;
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
import org.dmg.pmml.FieldName;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.True;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segment;
import org.dmg.pmml.mining.Segmentation;
import org.jpmml.evaluator.CacheUtil;
import org.jpmml.evaluator.Configuration;
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.InvalidAttributeException;
import org.jpmml.evaluator.InvalidElementException;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.MissingValueException;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.PMMLAttributes;
import org.jpmml.evaluator.PMMLElements;
import org.jpmml.evaluator.PMMLException;
import org.jpmml.evaluator.PMMLUtil;
import org.jpmml.evaluator.PredicateUtil;
import org.jpmml.evaluator.ProbabilityDistribution;
import org.jpmml.evaluator.Regression;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TargetUtil;
import org.jpmml.evaluator.UnsupportedAttributeException;
import org.jpmml.evaluator.UnsupportedElementException;
import org.jpmml.evaluator.Value;
import org.jpmml.evaluator.ValueFactory;
import org.jpmml.evaluator.ValueMap;
import org.jpmml.evaluator.ValueUtil;
import org.jpmml.evaluator.XPathUtil;

public class MiningModelEvaluator extends ModelEvaluator<MiningModel> implements HasEntityRegistry<Segment> {

	private ConcurrentMap<String, ModelEvaluator<?>> segmentModelEvaluators = new ConcurrentHashMap<>();

	transient
	private BiMap<String, Segment> entityRegistry = null;


	public MiningModelEvaluator(PMML pmml){
		this(pmml, PMMLUtil.findModel(pmml, MiningModel.class));
	}

	public MiningModelEvaluator(PMML pmml, MiningModel miningModel){
		super(pmml, miningModel);

		if(miningModel.hasEmbeddedModels()){
			List<EmbeddedModel> embeddedModels = miningModel.getEmbeddedModels();

			EmbeddedModel embeddedModel = Iterables.getFirst(embeddedModels, null);

			throw new UnsupportedElementException(embeddedModel);
		}

		Segmentation segmentation = miningModel.getSegmentation();
		if(segmentation == null){
			throw new MissingElementException(miningModel, PMMLElements.MININGMODEL_SEGMENTATION);
		}

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();
		if(multipleModelMethod == null){
			throw new MissingAttributeException(segmentation, PMMLAttributes.SEGMENTATION_MULTIPLEMODELMETHOD);
		} // End if

		if(!segmentation.hasSegments()){
			throw new MissingElementException(segmentation, PMMLElements.SEGMENTATION_SEGMENTS);
		}

		LocalTransformations localTransformations = segmentation.getLocalTransformations();
		if(localTransformations != null){
			throw new UnsupportedElementException(localTransformations);
		}
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
	protected DataField getDataField(){
		MiningModel miningModel = getModel();

		Segmentation segmentation = miningModel.getSegmentation();

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();
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
	public boolean isPure(){
		return false;
	}

	@Override
	public FieldName getTargetName(){
		List<TargetField> targetFields = super.getTargetFields();

		if(targetFields.size() == 0){
			return Evaluator.DEFAULT_TARGET_NAME;
		}

		return super.getTargetName();
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
	public ModelEvaluationContext createEvaluationContext(){
		return new MiningModelEvaluationContext(this);
	}

	@Override
	public Map<FieldName, ?> evaluateInternal(ModelEvaluationContext context){
		return super.evaluateInternal((MiningModelEvaluationContext)context);
	}

	@Override
	protected <V extends Number> Map<FieldName, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResult> segmentResults = evaluateSegmentation((MiningModelEvaluationContext)context);

		Map<FieldName, ?> predictions = getSegmentationResult(REGRESSION_METHODS, segmentResults);
		if(predictions != null){
			return predictions;
		}

		TargetField targetField = getTargetField();

		Segmentation segmentation = miningModel.getSegmentation();

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();
		Segmentation.MissingPredictionTreatment missingPredictionTreatment = segmentation.getMissingPredictionTreatment();
		Double missingThreshold = segmentation.getMissingThreshold();
		if(missingThreshold < 0 || missingThreshold > 1){
			throw new InvalidAttributeException(segmentation, PMMLAttributes.SEGMENTATION_MISSINGTHRESHOLD, missingThreshold);
		}

		Value<V> value;

		switch(multipleModelMethod){
			case AVERAGE:
			case WEIGHTED_AVERAGE:
			case MEDIAN:
			case WEIGHTED_MEDIAN:
			case SUM:
			case WEIGHTED_SUM:
				value = MiningModelUtil.aggregateValues(valueFactory, multipleModelMethod, missingPredictionTreatment, missingThreshold, segmentResults);
				if(value == null){
					return TargetUtil.evaluateRegressionDefault(valueFactory, targetField);
				}
				break;
			case MAJORITY_VOTE:
			case WEIGHTED_MAJORITY_VOTE:
			case MAX:
			case SELECT_FIRST:
			case SELECT_ALL:
			case MODEL_CHAIN:
				throw new InvalidAttributeException(segmentation, multipleModelMethod);
			default:
				throw new UnsupportedAttributeException(segmentation, multipleModelMethod);
		}

		value = TargetUtil.evaluateRegressionInternal(targetField, value);

		Regression<V> result = new MiningScore<V>(value){

			@Override
			public Collection<? extends SegmentResult> getSegmentResults(){
				return segmentResults;
			}
		};

		return TargetUtil.evaluateRegression(targetField, result);
	}

	@Override
	protected <V extends Number> Map<FieldName, ?> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResult> segmentResults = evaluateSegmentation((MiningModelEvaluationContext)context);

		Map<FieldName, ?> predictions = getSegmentationResult(CLASSIFICATION_METHODS, segmentResults);
		if(predictions != null){
			return predictions;
		}

		TargetField targetField = getTargetField();

		Segmentation segmentation = miningModel.getSegmentation();

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();
		Segmentation.MissingPredictionTreatment missingPredictionTreatment = segmentation.getMissingPredictionTreatment();
		Double missingThreshold = segmentation.getMissingThreshold();
		if(missingThreshold < 0 || missingThreshold > 1){
			throw new InvalidAttributeException(segmentation, PMMLAttributes.SEGMENTATION_MISSINGTHRESHOLD, missingThreshold);
		}

		ProbabilityDistribution<V> result;

		switch(multipleModelMethod){
			case MAJORITY_VOTE:
			case WEIGHTED_MAJORITY_VOTE:
				{
					ValueMap<String, V> values = MiningModelUtil.aggregateVotes(valueFactory, multipleModelMethod, missingPredictionTreatment, missingThreshold, segmentResults);
					if(values == null){
						return TargetUtil.evaluateClassificationDefault(valueFactory, targetField);
					}

					// Convert from votes to probabilities
					ValueUtil.normalizeSimpleMax(values);

					result = new MiningProbabilityDistribution<V>(values){

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
					List<String> targetCategories = targetField.getCategories();
					if(targetCategories != null && targetCategories.size() < 2){
						throw new InvalidElementException(miningModel);
					}

					ValueMap<String, V> values = MiningModelUtil.aggregateProbabilities(valueFactory, multipleModelMethod, missingPredictionTreatment, missingThreshold, targetCategories, segmentResults);
					if(values == null){
						return TargetUtil.evaluateClassificationDefault(valueFactory, targetField);
					}

					result = new MiningProbabilityDistribution<V>(values){

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
			case SELECT_FIRST:
			case SELECT_ALL:
			case MODEL_CHAIN:
				throw new InvalidAttributeException(segmentation, multipleModelMethod);
			default:
				throw new UnsupportedAttributeException(segmentation, multipleModelMethod);
		}

		return TargetUtil.evaluateClassification(targetField, result);
	}

	@Override
	protected <V extends Number> Map<FieldName, ?> evaluateClustering(ValueFactory<V> valueFactory, EvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResult> segmentResults = evaluateSegmentation((MiningModelEvaluationContext)context);

		Map<FieldName, ?> predictions = getSegmentationResult(CLUSTERING_METHODS, segmentResults);
		if(predictions != null){
			return predictions;
		}

		Segmentation segmentation = miningModel.getSegmentation();

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();
		Segmentation.MissingPredictionTreatment missingPredictionTreatment = segmentation.getMissingPredictionTreatment();
		Double missingThreshold = segmentation.getMissingThreshold();
		if(missingThreshold < 0 || missingThreshold > 1){
			throw new InvalidAttributeException(segmentation, PMMLAttributes.SEGMENTATION_MISSINGTHRESHOLD, missingThreshold);
		}

		MiningVoteDistribution<V> result;

		switch(multipleModelMethod){
			case MAJORITY_VOTE:
			case WEIGHTED_MAJORITY_VOTE:
				{
					ValueMap<String, V> values = MiningModelUtil.aggregateVotes(valueFactory, multipleModelMethod, missingPredictionTreatment, missingThreshold, segmentResults);
					if(values == null){
						return Collections.singletonMap(getTargetName(), null);
					}

					result = new MiningVoteDistribution<V>(values){

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
			case SELECT_FIRST:
			case SELECT_ALL:
			case MODEL_CHAIN:
				throw new InvalidAttributeException(segmentation, multipleModelMethod);
			default:
				throw new UnsupportedAttributeException(segmentation, multipleModelMethod);
		}

		result.computeResult(DataType.STRING);

		return Collections.singletonMap(getTargetName(), result);
	}

	@Override
	protected <V extends Number> Map<FieldName, ?> evaluateAssociationRules(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateAny(valueFactory, context);
	}

	@Override
	protected <V extends Number> Map<FieldName, ?> evaluateMixed(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateAny(valueFactory, context);
	}

	private <V extends Number> Map<FieldName, ?> evaluateAny(ValueFactory<V> valueFactory, EvaluationContext context){
		List<SegmentResult> segmentResults = evaluateSegmentation((MiningModelEvaluationContext)context);

		return getSegmentationResult(Collections.emptySet(), segmentResults);
	}

	private List<SegmentResult> evaluateSegmentation(MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();

		BiMap<String, Segment> entityRegistry = getEntityRegistry();

		MiningFunction miningFunction = miningModel.getMiningFunction();

		Segmentation segmentation = miningModel.getSegmentation();

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();

		Model lastModel = null;

		MiningModelEvaluationContext miningModelContext = null;

		ModelEvaluationContext modelContext = null;

		List<Segment> segments = segmentation.getSegments();

		List<SegmentResult> segmentResults = new ArrayList<>(segments.size());

		for(int i = 0, max = segments.size(); i < max; i++){
			Segment segment = segments.get(i);

			Boolean status = PredicateUtil.evaluatePredicateContainer(segment, context);
			if(status == null || !status.booleanValue()){
				continue;
			}

			Model model = segment.getModel();
			if(model == null){
				throw new MissingElementException(MissingElementException.formatMessage(XPathUtil.formatElement(segment.getClass()) + "/<Model>"), segment);
			}

			// "With the exception of modelChain models, all model elements used inside Segment elements in one MiningModel must have the same MINING-FUNCTION"
			switch(multipleModelMethod){
				case MODEL_CHAIN:
					break;
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

			Map<FieldName, ?> results;

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

			switch(multipleModelMethod){
				case MODEL_CHAIN:
					{
						List<OutputField> outputFields = segmentModelEvaluator.getOutputFields();
						for(OutputField outputField : outputFields){
							FieldName name = outputField.getFieldName();

							int depth = outputField.getDepth();
							if(depth > 0){
								continue;
							}

							context.putOutputField(outputField.getField());

							FieldValue value;

							try {
								value = segmentContext.lookup(name);
							} catch(MissingValueException mve){
								throw mve.ensureContext(segment);
							}

							context.declare(name, value);
						}
					}
					break;
				default:
					break;
			}

			List<String> segmentWarnings = segmentContext.getWarnings();
			if(segmentWarnings.size() > 0){

				for(String segmentWarning : segmentWarnings){
					context.addWarning(segmentWarning);
				}
			}

			boolean clearValues = !segmentModelEvaluator.isPure();

			segmentContext.reset(clearValues);

			switch(multipleModelMethod){
				case SELECT_FIRST:
					return Collections.singletonList(segmentResult);
				case MODEL_CHAIN:
					lastModel = model;
					// Falls through
				default:
					segmentResults.add(segmentResult);
					break;
			}
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
					throw new UnsupportedAttributeException(segmentation, multipleModelMethod);
				}
				break;
		}

		// "If no segments have predicates that evaluate to true, then the result is a missing value"
		if(segmentResults.size() == 0){
			return Collections.singletonMap(getTargetName(), null);
		}

		return null;
	}

	private List<Segment> getActiveHead(List<Segment> segments){

		for(int i = 0, max = segments.size(); i < max; i++){
			Segment segment = segments.get(i);

			Predicate predicate = PredicateUtil.ensurePredicate(segment);

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
				throw new MissingElementException(MissingElementException.formatMessage(XPathUtil.formatElement(segment.getClass()) + "/<Model>"), segment);
			}

			String segmentId = EntityUtil.getId(segment, entityRegistry);

			ModelEvaluator<?> segmentModelEvaluator = ensureSegmentModelEvaluator(segmentId, model);

			List<OutputField> outputFields = segmentModelEvaluator.getOutputFields();
			for(OutputField outputField : outputFields){
				OutputField nestedOutputField = new OutputField(outputField.getField(), outputField.getDepth() + 1);

				result.add(nestedOutputField);
			}
		}

		return ImmutableList.copyOf(result);
	}

	private ModelEvaluator<?> ensureSegmentModelEvaluator(String segmentId, Model model){
		ModelEvaluator<?> segmentModelEvaluator = this.segmentModelEvaluators.get(segmentId);

		if(segmentModelEvaluator == null){
			segmentModelEvaluator = createSegmentModelEvaluator(model);

			this.segmentModelEvaluators.putIfAbsent(segmentId, segmentModelEvaluator);
		}

		return segmentModelEvaluator;
	}

	private ModelEvaluator<?> createSegmentModelEvaluator(Model model){
		Configuration configuration = ensureConfiguration();

		ModelEvaluatorFactory modelEvaluatorFactory = configuration.getModelEvaluatorFactory();

		ModelEvaluator<?> modelEvaluator = modelEvaluatorFactory.newModelEvaluator(getPMML(), model);
		modelEvaluator.configure(configuration);

		return modelEvaluator;
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
				Function<Object, String> function = new Function<Object, String>(){

					@Override
					public String apply(Object object){
						return PMMLException.formatKey(object);
					}
				};

				throw new EvaluationException("Field sets " + Iterables.transform(keys, function) + " and " + Iterables.transform(segmentResult.keySet(), function) + " do not match");
			}

			for(FieldName key : keys){
				result.put(key, segmentResult.get(key));
			}
		}

		return result.asMap();
	}

	static
	private void checkMiningFunction(Model model, MiningFunction parentMiningFunction){
		MiningFunction miningFunction = model.getMiningFunction();

		if(miningFunction == null){
			throw new MissingAttributeException(MissingAttributeException.formatMessage(XPathUtil.formatElement(model.getClass()) + "@functionName"), model);
		} // End if

		if(!(miningFunction).equals(parentMiningFunction)){
			throw new InvalidAttributeException(InvalidAttributeException.formatMessage(XPathUtil.formatElement(model.getClass()) + "@functionName=" + miningFunction.value()), model);
		}
	}

	private static final Set<Segmentation.MultipleModelMethod> REGRESSION_METHODS = EnumSet.of(Segmentation.MultipleModelMethod.AVERAGE, Segmentation.MultipleModelMethod.WEIGHTED_AVERAGE, Segmentation.MultipleModelMethod.MEDIAN, Segmentation.MultipleModelMethod.WEIGHTED_MEDIAN, Segmentation.MultipleModelMethod.SUM, Segmentation.MultipleModelMethod.WEIGHTED_SUM);
	private static final Set<Segmentation.MultipleModelMethod> CLASSIFICATION_METHODS = EnumSet.of(Segmentation.MultipleModelMethod.MAJORITY_VOTE, Segmentation.MultipleModelMethod.WEIGHTED_MAJORITY_VOTE, Segmentation.MultipleModelMethod.AVERAGE, Segmentation.MultipleModelMethod.WEIGHTED_AVERAGE, Segmentation.MultipleModelMethod.MEDIAN, Segmentation.MultipleModelMethod.MAX);
	private static final Set<Segmentation.MultipleModelMethod> CLUSTERING_METHODS = EnumSet.of(Segmentation.MultipleModelMethod.MAJORITY_VOTE, Segmentation.MultipleModelMethod.WEIGHTED_MAJORITY_VOTE);

	private static final LoadingCache<MiningModel, BiMap<String, Segment>> entityCache = CacheUtil.buildLoadingCache(new CacheLoader<MiningModel, BiMap<String, Segment>>(){

		@Override
		public BiMap<String, Segment> load(MiningModel miningModel){
			Segmentation segmentation = miningModel.getSegmentation();

			return EntityUtil.buildBiMap(segmentation.getSegments());
		}
	});
}
