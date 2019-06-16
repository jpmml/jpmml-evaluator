/*
 * Copyright (c) 2019 Villu Ruusmann
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
import java.util.Collection;
import java.util.List;

import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.Function;
import org.jpmml.evaluator.TypeInfos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SplitFunctionTest {

	@Test
	public void evaluate(){
		assertEquals(Arrays.asList(""), evaluate("", ";"));
		assertEquals(Arrays.asList("", ""), evaluate(";", ";"));
		assertEquals(Arrays.asList("", "", ""), evaluate(";;", ";"));

		assertEquals(Arrays.asList("A"), evaluate("A", ","));
		assertEquals(Arrays.asList("", "A"), evaluate(",A", ","));
		assertEquals(Arrays.asList("A", ""), evaluate("A,", ","));
		assertEquals(Arrays.asList("", "A", ""), evaluate(",A,", ","));
	}

	public Collection<String> evaluate(String input, String pattern){
		Function split = new SplitFunction();

		List<FieldValue> arguments = Arrays.asList(
			FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, input),
			FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, pattern)
		);

		return (List)(split.evaluate(arguments)).asCollection();
	}
}