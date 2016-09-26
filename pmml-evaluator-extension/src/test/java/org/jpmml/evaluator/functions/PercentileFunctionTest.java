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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PercentileFunctionTest {

	@Test
	public void evaluate(){
		List<Double> values = Arrays.asList(1d, 2d, 3d, 4d);

		assertEquals(2.5d, evaluate(values, 50));
	}

	static
	private Number evaluate(List<Double> values, int quantile){
		Function percentile = new PercentileFunction();

		List<FieldValue> arguments = Arrays.asList(FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, values), FieldValueUtil.create(DataType.INTEGER, OpType.CONTINUOUS, quantile));

		return (percentile.evaluate(arguments)).asNumber();
	}
}