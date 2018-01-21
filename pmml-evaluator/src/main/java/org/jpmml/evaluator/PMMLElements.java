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

	public static final Field BASELINESTRATUM_BASELINECELLS = ReflectionUtil.getField(BaselineStratum.class, "baselineCells");
	public static final Field BASECUMHAZARDTABLES_BASELINECELLS = ReflectionUtil.getField(BaseCumHazardTables.class, "baselineCells");
	public static final Field BAYESINPUTS_BAYESINPUTS = ReflectionUtil.getField(BayesInputs.class, "bayesInputs");
	public static final Field BAYESOUTPUT_TARGETVALUECOUNTS = ReflectionUtil.getField(BayesOutput.class, "targetValueCounts");
	public static final Field CHARACTERISTICS_CHARACTERISTICS = ReflectionUtil.getField(Characteristics.class, "characteristics");
	public static final Field CLUSTERINGMODEL_CLUSTERINGFIELDS = ReflectionUtil.getField(ClusteringModel.class, "clusteringFields");
	public static final Field CLUSTERINGMODEL_CLUSTERS = ReflectionUtil.getField(ClusteringModel.class, "clusters");
	public static final Field CLUSTERINGMODEL_COMPARISONMEASURE = ReflectionUtil.getField(ClusteringModel.class, "comparisonMeasure");
	public static final Field GENERALREGRESSIONMODEL_BASECUMHAZARDTABLES = ReflectionUtil.getField(GeneralRegressionModel.class, "baseCumHazardTables");
	public static final Field GENERALREGRESSIONMODEL_PARAMETERLIST = ReflectionUtil.getField(GeneralRegressionModel.class, "parameterList");
	public static final Field GENERALREGRESSIONMODEL_PARAMMATRIX = ReflectionUtil.getField(GeneralRegressionModel.class, "paramMatrix");
	public static final Field GENERALREGRESSIONMODEL_PPMATRIX = ReflectionUtil.getField(GeneralRegressionModel.class, "ppMatrix");
	public static final Field INSTANCEFIELDS_INSTANCEFIELDS = ReflectionUtil.getField(InstanceFields.class, "instanceFields");
	public static final Field KNNINPUTS_KNNINPUTS = ReflectionUtil.getField(KNNInputs.class, "knnInputs");
	public static final Field MININGMODEL_SEGMENTATION = ReflectionUtil.getField(MiningModel.class, "segmentation");
	public static final Field MODELVERIFICATION_INLINETABLE = ReflectionUtil.getField(ModelVerification.class, "inlineTable");
	public static final Field MODELVERIFICATION_VERIFICATIONFIELDS = ReflectionUtil.getField(ModelVerification.class, "verificationFields");
	public static final Field NAIVEBAYESMODEL_BAYESINPUTS = ReflectionUtil.getField(NaiveBayesModel.class, "bayesInputs");
	public static final Field NAIVEBAYESMODEL_BAYESOUTPUT = ReflectionUtil.getField(NaiveBayesModel.class, "bayesOutput");
	public static final Field NEARESTNEIGHBORMODEL_COMPARISONMEASURE = ReflectionUtil.getField(NearestNeighborModel.class, "comparisonMeasure");
	public static final Field NEARESTNEIGHBORMODEL_KNNINPUTS = ReflectionUtil.getField(NearestNeighborModel.class, "knnInputs");
	public static final Field NEARESTNEIGHBORMODEL_TRAININGINSTANCES = ReflectionUtil.getField(NearestNeighborModel.class, "trainingInstances");
	public static final Field NEURALINPUT_DERIVEDFIELD = ReflectionUtil.getField(NeuralInput.class, "derivedField");
	public static final Field NEURALINPUTS_NEURALINPUTS = ReflectionUtil.getField(NeuralInputs.class, "neuralInputs");
	public static final Field NEURALNETWORK_NEURALINPUTS = ReflectionUtil.getField(NeuralNetwork.class, "neuralInputs");
	public static final Field NEURALNETWORK_NEURALLAYERS = ReflectionUtil.getField(NeuralNetwork.class, "neuralLayers");
	public static final Field NEURALNETWORK_NEURALOUTPUTS = ReflectionUtil.getField(NeuralNetwork.class, "neuralOutputs");
	public static final Field NEURALOUTPUT_DERIVEDFIELD = ReflectionUtil.getField(NeuralOutput.class, "derivedField");
	public static final Field NEURALOUTPUTS_NEURALOUTPUTS = ReflectionUtil.getField(NeuralOutputs.class, "neuralOutputs");
	public static final Field NORMCONTINUOUS_LINEARNORMS = ReflectionUtil.getField(NormContinuous.class, "linearNorms");
	public static final Field PAIRCOUNTS_TARGETVALUECOUNTS = ReflectionUtil.getField(PairCounts.class, "targetValueCounts");
	public static final Field PMML_DATADICTIONARY = ReflectionUtil.getField(PMML.class, "dataDictionary");
	public static final Field REGRESSIONMODEL_REGRESSIONTABLES = ReflectionUtil.getField(RegressionModel.class, "regressionTables");
	public static final Field RULESET_RULESELECTIONMETHODS = ReflectionUtil.getField(RuleSet.class, "ruleSelectionMethods");
	public static final Field RULESETMODEL_RULESET = ReflectionUtil.getField(RuleSetModel.class, "ruleSet");
	public static final Field SCORECARD_CHARACTERISTICS = ReflectionUtil.getField(Scorecard.class, "characteristics");
	public static final Field SEGMENTATION_SEGMENTS = ReflectionUtil.getField(Segmentation.class, "segments");
	public static final Field SUPPORTVECTORMACHINEMODEL_SUPPORTVECTORMACHINES = ReflectionUtil.getField(SupportVectorMachineModel.class, "supportVectorMachines");
	public static final Field SUPPORTVECTORMACHINEMODEL_VECTORDICTIONARY = ReflectionUtil.getField(SupportVectorMachineModel.class, "vectorDictionary");
	public static final Field TARGETVALUECOUNTS_TARGETVALUECOUNTS = ReflectionUtil.getField(TargetValueCounts.class, "targetValueCounts");
	public static final Field TRAININGINSTANCES_INSTANCEFIELDS = ReflectionUtil.getField(TrainingInstances.class, "instanceFields");
	public static final Field TREEMODEL_NODE = ReflectionUtil.getField(TreeModel.class, "node");
	public static final Field VECTORDICTIONARY_VECTORFIELDS = ReflectionUtil.getField(VectorDictionary.class, "vectorFields");
}