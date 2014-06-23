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
package org.jpmml.manager;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import com.google.common.collect.Lists;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldUsageType;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Target;
import org.dmg.pmml.Targets;

import static com.google.common.base.Preconditions.checkNotNull;

public class ModelManager<M extends Model> extends PMMLManager implements Consumer {

	private M model = null;


	public ModelManager(PMML pmml, M model){
		super(pmml);

		setModel(model);
	}

	public M getModel(){
		return this.model;
	}

	private void setModel(M model){
		this.model = checkNotNull(model);
	}

	@Override
	public String getSummary(){
		return null;
	}

	@Override
	public List<FieldName> getActiveFields(){
		return getMiningFields(EnumSet.of(FieldUsageType.ACTIVE));
	}

	@Override
	public List<FieldName> getGroupFields(){
		return getMiningFields(EnumSet.of(FieldUsageType.GROUP));
	}

	@Override
	public List<FieldName> getTargetFields(){
		return getMiningFields(EnumSet.of(FieldUsageType.PREDICTED, FieldUsageType.TARGET));
	}

	@Override
	public FieldName getTargetField(){
		List<FieldName> targetFields = getTargetFields();

		// "The definition of target fields in the MiningSchema is not required"
		if(targetFields.size() < 1){
			return null;
		} else

		if(targetFields.size() > 1){
			throw new InvalidFeatureException("Too many target fields", getMiningSchema());
		}

		return targetFields.get(0);
	}

	@Override
	public MiningField getMiningField(FieldName name){
		MiningSchema miningSchema = getMiningSchema();

		List<MiningField> miningFields = miningSchema.getMiningFields();

		return find(miningFields, name);
	}

	protected List<FieldName> getMiningFields(EnumSet<FieldUsageType> fieldUsageTypes){
		List<FieldName> result = Lists.newArrayList();

		MiningSchema miningSchema = getMiningSchema();

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
		LocalTransformations localTransformations = getLocalTransformations();
		if(localTransformations == null){
			return null;
		}

		List<DerivedField> derivedFields = localTransformations.getDerivedFields();

		return find(derivedFields, name);
	}

	public DerivedField resolveDerivedField(FieldName name){
		DerivedField derivedField = getLocalDerivedField(name);
		if(derivedField == null){
			return getDerivedField(name);
		}

		return derivedField;
	}

	@Override
	public OutputField getOutputField(FieldName name){
		Output output = getOutput();
		if(output == null){
			return null;
		}

		List<OutputField> outputFields = output.getOutputFields();

		return find(outputFields, name);
	}

	@Override
	public List<FieldName> getOutputFields(){
		Output output = getOutput();
		if(output == null){
			return Collections.emptyList();
		}

		List<FieldName> result = Lists.newArrayList();

		List<OutputField> outputFields = output.getOutputFields();
		for(OutputField outputField : outputFields){
			result.add(outputField.getName());
		}

		return result;
	}

	public Target getTarget(FieldName name){
		Targets targets = getTargets();
		if(targets == null){
			return null;
		}

		for(Target target : targets){

			if((target.getField()).equals(name)){
				return target;
			}
		}

		return null;
	}

	public MiningSchema getMiningSchema(){
		M model = getModel();

		return checkNotNull(model.getMiningSchema());
	}

	public LocalTransformations getLocalTransformations(){
		M model = getModel();

		return model.getLocalTransformations();
	}

	public Output getOutput(){
		M model = getModel();

		return model.getOutput();
	}

	public Targets getTargets(){
		M model = getModel();

		return model.getTargets();
	}
}