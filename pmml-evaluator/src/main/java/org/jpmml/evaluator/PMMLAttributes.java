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

import org.dmg.pmml.Aggregate;
import org.dmg.pmml.Apply;
import org.dmg.pmml.Array;
import org.dmg.pmml.CompoundPredicate;
import org.dmg.pmml.DiscretizeBin;
import org.dmg.pmml.FieldColumnPair;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.Interval;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.Minkowski;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.ScoreDistribution;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicate;
import org.dmg.pmml.TargetValue;
import org.dmg.pmml.TextIndex;
import org.dmg.pmml.TextIndexNormalization;
import org.dmg.pmml.Value;
import org.dmg.pmml.association.AssociationRule;
import org.dmg.pmml.association.Item;
import org.dmg.pmml.clustering.ClusteringField;
import org.dmg.pmml.general_regression.BaseCumHazardTables;
import org.dmg.pmml.general_regression.BaselineStratum;
import org.dmg.pmml.general_regression.Category;
import org.dmg.pmml.general_regression.GeneralRegressionModel;
import org.dmg.pmml.general_regression.PPCell;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.naive_bayes.BayesInput;
import org.dmg.pmml.naive_bayes.BayesOutput;
import org.dmg.pmml.naive_bayes.PairCounts;
import org.dmg.pmml.naive_bayes.TargetValueCount;
import org.dmg.pmml.naive_bayes.TargetValueStat;
import org.dmg.pmml.nearest_neighbor.InstanceField;
import org.dmg.pmml.nearest_neighbor.KNNInput;
import org.dmg.pmml.nearest_neighbor.NearestNeighborModel;
import org.dmg.pmml.neural_network.Connection;
import org.dmg.pmml.neural_network.NeuralNetwork;
import org.dmg.pmml.neural_network.NeuralOutput;
import org.dmg.pmml.regression.CategoricalPredictor;
import org.dmg.pmml.regression.NumericPredictor;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.regression.RegressionTable;
import org.dmg.pmml.rule_set.RuleSelectionMethod;
import org.dmg.pmml.rule_set.RuleSet;
import org.dmg.pmml.rule_set.SimpleRule;
import org.dmg.pmml.scorecard.Attribute;
import org.dmg.pmml.scorecard.Characteristic;
import org.dmg.pmml.support_vector_machine.SupportVector;
import org.dmg.pmml.support_vector_machine.SupportVectorMachine;
import org.dmg.pmml.support_vector_machine.SupportVectorMachineModel;
import org.dmg.pmml.support_vector_machine.VectorInstance;
import org.dmg.pmml.tree.Node;
import org.jpmml.model.ReflectionUtil;

public interface PMMLAttributes {

