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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TreeModelEvaluatorTest {

	@Test
	public void build(){
		PMML pmml = createPMML();

		TreeModelEvaluator treeModelEvaluator = build(pmml, EnumSet.noneOf(ResultFeature.class));

		assertTrue(treeModelEvaluator instanceof SimpleTreeModelEvaluator);

		treeModelEvaluator = build(pmml, EnumSet.of(ResultFeature.ENTITY_ID));

		assertTrue(treeModelEvaluator instanceof ComplexTreeModelEvaluator);

		OutputField predictedValueField = new OutputField("prediction", OpType.CONTINUOUS, DataType.DOUBLE)
			.setResultFeature(ResultFeature.PREDICTED_VALUE);

		pmml = createPMML(predictedValueField);

		treeModelEvaluator = build(pmml, EnumSet.noneOf(ResultFeature.class));

		assertTrue(treeModelEvaluator instanceof SimpleTreeModelEvaluator);

		OutputField entityIdField = new OutputField("nodeId", OpType.CATEGORICAL, DataType.STRING)
			.setResultFeature(ResultFeature.ENTITY_ID);

		pmml = createPMML(predictedValueField, entityIdField);

		treeModelEvaluator = build(pmml, EnumSet.noneOf(ResultFeature.class));

		assertTrue(treeModelEvaluator instanceof ComplexTreeModelEvaluator);
	}

	static
	private PMML createPMML(OutputField... outputFields){
		Node root = new ComplexNode(null, True.INSTANCE)
			.setScore(1d);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, new MiningSchema(), root);

		if(outputFields.length > 0){
			Output output = new Output()
				.addOutputFields(outputFields);

			treeModel.setOutput(output);
		}

		PMML pmml = new PMML(Version.PMML_4_4.getVersion(), new Header(), new DataDictionary())
			.addModels(treeModel);

		return pmml;
	}

	static
	private TreeModelEvaluator build(PMML pmml, Set<ResultFeature> extraResultFeatures){
		ModelEvaluatorBuilder modelEvaluatorBuilder = new ModelEvaluatorBuilder(pmml)
			.setExtraResultFeatures(extraResultFeatures);

		return (TreeModelEvaluator)modelEvaluatorBuilder.build();
	}
}