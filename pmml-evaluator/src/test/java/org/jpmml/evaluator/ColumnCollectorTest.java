/*
 * Copyright (c) 2025 Villu Ruusmann
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
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ColumnCollectorTest {

	@Test
	public void collect(){
		int max = 10 * 1000;

		Table table = new Table(max);

		assertEquals(0, table.getNumberOfRows());
		assertEquals(0, table.getNumberOfColumns());

		assertFalse(table.hasExceptions());

		IntStream.range(0, max)
			.parallel()
			.mapToObj((i) -> {
				return (i + 1);
			})
			.collect(new ColumnCollector(table, "A"));

		assertEquals(Arrays.asList("A"), table.getColumns());

		assertEquals(max, table.getNumberOfRows());
		assertEquals(1, table.getNumberOfColumns());

		assertFalse(table.hasExceptions());

		IntStream.range(0, max)
			.parallel()
			.mapToObj((i) -> {
				return (double)(i + 1);
			})
			.collect(new ColumnCollector(table, "B"));

		assertEquals(Arrays.asList("A", "B"), table.getColumns());

		assertEquals(max, table.getNumberOfRows());
		assertEquals(2, table.getNumberOfColumns());

		assertFalse(table.hasExceptions());

		(table.getValues("B")).stream()
			.sequential()
			.map((value) -> {
				int i = ((Double)value).intValue() - 1;

				if((i % 100) == 0){
					return new IllegalArgumentException();
				}

				return value;
			})
			.collect(new ColumnCollector(table, "B"));

		assertEquals(Arrays.asList("A", "B"), table.getColumns());

		assertEquals(max, table.getNumberOfRows());
		assertEquals(2, table.getNumberOfColumns());

		assertTrue(table.hasExceptions());

		assertThrows(IllegalArgumentException.class, () -> {
			IntStream.range(0, 1000)
				.mapToObj((i) -> {
					return String.valueOf(i + 1);
				})
				.collect(new ColumnCollector(table, "C"));
		});

		assertEquals(Arrays.asList("A", "B"), table.getColumns());

		(table.getValues("A")).stream()
			.sequential()
			.map(value -> {
				return String.valueOf(value);
			})
			.collect(new ColumnCollector(table, "C"));

		assertEquals(Arrays.asList("A", "B", "C"), table.getColumns());

		assertEquals(max, table.getNumberOfRows());
		assertEquals(3, table.getNumberOfColumns());

		assertTrue(table.hasExceptions());

		Table.Row row = table.createReaderRow(0);

		for(int i = 0; i < max; i++){
			assertEquals(3, row.size());

			int value = (i + 1);

			assertEquals(value, row.get("A"));

			if(i % 100 == 0){
				assertNotNull(row.getException());

				assertNull(row.get("B"));
			} else

			{
				assertNull(row.getException());

				assertEquals((double)value, row.get("B"));
			}

			assertEquals(String.valueOf(value), row.get("C"));

			row.advance();
		}
	}
}