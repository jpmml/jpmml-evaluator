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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Field;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.Output;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.Target;
import org.dmg.pmml.Targets;
import org.jpmml.model.UnsupportedElementException;
import org.jpmml.model.visitors.FieldResolver;

abstract
public class ModelManager<M extends Model> extends PMMLManager implements HasModel<M> {

	private M model = null;

	private DefaultDataField defaultDataField = null;

	private Map<String, MiningField> miningFields = Collections.emptyMap();

	private Map<String, DerivedField> localDerivedFields = Collections.emptyMap();

	private Map<String, Target> targets = Collections.emptyMap();

	private Map<String, org.dmg.pmml.OutputField> outputFields = Collections.emptyMap();

	private Set<ResultFeature> resultFeatures = Collections.emptySet();

	private List<InputField> inputFields = null;

	private List<InputField> activeInputFields = null;

	private List<TargetField> targetResultFields = null;

	private List<OutputField> outputResultFields = null;

	private ListMultimap<String, Field<?>> visibleFields = null;


	protected ModelManager(){
	}

	protected ModelManager(PMML pmml, M model){
		super(pmml);

		setModel(model);

		@SuppressWarnings("unused")
		MiningFunction miningFunction = model.requireMiningFunction();

		MiningSchema miningSchema = model.requireMiningSchema();
		if(miningSchema.hasMiningFields()){
			this.miningFields = ImmutableMap.copyOf(IndexableUtil.buildMap(miningSchema.getMiningFields()));
		}

		LocalTransformations localTransformations = model.getLocalTransformations();
		if(localTransformations != null && localTransformations.hasDerivedFields()){
			this.localDerivedFields = ImmutableMap.copyOf(IndexableUtil.buildMap(localTransformations.getDerivedFields()));
		}

		Targets targets = model.getTargets();
		if(targets != null && targets.hasTargets()){
			// Cannot use Guava's ImmutableMap, because it is null-hostile
			this.targets = Collections.unmodifiableMap(IndexableUtil.buildMap(targets.getTargets()));
		}

		Output output = model.getOutput();
		if(output != null && output.hasOutputFields()){
			this.outputFields = ImmutableMap.copyOf(IndexableUtil.buildMap(output.getOutputFields()));

			this.resultFeatures = Sets.immutableEnumSet(collectResultFeatures(output));
		}
	}

	public MiningFunction getMiningFunction(){
		M model = getModel();

		return model.requireMiningFunction();
	}

	public MathContext getMathContext(){
		M model = getModel();

		return model.getMathContext();
	}

	@Override
	public DataField getDataField(String name){

		if(Objects.equals(Evaluator.DEFAULT_TARGET_NAME, name)){
			return getDefaultDataField();
		}

		return super.getDataField(name);
	}

	/**
	 * @return A synthetic {@link DataField} element describing the default target field.
	 */
	public DefaultDataField getDefaultDataField(){

		if(this.defaultDataField != null){
			return this.defaultDataField;
		}

		MiningFunction miningFunction = getMiningFunction();
		switch(miningFunction){
			case REGRESSION:
				MathContext mathContext = getMathContext();

				switch(mathContext){
					case FLOAT:
						return DefaultDataField.CONTINUOUS_FLOAT;
					default:
						return DefaultDataField.CONTINUOUS_DOUBLE;
				}
			case CLASSIFICATION:
			case CLUSTERING:
				return DefaultDataField.CATEGORICAL_STRING;
			default:
				return null;
		}
	}

	public void setDefaultDataField(DefaultDataField defaultDataField){
		this.defaultDataField = defaultDataField;
	}

	public MiningField getMiningField(String name){

		if(Objects.equals(Evaluator.DEFAULT_TARGET_NAME, name)){
			return null;
		}

		return this.miningFields.get(name);
	}

	protected boolean hasLocalDerivedFields(){
		return !this.localDerivedFields.isEmpty();
	}

	public DerivedField getLocalDerivedField(String name){
		return this.localDerivedFields.get(name);
	}

	public Target getTarget(String name){
		return this.targets.get(name);
	}

	protected boolean hasOutputFields(){
		return !this.outputFields.isEmpty();
	}

