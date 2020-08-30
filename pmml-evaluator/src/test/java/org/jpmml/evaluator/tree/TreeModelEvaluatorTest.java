/*
 * Copyright (c) 2020 Villu Ruusmann
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
package org.jpmml.evaluator.tree;

import java.util.EnumSet;
import java.util.Set;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Header;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.True;
import org.dmg.pmml.Version;
import org.dmg.pmml.tree.ComplexNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.ModelEvaluatorBuilder;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TreeModelEvaluatorTest {

	@Test
	public void build(){
		Node root = new ComplexNode(True.INSTANCE)
			.setScore(1d);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, new MiningSchema(), root);

		PMML pmml = new PMML(Version.PMML_4_4.getVersion(), new Header(), new DataDictionary())
			.addModels(treeModel);

		TreeModelEvaluator treeModelEvaluator = build(pmml, EnumSet.noneOf(ResultFeature.class));

		assertTrue(treeModelEvaluator instanceof SimpleTreeModelEvaluator);

		treeModelEvaluator = build(pmml, EnumSet.of(ResultFeature.ENTITY_ID));

		assertTrue(treeModelEvaluator instanceof ComplexTreeModelEvaluator);

		OutputField predictedValueField = new OutputField(FieldName.create("prediction"), OpType.CONTINUOUS, DataType.DOUBLE)
			.setResultFeature(ResultFeature.PREDICTED_VALUE);

		Output output = new Output()
			.addOutputFields(predictedValueField);

		treeModel.setOutput(output);

		treeModelEvaluator = build(pmml, EnumSet.noneOf(ResultFeature.class));

		assertTrue(treeModelEvaluator instanceof SimpleTreeModelEvaluator);

		OutputField entityIdField = new OutputField(FieldName.create("nodeId"), OpType.CATEGORICAL, DataType.STRING)
			.setResultFeature(ResultFeature.ENTITY_ID);

		// XXX: Bypass element-based caching
		output = new Output()
			.addOutputFields(predictedValueField, entityIdField);

		treeModel.setOutput(output);

		treeModelEvaluator = build(pmml, EnumSet.noneOf(ResultFeature.class));

		assertTrue(treeModelEvaluator instanceof ComplexTreeModelEvaluator);
	}

	static
	private TreeModelEvaluator build(PMML pmml, Set<ResultFeature> extraResultFeatures){
		ModelEvaluatorBuilder modelEvaluatorBuilder = new ModelEvaluatorBuilder(pmml)
			.setExtraResultFeatures(extraResultFeatures);

		return (TreeModelEvaluator)modelEvaluatorBuilder.build();
	}
}