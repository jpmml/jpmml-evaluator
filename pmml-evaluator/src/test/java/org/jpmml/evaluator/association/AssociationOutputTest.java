/*
 * Copyright (c) 2014 Villu Ruusmann
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
package org.jpmml.evaluator.association;

import java.util.Arrays;
import java.util.Map;

import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorTest;
import org.jpmml.evaluator.OutputUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AssociationOutputTest extends ModelEvaluatorTest {

	@Test
	public void evaluate() throws Exception {
		ModelEvaluator<?> evaluator = createModelEvaluator();

		Map<FieldName, ?> arguments = createArguments("item", Arrays.asList("Cracker", "Coke"));

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		assertEquals("1", getOutput(result, "entityId"));

		checkValue(Arrays.asList("Cracker"), result, "antecedent");
		checkValue(Arrays.asList("Water"), result, "consequent");

		checkValue("{Cracker}->{Water}", result, "rule");
		checkValue("1", result, "ruleId");

		checkDataType(DataType.STRING, evaluator, "rule");
		checkDataType(DataType.STRING, evaluator, "ruleId");

		checkValue(1d, result, "support");
		checkValue(1d, result, "confidence");
		checkValue(1d, result, "lift");

		checkDataType(DataType.DOUBLE, evaluator, "support");
		checkDataType(DataType.DOUBLE, evaluator, "confidence");
		checkDataType(DataType.DOUBLE, evaluator, "lift");
	}

	static
	private void checkValue(Object expected, Map<FieldName, ?> result, String name){
		assertEquals(expected, getOutput(result, name));
		assertEquals(expected, getOutput(result, "deprecated_" + name));
	}

	static
	private void checkDataType(DataType expected, ModelEvaluator<?> evaluator, String name){
		assertEquals(expected, OutputUtil.getDataType(evaluator.getOutputField(FieldName.create(name)), evaluator));
		assertEquals(expected, OutputUtil.getDataType(evaluator.getOutputField(FieldName.create("deprecated_" + name)), evaluator));
	}
}