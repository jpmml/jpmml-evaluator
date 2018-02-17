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
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.MathContext;
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
import org.jpmml.evaluator.EntityUtil;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.HasEntityRegistry;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.InvalidAttributeException;
import org.jpmml.evaluator.MiningFieldUtil;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.MissingElementException;
import org.jpmml.evaluator.MissingValueException;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.PMMLAttributes;
import org.jpmml.evaluator.PMMLElements;
import org.jpmml.evaluator.PMMLException;
import org.jpmml.evaluator.PredicateUtil;
import org.jpmml.evaluator.ProbabilityDistribution;
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

	private ConcurrentMap<String, SegmentHandler> segmentHandlers = new ConcurrentHashMap<>();

	transient
	private BiMap<String, Segment> entityRegistry = null;


	public MiningModelEvaluator(PMML pmml){
		this(pmml, selectModel(pmml, MiningModel.class));
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
	public void configure(ModelEvaluatorFactory modelEvaluatorFactory){
		super.configure(modelEvaluatorFactory);

		this.segmentHandlers.clear();
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
	public boolean isPrimitive(){
		return false;
	}

	@Override
	public FieldName getTargetFieldName(){
		List<TargetField> targetFields = super.getTargetFields();

		if(targetFields.size() == 0){
			return Evaluator.DEFAULT_TARGET_NAME;
		}

		return super.getTargetFieldName();
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
		MiningModel miningModel = ensureScorableModel();

		ValueFactory<?> valueFactory;

		MathContext mathContext = miningModel.getMathContext();
		switch(mathContext){
			case FLOAT:
			case DOUBLE:
				valueFactory = ensureValueFactory();
				break;
			default:
				throw new UnsupportedAttributeException(miningModel, mathContext);
		}

		Map<FieldName, ?> predictions;

		MiningFunction miningFunction = miningModel.getMiningFunction();
		switch(miningFunction){
			case REGRESSION:
				predictions = evaluateRegression(valueFactory, context);
				break;
			case CLASSIFICATION:
				predictions = evaluateClassification(valueFactory, context);
				break;
			case CLUSTERING:
				predictions = evaluateClustering(valueFactory, context);
				break;
			default:
				predictions = evaluateAny(context);
				break;
		}

		return OutputUtil.evaluate(predictions, context);
	}

	private <V extends Number> Map<FieldName, ?> evaluateRegression(ValueFactory<V> valueFactory, MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResult> segmentResults = evaluateSegmentation(context);

		Map<FieldName, ?> predictions = getSegmentationResult(REGRESSION_METHODS, segmentResults);
		if(predictions != null){
			return predictions;
		}

		Segmentation segmentation = miningModel.getSegmentation();

		Value<V> result;

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case AVERAGE:
			case WEIGHTED_AVERAGE:
			case MEDIAN:
			case WEIGHTED_MEDIAN:
			case SUM:
			case WEIGHTED_SUM:
				result = MiningModelUtil.aggregateValues(valueFactory, multipleModelMethod, segmentResults);
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

		return TargetUtil.evaluateRegression(getTargetField(), result);
	}

	private <V extends Number> Map<FieldName, ?> evaluateClassification(ValueFactory<V> valueFactory, MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResult> segmentResults = evaluateSegmentation(context);

		Map<FieldName, ?> predictions = getSegmentationResult(CLASSIFICATION_METHODS, segmentResults);
		if(predictions != null){
			return predictions;
		}

		TargetField targetField = getTargetField();

		Segmentation segmentation = miningModel.getSegmentation();

		ProbabilityDistribution<V> result;

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case MAJORITY_VOTE:
			case WEIGHTED_MAJORITY_VOTE:
				{
					ValueMap<String, V> values = MiningModelUtil.aggregateVotes(valueFactory, multipleModelMethod, segmentResults);

					// Convert from votes to probabilities
					ValueUtil.normalizeSimpleMax(values);

					result = new ProbabilityDistribution<>(values);
				}
				break;
			case AVERAGE:
			case WEIGHTED_AVERAGE:
			case MEDIAN:
			case MAX:
				{
					List<String> categories = targetField.getCategories();

					ValueMap<String, V> values = MiningModelUtil.aggregateProbabilities(valueFactory, multipleModelMethod, categories, segmentResults);

					result = new ProbabilityDistribution<>(values);
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

	private <V extends Number> Map<FieldName, ?> evaluateClustering(ValueFactory<V> valueFactory, MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();

		List<SegmentResult> segmentResults = evaluateSegmentation(context);

		Map<FieldName, ?> predictions = getSegmentationResult(CLUSTERING_METHODS, segmentResults);
		if(predictions != null){
			return predictions;
		}

		Segmentation segmentation = miningModel.getSegmentation();

		VoteDistribution<V> result;

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();
		switch(multipleModelMethod){
			case MAJORITY_VOTE:
			case WEIGHTED_MAJORITY_VOTE:
				{
					ValueMap<String, V> values = MiningModelUtil.aggregateVotes(valueFactory, multipleModelMethod, segmentResults);

					result = new VoteDistribution<>(values);
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

		return Collections.singletonMap(getTargetFieldName(), result);
	}

	private Map<FieldName, ?> evaluateAny(MiningModelEvaluationContext context){
		List<SegmentResult> segmentResults = evaluateSegmentation(context);

		return getSegmentationResult(Collections.<Segmentation.MultipleModelMethod>emptySet(), segmentResults);
	}

	private List<SegmentResult> evaluateSegmentation(MiningModelEvaluationContext context){
		MiningModel miningModel = getModel();

		BiMap<String, Segment> entityRegistry = getEntityRegistry();

		MiningFunction miningFunction = miningModel.getMiningFunction();

		Segmentation segmentation = miningModel.getSegmentation();

		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();

		Model lastModel = null;

		boolean purge = false;

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
					lastModel = model;
					break;
				default:
					checkMiningFunction(model, miningFunction);
					break;
			}

			String segmentId = EntityUtil.getId(segment, entityRegistry);

			SegmentHandler segmentHandler = this.segmentHandlers.get(segmentId);
			if(segmentHandler == null){
				segmentHandler = createSegmentHandler(model);

				this.segmentHandlers.putIfAbsent(segmentId, segmentHandler);
			}

			ModelEvaluator<?> segmentModelEvaluator = segmentHandler.getModelEvaluator();

			purge |= !(segmentHandler.isCompatible() && segmentModelEvaluator.isPrimitive());

			ModelEvaluationContext segmentContext;

			if(segmentModelEvaluator instanceof MiningModelEvaluator){
				MiningModelEvaluator segmentMiningModelEvaluator = (MiningModelEvaluator)segmentModelEvaluator;

				if(miningModelContext == null){
					miningModelContext = new MiningModelEvaluationContext(context, segmentMiningModelEvaluator);
				} else

				{
					miningModelContext.reset(segmentMiningModelEvaluator, purge);
				}

				segmentContext = miningModelContext;
			} else

			{
				if(modelContext == null){
					modelContext = new ModelEvaluationContext(context, segmentModelEvaluator);
				} else

				{
					modelContext.reset(segmentModelEvaluator, purge);
				}

				segmentContext = modelContext;
			}

			segmentContext.setCompatible(segmentHandler.isCompatible());

			SegmentResult segmentResult;

			try {
				Map<FieldName, ?> result = segmentModelEvaluator.evaluate(segmentContext);

				FieldName segmentTargetFieldName = segmentModelEvaluator.getTargetFieldName();

				segmentResult = new SegmentResult(segment, segmentId, result, segmentTargetFieldName);
			} catch(PMMLException pe){
				throw pe.ensureContext(segment);
			}

			context.putResult(segmentId, segmentResult);

			switch(multipleModelMethod){
				case MODEL_CHAIN:
					{
						List<OutputField> outputFields = segmentModelEvaluator.getOutputFields();
						for(OutputField outputField : outputFields){
							FieldName name = outputField.getName();

							int depth = outputField.getDepth();
							if(depth > 0){
								continue;
							}

							context.putOutputField(outputField.getOutputField());

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
			return Collections.singletonMap(getTargetFieldName(), null);
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

			SegmentHandler segmentHandler = this.segmentHandlers.get(segmentId);
			if(segmentHandler == null){
				segmentHandler = createSegmentHandler(model);

				this.segmentHandlers.putIfAbsent(segmentId, segmentHandler);
			}

			ModelEvaluator<?> modelEvaluator = segmentHandler.getModelEvaluator();

			List<OutputField> outputFields = modelEvaluator.getOutputFields();
			for(OutputField outputField : outputFields){
				OutputField nestedOutputField = new OutputField(outputField);

				result.add(nestedOutputField);
			}
		}

		return ImmutableList.copyOf(result);
	}

	private SegmentHandler createSegmentHandler(Model model){
		ModelEvaluatorFactory modelEvaluatorFactory = ensureModelEvaluatorFactory();

		ModelEvaluator<?> modelEvaluator = modelEvaluatorFactory.newModelEvaluator(getPMML(), model);

		return new SegmentHandler(modelEvaluator);
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
	private void checkMiningFunction(Model model, MiningFunction miningFunction){
		MiningFunction modelMiningFunction = model.getMiningFunction();

		if(!(miningFunction).equals(modelMiningFunction)){
			throw new InvalidAttributeException(InvalidAttributeException.formatMessage(XPathUtil.formatElement(model.getClass()) + "@miningFunction=" + modelMiningFunction.value()), model);
		}
	}

	static
	private class SegmentHandler implements Serializable {

		private ModelEvaluator<?> modelEvaluator = null;

		private boolean compatible = false;


		private SegmentHandler(ModelEvaluator<?> modelEvaluator){
			setModelEvaluator(modelEvaluator);

			boolean compatible = true;

			List<InputField> inputFields = modelEvaluator.getInputFields();
			for(InputField inputField : inputFields){
				Field<?> field = inputField.getField();

				if(field instanceof DataField){
					MiningField miningField = inputField.getMiningField();

					compatible &= MiningFieldUtil.isDefault(miningField);
				}
			}

			setCompatible(compatible);
		}

		public ModelEvaluator<?> getModelEvaluator(){
			return this.modelEvaluator;
		}

		private void setModelEvaluator(ModelEvaluator<?> modelEvaluator){
			this.modelEvaluator = modelEvaluator;
		}

		public boolean isCompatible(){
			return this.compatible;
		}

		private void setCompatible(boolean compatible){
			this.compatible = compatible;
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
