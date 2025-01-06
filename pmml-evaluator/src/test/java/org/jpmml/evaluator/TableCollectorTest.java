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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TableCollectorTest {

	@Test
	public void collectSequential(){
		TableCollector tableCollector = new TableCollector(){

			private int counter = 0;


			@Override
			public BinaryOperator<List<Object>> combiner(){
				return (left, right) -> {
					left.addAll(right);

					this.counter += 1;

					return left;
				};
			}

			@Override
			public Function<List<Object>, Table> finisher(){
				assertTrue(this.counter == 0);

				return super.finisher();
			}
		};

		Table table = IntStream.range(0, 1000)
			.sequential()
			.mapToObj(createMapper())
			.collect(tableCollector);

		checkTable(table, 0, 1000);
	}

	@Test
	public void collectParallel(){
		TableCollector tableCollector = new TableCollector(){

			private int counter = 0;


			@Override
			public BinaryOperator<List<Object>> combiner(){
				return (left, right) -> {
					left.addAll(right);

					this.counter += 1;

					return left;
				};
			}

			@Override
			public Function<List<Object>, Table> finisher(){
				assertTrue(this.counter > 0);

				return super.finisher();
			}
		};

		Table table = IntStream.range(0, 10 * 1000)
			.parallel()
			.mapToObj(createMapper())
			.collect(tableCollector);

		checkTable(table, 0, 10 * 1000);
	}

	static
	private void checkTable(Table table, int begin, int end){
		assertEquals(Arrays.asList("A", "B", "C"), table.getColumns());

		assertEquals((end - begin), table.getNumberOfRows());
		assertEquals(3, table.getNumberOfColumns());

		assertTrue(table.hasExceptions());

		Table.Row row = table.new Row(0, table.getNumberOfRows());

		for(int i = begin; i < end; i++){
			assertEquals(3, row.size());

			int value = i;

			if(value % 100 == 0){
				assertNotNull(table.getException(i));

				assertNull(row.get("A"));
				assertNull(row.get("B"));
				assertNull(row.get("C"));
			} else

			{
				assertNull(table.getException(i));

				assertEquals((int)value, row.get("A"));
				assertEquals((double)value, row.get("B"));
				assertEquals(String.valueOf(value), row.get("C"));
			}

			row.advance();
		}
	}

	static
	private IntFunction<Object> createMapper(){
		return (value) -> {

			if(value % 100 == 0){
				return new IllegalArgumentException();
			} else

			{
				Map<String, Object> result = new LinkedHashMap<>();

				result.put("A", (int)value);
				result.put("B", (double)value);
				result.put("C", String.valueOf(value));

				return result;
			}
		};
	}
}