	public org.dmg.pmml.OutputField getOutputField(String name){
		return this.outputFields.get(name);
	}

	/**
	 * <p>
	 * Indicates if this model manager provides the specified result feature.
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
			List<InputField> inputFields = filterInputFields(createInputFields());

			this.inputFields = ImmutableList.copyOf(inputFields);
		}

		return this.inputFields;
	}

	public List<InputField> getActiveFields(){

		if(this.activeInputFields == null){
			List<InputField> activeInputFields = filterInputFields(createInputFields(MiningField.UsageType.ACTIVE));

			this.activeInputFields = ImmutableList.copyOf(activeInputFields);
		}

		return this.activeInputFields;
	}

	public List<TargetField> getTargetFields(){

		if(this.targetResultFields == null){
			List<TargetField> targetResultFields = filterTargetFields(createTargetFields());

			this.targetResultFields = ImmutableList.copyOf(targetResultFields);
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

	public String getTargetName(){
		TargetField targetField = getTargetField();

		return targetField.getFieldName();
	}

	TargetField findTargetField(String name){
		List<TargetField> targetFields = getTargetFields();

		if(targetFields.size() == 1){
			TargetField targetField = targetFields.get(0);

			if(Objects.equals(targetField.getFieldName(), name)){
				return targetField;
			}
		} else

		{
			for(TargetField targetField : targetFields){

				if(Objects.equals(targetField.getFieldName(), name)){
					return targetField;
				}
			}
		}

		return null;
	}

	public List<OutputField> getOutputFields(){

		if(this.outputResultFields == null){
			List<OutputField> outputResultFields = filterOutputFields(createOutputFields());

			this.outputResultFields = ImmutableList.copyOf(outputResultFields);
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

	protected Field<?> resolveField(String name){
		ListMultimap<String, Field<?>> visibleFields = getVisibleFields();

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

	protected ListMultimap<String, Field<?>> getVisibleFields(){

		if(this.visibleFields == null){
			this.visibleFields = collectVisibleFields();
		}

		return this.visibleFields;
	}

	protected EvaluationException createMiningSchemaException(String message){
		M model = getModel();

		MiningSchema miningSchema = model.requireMiningSchema();

		return new EvaluationException(message, miningSchema);
	}

	protected List<InputField> createInputFields(){
		List<InputField> inputFields = getActiveFields();

		List<OutputField> outputFields = getOutputFields();
		if(!outputFields.isEmpty()){
			List<InputField> expandedInputFields = null;

			for(OutputField outputField : outputFields){
				org.dmg.pmml.OutputField pmmlOutputField = outputField.getField();

				if(pmmlOutputField.getResultFeature() != ResultFeature.RESIDUAL){
					continue;
				}

				int depth = outputField.getDepth();
				if(depth > 0){
					throw new UnsupportedElementException(pmmlOutputField);
				}

				String targetFieldName = pmmlOutputField.getTargetField();
				if(targetFieldName == null){
					targetFieldName = getTargetName();
				}

				DataField dataField = getDataField(targetFieldName);
				if(dataField == null){
					throw new MissingFieldException(targetFieldName, pmmlOutputField);
				}

				MiningField miningField = getMiningField(targetFieldName);
				if(miningField == null){
					throw new InvisibleFieldException(targetFieldName, pmmlOutputField);
				}

				ResidualInputField residualInputField = new ResidualInputField(dataField, miningField);

				if(expandedInputFields == null){
					expandedInputFields = new ArrayList<>(inputFields);
				}

				expandedInputFields.add(residualInputField);
			}

			if(expandedInputFields != null){
				return expandedInputFields;
			}
		}

		return inputFields;
	}

	protected List<InputField> createInputFields(MiningField.UsageType usageType){
		M model = getModel();

		List<InputField> inputFields = new ArrayList<>();

		MiningSchema miningSchema = model.requireMiningSchema();
		if(miningSchema.hasMiningFields()){
			List<MiningField> miningFields = miningSchema.getMiningFields();

			for(MiningField miningField : miningFields){
				String fieldName = miningField.requireName();

				if(miningField.getUsageType() != usageType){
					continue;
				}

				Field<?> field = getDataField(fieldName);
				if(field == null){
					field = new VariableField(fieldName);
				}

				InputField inputField = new InputField(field, miningField);

				inputFields.add(inputField);
			}
		}

		return inputFields;
	}

	protected List<InputField> filterInputFields(List<InputField> inputFields){
		return inputFields;
	}

	protected List<TargetField> createTargetFields(){
		M model = getModel();

		List<TargetField> targetFields = new ArrayList<>();

		MiningSchema miningSchema = model.requireMiningSchema();
		if(miningSchema.hasMiningFields()){
			List<MiningField> miningFields = miningSchema.getMiningFields();

			for(MiningField miningField : miningFields){
				String fieldName = miningField.requireName();

				MiningField.UsageType usageType = miningField.getUsageType();
				switch(usageType){
					case PREDICTED:
					case TARGET:
						break;
					default:
						continue;
				}

				DataField dataField = getDataField(fieldName);
				if(dataField == null){
					throw new MissingFieldException(miningField);
				}

				Target target = getTarget(fieldName);

				TargetField targetField = new TargetField(dataField, miningField, target);

				targetFields.add(targetField);
			}
		}

		synthesis:
		if(targetFields.isEmpty()){
			DefaultDataField defaultDataField = getDefaultDataField();

			if(defaultDataField == null){
				break synthesis;
			}

			Target target = getTarget(defaultDataField.requireName());

			TargetField targetField = new SyntheticTargetField(defaultDataField, target);

			targetFields.add(targetField);
		}

		return targetFields;
	}

	protected List<TargetField> filterTargetFields(List<TargetField> targetFields){
		return targetFields;
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

		return outputFields;
	}

	protected List<OutputField> filterOutputFields(List<OutputField> outputFields){
		return outputFields;
	}

	private ListMultimap<String, Field<?>> collectVisibleFields(){
		PMML pmml = getPMML();
		Model model = getModel();

		ListMultimap<String, Field<?>> visibleFields = ArrayListMultimap.create();

		FieldResolver fieldResolver = new FieldResolver(){

			@Override
			public PMMLObject popParent(){
				PMMLObject parent = super.popParent();

				if(Objects.equals(model, parent)){
					Model model = (Model)parent;

					Collection<Field<?>> fields = getFields(model);
					for(Field<?> field : fields){
						visibleFields.put(field.requireName(), field);
					}
				}

				return parent;
			}
		};

		fieldResolver.applyTo(pmml);

		return ImmutableListMultimap.copyOf(visibleFields);
	}

	@Override
	public M getModel(){
		return this.model;
	}

	private void setModel(M model){
		this.model = Objects.requireNonNull(model);
	}

	static
	protected Set<ResultFeature> collectResultFeatures(Output output){
		Set<ResultFeature> result = EnumSet.noneOf(ResultFeature.class);

		if(output != null && output.hasOutputFields()){
			List<org.dmg.pmml.OutputField> pmmlOutputFields = output.getOutputFields();

			for(org.dmg.pmml.OutputField pmmlOutputField : pmmlOutputFields){
				String segmentId = pmmlOutputField.getSegmentId();

				if(segmentId != null){
					continue;
				}

				result.add(pmmlOutputField.getResultFeature());
			}
		}

		return result;
	}

	static
	protected Map<String, Set<ResultFeature>> collectSegmentResultFeatures(Output output){
		Map<String, Set<ResultFeature>> result = new LinkedHashMap<>();

		List<org.dmg.pmml.OutputField> pmmlOutputFields = output.getOutputFields();
		for(org.dmg.pmml.OutputField pmmlOutputField : pmmlOutputFields){
			String segmentId = pmmlOutputField.getSegmentId();

			if(segmentId == null){
				continue;
			}

			Set<ResultFeature> resultFeatures = result.get(segmentId);
			if(resultFeatures == null){
				resultFeatures = EnumSet.noneOf(ResultFeature.class);

				result.put(segmentId, resultFeatures);
			}

			resultFeatures.add(pmmlOutputField.getResultFeature());
		}

		return result;
	}
}