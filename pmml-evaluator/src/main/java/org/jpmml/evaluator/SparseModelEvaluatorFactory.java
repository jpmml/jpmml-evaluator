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
import java.util.Objects;

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
import org.jpmml.evaluator.java.JavaModel;
import org.jpmml.evaluator.java.JavaModelEvaluator;
import org.jpmml.evaluator.mining.MiningModelEvaluator;
import org.jpmml.evaluator.naive_bayes.NaiveBayesModelEvaluator;
import org.jpmml.evaluator.nearest_neighbor.NearestNeighborModelEvaluator;
import org.jpmml.evaluator.neural_network.NeuralNetworkEvaluator;
import org.jpmml.evaluator.regression.RegressionModelEvaluator;
import org.jpmml.evaluator.regression.SparseRegressionModelEvaluator;
import org.jpmml.evaluator.rule_set.RuleSetModelEvaluator;
import org.jpmml.evaluator.scorecard.ScorecardEvaluator;
import org.jpmml.evaluator.support_vector_machine.SupportVectorMachineModelEvaluator;
import org.jpmml.evaluator.tree.TreeModelEvaluator;

public class SparseModelEvaluatorFactory extends ModelEvaluatorFactory {

	private SparseEvaluatorConfig sparseEvaluatorConfig;

	public SparseModelEvaluatorFactory(SparseEvaluatorConfig sparseEvaluatorConfig) {
		super();
		this.sparseEvaluatorConfig = sparseEvaluatorConfig;
	}

	@Override
	protected ModelEvaluator<?> createModelEvaluator(PMML pmml, Model model){
		Objects.requireNonNull(pmml);
		Objects.requireNonNull(model);

		if(model instanceof AssociationModel){
			throw new UnsupportedOperationException(String.format("No support for sparse %s", model));
		} else

		if(model instanceof ClusteringModel){
			throw new UnsupportedOperationException(String.format("No support for sparse %s", model));
		} else

		if(model instanceof GeneralRegressionModel){
			throw new UnsupportedOperationException(String.format("No support for sparse %s", model));
		} else

		if(model instanceof MiningModel){
			throw new UnsupportedOperationException(String.format("No support for sparse %s", model));
		} else

		if(model instanceof NaiveBayesModel){
			throw new UnsupportedOperationException(String.format("No support for sparse %s", model));
		} else

		if(model instanceof NearestNeighborModel){
			throw new UnsupportedOperationException(String.format("No support for sparse %s", model));
		} else

		if(model instanceof NeuralNetwork){
			throw new UnsupportedOperationException(String.format("No support for sparse %s", model));
		} else

		if(model instanceof RegressionModel){
			return new SparseRegressionModelEvaluator(pmml, (RegressionModel)model, sparseEvaluatorConfig);
		} else

		if(model instanceof RuleSetModel){
			throw new UnsupportedOperationException(String.format("No support for sparse %s", model));
		} else

		if(model instanceof Scorecard){
			throw new UnsupportedOperationException(String.format("No support for sparse %s", model));
		} else

		if(model instanceof SupportVectorMachineModel){
			throw new UnsupportedOperationException(String.format("No support for sparse %s", model));
		} else

		if(model instanceof TreeModel){
			throw new UnsupportedOperationException(String.format("No support for sparse %s", model));
		} // End if

		if(model instanceof JavaModel){
			throw new UnsupportedOperationException(String.format("No support for sparse %s", model));
		}

		throw new UnsupportedElementException(model);
	}

}
