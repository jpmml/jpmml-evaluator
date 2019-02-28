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
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.jpmml.evaluator.CollectionValue;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ScalarValue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TransactionalSchemaTest extends AssociationSchemaTest {

	@Override
	public Map<FieldName, ?> createItemArguments(List<String> items){
		return createArguments("item", items);
	}

	@Test
	public void prepare() throws Exception {
		Evaluator evaluator = createModelEvaluator();

		InputField inputField = Iterables.getOnlyElement(evaluator.getInputFields());

		assertEquals(FieldName.create("item"), inputField.getName());

		FieldValue value = inputField.prepare("Cracker");

		assertTrue(value instanceof ScalarValue);

		assertEquals(DataType.STRING, value.getDataType());
		assertEquals(OpType.CATEGORICAL, value.getOpType());
		assertEquals("Cracker", value.getValue());

		value = inputField.prepare(Arrays.asList("Cracker", "Water", "Coke"));

		assertTrue(value instanceof CollectionValue);

		assertEquals(DataType.STRING, value.getDataType());
		assertEquals(OpType.CATEGORICAL, value.getOpType());
		assertEquals(Arrays.asList("Cracker", "Water", "Coke"), value.getValue());
	}
}