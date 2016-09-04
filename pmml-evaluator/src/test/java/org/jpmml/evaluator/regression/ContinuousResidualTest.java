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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.TargetReferenceField;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ContinuousResidualTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		List<InputField> activeFields = evaluator.getActiveFields();

		assertEquals(1, activeFields.size());

		List<InputField> inputFields = new ArrayList<>(evaluator.getInputFields());

		assertEquals(2, inputFields.size());

		inputFields.removeAll(activeFields);

		assertEquals(1, inputFields.size());

		InputField inputField = Iterables.getOnlyElement(inputFields);

		assertTrue(inputField instanceof TargetReferenceField);

		Map<FieldName, ?> arguments = createArguments("input", 0.8d, "target", 3d);

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		FieldName field = FieldName.create("residual");

		assertEquals(2.6d, (Double)getOutput(result, field), 1e-8);

		assertEquals(DataType.DOUBLE, OutputUtil.getDataType(evaluator.getOutputField(field), evaluator));
	}
}