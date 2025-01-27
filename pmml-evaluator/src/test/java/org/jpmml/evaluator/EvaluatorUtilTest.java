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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

		assertThrows(UnsupportedOperationException.class, () -> EvaluatorUtil.decode(invalidValue));
	}

	@Test
	public void decodeAll(){
		Computable value = new Computable(){

			@Override
			public String getResult(){
				return "value";
			}
		};

		assertEquals(Collections.singletonMap(null, "value"), EvaluatorUtil.decodeAll(Collections.singletonMap(null, value)));
		assertEquals(Collections.singletonMap("key", "value"), EvaluatorUtil.decodeAll(Collections.singletonMap("key", value)));

		Computable invalidValue = new Computable(){

			@Override
			public Object getResult(){
				throw new UnsupportedOperationException();
			}
		};

		assertEquals(Collections.emptyMap(), EvaluatorUtil.decodeAll(Collections.singletonMap(null, invalidValue)));
		assertEquals(Collections.emptyMap(), EvaluatorUtil.decodeAll(Collections.singletonMap("key", invalidValue)));

		Map<String, Object> results = new LinkedHashMap<>();
		results.put(null, invalidValue);
		results.put("decision", Boolean.TRUE);

		assertEquals(Collections.singletonMap("decision", Boolean.TRUE), EvaluatorUtil.decodeAll(results));
	}

	@Test
	public void groupRows(){
		List<Map<String, Object>> table = new ArrayList<>();
		table.add(createRow("1", "Cracker"));
		table.add(createRow("2", "Cracker"));
		table.add(createRow("1", "Coke"));
		table.add(createRow("3", "Cracker"));
		table.add(createRow("3", "Water"));
		table.add(createRow("3", "Coke"));
		table.add(createRow("2", "Water"));

		table = EvaluatorUtil.groupRows("transaction", table);

		checkGroupedRow(table.get(0), "1", Arrays.asList("Cracker", "Coke"));
		checkGroupedRow(table.get(1), "2", Arrays.asList("Cracker", "Water"));
		checkGroupedRow(table.get(2), "3", Arrays.asList("Cracker", "Water", "Coke"));
	}

	static
	private Map<String, Object> createRow(String transaction, String item){
		Map<String, Object> result = new HashMap<>();
		result.put("transaction", transaction);
		result.put("item", item);

		return result;
	}

	static
	private void checkGroupedRow(Map<String, Object> row, String transaction, List<String> items){
		assertEquals(2, row.size());

		assertEquals(transaction, row.get("transaction"));
		assertEquals(items, row.get("item"));
	}
}