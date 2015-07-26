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

import org.dmg.pmml.ActivationFunctionType;
import org.dmg.pmml.Aggregate;
import org.dmg.pmml.BaselineModel;
import org.dmg.pmml.Categories;
import org.dmg.pmml.CenterFields;
import org.dmg.pmml.ClusteringModel;
import org.dmg.pmml.DecisionTree;
import org.dmg.pmml.FeatureType;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.Matrix;
import org.dmg.pmml.MissingValueStrategyType;
import org.dmg.pmml.NeuralLayer;
import org.dmg.pmml.NeuralNetwork;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.Predictor;
import org.dmg.pmml.Regression;
import org.dmg.pmml.Segmentation;
import org.dmg.pmml.SequenceModel;
import org.dmg.pmml.SupportVectorMachineModel;
import org.dmg.pmml.SvmRepresentationType;
import org.dmg.pmml.TableLocator;
import org.dmg.pmml.TextModel;
import org.dmg.pmml.TimeSeriesModel;
import org.dmg.pmml.TreeModel;
import org.dmg.pmml.Visitable;
import org.dmg.pmml.VisitorAction;
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
		ActivationFunctionType activationFunction = neuralNetwork.getActivationFunction();

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
		ActivationFunctionType activationFunction = neuralLayer.getActivationFunction();

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
		FeatureType feature = outputField.getFeature();

		switch(feature){
			case STANDARD_ERROR:
				report(new UnsupportedFeatureException(outputField, feature));
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
		SvmRepresentationType svmRepresentation = supportVectorMachineModel.getSvmRepresentation();

		switch(svmRepresentation){
			case COEFFICIENTS:
				report(new UnsupportedFeatureException(supportVectorMachineModel, svmRepresentation));
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
		MissingValueStrategyType missingValueStrategy = treeModel.getMissingValueStrategy();

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