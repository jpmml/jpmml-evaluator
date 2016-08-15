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
package org.jpmml.evaluator.visitors;

import org.dmg.pmml.Aggregate;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.Matrix;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.TableLocator;
import org.dmg.pmml.Visitable;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.baseline.BaselineModel;
import org.dmg.pmml.clustering.CenterFields;
import org.dmg.pmml.clustering.ClusteringModel;
import org.dmg.pmml.general_regression.Categories;
import org.dmg.pmml.general_regression.Predictor;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.neural_network.NeuralLayer;
import org.dmg.pmml.neural_network.NeuralNetwork;
import org.dmg.pmml.regression.Regression;
import org.dmg.pmml.sequence.SequenceModel;
import org.dmg.pmml.support_vector_machine.SupportVectorMachineModel;
import org.dmg.pmml.text.TextModel;
import org.dmg.pmml.time_series.TimeSeriesModel;
import org.dmg.pmml.tree.DecisionTree;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.UnsupportedFeatureException;

/**
 * <p>
 * A Visitor that inspects a class model object for unsupported features.
 * </p>
 *
 * @see FeatureInspector#applyTo(Visitable)
 * @see UnsupportedFeatureException
 */
public class UnsupportedFeatureInspector extends FeatureInspector<UnsupportedFeatureException> {

	@Override
	public VisitorAction visit(Aggregate aggregate){
		Aggregate.Function function = aggregate.getFunction();

		switch(function){
			case MULTISET:
				report(new UnsupportedFeatureException(aggregate, function));
				break;
			default:
				break;
		}

		return super.visit(aggregate);
	}

	@Override
	public VisitorAction visit(BaselineModel baselineModel){
		report(new UnsupportedFeatureException(baselineModel));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(CenterFields centerFields){
		report(new UnsupportedFeatureException(centerFields));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(ClusteringModel clusteringModel){
		ClusteringModel.ModelClass modelClass = clusteringModel.getModelClass();

		switch(modelClass){
			case DISTRIBUTION_BASED:
				report(new UnsupportedFeatureException(clusteringModel, modelClass));
				break;
			default:
				break;
		}

		return super.visit(clusteringModel);
	}

	@Override
	public VisitorAction visit(DecisionTree decisionTree){
		report(new UnsupportedFeatureException(decisionTree));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(NeuralNetwork neuralNetwork){
		NeuralNetwork.ActivationFunction activationFunction = neuralNetwork.getActivationFunction();

		switch(activationFunction){
			case RADIAL_BASIS:
				report(new UnsupportedFeatureException(neuralNetwork, activationFunction));
				break;
			default:
				break;
		}

		return super.visit(neuralNetwork);
	}

	@Override
	public VisitorAction visit(NeuralLayer neuralLayer){
		NeuralNetwork.ActivationFunction activationFunction = neuralLayer.getActivationFunction();

		if(activationFunction != null){

			switch(activationFunction){
				case RADIAL_BASIS:
					report(new UnsupportedFeatureException(neuralLayer, activationFunction));
					break;
				default:
					break;
			}
		}

		return super.visit(neuralLayer);
	}

	@Override
	public VisitorAction visit(NormDiscrete normDiscrete){
		NormDiscrete.Method method = normDiscrete.getMethod();

		switch(method){
			case THERMOMETER:
				report(new UnsupportedFeatureException(normDiscrete, method));
				break;
			default:
				break;
		}

		return super.visit(normDiscrete);
	}

	@Override
	public VisitorAction visit(OutputField outputField){
		ResultFeature resultFeature = outputField.getResultFeature();

		switch(resultFeature){
			case STANDARD_ERROR:
				report(new UnsupportedFeatureException(outputField, resultFeature));
				break;
			default:
				break;
		}

		return super.visit(outputField);
	}

	@Override
	public VisitorAction visit(Predictor predictor){
		Matrix matrix = predictor.getMatrix();

		if(matrix != null){
			Categories categories = predictor.getCategories();

			if(categories == null){
				report(new UnsupportedFeatureException(predictor));
			}
		}

		return super.visit(predictor);
	}

	@Override
	public VisitorAction visit(Regression regression){
		report(new UnsupportedFeatureException(regression));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(Segmentation segmentation){
		LocalTransformations localTransformations = segmentation.getLocalTransformations();

		if(localTransformations != null){
			report(new UnsupportedFeatureException(localTransformations));
		}

		return super.visit(segmentation);
	}

	@Override
	public VisitorAction visit(SequenceModel sequenceModel){
		report(new UnsupportedFeatureException(sequenceModel));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(SupportVectorMachineModel supportVectorMachineModel){
		SupportVectorMachineModel.Representation representation = supportVectorMachineModel.getRepresentation();

		switch(representation){
			case COEFFICIENTS:
				report(new UnsupportedFeatureException(supportVectorMachineModel, representation));
				break;
			default:
				break;
		}

		return super.visit(supportVectorMachineModel);
	}

	@Override
	public VisitorAction visit(TableLocator tableLocator){
		report(new UnsupportedFeatureException(tableLocator));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(TextModel textModel){
		report(new UnsupportedFeatureException(textModel));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(TimeSeriesModel timeSeriesModel){
		report(new UnsupportedFeatureException(timeSeriesModel));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(TreeModel treeModel){
		TreeModel.MissingValueStrategy missingValueStrategy = treeModel.getMissingValueStrategy();

		switch(missingValueStrategy){
			case AGGREGATE_NODES:
			case WEIGHTED_CONFIDENCE:
				report(new UnsupportedFeatureException(treeModel, missingValueStrategy));
				break;
			default:
				break;
		}

		return super.visit(treeModel);
	}
}