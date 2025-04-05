/*
 * Copyright (c) 2024 Villu Ruusmann
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
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TableTest {

	@Test
	public void construct(){
		Table table = new Table(1);

		assertEquals(Arrays.asList(), table.getColumns());

		table.setValues("A", null);

		assertEquals(Arrays.asList("A"), table.getColumns());

		table = new Table(Collections.unmodifiableList(table.getColumns()), 1);

		table.setValues("A", Collections.singletonList(1));

		try {
			table.setValues("B", Collections.singletonList(1d));

			fail();
		} catch(UnsupportedOperationException uoe){
			// Ignored
		}

		assertEquals(Arrays.asList("A"), table.getColumns());

		table = new Table(new ArrayList<>(table.getColumns()), 1);

		table.setValues(null, Collections.singletonList(null));
		table.setValues("C", Collections.singletonList("1"));

		assertEquals(Arrays.asList("A", null, "C"), table.getColumns());

		assertEquals(null, table.getValues("A"));
		assertEquals(Collections.singletonList(null), table.getValues(null));
		assertEquals(Arrays.asList("1"), table.getValues("C"));

		table.removeColumn(null);

		assertEquals(Arrays.asList("A", "C"), table.getColumns());
	}

	@Test
	public void copy(){
		Table argumentsTable = new Table(new ArrayList<>(Collections.singletonList("A")), 3);

		assertEquals(Arrays.asList("A"), argumentsTable.getColumns());

		assertEquals(1, argumentsTable.getNumberOfColumns());
		assertEquals(0, argumentsTable.getNumberOfRows());

		assertFalse(argumentsTable.hasExceptions());

		argumentsTable.setValues("A", Arrays.asList(1, 2, 3));
		argumentsTable.setValues("B", Arrays.asList(1.0, 2.0, 3.0));

		assertEquals(Arrays.asList("A", "B"), argumentsTable.getColumns());

		assertEquals(2, argumentsTable.getNumberOfColumns());
		assertEquals(3, argumentsTable.getNumberOfRows());

		Table.Row argumentRow = argumentsTable.createReaderRow(0);

		Table resultsTable = new Table(5);

		assertEquals(Collections.emptyList(), resultsTable.getColumns());

		assertEquals(0, resultsTable.getNumberOfColumns());
		assertEquals(0, resultsTable.getNumberOfRows());

		assertFalse(resultsTable.hasExceptions());

		Table.Row resultRow = resultsTable.createWriterRow(0);

		for(int i = 0; argumentRow.canAdvance(); i++){
			resultRow.putAll(argumentRow);

			resultRow.put("C", String.valueOf(i + 1));

			argumentRow.advance();
			resultRow.advance();
		}

		assertEquals(Arrays.asList("A", "B", "C"), resultsTable.getColumns());

		assertEquals(3, resultsTable.getNumberOfColumns());
		assertEquals(3, resultsTable.getNumberOfRows());

		assertFalse(resultsTable.hasExceptions());

		assertNull(resultsTable.getException(0));
		assertNull(resultsTable.getException(1));
		assertNull(resultsTable.getException(2));

		assertEquals(Arrays.asList(1, 2, 3), resultsTable.getValues("A"));
		assertEquals(Arrays.asList(1.0, 2.0, 3.0), resultsTable.getValues("B"));
		assertEquals(Arrays.asList("1", "2", "3"), resultsTable.getValues("C"));

		resultRow.setException(new Exception());

		resultRow.advance();

		resultRow.put("C", "5");

		resultsTable.canonicalize();

		assertEquals(3, resultsTable.getNumberOfColumns());
		assertEquals(5, resultsTable.getNumberOfRows());

		assertTrue(resultsTable.hasExceptions());

		assertNotNull(resultsTable.getException(3));
		assertNull(resultsTable.getException(4));

		assertEquals(Arrays.asList(1, 2, 3, null, null), resultsTable.getValues("A"));
		assertEquals(Arrays.asList(1.0, 2.0, 3.0, null, null), resultsTable.getValues("B"));
		assertEquals(Arrays.asList("1", "2", "3", null, "5"), resultsTable.getValues("C"));

		resultsTable.clearExceptions();

		Function<Object, String> function = (value) -> {

			if(value == null){
				return "N/A";
			}

			return value.toString();
		};

		resultsTable.apply(function);

		assertFalse(resultsTable.hasExceptions());

		assertEquals(Arrays.asList("1", "2", "3", "N/A", "N/A"), resultsTable.getValues("A"));
		assertEquals(Arrays.asList("1.0", "2.0", "3.0", "N/A", "N/A"), resultsTable.getValues("B"));
		assertEquals(Arrays.asList("1", "2", "3", "N/A", "5"), resultsTable.getValues("C"));
	}

	@Test
	public void blockIndicators(){
		Table table = new Table(Arrays.asList("A", "B", "C"), 100);

		Table.Row row = table.createWriterRow(0);

		for(int i = 1; i <= 100; i++){
			row.put("A", i);
			row.put("B", Boolean.valueOf(i % 2 == 0));
			row.put("C", Integer.valueOf(i <= 50 ? 1 : 0));

			row.advance();
		}

		assertEquals(3, table.getNumberOfColumns());
		assertEquals(100, table.getNumberOfRows());

		row.setOrigin(0);

		assertEquals(Map.of("A", 1, "B", false, "C", 1), row);

		assertNull(row.getLagged("A", 1, Collections.emptyList()));
		assertNull(row.getAggregated("A", "sum", 100, Collections.emptyList()));

		row.setOrigin(49);

		assertEquals(Map.of("A", 50, "B", true, "C", 1), row);

		assertEquals(49, row.getLagged("A", 1, Collections.emptyList()));

		assertEquals(48, row.getLagged("A", 1, Collections.singletonList("B")));
		assertEquals(49, row.getLagged("A", 1, Collections.singletonList("C")));

		assertEquals(1225d, row.getAggregated("A", "sum", 100, Collections.emptyList()));

		assertEquals(600d, row.getAggregated("A", "sum", 100, Collections.singletonList("B")));
		assertEquals(1225d, row.getAggregated("A", "sum", 100, Collections.singletonList("C")));

		assertEquals(600d, row.getAggregated("A", "sum", 100, Arrays.asList("B", "C")));

		row.setOrigin(50);

		assertEquals(Map.of("A", 51, "B", false, "C", 0), row);

		assertEquals(50, row.getLagged("A", 1, Collections.emptyList()));

		assertEquals(49, row.getLagged("A", 1, Collections.singletonList("B")));
		assertNull(row.getLagged("A", 1, Collections.singletonList("C")));

		assertEquals(1275d, row.getAggregated("A", "sum", 100, Collections.emptyList()));

		assertEquals(null, row.getAggregated("A", "sum", 100, Arrays.asList("B", "C")));

		row.setOrigin(99);

		assertEquals(Map.of("A", 100, "B", true, "C", 0), row);

		assertEquals(99, row.getLagged("A", 1, Collections.emptyList()));

		assertEquals(98, row.getLagged("A", 1, Collections.singletonList("B")));
		assertEquals(99, row.getLagged("A", 1, Collections.singletonList("C")));

		assertEquals(4950d, row.getAggregated("A", "sum", 100, Collections.emptyList()));

		assertEquals(2450d, row.getAggregated("A", "sum", 100, Collections.singletonList("B")));
		assertEquals(3675d, row.getAggregated("A", "sum", 100, Collections.singletonList("C")));

		assertEquals(1800d, row.getAggregated("A", "sum", 100, Arrays.asList("B", "C")));
	}
}