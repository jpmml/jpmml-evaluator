/*
 * Copyright (c) 2016 Villu Ruusmann
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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.Table;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.ModelVerification;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Output;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLAttributes;
import org.dmg.pmml.PMMLElements;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.Target;
import org.dmg.pmml.Targets;
import org.dmg.pmml.TransformationDictionary;
import org.dmg.pmml.VerificationField;
import org.dmg.pmml.VerificationFields;
import org.jpmml.model.XPathUtil;
import org.jpmml.model.visitors.FieldResolver;

/**
 * @see ModelEvaluatorBuilder
 */
@SuppressWarnings (
	value = {"unused"}
)
abstract
public class ModelEvaluator<M extends Model> implements Evaluator, HasModel<M>, Serializable {

	private PMML pmml = null;

	private M model = null;

	private Configuration configuration = null;

	private InputMapper inputMapper = null;

	private ResultMapper resultMapper = null;

	private ValueFactory<?> valueFactory = null;

	private DataField defaultDataField = null;

	private Map<FieldName, DataField> dataFields = Collections.emptyMap();

	private Map<FieldName, DerivedField> derivedFields = Collections.emptyMap();

	private Map<String, DefineFunction> defineFunctions = Collections.emptyMap();

	private Map<FieldName, MiningField> miningFields = Collections.emptyMap();

	private Map<FieldName, DerivedField> localDerivedFields = Collections.emptyMap();

	private Map<FieldName, Target> targets = Collections.emptyMap();

	private Map<FieldName, org.dmg.pmml.OutputField> outputFields = Collections.emptyMap();

	private Set<ResultFeature> resultFeatures = Collections.emptySet();

	transient
	private Boolean parentCompatible = null;

	transient
	private Boolean pure = null;

	transient
	private List<InputField> inputFields = null;

	transient
	private List<InputField> activeInputFields = null;

	transient
	private List<TargetField> targetResultFields = null;

	transient
	private List<OutputField> outputResultFields = null;

	transient
	private Map<FieldName, Field<?>> resolvedFields = null;


	protected ModelEvaluator(PMML pmml, M model){
		setPMML(Objects.requireNonNull(pmml));
		setModel(Objects.requireNonNull(model));

		DataDictionary dataDictionary = pmml.getDataDictionary();
		if(dataDictionary == null){
			throw new MissingElementException(pmml, PMMLElements.PMML_DATADICTIONARY);
		} // End if

		if(dataDictionary.hasDataFields()){
			this.dataFields = CacheUtil.getValue(dataDictionary, ModelEvaluator.dataFieldCache);
		}

		TransformationDictionary transformationDictionary = pmml.getTransformationDictionary();
		if(transformationDictionary != null && transformationDictionary.hasDerivedFields()){
			this.derivedFields = CacheUtil.getValue(transformationDictionary, ModelEvaluator.derivedFieldCache);
		} // End if

		if(transformationDictionary != null && transformationDictionary.hasDefineFunctions()){
			this.defineFunctions = CacheUtil.getValue(transformationDictionary, ModelEvaluator.defineFunctionCache);
		}

		MiningFunction miningFunction = model.getMiningFunction();
		if(miningFunction == null){
			throw new MissingAttributeException(MissingAttributeException.formatMessage(XPathUtil.formatElement(model.getClass()) + "@functionName"), model);
		}

		MiningSchema miningSchema = model.getMiningSchema();
		if(miningSchema == null){
			throw new MissingElementException(MissingElementException.formatMessage(XPathUtil.formatElement(model.getClass()) + "/" + XPathUtil.formatElement(MiningSchema.class)), model);
		} // End if

		if(miningSchema.hasMiningFields()){
			List<MiningField> miningFields = miningSchema.getMiningFields();

			for(MiningField miningField : miningFields){
				FieldName name = miningField.getName();
				if(name == null){
					throw new MissingAttributeException(miningField, PMMLAttributes.MININGFIELD_NAME);
				}
			}

			this.miningFields = CacheUtil.getValue(miningSchema, ModelEvaluator.miningFieldCache);
		}

		LocalTransformations localTransformations = model.getLocalTransformations();
		if(localTransformations != null && localTransformations.hasDerivedFields()){
			this.localDerivedFields = CacheUtil.getValue(localTransformations, ModelEvaluator.localDerivedFieldCache);
		}

		Targets targets = model.getTargets();
		if(targets != null && targets.hasTargets()){
			this.targets = CacheUtil.getValue(targets, ModelEvaluator.targetCache);
		}

		Output output = model.getOutput();
		if(output != null && output.hasOutputFields()){
			this.outputFields = CacheUtil.getValue(output, ModelEvaluator.outputFieldCache);
			this.resultFeatures = CacheUtil.getValue(output, ModelEvaluator.resultFeaturesCache);
		}
	}

