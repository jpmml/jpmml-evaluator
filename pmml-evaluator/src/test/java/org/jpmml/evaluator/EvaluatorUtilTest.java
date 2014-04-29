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
package org.jpmml.evaluator;

import java.util.*;

import com.google.common.collect.*;

import org.dmg.pmml.*;

import org.junit.*;

import static org.junit.Assert.*;

public class EvaluatorUtilTest {

	@Test
	public void decode(){
		Computable value = new Computable(){

			@Override
			public String getResult(){
				return "value";
			}
		};

		assertEquals("value", EvaluatorUtil.decode(value));

		assertEquals(Arrays.asList("value"), EvaluatorUtil.decode(Arrays.asList(value)));
		assertEquals(Arrays.asList("value", "value"), EvaluatorUtil.decode(Arrays.asList(value, value)));
	}

	@Test
	public void prepare() throws Exception {
		Evaluator evaluator = ModelEvaluatorTest.createModelEvaluator(AssociationAlgorithmTest.class);

		FieldValue simple = EvaluatorUtil.prepare(evaluator, new FieldName("item"), "Cracker");

		assertEquals("Cracker", simple.getValue());
		assertEquals(DataType.STRING, simple.getDataType());
		assertEquals(OpType.CATEGORICAL, simple.getOpType());

		FieldValue collection = EvaluatorUtil.prepare(evaluator, new FieldName("item"), Arrays.asList("Cracker", "Water", "Coke"));

		assertEquals(Arrays.asList("Cracker", "Water", "Coke"), collection.getValue());
		assertEquals(DataType.STRING, collection.getDataType());
		assertEquals(OpType.CATEGORICAL, collection.getOpType());
	}

	@Test
	public void groupRows(){
		List<Map<FieldName, Object>> table = Lists.newArrayList();
		table.add(createRow("1", "Cracker"));
		table.add(createRow("2", "Cracker"));
		table.add(createRow("1", "Coke"));
		table.add(createRow("3", "Cracker"));
		table.add(createRow("3", "Water"));
		table.add(createRow("3", "Coke"));
		table.add(createRow("2", "Water"));

		table = EvaluatorUtil.groupRows(new FieldName("transaction"), table);

		checkGroupedRow(table.get(0), "1", Arrays.asList("Cracker", "Coke"));
		checkGroupedRow(table.get(1), "2", Arrays.asList("Cracker", "Water"));
		checkGroupedRow(table.get(2), "3", Arrays.asList("Cracker", "Water", "Coke"));
	}

	static
	private Map<FieldName, Object> createRow(String transaction, String item){
		Map<FieldName, Object> result = Maps.newLinkedHashMap();
		result.put(new FieldName("transaction"), transaction);
		result.put(new FieldName("item"), item);

		return result;
	}

	static
	private void checkGroupedRow(Map<FieldName, Object> row, String transaction, List<String> items){
		assertEquals(2, row.size());

		assertEquals(transaction, row.get(new FieldName("transaction")));
		assertEquals(items, row.get(new FieldName("item")));
	}
}