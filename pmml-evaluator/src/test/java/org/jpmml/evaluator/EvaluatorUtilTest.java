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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
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
		Table table = new Table(5);
		table.setValues("transaction", Arrays.asList("1", "2", "1", "3", "3", "3", "2"));
		table.setValues("item", Arrays.asList("Cracker", "Cracker", "Coke", "Cracker", "Water", "Coke", "Water"));

		assertEquals(7, table.getNumberOfRows());
		assertEquals(2, table.getNumberOfColumns());

		Table groupedTable = EvaluatorUtil.groupRows("transaction", table);

		assertEquals(3, groupedTable.getNumberOfRows());
		assertEquals(2, groupedTable.getNumberOfColumns());

		Table.Row groupedRow = groupedTable.createReaderRow(0);

		assertEquals("1", groupedRow.get("transaction"));
		assertEquals(Arrays.asList("Cracker", "Coke"), groupedRow.get("item"));

		groupedRow.advance();

		assertEquals("2", groupedRow.get("transaction"));
		assertEquals(Arrays.asList("Cracker", "Water"), groupedRow.get("item"));

		groupedRow.advance();

		assertEquals("3", groupedRow.get("transaction"));
		assertEquals(Arrays.asList("Cracker", "Water", "Coke"), groupedRow.get("item"));
	}
}