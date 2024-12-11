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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TableTest {

	@Test
	public void copy(){
		List<String> columns = new ArrayList<>();
		columns.add("A");
		columns.add("B");

		Table argumentsTable = new Table(columns);

		argumentsTable.setValues("A", Arrays.asList(1, 2, 3));
		argumentsTable.setValues("B", Arrays.asList(1.0, 2.0, 3.0));

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

		assertEquals(0, resultsTable.getNumberOfColumns());
		assertEquals(0, resultsTable.getNumberOfRows());

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

		assertEquals(3, resultsTable.getNumberOfColumns());
		assertEquals(3, resultsTable.getNumberOfRows());

		assertEquals(Arrays.asList("A", "B", "C"), resultsTable.getColumns());

		assertEquals(Arrays.asList(1, 2, 3), resultsTable.getValues("A"));
		assertEquals(Arrays.asList(1.0, 2.0, 3.0), resultsTable.getValues("B"));
		assertEquals(Arrays.asList("1", "2", "3"), resultsTable.getValues("C"));

		tableWriter.next();

		tableWriter.put("A", 4);

		tableWriter.next();

		tableWriter.put("C", "5");

		resultsTable.canonicalize();

		assertEquals(3, resultsTable.getNumberOfColumns());
		assertEquals(5, resultsTable.getNumberOfRows());

		assertEquals(Arrays.asList(1, 2, 3, 4, null), resultsTable.getValues("A"));
		assertEquals(Arrays.asList(1.0, 2.0, 3.0, null, null), resultsTable.getValues("B"));
		assertEquals(Arrays.asList("1", "2", "3", null, "5"), resultsTable.getValues("C"));
	}
}