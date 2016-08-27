/*
 * Copyright (c) 2013 Villu Ruusmann
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
package org.jpmml.evaluator.regression;

import java.util.List;
import java.util.Map;

import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Model;
import org.dmg.pmml.Target;
import org.dmg.pmml.TargetValue;
import org.dmg.pmml.Targets;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.OutputUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PriorProbabilitiesTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		Model model = evaluator.getModel();

		Map<FieldName, ?> arguments = createArguments("input", null);

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		assertEquals(5, result.size());

		assertEquals("NO", getTarget(result, Evaluator.DEFAULT_TARGET_NAME));

		FieldName field = FieldName.create("I_response");
		FieldName displayField = FieldName.create("U_response");

		assertEquals("NO", getOutput(result, field));
		assertEquals("No", getOutput(result, displayField));

		assertEquals(DataType.STRING, OutputUtil.getDataType(evaluator.getOutputField(field), evaluator));
		assertEquals(DataType.STRING, OutputUtil.getDataType(evaluator.getOutputField(displayField), evaluator));

		assertEquals(0.02d, getOutput(result, "P_responseYes"));
		assertEquals(0.98d, getOutput(result, "P_responseNo"));

		Targets targets = model.getTargets();
		for(Target target : targets){
			List<TargetValue> targetValues = target.getTargetValues();

			targetValues.clear();
		}

		result = evaluator.evaluate(arguments);

		assertEquals(1, result.size());

		assertEquals(null, getTarget(result, Evaluator.DEFAULT_TARGET_NAME));
	}
}