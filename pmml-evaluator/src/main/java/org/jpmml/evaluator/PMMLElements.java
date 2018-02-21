/*
 * Copyright (c) 2018 Villu Ruusmann
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

import java.lang.reflect.Field;

import org.dmg.pmml.ModelVerification;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.PMML;
import org.dmg.pmml.SimpleSetPredicate;
import org.dmg.pmml.clustering.ClusteringModel;
import org.dmg.pmml.general_regression.BaseCumHazardTables;
import org.dmg.pmml.general_regression.BaselineStratum;
import org.dmg.pmml.general_regression.GeneralRegressionModel;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.naive_bayes.BayesInputs;
import org.dmg.pmml.naive_bayes.BayesOutput;
import org.dmg.pmml.naive_bayes.NaiveBayesModel;
import org.dmg.pmml.naive_bayes.PairCounts;
import org.dmg.pmml.naive_bayes.TargetValueCounts;
import org.dmg.pmml.nearest_neighbor.InstanceFields;
import org.dmg.pmml.nearest_neighbor.KNNInputs;
import org.dmg.pmml.nearest_neighbor.NearestNeighborModel;
import org.dmg.pmml.nearest_neighbor.TrainingInstances;
import org.dmg.pmml.neural_network.NeuralInput;
import org.dmg.pmml.neural_network.NeuralInputs;
import org.dmg.pmml.neural_network.NeuralNetwork;
import org.dmg.pmml.neural_network.NeuralOutput;
import org.dmg.pmml.neural_network.NeuralOutputs;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.rule_set.RuleSet;
import org.dmg.pmml.rule_set.RuleSetModel;
import org.dmg.pmml.scorecard.Characteristics;
import org.dmg.pmml.scorecard.Scorecard;
import org.dmg.pmml.support_vector_machine.SupportVectorMachineModel;
import org.dmg.pmml.support_vector_machine.VectorDictionary;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.model.ReflectionUtil;

public interface PMMLElements {

	Field BASELINESTRATUM_BASELINECELLS = ReflectionUtil.getField(BaselineStratum.class, "baselineCells");
	Field BASECUMHAZARDTABLES_BASELINECELLS = ReflectionUtil.getField(BaseCumHazardTables.class, "baselineCells");
	Field BAYESINPUTS_BAYESINPUTS = ReflectionUtil.getField(BayesInputs.class, "bayesInputs");
	Field BAYESOUTPUT_TARGETVALUECOUNTS = ReflectionUtil.getField(BayesOutput.class, "targetValueCounts");
	Field CHARACTERISTICS_CHARACTERISTICS = ReflectionUtil.getField(Characteristics.class, "characteristics");
	Field CLUSTERINGMODEL_CLUSTERINGFIELDS = ReflectionUtil.getField(ClusteringModel.class, "clusteringFields");
	Field CLUSTERINGMODEL_CLUSTERS = ReflectionUtil.getField(ClusteringModel.class, "clusters");
	Field CLUSTERINGMODEL_COMPARISONMEASURE = ReflectionUtil.getField(ClusteringModel.class, "comparisonMeasure");
	Field GENERALREGRESSIONMODEL_BASECUMHAZARDTABLES = ReflectionUtil.getField(GeneralRegressionModel.class, "baseCumHazardTables");
	Field GENERALREGRESSIONMODEL_PARAMETERLIST = ReflectionUtil.getField(GeneralRegressionModel.class, "parameterList");
	Field GENERALREGRESSIONMODEL_PARAMMATRIX = ReflectionUtil.getField(GeneralRegressionModel.class, "paramMatrix");
	Field GENERALREGRESSIONMODEL_PPMATRIX = ReflectionUtil.getField(GeneralRegressionModel.class, "ppMatrix");
	Field INSTANCEFIELDS_INSTANCEFIELDS = ReflectionUtil.getField(InstanceFields.class, "instanceFields");
	Field KNNINPUTS_KNNINPUTS = ReflectionUtil.getField(KNNInputs.class, "knnInputs");
	Field MININGMODEL_SEGMENTATION = ReflectionUtil.getField(MiningModel.class, "segmentation");
	Field MODELVERIFICATION_INLINETABLE = ReflectionUtil.getField(ModelVerification.class, "inlineTable");
	Field MODELVERIFICATION_VERIFICATIONFIELDS = ReflectionUtil.getField(ModelVerification.class, "verificationFields");
	Field NAIVEBAYESMODEL_BAYESINPUTS = ReflectionUtil.getField(NaiveBayesModel.class, "bayesInputs");
	Field NAIVEBAYESMODEL_BAYESOUTPUT = ReflectionUtil.getField(NaiveBayesModel.class, "bayesOutput");
	Field NEARESTNEIGHBORMODEL_COMPARISONMEASURE = ReflectionUtil.getField(NearestNeighborModel.class, "comparisonMeasure");
	Field NEARESTNEIGHBORMODEL_KNNINPUTS = ReflectionUtil.getField(NearestNeighborModel.class, "knnInputs");
	Field NEARESTNEIGHBORMODEL_TRAININGINSTANCES = ReflectionUtil.getField(NearestNeighborModel.class, "trainingInstances");
	Field NEURALINPUT_DERIVEDFIELD = ReflectionUtil.getField(NeuralInput.class, "derivedField");
	Field NEURALINPUTS_NEURALINPUTS = ReflectionUtil.getField(NeuralInputs.class, "neuralInputs");
	Field NEURALNETWORK_NEURALINPUTS = ReflectionUtil.getField(NeuralNetwork.class, "neuralInputs");
	Field NEURALNETWORK_NEURALLAYERS = ReflectionUtil.getField(NeuralNetwork.class, "neuralLayers");
	Field NEURALNETWORK_NEURALOUTPUTS = ReflectionUtil.getField(NeuralNetwork.class, "neuralOutputs");
	Field NEURALOUTPUT_DERIVEDFIELD = ReflectionUtil.getField(NeuralOutput.class, "derivedField");
	Field NEURALOUTPUTS_NEURALOUTPUTS = ReflectionUtil.getField(NeuralOutputs.class, "neuralOutputs");
	Field NORMCONTINUOUS_LINEARNORMS = ReflectionUtil.getField(NormContinuous.class, "linearNorms");
	Field PAIRCOUNTS_TARGETVALUECOUNTS = ReflectionUtil.getField(PairCounts.class, "targetValueCounts");
	Field PMML_DATADICTIONARY = ReflectionUtil.getField(PMML.class, "dataDictionary");
	Field REGRESSIONMODEL_REGRESSIONTABLES = ReflectionUtil.getField(RegressionModel.class, "regressionTables");
	Field RULESET_RULESELECTIONMETHODS = ReflectionUtil.getField(RuleSet.class, "ruleSelectionMethods");
	Field RULESETMODEL_RULESET = ReflectionUtil.getField(RuleSetModel.class, "ruleSet");
	Field SCORECARD_CHARACTERISTICS = ReflectionUtil.getField(Scorecard.class, "characteristics");
	Field SEGMENTATION_SEGMENTS = ReflectionUtil.getField(Segmentation.class, "segments");
	Field SIMPLESETPREDICATE_ARRAY = ReflectionUtil.getField(SimpleSetPredicate.class, "array");
	Field SUPPORTVECTORMACHINEMODEL_SUPPORTVECTORMACHINES = ReflectionUtil.getField(SupportVectorMachineModel.class, "supportVectorMachines");
	Field SUPPORTVECTORMACHINEMODEL_VECTORDICTIONARY = ReflectionUtil.getField(SupportVectorMachineModel.class, "vectorDictionary");
	Field TARGETVALUECOUNTS_TARGETVALUECOUNTS = ReflectionUtil.getField(TargetValueCounts.class, "targetValueCounts");
	Field TRAININGINSTANCES_INSTANCEFIELDS = ReflectionUtil.getField(TrainingInstances.class, "instanceFields");
	Field TREEMODEL_NODE = ReflectionUtil.getField(TreeModel.class, "node");
	Field VECTORDICTIONARY_VECTORFIELDS = ReflectionUtil.getField(VectorDictionary.class, "vectorFields");
}
