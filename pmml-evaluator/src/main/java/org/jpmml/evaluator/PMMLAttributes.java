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

	public static final Field AGGREGATE_FUNCTION = ReflectionUtil.getField(Aggregate.class, "function");
	public static final Field ARRAY_TYPE = ReflectionUtil.getField(Array.class, "type");
	public static final Field ASSOCIATIONRULE_AFFINITY = ReflectionUtil.getField(AssociationRule.class, "affinity");
	public static final Field ASSOCIATIONRULE_ANTECEDENT = ReflectionUtil.getField(AssociationRule.class, "antecedent");
	public static final Field ASSOCIATIONRULE_CONSEQUENT = ReflectionUtil.getField(AssociationRule.class, "consequent");
	public static final Field ASSOCIATIONRULE_LEVERAGE = ReflectionUtil.getField(AssociationRule.class, "leverage");
	public static final Field ASSOCIATIONRULE_LIFT = ReflectionUtil.getField(AssociationRule.class, "lift");
	public static final Field APPLY_FUNCTION = ReflectionUtil.getField(Apply.class, "function");
	public static final Field ATTRIBUTE_PARTIALSCORE = ReflectionUtil.getField(Attribute.class, "partialScore");
	public static final Field ATTRIBUTE_REASONCODE = ReflectionUtil.getField(Attribute.class, "reasonCode");
	public static final Field BASECUMHAZARDTABLES_MAXTIME = ReflectionUtil.getField(BaseCumHazardTables.class, "maxTime");
	public static final Field BASELINESTRATUM_VALUE = ReflectionUtil.getField(BaselineStratum.class, "value");
	public static final Field BAYESINPUT_FIELDNAME = ReflectionUtil.getField(BayesInput.class, "fieldName");
	public static final Field BAYESOUTPUT_FIELDNAME = ReflectionUtil.getField(BayesOutput.class, "fieldName");
	public static final Field CATEGORY_VALUE = ReflectionUtil.getField(Category.class, "value");
	public static final Field CATEGORICALPREDICTOR_COEFFICIENT = ReflectionUtil.getField(CategoricalPredictor.class, "coefficient");
	public static final Field CATEGORICALPREDICTOR_NAME = ReflectionUtil.getField(CategoricalPredictor.class, "name");
	public static final Field CHARACTERISTIC_BASELINESCORE = ReflectionUtil.getField(Characteristic.class, "baselineScore");
	public static final Field CLUSTERINGFIELD_FIELD = ReflectionUtil.getField(ClusteringField.class, "field");
	public static final Field CONNECTION_FROM = ReflectionUtil.getField(Connection.class, "from");
	public static final Field COMPOUNDPREDICATE_BOOLEANOPERATOR = ReflectionUtil.getField(CompoundPredicate.class, "booleanOperator");
	public static final Field DISCRETIZEBIN_BINVALUE = ReflectionUtil.getField(DiscretizeBin.class, "binValue");
	public static final Field DISCRETIZEBIN_INTERVAL = ReflectionUtil.getField(DiscretizeBin.class, "interval");
	public static final Field FIELDCOLUMNPAIR_COLUMN = ReflectionUtil.getField(FieldColumnPair.class, "column");
	public static final Field FIELDCOLUMNPAIR_FIELD = ReflectionUtil.getField(FieldColumnPair.class, "field");
	public static final Field FIELDREF_FIELD = ReflectionUtil.getField(FieldRef.class, "field");
	public static final Field GENERALREGRESSIONMODEL_CUMULATIVELINKFUNCTION = ReflectionUtil.getField(GeneralRegressionModel.class, "cumulativeLinkFunction");
	public static final Field GENERALREGRESSIONMODEL_DISTPARAMETER = ReflectionUtil.getField(GeneralRegressionModel.class, "distParameter");
	public static final Field GENERALREGRESSIONMODEL_ENDTIMEVARIABLE = ReflectionUtil.getField(GeneralRegressionModel.class, "endTimeVariable");
	public static final Field GENERALREGRESSIONMODEL_LINKFUNCTION = ReflectionUtil.getField(GeneralRegressionModel.class, "linkFunction");
	public static final Field GENERALREGRESSIONMODEL_LINKPARAMETER = ReflectionUtil.getField(GeneralRegressionModel.class, "linkParameter");
	public static final Field GENERALREGRESSIONMODEL_MODELTYPE = ReflectionUtil.getField(GeneralRegressionModel.class, "modelType");
	public static final Field INSTANCEFIELD_FIELD = ReflectionUtil.getField(InstanceField.class, "field");
	public static final Field INTERVAL_CLOSURE = ReflectionUtil.getField(Interval.class, "closure");
	public static final Field INTERVAL_LEFTMARGIN = ReflectionUtil.getField(Interval.class, "leftMargin");
	public static final Field INTERVAL_RIGHTMARGIN = ReflectionUtil.getField(Interval.class, "rightMargin");
	public static final Field ITEM_CATEGORY = ReflectionUtil.getField(Item.class, "category");
	public static final Field ITEM_ID = ReflectionUtil.getField(Item.class, "id");
	public static final Field ITEM_VALUE = ReflectionUtil.getField(Item.class, "value");
	public static final Field KNNINPUT_FIELD = ReflectionUtil.getField(KNNInput.class, "field");
	public static final Field MAPVALUES_OUTPUTCOLUMN = ReflectionUtil.getField(MapValues.class, "outputColumn");
	public static final Field MINKOWSKI_PPARAMETER = ReflectionUtil.getField(Minkowski.class, "pParameter");
	public static final Field MININGFIELD_HIGHVALUE = ReflectionUtil.getField(MiningField.class, "highValue");
	public static final Field MININGFIELD_INVALIDVALUEREPLACEMENT = ReflectionUtil.getField(MiningField.class, "invalidValueReplacement");
	public static final Field MININGFIELD_LOWVALUE = ReflectionUtil.getField(MiningField.class, "lowValue");
	public static final Field MININGFIELD_MISSINGVALUEREPLACEMENT = ReflectionUtil.getField(MiningField.class, "missingValueReplacement");
	public static final Field NEARESTNEIGHBORMODEL_INSTANCEIDVARIABLE = ReflectionUtil.getField(NearestNeighborModel.class, "instanceIdVariable");
	public static final Field NEARESTNEIGHBORMODEL_NUMBEROFNEIGHBORS = ReflectionUtil.getField(NearestNeighborModel.class, "numberOfNeighbors");
	public static final Field NEURALNETWORK_ACTIVATIONFUNCTION = ReflectionUtil.getField(NeuralNetwork.class, "activationFunction");
	public static final Field NEURALNETWORK_THRESHOLD = ReflectionUtil.getField(NeuralNetwork.class, "threshold");
	public static final Field NEURALOUTPUT_OUTPUTNEURON = ReflectionUtil.getField(NeuralOutput.class, "outputNeuron");
	public static final Field NODE_DEFAULTCHILD = ReflectionUtil.getField(Node.class, "defaultChild");
	public static final Field NODE_SCORE = ReflectionUtil.getField(Node.class, "score");
	public static final Field NORMCONTINUOUS_MAPMISSINGTO = ReflectionUtil.getField(NormContinuous.class, "mapMissingTo");
	public static final Field NORMDISCRETE_VALUE = ReflectionUtil.getField(NormDiscrete.class, "value");
	public static final Field NUMERICPREDICTOR_NAME = ReflectionUtil.getField(NumericPredictor.class, "name");
	public static final Field OUTPUTFIELD_ISMULTIVALUED = ReflectionUtil.getField(OutputField.class, "isMultiValued");
	public static final Field OUTPUTFIELD_RANK = ReflectionUtil.getField(OutputField.class, "rank");
	public static final Field OUTPUTFIELD_REPORTFIELD = ReflectionUtil.getField(OutputField.class, "reportField");
	public static final Field OUTPUTFIELD_SEGMENTID = ReflectionUtil.getField(OutputField.class, "segmentId");
	public static final Field OUTPUTFIELD_VALUE = ReflectionUtil.getField(OutputField.class, "value");
	public static final Field PAIRCOUNTS_VALUE = ReflectionUtil.getField(PairCounts.class, "value");
	public static final Field PPCELL_PREDICTORNAME = ReflectionUtil.getField(PPCell.class, "predictorName");
	public static final Field PPCELL_VALUE = ReflectionUtil.getField(PPCell.class, "value");
	public static final Field REGRESSIONMODEL_TARGETFIELDNAME = ReflectionUtil.getField(RegressionModel.class, "targetFieldName");
	public static final Field REGRESSIONTABLE_TARGETCATEGORY = ReflectionUtil.getField(RegressionTable.class, "targetCategory");
	public static final Field RULESET_DEFAULTCONFIDENCE = ReflectionUtil.getField(RuleSet.class, "defaultConfidence");
	public static final Field RULESET_DEFAULTSCORE = ReflectionUtil.getField(RuleSet.class, "defaultScore");
	public static final Field RULESELECTIONMETHOD_CRITERION = ReflectionUtil.getField(RuleSelectionMethod.class, "criterion");
	public static final Field SCOREDISTRIBUTION_PROBABILITY = ReflectionUtil.getField(ScoreDistribution.class, "probability");
	public static final Field SEGMENTATION_MULTIPLEMODELMETHOD = ReflectionUtil.getField(Segmentation.class, "multipleModelMethod");
	public static final Field SIMPLEPREDICATE_FIELD = ReflectionUtil.getField(SimplePredicate.class, "field");
	public static final Field SIMPLEPREDICATE_OPERATOR = ReflectionUtil.getField(SimplePredicate.class, "operator");
	public static final Field SIMPLEPREDICATE_VALUE = ReflectionUtil.getField(SimplePredicate.class, "value");
	public static final Field SIMPLERULE_SCORE = ReflectionUtil.getField(SimpleRule.class, "score");
	public static final Field SIMPLESETPREDICATE_BOOLEANOPERATOR = ReflectionUtil.getField(SimpleSetPredicate.class, "booleanOperator");
	public static final Field SIMPLESETPREDICATE_FIELD = ReflectionUtil.getField(SimpleSetPredicate.class, "field");
	public static final Field SUPPORTVECTOR_VECTORID = ReflectionUtil.getField(SupportVector.class, "vectorId");
	public static final Field SUPPORTVECTORMACHINE_ALTERNATETARGETCATEGORY = ReflectionUtil.getField(SupportVectorMachine.class, "alternateTargetCategory");
	public static final Field SUPPORTVECTORMACHINE_TARGETCATEGORY = ReflectionUtil.getField(SupportVectorMachine.class, "targetCategory");
	public static final Field SUPPORTVECTORMACHINEMODEL_MAXWINS = ReflectionUtil.getField(SupportVectorMachineModel.class, "maxWins");
	public static final Field SUPPORTVECTORMACHINEMODEL_CLASSIFICATIONMETHOD = ReflectionUtil.getField(SupportVectorMachineModel.class, "classificationMethod");
	public static final Field TARGETVALUE_DEFAULTVALUE = ReflectionUtil.getField(TargetValue.class, "defaultValue");
	public static final Field TARGETVALUE_PRIORPROBABILITY = ReflectionUtil.getField(TargetValue.class, "priorProbability");
	public static final Field TARGETVALUE_VALUE = ReflectionUtil.getField(TargetValue.class, "value");
	public static final Field TARGETVALUECOUNT_VALUE = ReflectionUtil.getField(TargetValueCount.class, "value");
	public static final Field TARGETVALUESTAT_VALUE = ReflectionUtil.getField(TargetValueStat.class, "value");
	public static final Field TEXTINDEX_MAXLEVENSHTEINDISTANCE = ReflectionUtil.getField(TextIndex.class, "maxLevenshteinDistance");
	public static final Field TEXTINDEX_TEXTFIELD = ReflectionUtil.getField(TextIndex.class, "textField");
	public static final Field TEXTINDEX_TOKENIZE = ReflectionUtil.getField(TextIndex.class, "tokenize");
	public static final Field TEXTINDEXNORMALIZATION_MAXLEVENSHTEINDISTANCE = ReflectionUtil.getField(TextIndexNormalization.class, "maxLevenshteinDistance");
	public static final Field TEXTINDEXNORMALIZATION_TOKENIZE = ReflectionUtil.getField(TextIndexNormalization.class, "tokenize");
	public static final Field VALUE_VALUE = ReflectionUtil.getField(Value.class, "value");
	public static final Field VECTORINSTANCE_ID = ReflectionUtil.getField(VectorInstance.class, "id");
}