	Field AGGREGATE_FUNCTION = ReflectionUtil.getField(Aggregate.class, "function");
	Field ARRAY_TYPE = ReflectionUtil.getField(Array.class, "type");
	Field ASSOCIATIONRULE_AFFINITY = ReflectionUtil.getField(AssociationRule.class, "affinity");
	Field ASSOCIATIONRULE_ANTECEDENT = ReflectionUtil.getField(AssociationRule.class, "antecedent");
	Field ASSOCIATIONRULE_CONSEQUENT = ReflectionUtil.getField(AssociationRule.class, "consequent");
	Field ASSOCIATIONRULE_LEVERAGE = ReflectionUtil.getField(AssociationRule.class, "leverage");
	Field ASSOCIATIONRULE_LIFT = ReflectionUtil.getField(AssociationRule.class, "lift");
	Field APPLY_FUNCTION = ReflectionUtil.getField(Apply.class, "function");
	Field ATTRIBUTE_PARTIALSCORE = ReflectionUtil.getField(Attribute.class, "partialScore");
	Field ATTRIBUTE_REASONCODE = ReflectionUtil.getField(Attribute.class, "reasonCode");
	Field BASECUMHAZARDTABLES_MAXTIME = ReflectionUtil.getField(BaseCumHazardTables.class, "maxTime");
	Field BASELINESTRATUM_VALUE = ReflectionUtil.getField(BaselineStratum.class, "value");
	Field BAYESINPUT_FIELD = ReflectionUtil.getField(BayesInput.class, "field");
	Field BAYESOUTPUT_FIELD = ReflectionUtil.getField(BayesOutput.class, "field");
	Field CATEGORY_VALUE = ReflectionUtil.getField(Category.class, "value");
	Field CATEGORICALPREDICTOR_COEFFICIENT = ReflectionUtil.getField(CategoricalPredictor.class, "coefficient");
	Field CATEGORICALPREDICTOR_FIELD = ReflectionUtil.getField(CategoricalPredictor.class, "field");
	Field CATEGORICALPREDICTOR_VALUE = ReflectionUtil.getField(CategoricalPredictor.class, "value");
	Field CHARACTERISTIC_BASELINESCORE = ReflectionUtil.getField(Characteristic.class, "baselineScore");
	Field CLUSTERINGFIELD_FIELD = ReflectionUtil.getField(ClusteringField.class, "field");
	Field CONNECTION_FROM = ReflectionUtil.getField(Connection.class, "from");
	Field COMPOUNDPREDICATE_BOOLEANOPERATOR = ReflectionUtil.getField(CompoundPredicate.class, "booleanOperator");
	Field DISCRETIZEBIN_BINVALUE = ReflectionUtil.getField(DiscretizeBin.class, "binValue");
	Field DISCRETIZEBIN_INTERVAL = ReflectionUtil.getField(DiscretizeBin.class, "interval");
	Field FIELDCOLUMNPAIR_COLUMN = ReflectionUtil.getField(FieldColumnPair.class, "column");
	Field FIELDCOLUMNPAIR_FIELD = ReflectionUtil.getField(FieldColumnPair.class, "field");
	Field FIELDREF_FIELD = ReflectionUtil.getField(FieldRef.class, "field");
	Field GENERALREGRESSIONMODEL_CUMULATIVELINKFUNCTION = ReflectionUtil.getField(GeneralRegressionModel.class, "cumulativeLinkFunction");
	Field GENERALREGRESSIONMODEL_DISTPARAMETER = ReflectionUtil.getField(GeneralRegressionModel.class, "distParameter");
	Field GENERALREGRESSIONMODEL_ENDTIMEVARIABLE = ReflectionUtil.getField(GeneralRegressionModel.class, "endTimeVariable");
	Field GENERALREGRESSIONMODEL_LINKFUNCTION = ReflectionUtil.getField(GeneralRegressionModel.class, "linkFunction");
	Field GENERALREGRESSIONMODEL_LINKPARAMETER = ReflectionUtil.getField(GeneralRegressionModel.class, "linkParameter");
	Field GENERALREGRESSIONMODEL_MODELTYPE = ReflectionUtil.getField(GeneralRegressionModel.class, "modelType");
	Field INSTANCEFIELD_FIELD = ReflectionUtil.getField(InstanceField.class, "field");
	Field INTERVAL_CLOSURE = ReflectionUtil.getField(Interval.class, "closure");
	Field INTERVAL_LEFTMARGIN = ReflectionUtil.getField(Interval.class, "leftMargin");
	Field INTERVAL_RIGHTMARGIN = ReflectionUtil.getField(Interval.class, "rightMargin");
	Field ITEM_CATEGORY = ReflectionUtil.getField(Item.class, "category");
	Field ITEM_ID = ReflectionUtil.getField(Item.class, "id");
	Field ITEM_VALUE = ReflectionUtil.getField(Item.class, "value");
	Field KNNINPUT_FIELD = ReflectionUtil.getField(KNNInput.class, "field");
	Field MAPVALUES_OUTPUTCOLUMN = ReflectionUtil.getField(MapValues.class, "outputColumn");
	Field MINKOWSKI_PPARAMETER = ReflectionUtil.getField(Minkowski.class, "pParameter");
	Field MININGFIELD_HIGHVALUE = ReflectionUtil.getField(MiningField.class, "highValue");
	Field MININGFIELD_INVALIDVALUEREPLACEMENT = ReflectionUtil.getField(MiningField.class, "invalidValueReplacement");
	Field MININGFIELD_LOWVALUE = ReflectionUtil.getField(MiningField.class, "lowValue");
	Field MININGFIELD_MISSINGVALUEREPLACEMENT = ReflectionUtil.getField(MiningField.class, "missingValueReplacement");
	Field NEARESTNEIGHBORMODEL_INSTANCEIDVARIABLE = ReflectionUtil.getField(NearestNeighborModel.class, "instanceIdVariable");
	Field NEARESTNEIGHBORMODEL_NUMBEROFNEIGHBORS = ReflectionUtil.getField(NearestNeighborModel.class, "numberOfNeighbors");
	Field NEURALNETWORK_ACTIVATIONFUNCTION = ReflectionUtil.getField(NeuralNetwork.class, "activationFunction");
	Field NEURALNETWORK_THRESHOLD = ReflectionUtil.getField(NeuralNetwork.class, "threshold");
	Field NEURALOUTPUT_OUTPUTNEURON = ReflectionUtil.getField(NeuralOutput.class, "outputNeuron");
	Field NODE_DEFAULTCHILD = ReflectionUtil.getField(Node.class, "defaultChild");
	Field NODE_SCORE = ReflectionUtil.getField(Node.class, "score");
	Field NORMCONTINUOUS_MAPMISSINGTO = ReflectionUtil.getField(NormContinuous.class, "mapMissingTo");
	Field NORMDISCRETE_VALUE = ReflectionUtil.getField(NormDiscrete.class, "value");
	Field NUMERICPREDICTOR_FIELD = ReflectionUtil.getField(NumericPredictor.class, "field");
	Field OUTPUTFIELD_ISMULTIVALUED = ReflectionUtil.getField(OutputField.class, "isMultiValued");
	Field OUTPUTFIELD_RANK = ReflectionUtil.getField(OutputField.class, "rank");
	Field OUTPUTFIELD_REPORTFIELD = ReflectionUtil.getField(OutputField.class, "reportField");
	Field OUTPUTFIELD_SEGMENTID = ReflectionUtil.getField(OutputField.class, "segmentId");
	Field OUTPUTFIELD_VALUE = ReflectionUtil.getField(OutputField.class, "value");
	Field PAIRCOUNTS_VALUE = ReflectionUtil.getField(PairCounts.class, "value");
	Field PPCELL_FIELD = ReflectionUtil.getField(PPCell.class, "field");
	Field PPCELL_VALUE = ReflectionUtil.getField(PPCell.class, "value");
	Field REGRESSIONMODEL_TARGETFIELDNAME = ReflectionUtil.getField(RegressionModel.class, "targetFieldName");
	Field REGRESSIONTABLE_TARGETCATEGORY = ReflectionUtil.getField(RegressionTable.class, "targetCategory");
	Field RULESET_DEFAULTCONFIDENCE = ReflectionUtil.getField(RuleSet.class, "defaultConfidence");
	Field RULESET_DEFAULTSCORE = ReflectionUtil.getField(RuleSet.class, "defaultScore");
	Field RULESELECTIONMETHOD_CRITERION = ReflectionUtil.getField(RuleSelectionMethod.class, "criterion");
	Field SCOREDISTRIBUTION_PROBABILITY = ReflectionUtil.getField(ScoreDistribution.class, "probability");
	Field SEGMENTATION_MULTIPLEMODELMETHOD = ReflectionUtil.getField(Segmentation.class, "multipleModelMethod");
	Field SIMPLEPREDICATE_FIELD = ReflectionUtil.getField(SimplePredicate.class, "field");
	Field SIMPLEPREDICATE_OPERATOR = ReflectionUtil.getField(SimplePredicate.class, "operator");
	Field SIMPLEPREDICATE_VALUE = ReflectionUtil.getField(SimplePredicate.class, "value");
	Field SIMPLERULE_SCORE = ReflectionUtil.getField(SimpleRule.class, "score");
	Field SIMPLESETPREDICATE_BOOLEANOPERATOR = ReflectionUtil.getField(SimpleSetPredicate.class, "booleanOperator");
	Field SIMPLESETPREDICATE_FIELD = ReflectionUtil.getField(SimpleSetPredicate.class, "field");
	Field SUPPORTVECTOR_VECTORID = ReflectionUtil.getField(SupportVector.class, "vectorId");
	Field SUPPORTVECTORMACHINE_ALTERNATETARGETCATEGORY = ReflectionUtil.getField(SupportVectorMachine.class, "alternateTargetCategory");
	Field SUPPORTVECTORMACHINE_TARGETCATEGORY = ReflectionUtil.getField(SupportVectorMachine.class, "targetCategory");
	Field SUPPORTVECTORMACHINEMODEL_MAXWINS = ReflectionUtil.getField(SupportVectorMachineModel.class, "maxWins");
	Field SUPPORTVECTORMACHINEMODEL_CLASSIFICATIONMETHOD = ReflectionUtil.getField(SupportVectorMachineModel.class, "classificationMethod");
	Field TARGETVALUE_DEFAULTVALUE = ReflectionUtil.getField(TargetValue.class, "defaultValue");
	Field TARGETVALUE_PRIORPROBABILITY = ReflectionUtil.getField(TargetValue.class, "priorProbability");
	Field TARGETVALUE_VALUE = ReflectionUtil.getField(TargetValue.class, "value");
	Field TARGETVALUECOUNT_VALUE = ReflectionUtil.getField(TargetValueCount.class, "value");
	Field TARGETVALUESTAT_VALUE = ReflectionUtil.getField(TargetValueStat.class, "value");
	Field TEXTINDEX_MAXLEVENSHTEINDISTANCE = ReflectionUtil.getField(TextIndex.class, "maxLevenshteinDistance");
	Field TEXTINDEX_TEXTFIELD = ReflectionUtil.getField(TextIndex.class, "textField");
	Field TEXTINDEX_TOKENIZE = ReflectionUtil.getField(TextIndex.class, "tokenize");
	Field TEXTINDEXNORMALIZATION_MAXLEVENSHTEINDISTANCE = ReflectionUtil.getField(TextIndexNormalization.class, "maxLevenshteinDistance");
	Field TEXTINDEXNORMALIZATION_TOKENIZE = ReflectionUtil.getField(TextIndexNormalization.class, "tokenize");
	Field VALUE_VALUE = ReflectionUtil.getField(Value.class, "value");
	Field VECTORINSTANCE_ID = ReflectionUtil.getField(VectorInstance.class, "id");
}