	/**
	 * <p>
	 * Configures the runtime behaviour of this model evaluator.
	 * </p>
	 *
	 * <p>
	 * Must be called once before the first evaluation.
	 * May be called any number of times between subsequent evaluations.
	 * </p>
	 */
	public void configure(Configuration configuration){
		setConfiguration(Objects.requireNonNull(configuration));

		setValueFactory(null);

		resetInputFields();
		resetResultFields();
	}

	@Override
	public MiningFunction getMiningFunction(){
		M model = getModel();

		return model.getMiningFunction();
	}

	public MathContext getMathContext(){
		M model = getModel();

		return model.getMathContext();
	}

	public DataField getDataField(FieldName name){

		if(Objects.equals(Evaluator.DEFAULT_TARGET_NAME, name)){
			return getDefaultDataField();
		}

		return this.dataFields.get(name);
	}

	/**
	 * @return A synthetic {@link DataField} element describing the default target field.
	 */
	public DataField getDefaultDataField(){

		if(this.defaultDataField != null){
			return this.defaultDataField;
		}

		MiningFunction miningFunction = getMiningFunction();
		switch(miningFunction){
			case REGRESSION:
				MathContext mathContext = getMathContext();

				switch(mathContext){
					case FLOAT:
						return ModelEvaluator.DEFAULT_TARGET_CONTINUOUS_FLOAT;
					default:
						return ModelEvaluator.DEFAULT_TARGET_CONTINUOUS_DOUBLE;
				}
			case CLASSIFICATION:
			case CLUSTERING:
				return ModelEvaluator.DEFAULT_TARGET_CATEGORICAL_STRING;
			default:
				return null;
		}
	}

	public void setDefaultDataField(DataField defaultDataField){
		this.defaultDataField = defaultDataField;
	}

	public DerivedField getDerivedField(FieldName name){
		return this.derivedFields.get(name);
	}

	public DefineFunction getDefineFunction(String name){
		return this.defineFunctions.get(name);
	}

	public MiningField getMiningField(FieldName name){

		if(Objects.equals(Evaluator.DEFAULT_TARGET_NAME, name)){
			return null;
		}

		return this.miningFields.get(name);
	}

	public DerivedField getLocalDerivedField(FieldName name){
		return this.localDerivedFields.get(name);
	}

	public Target getTarget(FieldName name){
		return this.targets.get(name);
	}

	public org.dmg.pmml.OutputField getOutputField(FieldName name){
		return this.outputFields.get(name);
	}

	/**
	 * <p>
	 * Indicates if this model evaluator provides the specified result feature.
	 * </p>
	 *
	 * <p>
	 * A result feature is first and foremost manifested through output fields.
	 * However, selected result features may make a secondary manifestation through a target field.
	 * </p>
	 *
	 * @see org.dmg.pmml.OutputField#getResultFeature()
	 */
	public boolean hasResultFeature(ResultFeature resultFeature){
		Set<ResultFeature> resultFeatures = getResultFeatures();

		return resultFeatures.contains(resultFeature);
	}

	public void addResultFeatures(Set<ResultFeature> resultFeatures){
		this.resultFeatures = Sets.immutableEnumSet(Iterables.concat(this.resultFeatures, resultFeatures));
	}

	protected Set<ResultFeature> getResultFeatures(){
		return this.resultFeatures;
	}

	/**
	 * <p>
	 * Indicates if this model evaluator is compatible with its parent model evaluator.
	 * </p>
	 *
	 * <p>
	 * A parent compatible model evaluator inherits {@link DataField} declarations unchanged,
	 * which makes it possible to propagate {@link DataField} and global {@link DerivedField} values between evaluation contexts during evaluation.
	 * </p>
	 */
	public boolean isParentCompatible(){

		if(this.parentCompatible == null){
			this.parentCompatible = assessParentCompatibility();
		}

		return this.parentCompatible;
	}

	/**
	 * <p>
	 * Indicates if this model evaluator represents a pure function.
	 * </p>
	 *
	 * <p>
	 * A pure model evaluator does not tamper with the evaluation context during evaluation.
	 * </p>
	 */
	public boolean isPure(){

		if(this.pure == null){
			this.pure = assessPurity();
		}

		return this.pure;
	}

