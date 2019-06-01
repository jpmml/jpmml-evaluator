/*
 * Copyright (c) 2014 Villu Ruusmann
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

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.BiMap;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.mining.Segment;
import org.jpmml.evaluator.ModelEvaluationContext;

public class MiningModelEvaluationContext extends ModelEvaluationContext {

	private Map<String, SegmentResult> results = null;

	private Map<FieldName, OutputField> outputFields = null;


	public MiningModelEvaluationContext(MiningModelEvaluator miningModelEvaluator){
		super(miningModelEvaluator);

		BiMap<String, Segment> entityRegistry = miningModelEvaluator.getEntityRegistry();

		this.results = new HashMap<>(2 * entityRegistry.size());
	}

	@Override
	public void reset(boolean clearValues){
		super.reset(clearValues);

		if(!this.results.isEmpty()){
			this.results.clear();
		} // End if

		if(this.outputFields != null && !this.outputFields.isEmpty()){
			this.outputFields.clear();
		}
	}

	@Override
	public MiningModelEvaluator getModelEvaluator(){
		return (MiningModelEvaluator)super.getModelEvaluator();
	}

	public SegmentResult getResult(String id){
		return this.results.get(id);
	}

	void putResult(String id, SegmentResult result){
		this.results.put(id, result);
	}

	public OutputField getOutputField(FieldName name){

		if(this.outputFields == null){
			return null;
		}

		return this.outputFields.get(name);
	}

	void putOutputField(FieldName name, OutputField outputField){

		if(this.outputFields == null){
			this.outputFields = new HashMap<>();
		}

		this.outputFields.put(name, outputField);
	}

	public DerivedField getLocalDerivedField(FieldName name){
		MiningModelEvaluator miningModelEvaluator = getModelEvaluator();

		return miningModelEvaluator.getLocalDerivedField(name);
	}
}