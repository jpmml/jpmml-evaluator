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

import java.util.*;

import org.dmg.pmml.*;

import com.google.common.collect.*;

import static com.google.common.base.Preconditions.*;

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
		checkNotNull(model);

		this.model = model;
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

	private List<FieldName> getMiningFields(EnumSet<FieldUsageType> fieldUsageTypes){
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
		LocalTransformations localTransformations = getOrCreateLocalTransformations();

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
		Output output = getOrCreateOutput();

		List<OutputField> outputFields = output.getOutputFields();

		return find(outputFields, name);
	}

	@Override
	public List<FieldName> getOutputFields(){
		List<FieldName> result = Lists.newArrayList();

		Output output = getOrCreateOutput();

		List<OutputField> outputFields = output.getOutputFields();
		for(OutputField outputField : outputFields){
			result.add(outputField.getName());
		}

		return result;
	}

	public Target getTarget(FieldName name){
		Targets targets = getOrCreateTargets();

		for(Target target : targets){

			if((target.getField()).equals(name)){
				return target;
			}
		}

		return null;
	}

	public MiningSchema getMiningSchema(){
		M model = getModel();

		return model.getMiningSchema();
	}

	public LocalTransformations getOrCreateLocalTransformations(){
		M model = getModel();

		LocalTransformations localTransformations = model.getLocalTransformations();
		if(localTransformations == null){
			localTransformations = new LocalTransformations();

			model.setLocalTransformations(localTransformations);
		}

		return localTransformations;
	}

	public Output getOrCreateOutput(){
		M model = getModel();

		Output output = model.getOutput();
		if(output == null){
			output = new Output();

			model.setOutput(output);
		}

		return output;
	}

	public Targets getOrCreateTargets(){
		M model = getModel();

		Targets targets = model.getTargets();
		if(targets == null){
			targets = new Targets();

			model.setTargets(targets);
		}

		return targets;
	}
}