	@Override
	public List<InputField> getInputFields(){

		if(this.inputFields == null){
			InputMapper inputMapper = getInputMapper();

			this.inputFields = updateNames(createInputFields(), inputMapper);
		}

		return this.inputFields;
	}

	@Override
	public List<InputField> getActiveFields(){

		if(this.activeInputFields == null){
			InputMapper inputMapper = getInputMapper();

			this.activeInputFields = updateNames(createInputFields(MiningField.UsageType.ACTIVE), inputMapper);
		}

		return this.activeInputFields;
	}

	@Override
	public List<TargetField> getTargetFields(){

		if(this.targetResultFields == null){
			ResultMapper resultMapper = getResultMapper();

			this.targetResultFields = updateNames(createTargetFields(), resultMapper);
		}

		return this.targetResultFields;
	}

	public TargetField getTargetField(){
		List<TargetField> targetFields = getTargetFields();

		if(targetFields.size() != 1){
			throw createMiningSchemaException("Expected 1 target field, got " + targetFields.size() + " target fields");
		}

		TargetField targetField = targetFields.get(0);

		return targetField;
	}

	public FieldName getTargetName(){
		TargetField targetField = getTargetField();

		return targetField.getFieldName();
	}

	TargetField findTargetField(FieldName name){
		List<TargetField> targetFields = getTargetFields();

		for(TargetField targetField : targetFields){

			if(Objects.equals(targetField.getFieldName(), name)){
				return targetField;
			}
		}

		return null;
	}

	@Override
	public List<OutputField> getOutputFields(){

		if(this.outputResultFields == null){
			ResultMapper resultMapper = getResultMapper();

			this.outputResultFields = updateNames(createOutputFields(), resultMapper);
		}

		return this.outputResultFields;
	}

	protected EvaluationException createMiningSchemaException(String message){
		M model = getModel();

		MiningSchema miningSchema = model.getMiningSchema();

		return new EvaluationException(message, miningSchema);
	}

	@Override
	public ModelEvaluator<M> verify(){
		M model = getModel();

		ModelVerification modelVerification = model.getModelVerification();
		if(modelVerification == null){
			return this;
		}

		VerificationBatch batch = CacheUtil.getValue(modelVerification, ModelEvaluator.batchCache);

		List<? extends Map<FieldName, ?>> records = batch.getRecords();

		List<InputField> inputFields = getInputFields();

		if(this instanceof HasGroupFields){
			HasGroupFields hasGroupFields = (HasGroupFields)this;

			records = EvaluatorUtil.groupRows(hasGroupFields, records);
		}

		List<TargetField> targetFields = getTargetFields();
		List<OutputField> outputFields = getOutputFields();

		SetView<FieldName> intersection = Sets.intersection(batch.keySet(), new LinkedHashSet<>(Lists.transform(outputFields, OutputField::getFieldName)));

		boolean disjoint = intersection.isEmpty();

		for(Map<FieldName, ?> record : records){
			Map<FieldName, Object> arguments = new LinkedHashMap<>();

			for(InputField inputField : inputFields){
				FieldName name = inputField.getFieldName();

				FieldValue value = inputField.prepare(record.get(name));

				arguments.put(name, value);
			}

			ModelEvaluationContext context = createEvaluationContext();
			context.setArguments(arguments);

			Map<FieldName, ?> results = evaluateInternal(context);

			// "If there exist VerificationField elements that refer to OutputField elements,
			// then any VerificationField element that refers to a MiningField element whose "usageType=target" should be ignored,
			// because they are considered to represent a dependent variable from the training data set, not an expected output"
			if(!disjoint){

				for(OutputField outputField : outputFields){
					FieldName name = outputField.getFieldName();

					VerificationField verificationField = batch.get(name);
					if(verificationField == null){
						continue;
					}

					verify(record.get(name), results.get(name), verificationField.getPrecision(), verificationField.getZeroThreshold());
				}
			} else

			// "If there are no such VerificationField elements,
			// then any VerificationField element that refers to a MiningField element whose "usageType=target" should be considered to represent an expected output"
			{
				for(TargetField targetField : targetFields){
					FieldName name = targetField.getFieldName();

					VerificationField verificationField = batch.get(name);
					if(verificationField == null){
						continue;
					}

					Number precision = verificationField.getPrecision();
					Number zeroThreshold = verificationField.getZeroThreshold();

					verify(record.get(name), EvaluatorUtil.decode(results.get(name)), precision, zeroThreshold);
				}
			}
		}

		return this;
	}

