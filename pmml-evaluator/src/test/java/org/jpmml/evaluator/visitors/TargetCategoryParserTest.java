/*
 * Copyright (c) 2026 Villu Ruusmann
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

import java.util.Arrays;
import java.util.Collections;

import org.dmg.pmml.ComplexScoreDistribution;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.ScoreDistribution;
import org.dmg.pmml.True;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.regression.RegressionTable;
import org.dmg.pmml.tree.ComplexNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TargetCategoryParserTest extends ParserTest {

	@Test
	public void parseRegressionModel(){
		DataField firstDataField = new DataField("y1", OpType.CATEGORICAL, DataType.INTEGER);
		DataField secondDataField = new DataField("y2", OpType.CATEGORICAL, DataType.STRING);

		DataDictionary dataDictionary = createDataDictionary(firstDataField, secondDataField);

		RegressionTable firstRegressionTable = new RegressionTable(0d)
			.setTargetField(firstDataField.requireName())
			.setTargetCategory("1");

		RegressionTable secondRegressionTable = new RegressionTable(0d)
			.setTargetField(secondDataField.requireName())
			.setTargetCategory("1");

		MiningSchema miningSchema = createMiningSchema(Collections.emptyList(), Arrays.asList(firstDataField, secondDataField));

		RegressionModel regressionModel = new RegressionModel(MiningFunction.CLASSIFICATION, miningSchema, null)
			.addRegressionTables(firstRegressionTable, secondRegressionTable);

		PMML pmml = createPMML(dataDictionary, regressionModel);

		TargetCategoryParser parser = new TargetCategoryParser();
		parser.applyTo(pmml);

		assertEquals(1, firstRegressionTable.requireTargetCategory());
		assertEquals("1", secondRegressionTable.requireTargetCategory());
	}

	@Test
	public void parseTreeModel(){
		DataField firstDataField = new DataField("y1", OpType.CATEGORICAL, DataType.INTEGER);
		DataField secondDataField = new DataField("y2", OpType.CATEGORICAL, DataType.STRING);

		DataDictionary dataDictionary = createDataDictionary(firstDataField, secondDataField);

		ScoreDistribution firstScoreDistribution = new ComplexScoreDistribution("0", 1)
			.setTargetField(firstDataField.requireName());

		ScoreDistribution secondScoreDistribution = new ComplexScoreDistribution("0", 1)
			.setTargetField(secondDataField.requireName());

		Node root = new ComplexNode(null, True.INSTANCE)
			.addScoreDistributions(firstScoreDistribution, secondScoreDistribution);

		MiningSchema miningSchema = createMiningSchema(Collections.emptyList(), Arrays.asList(firstDataField, secondDataField));

		TreeModel treeModel = new TreeModel(MiningFunction.CLASSIFICATION, miningSchema, root);

		PMML pmml = createPMML(dataDictionary, treeModel);

		TargetCategoryParser parser = new TargetCategoryParser();
		parser.applyTo(pmml);

		assertEquals(0, firstScoreDistribution.requireValue());
		assertEquals("0", secondScoreDistribution.requireValue());
	}
}