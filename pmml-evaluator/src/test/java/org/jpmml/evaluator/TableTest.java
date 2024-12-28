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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TableTest {

	@Test
	public void construct(){
		List<String> columns = new ArrayList<>();
		columns.add("A");

		Table table = new Table();

		assertEquals(Arrays.asList(), table.getColumns());

		table.setValues("A", null);

		assertEquals(Arrays.asList("A"), table.getColumns());

		table = new Table(Collections.unmodifiableList(table.getColumns()));

		table.setValues("A", Collections.singletonList(1));

		try {
			table.setValues("B", Collections.singletonList(1d));

			fail();
		} catch(UnsupportedOperationException uoe){
			// Ignored
		}

		assertEquals(Arrays.asList("A"), table.getColumns());

		table = new Table(new ArrayList<>(table.getColumns()));

		table.setValues("C", Collections.singletonList("1"));

		assertEquals(Arrays.asList("A", "C"), table.getColumns());
	}

	@Test
	public void copy(){
		Table argumentsTable = new Table(new ArrayList<>(Collections.singletonList("A")));

		assertEquals(Arrays.asList("A"), argumentsTable.getColumns());

		assertEquals(1, argumentsTable.getNumberOfColumns());
		assertEquals(0, argumentsTable.getNumberOfRows());

		assertFalse(argumentsTable.hasExceptions());

		argumentsTable.setValues("A", Arrays.asList(1, 2, 3));
		argumentsTable.setValues("B", Arrays.asList(1.0, 2.0, 3.0));

		assertEquals(Arrays.asList("A", "B"), argumentsTable.getColumns());

		assertEquals(2, argumentsTable.getNumberOfColumns());
		assertEquals(3, argumentsTable.getNumberOfRows());

		TableReader tableReader = new TableReader(argumentsTable);

		try {
			tableReader.ensurePosition();

			fail();
		} catch(IllegalStateException ise){
			// Ignored
		}

		Table resultsTable = new Table();

		assertEquals(Collections.emptyList(), resultsTable.getColumns());

		assertEquals(0, resultsTable.getNumberOfColumns());
		assertEquals(0, resultsTable.getNumberOfRows());

		assertFalse(resultsTable.hasExceptions());

		TableWriter tableWriter = new TableWriter(resultsTable);

		try {
			tableWriter.ensurePosition();

			fail();
		} catch(IllegalStateException ise){
			// Ignored
		}

		for(int i = 0; tableReader.hasNext(); i++){
			Map<String, ?> row = tableReader.next();

			tableWriter.next();

			tableWriter.putAll(row);

			tableWriter.put("C", String.valueOf(i + 1));
		}

		try {
			tableReader.next();

			fail();
		} catch(NoSuchElementException nsee){
			// Ignored
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

		tableWriter.next();

		tableWriter.put(new Exception());

		tableWriter.next();

		tableWriter.put("C", "5");

		resultsTable.canonicalize();

		assertEquals(3, resultsTable.getNumberOfColumns());
		assertEquals(5, resultsTable.getNumberOfRows());

		assertTrue(resultsTable.hasExceptions());

		assertNotNull(resultsTable.getException(3));
		assertNull(resultsTable.getException(4));

		assertEquals(Arrays.asList(1, 2, 3, null, null), resultsTable.getValues("A"));
		assertEquals(Arrays.asList(1.0, 2.0, 3.0, null, null), resultsTable.getValues("B"));
		assertEquals(Arrays.asList("1", "2", "3", null, "5"), resultsTable.getValues("C"));
	}
}