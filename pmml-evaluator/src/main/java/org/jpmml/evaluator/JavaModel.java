/*
 * Copyright (c) 2017 Villu Ruusmann
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

import java.util.Map;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.ModelExplanation;
import org.dmg.pmml.ModelStats;
import org.dmg.pmml.ModelVerification;
import org.dmg.pmml.Output;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.Targets;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.jpmml.model.Property;
import org.jpmml.schema.Extension;

@Extension
abstract
public class JavaModel extends Model {

	private String modelName = null;

	private MiningFunction miningFunction = null;

	private String algorithmName = null;

	private Boolean scorable = null;

	private MathContext mathContext = null;

	private MiningSchema miningSchema = null;

	private LocalTransformations localTransformations = null;

	private Targets targets = null;

	private Output output = null;

	private ModelStats modelStats = null;

	private ModelExplanation modelExplanation = null;

	private ModelVerification modelVerification = null;


	abstract
	public Map<FieldName, ?> evaluate(ModelEvaluationContext context);

	@Override
	public String getModelName(){
		return this.modelName;
	}

	@Override
	public JavaModel setModelName(@Property("modelName") String modelName){
		this.modelName = modelName;

		return this;
	}

	@Override
	public MiningFunction getMiningFunction(){
		return this.miningFunction;
	}

	@Override
	public JavaModel setMiningFunction(@Property("miningFunction") MiningFunction miningFunction){
		this.miningFunction = miningFunction;

		return this;
	}

	@Override
	public String getAlgorithmName(){
		return this.algorithmName;
	}

	@Override
	public JavaModel setAlgorithmName(@Property("algorithmName") String algorithmName){
		this.algorithmName = algorithmName;

		return this;
	}

	@Override
	public boolean isScorable(){

		if(this.scorable == null){
			return true;
		}

		return this.scorable;
	}

	@Override
	public JavaModel setScorable(@Property("scorable") Boolean scorable){
		this.scorable = scorable;

		return this;
	}

	@Override
	public MathContext getMathContext(){

		if(this.mathContext == null){
			return MathContext.DOUBLE;
		}

		return this.mathContext;
	}

	@Override
	public JavaModel setMathContext(MathContext mathContext){
		this.mathContext = mathContext;

		return this;
	}

	@Override
	public MiningSchema getMiningSchema(){
		return this.miningSchema;
	}

	@Override
	public JavaModel setMiningSchema(@Property("miningSchema") MiningSchema miningSchema){
		this.miningSchema = miningSchema;

		return this;
	}

	@Override
	public LocalTransformations getLocalTransformations(){
		return this.localTransformations;
	}

	@Override
	public JavaModel setLocalTransformations(@Property("localTransformations") LocalTransformations localTransformations){
		this.localTransformations = localTransformations;

		return this;
	}

	@Override
	public Targets getTargets(){
		return this.targets;
	}

	@Override
	public JavaModel setTargets(@Property("targets") Targets targets){
		this.targets = targets;

		return this;
	}

	@Override
	public Output getOutput(){
		return this.output;
	}

	@Override
	public JavaModel setOutput(@Property("output") Output output){
		this.output = output;

		return this;
	}

	@Override
	public ModelStats getModelStats(){
		return this.modelStats;
	}

	@Override
	public JavaModel setModelStats(@Property("modelStats") ModelStats modelStats){
		this.modelStats = modelStats;

		return this;
	}

	@Override
	public ModelExplanation getModelExplanation(){
		return this.modelExplanation;
	}

	@Override
	public JavaModel setModelExplanation(@Property("modelExplanation") ModelExplanation modelExplanation){
		this.modelExplanation = modelExplanation;

		return this;
	}

	@Override
	public ModelVerification getModelVerification(){
		return this.modelVerification;
	}

	@Override
	public JavaModel setModelVerification(@Property("modelVerification") ModelVerification modelVerification){
		this.modelVerification = modelVerification;

		return this;
	}

	@Override
	public VisitorAction accept(Visitor visitor){
		visitor.pushParent(this);

		VisitorAction status = PMMLObject.traverse(visitor, getMiningSchema(), getLocalTransformations(), getTargets(), getOutput(), getModelStats(), getModelExplanation(), getModelVerification());

		visitor.popParent();

		if(status == VisitorAction.TERMINATE){
			return VisitorAction.TERMINATE;
		}

		return VisitorAction.CONTINUE;
	}
}