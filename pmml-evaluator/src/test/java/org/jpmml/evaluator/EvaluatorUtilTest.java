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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EvaluatorUtilTest {

	@Test
	public void decode(){
		assertEquals(null, EvaluatorUtil.decode((Object)null));

		Computable value = new Computable(){

			@Override
			public String getResult(){
				return "value";
			}
		};

		assertEquals("value", EvaluatorUtil.decode(value));

		assertEquals(Arrays.asList("value"), EvaluatorUtil.decode(Arrays.asList(value)));
		assertEquals(Arrays.asList("value", "value"), EvaluatorUtil.decode(Arrays.asList(value, value)));

		Computable invalidValue = new Computable(){

			@Override
			public Object getResult(){
				throw new UnsupportedOperationException();
			}
		};

		try {
			EvaluatorUtil.decode(invalidValue);

			fail();
		} catch(UnsupportedOperationException uoe){
			// Ignored
		}
	}

	@Test
	public void decodeAll(){
		Computable value = new Computable(){

			@Override
			public String getResult(){
				return "value";
			}
		};

		assertEquals(Collections.singletonMap((String)null, "value"), EvaluatorUtil.decodeAll(Collections.singletonMap((FieldName)null, value)));
		assertEquals(Collections.singletonMap("key", "value"), EvaluatorUtil.decodeAll(Collections.singletonMap(FieldName.create("key"), value)));

		Computable invalidValue = new Computable(){

			@Override
			public Object getResult(){
				throw new UnsupportedOperationException();
			}
		};

		assertEquals(Collections.emptyMap(), EvaluatorUtil.decodeAll(Collections.singletonMap((FieldName)null, invalidValue)));
		assertEquals(Collections.emptyMap(), EvaluatorUtil.decodeAll(Collections.singletonMap(FieldName.create("key"), invalidValue)));

		Map<FieldName, Object> results = new LinkedHashMap<>();
		results.put((FieldName)null, invalidValue);
		results.put(FieldName.create("decision"), Boolean.TRUE);

		assertEquals(Collections.singletonMap("decision", Boolean.TRUE), EvaluatorUtil.decodeAll(results));
	}

	@Test
	public void groupRows(){
		List<Map<FieldName, Object>> table = new ArrayList<>();
		table.add(createRow("1", "Cracker"));
		table.add(createRow("2", "Cracker"));
		table.add(createRow("1", "Coke"));
		table.add(createRow("3", "Cracker"));
		table.add(createRow("3", "Water"));
		table.add(createRow("3", "Coke"));
		table.add(createRow("2", "Water"));

		table = EvaluatorUtil.groupRows(FieldName.create("transaction"), table);

		checkGroupedRow(table.get(0), "1", Arrays.asList("Cracker", "Coke"));
		checkGroupedRow(table.get(1), "2", Arrays.asList("Cracker", "Water"));
		checkGroupedRow(table.get(2), "3", Arrays.asList("Cracker", "Water", "Coke"));
	}

	static
	private Map<FieldName, Object> createRow(String transaction, String item){
		Map<FieldName, Object> result = new HashMap<>();
		result.put(FieldName.create("transaction"), transaction);
		result.put(FieldName.create("item"), item);

		return result;
	}

	static
	private void checkGroupedRow(Map<FieldName, Object> row, String transaction, List<String> items){
		assertEquals(2, row.size());

		assertEquals(transaction, row.get(FieldName.create("transaction")));
		assertEquals(items, row.get(FieldName.create("item")));
	}
}