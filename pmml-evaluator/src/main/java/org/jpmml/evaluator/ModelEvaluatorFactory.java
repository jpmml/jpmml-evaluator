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

import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.dmg.pmml.association.AssociationModel;
import org.dmg.pmml.clustering.ClusteringModel;
import org.dmg.pmml.general_regression.GeneralRegressionModel;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.naive_bayes.NaiveBayesModel;
import org.dmg.pmml.nearest_neighbor.NearestNeighborModel;
import org.dmg.pmml.neural_network.NeuralNetwork;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.rule_set.RuleSetModel;
import org.dmg.pmml.scorecard.Scorecard;
import org.dmg.pmml.support_vector_machine.SupportVectorMachineModel;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.association.AssociationModelEvaluator;
import org.jpmml.evaluator.clustering.ClusteringModelEvaluator;
import org.jpmml.evaluator.general_regression.GeneralRegressionModelEvaluator;
import org.jpmml.evaluator.mining.MiningModelEvaluator;
import org.jpmml.evaluator.naive_bayes.NaiveBayesModelEvaluator;
import org.jpmml.evaluator.nearest_neighbor.NearestNeighborModelEvaluator;
import org.jpmml.evaluator.neural_network.NeuralNetworkEvaluator;
import org.jpmml.evaluator.regression.RegressionModelEvaluator;
import org.jpmml.evaluator.rule_set.RuleSetModelEvaluator;
import org.jpmml.evaluator.scorecard.ScorecardEvaluator;
import org.jpmml.evaluator.support_vector_machine.SupportVectorMachineModelEvaluator;
import org.jpmml.evaluator.tree.TreeModelEvaluator;

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