	private void verify(Object expected, Object actual, Number precision, Number zeroThreshold){

		if(expected == null){
			return;
		} // End if

		if(actual instanceof Collection){
			// Ignored
		} else

		{
			DataType dataType = TypeUtil.getDataType(actual);

			expected = TypeUtil.parseOrCast(dataType, expected);
		}

		boolean acceptable = VerificationUtil.acceptable(expected, actual, precision.doubleValue(), zeroThreshold.doubleValue());
		if(!acceptable){
			throw new EvaluationException("Values " + PMMLException.formatValue(expected) + " and " + PMMLException.formatValue(actual) + " do not match");
		}
	}

	public ModelEvaluationContext createEvaluationContext(){
		return new ModelEvaluationContext(this);
	}

	@Override
	public Map<FieldName, ?> evaluate(Map<FieldName, ?> arguments){
		Configuration configuration = ensureConfiguration();

		SymbolTable<FieldName> prevDerivedFieldGuard = null;
		SymbolTable<FieldName> derivedFieldGuard = configuration.getDerivedFieldGuard();

		SymbolTable<String> prevFunctionGuard = null;
		SymbolTable<String> functionGuard = configuration.getFunctionGuard();

		arguments = processArguments(arguments);

		ModelEvaluationContext context = createEvaluationContext();
		context.setArguments(arguments);

		Map<FieldName, ?> results;

		try {
			if(derivedFieldGuard != null){
				prevDerivedFieldGuard = EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.get();

				EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(derivedFieldGuard.fork());
			} // End if

			if(functionGuard != null){
				prevFunctionGuard = EvaluationContext.FUNCTION_GUARD_PROVIDER.get();

				EvaluationContext.FUNCTION_GUARD_PROVIDER.set(functionGuard.fork());
			}

			results = evaluateInternal(context);
		} finally {

			if(derivedFieldGuard != null){
				EvaluationContext.DERIVEDFIELD_GUARD_PROVIDER.set(prevDerivedFieldGuard);
			} // End if

			if(functionGuard != null){
				EvaluationContext.FUNCTION_GUARD_PROVIDER.set(prevFunctionGuard);
			}
		}

		results = processResults(results);

		return results;
	}

	protected Map<FieldName, ?> processArguments(Map<FieldName, ?> arguments){
		InputMapper inputMapper = getInputMapper();

		if(inputMapper != null){
			Map<FieldName, Object> remappedArguments = new AbstractMap<FieldName, Object>(){

				@Override
				public Object get(Object key){
					return arguments.get(inputMapper.apply((FieldName)key));
				}

				@Override
				public Set<Map.Entry<FieldName, Object>> entrySet(){
					throw new UnsupportedOperationException();
				}
			};

			return remappedArguments;
		}

		return arguments;
	}

	protected Map<FieldName, ?> processResults(Map<FieldName, ?> results){
		ResultMapper resultMapper = getResultMapper();

		if(results instanceof OutputMap){
			OutputMap outputMap = (OutputMap)results;

			outputMap.clearPrivate();
		} // End if

		if(resultMapper != null){

			if(results.size() == 0){
				return results;
			} else

			if(results.size() == 1){
				Map.Entry<FieldName, ?> entry = Iterables.getOnlyElement(results.entrySet());

				return Collections.singletonMap(resultMapper.apply(entry.getKey()), entry.getValue());
			}

			Map<FieldName, Object> remappedResults = new LinkedHashMap<>(2 * results.size());

			Collection<? extends Map.Entry<FieldName, ?>> entries = results.entrySet();
			for(Map.Entry<FieldName, ?> entry : entries){
				remappedResults.put(resultMapper.apply(entry.getKey()), entry.getValue());
			}

			return remappedResults;
		}

		return results;
	}

