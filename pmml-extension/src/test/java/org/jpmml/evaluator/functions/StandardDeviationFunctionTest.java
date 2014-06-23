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
package org.jpmml.evaluator.functions;

import java.util.Arrays;
import java.util.List;

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.Function;
import org.jpmml.evaluator.VerificationUtil;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class StandardDeviationFunctionTest {

	@Test
	public void evaluate(){
		List<Double> values = Arrays.asList(2d, 4d, 4d, 4d, 5d, 5d, 7d, 9d);

		assertTrue(VerificationUtil.acceptable(Math.sqrt(32d / 8d), evaluate(values), 1.0e-8d, 1.0e-8d));

		assertTrue(VerificationUtil.acceptable(Math.sqrt(32d / 7d), evaluate(values, true), 1.0e-8d, 1.0e-8d));
		assertTrue(VerificationUtil.acceptable(Math.sqrt(32d / 8d), evaluate(values, false), 1.0e-8d, 1.0e-8d));
	}

	static
	private Number evaluate(List<Double> values){
		Function standardDeviation = new StandardDeviationFunction();

		List<FieldValue> arguments = Arrays.asList(FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, values));

		return (standardDeviation.evaluate(arguments)).asNumber();
	}

	static
	private Number evaluate(List<Double> values, boolean flag){
		Function standardDeviation = new StandardDeviationFunction();

		List<FieldValue> arguments = Arrays.asList(FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, values), FieldValueUtil.create(flag));

		return (standardDeviation.evaluate(arguments)).asNumber();
	}
}