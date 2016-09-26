/*
 * Copyright (c) 2015 Villu Ruusmann
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
package org.jpmml.sas.functions;

import java.util.Arrays;
import java.util.List;

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringNormalizeTest {

	@Test
	public void evaluate(){
		assertEquals("V", evaluate(1, "value"));
		assertEquals("V", evaluate(1, " value "));

		assertEquals("VALUE", evaluate(8, "value"));
		assertEquals("VALUE ", evaluate(8, " value "));
	}

	static
	private String evaluate(int length, String string){
		StringNormalize stringNormalize = new StringNormalize();

		List<FieldValue> arguments = Arrays.asList(FieldValueUtil.create(DataType.INTEGER, OpType.CONTINUOUS, length), FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, string));

		return (stringNormalize.evaluate(arguments)).asString();
	}
}