/*
 * Copyright (c) 2012 University of Tartu
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

import org.dmg.pmml.AssociationModel;
import org.dmg.pmml.ClusteringModel;
import org.dmg.pmml.GeneralRegressionModel;
import org.dmg.pmml.MiningModel;
import org.dmg.pmml.Model;
import org.dmg.pmml.NaiveBayesModel;
import org.dmg.pmml.NearestNeighborModel;
import org.dmg.pmml.NeuralNetwork;
import org.dmg.pmml.PMML;
import org.dmg.pmml.RegressionModel;
import org.dmg.pmml.RuleSetModel;
import org.dmg.pmml.Scorecard;
import org.dmg.pmml.SupportVectorMachineModel;
import org.dmg.pmml.TreeModel;

public class ModelEvaluatorFactory extends ModelManagerFactory<ModelEvaluator<? extends Model>> {

	protected ModelEvaluatorFactory(){
	}

	@Override
	public ModelEvaluator<? extends Model> newModelManager(PMML pmml, Model model){

		if(model instanceof AssociationModel){
			return new AssociationModelEvaluator(pmml, (AssociationModel)model);
		} else

		if(model instanceof ClusteringModel){
			 return new ClusteringModelEvaluator(pmml, (ClusteringModel)model);
		} else

		if(model instanceof GeneralRegressionModel){
			return new GeneralRegressionModelEvaluator(pmml, (GeneralRegressionModel)model);
		} else

		if(model instanceof MiningModel){
			MiningModelEvaluator evaluator = new MiningModelEvaluator(pmml, (MiningModel)model);
			evaluator.setEvaluatorFactory(this);

			return evaluator;
		} else

		if(model instanceof NaiveBayesModel){
			return new NaiveBayesModelEvaluator(pmml, (NaiveBayesModel)model);
		} else

		if(model instanceof NearestNeighborModel){
			return new NearestNeighborModelEvaluator(pmml, (NearestNeighborModel)model);
		} else

		if(model instanceof NeuralNetwork){
			return new NeuralNetworkEvaluator(pmml, (NeuralNetwork)model);
		} else

		if(model instanceof RegressionModel){
			return new RegressionModelEvaluator(pmml, (RegressionModel)model);
		} else

		if(model instanceof RuleSetModel){
			return new RuleSetModelEvaluator(pmml, (RuleSetModel)model);
		} else

		if(model instanceof Scorecard){
			return new ScorecardEvaluator(pmml, (Scorecard)model);
		} else

		if(model instanceof SupportVectorMachineModel){
			return new SupportVectorMachineModelEvaluator(pmml, (SupportVectorMachineModel)model);
		} else

		if(model instanceof TreeModel){
			return new TreeModelEvaluator(pmml, (TreeModel)model);
		}

		throw new UnsupportedFeatureException(model);
	}

	static
	public ModelEvaluatorFactory newInstance(){
		return new ModelEvaluatorFactory();
	}
}