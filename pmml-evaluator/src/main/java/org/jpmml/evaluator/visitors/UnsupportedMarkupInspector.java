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
import org.dmg.pmml.Apply;
import org.dmg.pmml.Lag;
import org.dmg.pmml.LocalTransformations;
import org.dmg.pmml.Matrix;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMMLAttributes;
import org.dmg.pmml.PMMLFunctions;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.TableLocator;
import org.dmg.pmml.TextIndex;
import org.dmg.pmml.Visitable;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.baseline.BaselineModel;
import org.dmg.pmml.bayesian_network.BayesianNetworkModel;
import org.dmg.pmml.clustering.CenterFields;
import org.dmg.pmml.clustering.ClusteringModel;
import org.dmg.pmml.gaussian_process.GaussianProcessModel;
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
import org.jpmml.evaluator.UnsupportedAttributeException;
import org.jpmml.evaluator.UnsupportedElementException;
import org.jpmml.evaluator.UnsupportedMarkupException;

/**
 * <p>
 * A Visitor that inspects a class model object for unsupported features.
 * </p>
 *
 * @see MarkupInspector#applyTo(Visitable)
 * @see UnsupportedMarkupException
 */
public class UnsupportedMarkupInspector extends MarkupInspector<UnsupportedMarkupException> {

	@Override
	public VisitorAction visit(Aggregate aggregate){
		Aggregate.Function function = aggregate.getFunction();

		switch(function){
			case MULTISET:
				report(new UnsupportedAttributeException(aggregate, function));
				break;
			default:
				break;
		}

		return super.visit(aggregate);
	}

	@Override
	public VisitorAction visit(Apply apply){
		String function = apply.getFunction();

		switch(function){
			case PMMLFunctions.ERF:
			case PMMLFunctions.NORMALCDF:
			case PMMLFunctions.NORMALIDF:
			case PMMLFunctions.NORMALPDF:
			case PMMLFunctions.STDNORMALCDF:
			case PMMLFunctions.STDNORMALIDF:
			case PMMLFunctions.STDNORMALPDF:
				report(new UnsupportedAttributeException(apply, PMMLAttributes.APPLY_FUNCTION, function));
				break;
			default:
				break;
		}

		return super.visit(apply);
	}

	@Override
	public VisitorAction visit(BaselineModel baselineModel){
		report(new UnsupportedElementException(baselineModel));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(BayesianNetworkModel bayesianNetworkModel){
		report(new UnsupportedElementException(bayesianNetworkModel));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(CenterFields centerFields){
		report(new UnsupportedElementException(centerFields));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(ClusteringModel clusteringModel){
		ClusteringModel.ModelClass modelClass = clusteringModel.getModelClass();

		switch(modelClass){
			case DISTRIBUTION_BASED:
				report(new UnsupportedAttributeException(clusteringModel, modelClass));
				break;
			default:
				break;
		}

		return super.visit(clusteringModel);
	}

	@Override
	public VisitorAction visit(DecisionTree decisionTree){
		report(new UnsupportedElementException(decisionTree));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(GaussianProcessModel gaussianProcessModel){
		report(new UnsupportedElementException(gaussianProcessModel));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(Lag lag){
		report(new UnsupportedElementException(lag));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(NeuralNetwork neuralNetwork){
		NeuralNetwork.ActivationFunction activationFunction = neuralNetwork.getActivationFunction();

		switch(activationFunction){
			case RADIAL_BASIS:
				report(new UnsupportedAttributeException(neuralNetwork, activationFunction));
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
					report(new UnsupportedAttributeException(neuralLayer, activationFunction));
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
				report(new UnsupportedAttributeException(normDiscrete, method));
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
				report(new UnsupportedAttributeException(outputField, resultFeature));
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
				report(new UnsupportedElementException(predictor));
			}
		}

		return super.visit(predictor);
	}

	@Override
	public VisitorAction visit(Regression regression){
		report(new UnsupportedElementException(regression));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(Segmentation segmentation){
		LocalTransformations localTransformations = segmentation.getLocalTransformations();

		if(localTransformations != null){
			report(new UnsupportedElementException(localTransformations));
		}

		return super.visit(segmentation);
	}

	@Override
	public VisitorAction visit(SequenceModel sequenceModel){
		report(new UnsupportedElementException(sequenceModel));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(SupportVectorMachineModel supportVectorMachineModel){
		boolean maxWins = supportVectorMachineModel.isMaxWins();
		if(maxWins){
			report(new UnsupportedAttributeException(supportVectorMachineModel, org.dmg.pmml.support_vector_machine.PMMLAttributes.SUPPORTVECTORMACHINEMODEL_MAXWINS, true));
		}

		SupportVectorMachineModel.Representation representation = supportVectorMachineModel.getRepresentation();
		switch(representation){
			case COEFFICIENTS:
				report(new UnsupportedAttributeException(supportVectorMachineModel, representation));
				break;
			default:
				break;
		}

		return super.visit(supportVectorMachineModel);
	}

	@Override
	public VisitorAction visit(TableLocator tableLocator){
		report(new UnsupportedElementException(tableLocator));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(TextIndex textIndex){
		boolean tokenize = textIndex.isTokenize();
		if(!tokenize){
			report(new UnsupportedAttributeException(textIndex, PMMLAttributes.TEXTINDEX_TOKENIZE, false));
		}

		TextIndex.LocalTermWeights localTermWeights = textIndex.getLocalTermWeights();
		switch(localTermWeights){
			case AUGMENTED_NORMALIZED_TERM_FREQUENCY:
				report(new UnsupportedAttributeException(textIndex, localTermWeights));
				break;
			default:
				break;
		}

		return super.visit(textIndex);
	}

	@Override
	public VisitorAction visit(TextModel textModel){
		report(new UnsupportedElementException(textModel));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(TimeSeriesModel timeSeriesModel){
		report(new UnsupportedElementException(timeSeriesModel));

		return VisitorAction.SKIP;
	}

	@Override
	public VisitorAction visit(TreeModel treeModel){
		TreeModel.MissingValueStrategy missingValueStrategy = treeModel.getMissingValueStrategy();

		switch(missingValueStrategy){
			case AGGREGATE_NODES:
			case WEIGHTED_CONFIDENCE:
				report(new UnsupportedAttributeException(treeModel, missingValueStrategy));
				break;
			default:
				break;
		}

		return super.visit(treeModel);
	}
}