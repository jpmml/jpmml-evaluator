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
import java.util.List;

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

public class ModelEvaluatorFactory implements Serializable {

	private ValueFactoryFactory valueFactoryFactory = null;


	protected ModelEvaluatorFactory(){
	}

	public ModelEvaluator<? extends Model> newModelEvaluator(PMML pmml){

		if(!pmml.hasModels()){
			throw new InvalidFeatureException(pmml);
		}

		List<Model> models = pmml.getModels();

		Model model = models.get(0);

		return newModelEvaluator(pmml, model);
	}

	public ModelEvaluator<? extends Model> newModelEvaluator(PMML pmml, Model model){
		ModelEvaluator<?> modelEvaluator = createModelEvaluator(pmml, model);
		modelEvaluator.configure(this);

		return modelEvaluator;
	}

	private ModelEvaluator<? extends Model> createModelEvaluator(PMML pmml, Model model){

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
			return new MiningModelEvaluator(pmml, (MiningModel)model);
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
		} // End if

		if(model instanceof JavaModel){
			return new JavaModelEvaluator(pmml, (JavaModel)model);
		}

		throw new UnsupportedFeatureException(model);
	}

	public ValueFactoryFactory getValueFactoryFactory(){
		return this.valueFactoryFactory;
	}

	public void setValueFactoryFactory(ValueFactoryFactory valueFactoryFactory){
		this.valueFactoryFactory = valueFactoryFactory;
	}

	static
	public ModelEvaluatorFactory newInstance(){
		return new ModelEvaluatorFactory();
	}
}