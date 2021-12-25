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
package org.jpmml.evaluator.mining;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.Header;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.PMML;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.True;
import org.dmg.pmml.Version;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segment;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.tree.ComplexNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorBuilder;
import org.jpmml.evaluator.ModelEvaluatorBuilder;
import org.jpmml.evaluator.tree.HasDecisionPath;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MiningModelEvaluatorTest {

	@Test
	public void build(){
		PMML pmml = createPMML();

		Evaluator evaluator = build(pmml, Collections.emptySet());

		Object targetValue = evaluateDefault(evaluator);

		assertFalse(targetValue instanceof HasDecisionPath);

		evaluator = build(pmml, EnumSet.of(ResultFeature.ENTITY_ID));

		targetValue = evaluateDefault(evaluator);

		assertTrue(targetValue instanceof HasDecisionPath);
	}

	static
	private PMML createPMML(){
		Node root = new ComplexNode(null, True.INSTANCE)
			.setScore(1d);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, new MiningSchema(), root);

		Segmentation segmentation = new Segmentation(Segmentation.MultipleModelMethod.MODEL_CHAIN, null)
			.addSegments(new Segment(True.INSTANCE, treeModel));

		MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, new MiningSchema())
			.setSegmentation(segmentation);

		PMML pmml = new PMML(Version.PMML_4_4.getVersion(), new Header(), new DataDictionary())
			.addModels(miningModel);

		return pmml;
	}

	static
	private Evaluator build(PMML pmml, Set<ResultFeature> extraResultFeatures){
		EvaluatorBuilder evaluatorBuilder = new ModelEvaluatorBuilder(pmml)
			.setExtraResultFeatures(extraResultFeatures)
			.setCheckSchema(false);

		return evaluatorBuilder.build();
	}

	static
	private Object evaluateDefault(Evaluator evaluator){
		Map<String, ?> results = evaluator.evaluate(Collections.emptyMap());

		assertTrue(results.containsKey(Evaluator.DEFAULT_TARGET_NAME));

		return results.get(Evaluator.DEFAULT_TARGET_NAME);
	}
}