/*
 * Copyright (c) 2009 University of Tartu
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jpmml.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import org.dmg.pmml.DerivedField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldUsageType;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Target;
import org.dmg.pmml.Targets;

public class ModelManager<M extends Model> extends PMMLManager implements Consumer {

	private M model = null;


	public ModelManager(PMML pmml, M model){
		super(pmml);

		setModel(Objects.requireNonNull(model));

		MiningSchema miningSchema = model.getMiningSchema();
		if(miningSchema == null){
			throw new InvalidFeatureException(model);
		}
	}

	@Override
	public String getSummary(){
		return null;
	}

	@Override
	public MiningFunctionType getMiningFunction(){
		Model model = getModel();

		return model.getFunctionName();
	}

	@Override
	public List<FieldName> getActiveFields(){
		return getMiningFields(ModelManager.ACTIVE_TYPES);
	}

	@Override
	public List<FieldName> getGroupFields(){
		return getMiningFields(ModelManager.GROUP_TYPES);
	}

	@Override
	public List<FieldName> getOrderFields(){
		return getMiningFields(ModelManager.ORDER_TYPES);
	}

	@Override
	public List<FieldName> getTargetFields(){
		return getMiningFields(ModelManager.TARGET_TYPES);
	}

	@Override
	public FieldName getTargetField(){
		M model = getModel();

		List<FieldName> targetFields = getTargetFields();

		// "The definition of target fields in the MiningSchema is not required"
		if(targetFields.size() < 1){
			return TargetUtil.DEFAULT_NAME;
		} else

		if(targetFields.size() > 1){
			MiningSchema miningSchema = model.getMiningSchema();

			throw new InvalidFeatureException("Too many target fields", miningSchema);
		}

		return targetFields.get(0);
	}

	@Override
	public MiningField getMiningField(FieldName name){
		M model = getModel();

		MiningSchema miningSchema = model.getMiningSchema();

		return IndexableUtil.find(miningSchema.getMiningFields(), name);
	}

	protected List<FieldName> getMiningFields(EnumSet<FieldUsageType> fieldUsageTypes){
		M model = getModel();

		MiningSchema miningSchema = model.getMiningSchema();

		List<FieldName> result = new ArrayList<>();

		List<MiningField> miningFields = miningSchema.getMiningFields();
		for(MiningField miningField : miningFields){
			FieldUsageType fieldUsageType = miningField.getUsageType();

			if(fieldUsageTypes.contains(fieldUsageType)){
				result.add(miningField.getName());
			}
		}

		return result;
	}

	public DerivedField getLocalDerivedField(FieldName name){
		M model = getModel();

		LocalTransformations localTransformations = model.getLocalTransformations();
		if(localTransformations != null && localTransformations.hasDerivedFields()){
			return IndexableUtil.find(localTransformations.getDerivedFields(), name);
		}

		return null;
	}

	@Override
	public Target getTarget(FieldName name){
		M model = getModel();

		Targets targets = model.getTargets();
		if(targets != null){
			return IndexableUtil.find(targets.getTargets(), name);
		}

		return null;
	}

	@Override
	public List<FieldName> getOutputFields(){
		M model = getModel();

		Output output = model.getOutput();
		if(output == null){
			return Collections.emptyList();
		}

		List<FieldName> result = new ArrayList<>();

		List<OutputField> outputFields = output.getOutputFields();
		for(OutputField outputField : outputFields){
			result.add(outputField.getName());
		}

		return result;
	}

	@Override
	public OutputField getOutputField(FieldName name){
		M model = getModel();

		Output output = model.getOutput();
		if(output != null){
			return IndexableUtil.find(output.getOutputFields(), name);
		}

		return null;
	}

	public M getModel(){
		return this.model;
	}

	private void setModel(M model){
		this.model = model;
	}

	protected static final EnumSet<FieldUsageType> ACTIVE_TYPES = EnumSet.of(FieldUsageType.ACTIVE);
	protected static final EnumSet<FieldUsageType> GROUP_TYPES = EnumSet.of(FieldUsageType.GROUP);
	protected static final EnumSet<FieldUsageType> ORDER_TYPES = EnumSet.of(FieldUsageType.ORDER);
	protected static final EnumSet<FieldUsageType> TARGET_TYPES = EnumSet.of(FieldUsageType.PREDICTED, FieldUsageType.TARGET);
}