	public Map<FieldName, ?> evaluateInternal(ModelEvaluationContext context){
		M model = getModel();

		if(!model.isScorable()){
			throw new EvaluationException("Model is not scorable", model);
		}

		ValueFactory<?> valueFactory;

		MathContext mathContext = model.getMathContext();
		switch(mathContext){
			case FLOAT:
			case DOUBLE:
				valueFactory = ensureValueFactory();
				break;
			default:
				throw new UnsupportedAttributeException(model, mathContext);
		}

		Map<FieldName, ?> predictions;

		MiningFunction miningFunction = model.getMiningFunction();
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
			case ASSOCIATION_RULES:
				predictions = evaluateAssociationRules(valueFactory, context);
				break;
			case SEQUENCES:
				predictions = evaluateSequences(valueFactory, context);
				break;
			case TIME_SERIES:
				predictions = evaluateTimeSeries(valueFactory, context);
				break;
			case MIXED:
				predictions = evaluateMixed(valueFactory, context);
				break;
			default:
				throw new UnsupportedAttributeException(model, miningFunction);
		}

		return OutputUtil.evaluate(predictions, context);
	}

	protected <V extends Number> Map<FieldName, ?> evaluateRegression(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateDefault();
	}

	protected <V extends Number> Map<FieldName, ?> evaluateClassification(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateDefault();
	}

	protected <V extends Number> Map<FieldName, ?> evaluateClustering(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateDefault();
	}

	protected <V extends Number> Map<FieldName, ?> evaluateAssociationRules(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateDefault();
	}

	protected <V extends Number> Map<FieldName, ?> evaluateSequences(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateDefault();
	}

	protected <V extends Number> Map<FieldName, ?> evaluateTimeSeries(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateDefault();
	}

	protected <V extends Number> Map<FieldName, ?> evaluateMixed(ValueFactory<V> valueFactory, EvaluationContext context){
		return evaluateDefault();
	}

	private <V extends Number> Map<FieldName, ?> evaluateDefault(){
		Model model = getModel();

		MiningFunction miningFunction = model.getMiningFunction();

		throw new InvalidAttributeException(model, miningFunction);
	}

	protected <V extends Number> Classification<Object, V> createClassification(ValueMap<Object, V> values){
		Set<ResultFeature> resultFeatures = getResultFeatures();

		if(resultFeatures.contains(ResultFeature.PROBABILITY) || resultFeatures.contains(ResultFeature.RESIDUAL)){
			return new ProbabilityDistribution<>(values);
		} else

		if(resultFeatures.contains(ResultFeature.CONFIDENCE)){
			return new ConfidenceDistribution<>(values);
		} else

		{
			return new Classification<>(Classification.Type.VOTE, values);
		}
	}

	protected boolean assessParentCompatibility(){
		List<InputField> inputFields = getInputFields();

		for(InputField inputField : inputFields){
			Field<?> field = inputField.getField();
			MiningField miningField = inputField.getMiningField();

			if(!(field instanceof DataField)){
				continue;
			} // End if

			if(!InputFieldUtil.isDefault(field, miningField)){
				return false;
			}
		}

		return true;
	}

	protected boolean assessPurity(){
		List<InputField> inputFields = getInputFields();

		for(InputField inputField : inputFields){
			Field<?> field = inputField.getField();
			MiningField miningField = inputField.getMiningField();

			if(!InputFieldUtil.isDefault(field, miningField)){
				return false;
			}
		}

		return this.localDerivedFields.isEmpty() && this.outputFields.isEmpty();
	}

	protected List<InputField> createInputFields(){
		List<InputField> inputFields = getActiveFields();

		List<OutputField> outputFields = getOutputFields();
		if(outputFields.size() > 0){
			List<ResidualInputField> residualInputFields = null;

			for(OutputField outputField : outputFields){
				org.dmg.pmml.OutputField pmmlOutputField = outputField.getField();

				if(!(ResultFeature.RESIDUAL).equals(pmmlOutputField.getResultFeature())){
					continue;
				}

				int depth = outputField.getDepth();
				if(depth > 0){
					throw new UnsupportedElementException(pmmlOutputField);
				}

				FieldName targetName = pmmlOutputField.getTargetField();
				if(targetName == null){
					targetName = getTargetName();
				}

				DataField dataField = getDataField(targetName);
				if(dataField == null){
					throw new MissingFieldException(targetName, pmmlOutputField);
				}

				MiningField miningField = getMiningField(targetName);
				if(miningField == null){
					throw new InvisibleFieldException(targetName, pmmlOutputField);
				}

				ResidualInputField residualInputField = new ResidualInputField(dataField, miningField);

				if(residualInputFields == null){
					residualInputFields = new ArrayList<>();
				}

				residualInputFields.add(residualInputField);
			}

			if(residualInputFields != null && residualInputFields.size() > 0){
				inputFields = ImmutableList.copyOf(Iterables.concat(inputFields, residualInputFields));
			}
		}

		return inputFields;
	}

	protected List<InputField> createInputFields(MiningField.UsageType usageType){
		M model = getModel();

		MiningSchema miningSchema = model.getMiningSchema();

		List<InputField> inputFields = new ArrayList<>();

		if(miningSchema.hasMiningFields()){
			List<MiningField> miningFields = miningSchema.getMiningFields();

			for(MiningField miningField : miningFields){
				FieldName name = miningField.getName();

				if(!(miningField.getUsageType()).equals(usageType)){
					continue;
				}

				Field<?> field = getDataField(name);
				if(field == null){
					field = new VariableField(name);
				}

				InputField inputField = new InputField(field, miningField);

				inputFields.add(inputField);
			}
		}

		return ImmutableList.copyOf(inputFields);
	}

	protected List<TargetField> createTargetFields(){
		M model = getModel();

		MiningSchema miningSchema = model.getMiningSchema();

		List<TargetField> targetFields = new ArrayList<>();

		if(miningSchema.hasMiningFields()){
			List<MiningField> miningFields = miningSchema.getMiningFields();

			for(MiningField miningField : miningFields){
				FieldName name = miningField.getName();

				MiningField.UsageType usageType = miningField.getUsageType();
				switch(usageType){
					case PREDICTED:
					case TARGET:
						break;
					default:
						continue;
				}

				DataField dataField = getDataField(name);
				if(dataField == null){
					throw new MissingFieldException(name, miningField);
				}

				Target target = getTarget(name);

				TargetField targetField = new TargetField(dataField, miningField, target);

				targetFields.add(targetField);
			}
		}

		synthesis:
		if(targetFields.isEmpty()){
			DataField dataField = getDefaultDataField();

			if(dataField == null){
				break synthesis;
			}

			Target target = getTarget(dataField.getName());

			TargetField targetField = new DefaultTargetField(dataField, target);

			targetFields.add(targetField);
		}

		return ImmutableList.copyOf(targetFields);
	}

	protected List<OutputField> createOutputFields(){
		M model = getModel();

		Output output = model.getOutput();

		List<OutputField> outputFields = new ArrayList<>();

		if(output != null && output.hasOutputFields()){
			Predicate<org.dmg.pmml.OutputField> outputFilter = ensureOutputFilter();

			List<org.dmg.pmml.OutputField> pmmlOutputFields = output.getOutputFields();
			for(org.dmg.pmml.OutputField pmmlOutputField : pmmlOutputFields){

				if(outputFilter.test(pmmlOutputField)){
					OutputField outputField = new OutputField(pmmlOutputField);

					outputFields.add(outputField);
				}
			}
		}

		return ImmutableList.copyOf(outputFields);
	}

	protected Field<?> resolveField(FieldName name){

		if(this.resolvedFields == null){
			this.resolvedFields = resolveFields();
		}

		return this.resolvedFields.get(name);
	}

	private Map<FieldName, Field<?>> resolveFields(){
		Map<FieldName, Field<?>> result = new HashMap<>();

		PMML pmml = getPMML();
		Model model = getModel();

		FieldResolver fieldResolver = new FieldResolver(){

			@Override
			public PMMLObject popParent(){
				PMMLObject parent = super.popParent();

				if(Objects.equals(model, parent)){
					Model model = (Model)parent;

					Collection<Field<?>> fields = getFields(model);
					for(Field<?> field : fields){
						FieldName name = field.getName();

						Field<?> prevField = result.put(name, field);
						if(prevField != null){
							throw new DuplicateFieldException(name);
						}
					}
				}

				return parent;
			}
		};

		fieldResolver.applyTo(pmml);

		return result;
	}

	private void resetInputFields(){
		this.inputFields = null;
		this.activeInputFields = null;
	}

	private void resetResultFields(){
		this.targetResultFields = null;
		this.outputResultFields = null;
	}

	public <V> V getValue(LoadingCache<M, V> cache){
		M model = getModel();

		return CacheUtil.getValue(model, cache);
	}

	public <V> V getValue(Cache<M, V> cache, Callable<? extends V> loader){
		M model = getModel();

		return CacheUtil.getValue(model, cache, loader);
	}

	protected Configuration ensureConfiguration(){
		Configuration configuration = getConfiguration();

		if(this.configuration == null){
			throw new IllegalStateException();
		}

		return this.configuration;
	}

	protected ModelEvaluatorFactory ensureModelEvaluatorFactory(){
		Configuration configuration = ensureConfiguration();

		return configuration.getModelEvaluatorFactory();
	}

	protected ValueFactoryFactory ensureValueFactoryFactory(){
		Configuration configuration = ensureConfiguration();

		return configuration.getValueFactoryFactory();
	}

	protected Predicate<org.dmg.pmml.OutputField> ensureOutputFilter(){
		Configuration configuration = ensureConfiguration();

		return configuration.getOutputFilter();
	}

	protected ValueFactory<?> ensureValueFactory(){
		ValueFactory<?> valueFactory = getValueFactory();

		if(valueFactory == null){
			ValueFactoryFactory valueFactoryFactory = ensureValueFactoryFactory();

			MathContext mathContext = getMathContext();

			valueFactory = valueFactoryFactory.newValueFactory(mathContext);

			setValueFactory(valueFactory);
		}

		return valueFactory;
	}

	@Override
	public PMML getPMML(){
		return this.pmml;
	}

	private void setPMML(PMML pmml){
		this.pmml = pmml;
	}

	@Override
	public M getModel(){
		return this.model;
	}

	private void setModel(M model){
		this.model = model;
	}

	public Configuration getConfiguration(){
		return this.configuration;
	}

	private void setConfiguration(Configuration configuration){
		this.configuration = configuration;
	}

	public InputMapper getInputMapper(){
		return this.inputMapper;
	}

	void setInputMapper(InputMapper inputMapper){
		this.inputMapper = inputMapper;

		resetInputFields();
	}

	public ResultMapper getResultMapper(){
		return this.resultMapper;
	}

	void setResultMapper(ResultMapper resultMapper){
		this.resultMapper = resultMapper;

		resetResultFields();
	}

	private ValueFactory<?> getValueFactory(){
		return this.valueFactory;
	}

	private void setValueFactory(ValueFactory<?> valueFactory){
		this.valueFactory = valueFactory;
	}

	static
	private VerificationBatch parseModelVerification(ModelVerification modelVerification){
		VerificationBatch result = new VerificationBatch();

		VerificationFields verificationFields = modelVerification.getVerificationFields();
		if(verificationFields == null){
			throw new MissingElementException(modelVerification, PMMLElements.MODELVERIFICATION_VERIFICATIONFIELDS);
		}

		for(VerificationField verificationField : verificationFields){
			result.put(verificationField.getField(), verificationField);
		}

		InlineTable inlineTable = modelVerification.getInlineTable();
		if(inlineTable == null){
			throw new MissingElementException(modelVerification, PMMLElements.MODELVERIFICATION_INLINETABLE);
		}

		Table<Integer, String, Object> table = InlineTableUtil.getContent(inlineTable);

		List<Map<FieldName, Object>> records = new ArrayList<>();

		Set<Integer> rowKeys = table.rowKeySet();
		for(Integer rowKey : rowKeys){
			Map<String, Object> row = table.row(rowKey);

			Map<FieldName, Object> record = new LinkedHashMap<>();

			for(VerificationField verificationField : verificationFields){
				FieldName name = verificationField.getField();
				String column = verificationField.getColumn();

				if(column == null){
					column = name.getValue();
				} // End if

				if(!row.containsKey(column)){
					continue;
				}

				record.put(name, row.get(column));
			}

			records.add(record);
		}

		Integer recordCount = modelVerification.getRecordCount();
		if(recordCount != null && recordCount != records.size()){
			throw new InvalidElementException(modelVerification);
		}

		result.setRecords(records);

		return result;
	}

	static
	private <F extends ModelField> List<F> updateNames(List<F> fields, java.util.function.Function<FieldName, FieldName> mapper){

		if(mapper == null){
			return fields;
		}

		for(F field : fields){
			FieldName name = field.getFieldName();

			FieldName mappedName = mapper.apply(name);
			if(mappedName != null && !Objects.equals(mappedName, name)){
				field.setName(mappedName);
			}
		}

		return fields;
	}

	private static final DataField DEFAULT_TARGET_CONTINUOUS_FLOAT = new DataField(Evaluator.DEFAULT_TARGET_NAME, OpType.CONTINUOUS, DataType.FLOAT);
	private static final DataField DEFAULT_TARGET_CONTINUOUS_DOUBLE = new DataField(Evaluator.DEFAULT_TARGET_NAME, OpType.CONTINUOUS, DataType.DOUBLE);
	private static final DataField DEFAULT_TARGET_CATEGORICAL_STRING = new DataField(Evaluator.DEFAULT_TARGET_NAME, OpType.CATEGORICAL, DataType.STRING);

	private static final LoadingCache<DataDictionary, Map<FieldName, DataField>> dataFieldCache = CacheUtil.buildLoadingCache(new CacheLoader<DataDictionary, Map<FieldName, DataField>>(){

		@Override
		public Map<FieldName, DataField> load(DataDictionary dataDictionary){
			return IndexableUtil.buildMap(dataDictionary.getDataFields());
		}
	});

	private static final LoadingCache<TransformationDictionary, Map<FieldName, DerivedField>> derivedFieldCache = CacheUtil.buildLoadingCache(new CacheLoader<TransformationDictionary, Map<FieldName, DerivedField>>(){

		@Override
		public Map<FieldName, DerivedField> load(TransformationDictionary transformationDictionary){
			return IndexableUtil.buildMap(transformationDictionary.getDerivedFields());
		}
	});

	private static final LoadingCache<TransformationDictionary, Map<String, DefineFunction>> defineFunctionCache = CacheUtil.buildLoadingCache(new CacheLoader<TransformationDictionary, Map<String, DefineFunction>>(){

		@Override
		public Map<String, DefineFunction> load(TransformationDictionary transformationDictionary){
			return IndexableUtil.buildMap(transformationDictionary.getDefineFunctions());
		}
	});

	private static final LoadingCache<MiningSchema, Map<FieldName, MiningField>> miningFieldCache = CacheUtil.buildLoadingCache(new CacheLoader<MiningSchema, Map<FieldName, MiningField>>(){

		@Override
		public Map<FieldName, MiningField> load(MiningSchema miningSchema){
			return IndexableUtil.buildMap(miningSchema.getMiningFields());
		}
	});

	private static final LoadingCache<LocalTransformations, Map<FieldName, DerivedField>> localDerivedFieldCache = CacheUtil.buildLoadingCache(new CacheLoader<LocalTransformations, Map<FieldName, DerivedField>>(){

		@Override
		public Map<FieldName, DerivedField> load(LocalTransformations localTransformations){
			return IndexableUtil.buildMap(localTransformations.getDerivedFields());
		}
	});

	private static final LoadingCache<Targets, Map<FieldName, Target>> targetCache = CacheUtil.buildLoadingCache(new CacheLoader<Targets, Map<FieldName, Target>>(){

		@Override
		public Map<FieldName, Target> load(Targets targets){
			return IndexableUtil.buildMap(targets.getTargets(), true);
		}
	});

	private static final LoadingCache<Output, Map<FieldName, org.dmg.pmml.OutputField>> outputFieldCache = CacheUtil.buildLoadingCache(new CacheLoader<Output, Map<FieldName, org.dmg.pmml.OutputField>>(){

		@Override
		public Map<FieldName, org.dmg.pmml.OutputField> load(Output output){
			return IndexableUtil.buildMap(output.getOutputFields());
		}
	});

	private static final LoadingCache<Output, Set<ResultFeature>> resultFeaturesCache = CacheUtil.buildLoadingCache(new CacheLoader<Output, Set<ResultFeature>>(){

		@Override
		public Set<ResultFeature> load(Output output){
			Set<ResultFeature> result = EnumSet.noneOf(ResultFeature.class);

			List<org.dmg.pmml.OutputField> pmmlOutputFields = output.getOutputFields();
			for(org.dmg.pmml.OutputField pmmlOutputField : pmmlOutputFields){
				String segmentId = pmmlOutputField.getSegmentId();

				if(segmentId != null){
					continue;
				}

				result.add(pmmlOutputField.getResultFeature());
			}

			return Sets.immutableEnumSet(result);
		}
	});

	static
	private class VerificationBatch extends LinkedHashMap<FieldName, VerificationField> {

		private List<Map<FieldName, Object>> records = null;


		public List<Map<FieldName, Object>> getRecords(){
			return this.records;
		}

		private void setRecords(List<Map<FieldName, Object>> records){
			this.records = records;
		}
	}

	private static final LoadingCache<ModelVerification, VerificationBatch> batchCache = CacheUtil.buildLoadingCache(new CacheLoader<ModelVerification, VerificationBatch>(){

		@Override
		public VerificationBatch load(ModelVerification modelVerification){
			return parseModelVerification(modelVerification);
		}
	});
}
