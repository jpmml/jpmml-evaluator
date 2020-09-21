/*
 * Copyright (c) 2020 Villu Ruusmann
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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
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
import org.jpmml.model.XPathUtil;
import org.jpmml.model.visitors.FieldResolver;

abstract
public class ModelManager<M extends Model> implements HasModel<M>, Serializable {

	private PMML pmml = null;

	private M model = null;

	private DataField defaultDataField = null;

	private Map<FieldName, DataField> dataFields = Collections.emptyMap();

	private Map<FieldName, DerivedField> derivedFields = Collections.emptyMap();

	private Map<String, DefineFunction> defineFunctions = Collections.emptyMap();

	private Map<FieldName, MiningField> miningFields = Collections.emptyMap();

	private Map<FieldName, DerivedField> localDerivedFields = Collections.emptyMap();

	private Map<FieldName, Target> targets = Collections.emptyMap();

	private Map<FieldName, org.dmg.pmml.OutputField> outputFields = Collections.emptyMap();

	private Set<ResultFeature> resultFeatures = Collections.emptySet();

	private List<InputField> inputFields = null;

	private List<InputField> activeInputFields = null;

	private List<TargetField> targetResultFields = null;

	private List<OutputField> outputResultFields = null;

	private ListMultimap<FieldName, Field<?>> visibleFields = null;


	protected ModelManager(){
	}

	protected ModelManager(PMML pmml, M model){
		setPMML(Objects.requireNonNull(pmml));
		setModel(Objects.requireNonNull(model));

		DataDictionary dataDictionary = pmml.getDataDictionary();
		if(dataDictionary == null){
			throw new MissingElementException(pmml, PMMLElements.PMML_DATADICTIONARY);
		} // End if

		if(dataDictionary.hasDataFields()){
			this.dataFields = CacheUtil.getValue(dataDictionary, ModelManager.dataFieldCache);
		}

		TransformationDictionary transformationDictionary = pmml.getTransformationDictionary();
		if(transformationDictionary != null && transformationDictionary.hasDerivedFields()){
			this.derivedFields = CacheUtil.getValue(transformationDictionary, ModelManager.derivedFieldCache);
		} // End if

		if(transformationDictionary != null && transformationDictionary.hasDefineFunctions()){
			this.defineFunctions = CacheUtil.getValue(transformationDictionary, ModelManager.defineFunctionCache);
		}

		MiningFunction miningFunction = model.getMiningFunction();
		if(miningFunction == null){
			throw new MissingAttributeException(MissingAttributeException.formatMessage(XPathUtil.formatElement(model.getClass()) + "@miningFunction"), model);
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

			this.miningFields = CacheUtil.getValue(miningSchema, ModelManager.miningFieldCache);
		}

		LocalTransformations localTransformations = model.getLocalTransformations();
		if(localTransformations != null && localTransformations.hasDerivedFields()){
			this.localDerivedFields = CacheUtil.getValue(localTransformations, ModelManager.localDerivedFieldCache);
		}

		Targets targets = model.getTargets();
		if(targets != null && targets.hasTargets()){
			this.targets = CacheUtil.getValue(targets, ModelManager.targetCache);
		}

		Output output = model.getOutput();
		if(output != null && output.hasOutputFields()){
			this.outputFields = CacheUtil.getValue(output, ModelManager.outputFieldCache);
			this.resultFeatures = CacheUtil.getValue(output, ModelManager.resultFeaturesCache);
		}
	}

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
						return ModelManager.DEFAULT_TARGET_CONTINUOUS_FLOAT;
					default:
						return ModelManager.DEFAULT_TARGET_CONTINUOUS_DOUBLE;
				}
			case CLASSIFICATION:
			case CLUSTERING:
				return ModelManager.DEFAULT_TARGET_CATEGORICAL_STRING;
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

	protected boolean hasLocalDerivedFields(){
		return !this.localDerivedFields.isEmpty();
	}

	public DerivedField getLocalDerivedField(FieldName name){
		return this.localDerivedFields.get(name);
	}

	public Target getTarget(FieldName name){
		return this.targets.get(name);
	}

	protected boolean hasOutputFields(){
		return !this.outputFields.isEmpty();
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

	public List<InputField> getInputFields(){

		if(this.inputFields == null){
			this.inputFields = createInputFields();
		}

		return this.inputFields;
	}

	public List<InputField> getActiveFields(){

		if(this.activeInputFields == null){
			this.activeInputFields = createInputFields(MiningField.UsageType.ACTIVE);
		}

		return this.activeInputFields;
	}

	public List<TargetField> getTargetFields(){

		if(this.targetResultFields == null){
			this.targetResultFields = createTargetFields();
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

	public List<OutputField> getOutputFields(){

		if(this.outputResultFields == null){
			this.outputResultFields = createOutputFields();
		}

		return this.outputResultFields;
	}

	protected void resetInputFields(){
		this.inputFields = null;
		this.activeInputFields = null;
	}

	protected void resetResultFields(){
		this.targetResultFields = null;
		this.outputResultFields = null;
	}

	protected Field<?> resolveField(FieldName name){
		ListMultimap<FieldName, Field<?>> visibleFields = getVisibleFields();

		List<Field<?>> fields = visibleFields.get(name);

		if(fields.isEmpty()){
			return null;
		} else

		if(fields.size() == 1){
			return fields.get(0);
		} else

		{
			throw new DuplicateFieldException(name);
		}
	}

	protected ListMultimap<FieldName, Field<?>> getVisibleFields(){

		if(this.visibleFields == null){
			this.visibleFields = collectVisibleFields();
		}

		return this.visibleFields;
	}

	protected EvaluationException createMiningSchemaException(String message){
		M model = getModel();

		MiningSchema miningSchema = model.getMiningSchema();

		return new EvaluationException(message, miningSchema);
	}

	protected List<InputField> createInputFields(){
		List<InputField> inputFields = getActiveFields();

		List<OutputField> outputFields = getOutputFields();
		if(!outputFields.isEmpty()){
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

			if(residualInputFields != null && !residualInputFields.isEmpty()){
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
			List<org.dmg.pmml.OutputField> pmmlOutputFields = output.getOutputFields();

			for(org.dmg.pmml.OutputField pmmlOutputField : pmmlOutputFields){
				OutputField outputField = new OutputField(pmmlOutputField);

				outputFields.add(outputField);
			}
		}

		return ImmutableList.copyOf(outputFields);
	}

	private ListMultimap<FieldName, Field<?>> collectVisibleFields(){
		PMML pmml = getPMML();
		Model model = getModel();

		ListMultimap<FieldName, Field<?>> visibleFields = ArrayListMultimap.create();

		FieldResolver fieldResolver = new FieldResolver(){

			@Override
			public PMMLObject popParent(){
				PMMLObject parent = super.popParent();

				if(Objects.equals(model, parent)){
					Model model = (Model)parent;

					Collection<Field<?>> fields = getFields(model);
					for(Field<?> field : fields){
						visibleFields.put(field.getName(), field);
					}
				}

				return parent;
			}
		};

		fieldResolver.applyTo(pmml);

		return ImmutableListMultimap.copyOf(visibleFields);
	}

	public <V> V getValue(LoadingCache<M, V> cache){
		M model = getModel();

		return CacheUtil.getValue(model, cache);
	}

	public <V> V getValue(Cache<M, V> cache, Callable<? extends V> loader){
		M model = getModel();

		return CacheUtil.getValue(model, cache, loader);
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

	private static final DataField DEFAULT_TARGET_CONTINUOUS_FLOAT = new DataField(Evaluator.DEFAULT_TARGET_NAME, OpType.CONTINUOUS, DataType.FLOAT);
	private static final DataField DEFAULT_TARGET_CONTINUOUS_DOUBLE = new DataField(Evaluator.DEFAULT_TARGET_NAME, OpType.CONTINUOUS, DataType.DOUBLE);
	private static final DataField DEFAULT_TARGET_CATEGORICAL_STRING = new DataField(Evaluator.DEFAULT_TARGET_NAME, OpType.CATEGORICAL, DataType.STRING);

	private static final LoadingCache<DataDictionary, Map<FieldName, DataField>> dataFieldCache = CacheUtil.buildLoadingCache(new CacheLoader<DataDictionary, Map<FieldName, DataField>>(){

		@Override
		public Map<FieldName, DataField> load(DataDictionary dataDictionary){
			return ImmutableMap.copyOf(IndexableUtil.buildMap(dataDictionary.getDataFields()));
		}
	});

	private static final LoadingCache<TransformationDictionary, Map<FieldName, DerivedField>> derivedFieldCache = CacheUtil.buildLoadingCache(new CacheLoader<TransformationDictionary, Map<FieldName, DerivedField>>(){

		@Override
		public Map<FieldName, DerivedField> load(TransformationDictionary transformationDictionary){
			return ImmutableMap.copyOf(IndexableUtil.buildMap(transformationDictionary.getDerivedFields()));
		}
	});

	private static final LoadingCache<TransformationDictionary, Map<String, DefineFunction>> defineFunctionCache = CacheUtil.buildLoadingCache(new CacheLoader<TransformationDictionary, Map<String, DefineFunction>>(){

		@Override
		public Map<String, DefineFunction> load(TransformationDictionary transformationDictionary){
			return ImmutableMap.copyOf(IndexableUtil.buildMap(transformationDictionary.getDefineFunctions()));
		}
	});

	private static final LoadingCache<MiningSchema, Map<FieldName, MiningField>> miningFieldCache = CacheUtil.buildLoadingCache(new CacheLoader<MiningSchema, Map<FieldName, MiningField>>(){

		@Override
		public Map<FieldName, MiningField> load(MiningSchema miningSchema){
			return ImmutableMap.copyOf(IndexableUtil.buildMap(miningSchema.getMiningFields()));
		}
	});

	private static final LoadingCache<LocalTransformations, Map<FieldName, DerivedField>> localDerivedFieldCache = CacheUtil.buildLoadingCache(new CacheLoader<LocalTransformations, Map<FieldName, DerivedField>>(){

		@Override
		public Map<FieldName, DerivedField> load(LocalTransformations localTransformations){
			return ImmutableMap.copyOf(IndexableUtil.buildMap(localTransformations.getDerivedFields()));
		}
	});

	private static final LoadingCache<Targets, Map<FieldName, Target>> targetCache = CacheUtil.buildLoadingCache(new CacheLoader<Targets, Map<FieldName, Target>>(){

		@Override
		public Map<FieldName, Target> load(Targets targets){
			// Cannot use Guava's ImmutableMap, because it is null-hostile
			return Collections.unmodifiableMap(IndexableUtil.buildMap(targets.getTargets(), true));
		}
	});

	private static final LoadingCache<Output, Map<FieldName, org.dmg.pmml.OutputField>> outputFieldCache = CacheUtil.buildLoadingCache(new CacheLoader<Output, Map<FieldName, org.dmg.pmml.OutputField>>(){

		@Override
		public Map<FieldName, org.dmg.pmml.OutputField> load(Output output){
			return ImmutableMap.copyOf(IndexableUtil.buildMap(output.getOutputFields()));
		}
	});

	static final LoadingCache<Output, Set<ResultFeature>> resultFeaturesCache = CacheUtil.buildLoadingCache(new CacheLoader<Output, Set<ResultFeature>>(){

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
}