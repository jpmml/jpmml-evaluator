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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TableSpliteratorTest {

	@Test
	public void spliterator(){
		Table table = createTable(100);

		Spliterator<Map<String, Object>> spliterator = table.spliterator();

		assertEquals(100, spliterator.estimateSize());

		Spliterator<Map<String, Object>> splitSpliterator = spliterator.trySplit();

		assertEquals(50, spliterator.estimateSize());
		assertEquals(50, splitSpliterator.estimateSize());

		checkValues(spliterator, 50, 99);
		checkValues(splitSpliterator, 0, 49);

		spliterator = table.spliterator();

		assertEquals(100, spliterator.estimateSize());

		checkRemainingValues(spliterator, 0);
	}

	static
	private Table createTable(int size){
		List<Integer> intValues = new ArrayList<>(size);
		List<Double> doubleValues = new ArrayList<>(size);

		for(int i = 0; i < size; i++){
			intValues.add(i);
			doubleValues.add((double)i);
		}

		Table table = new Table(Arrays.asList("A", "B"));
		table.setValues("A", intValues);
		table.setValues("B", doubleValues);

		return table;
	}

	static
	private void checkValues(Spliterator<Map<String, Object>> spliterator, int start, int end){

		for(int i = start; i <= end; i++){
			int index = i;

			Consumer<Map<String, Object>> action = new Consumer<Map<String, Object>>(){

				@Override
				public void accept(Map<String, Object> row){
					assertEquals((int)index, row.get("A"));
					assertEquals((double)index, row.get("B"));
				}
			};

			assertTrue(spliterator.tryAdvance(action));
		}

		assertFalse(spliterator.tryAdvance(row -> fail()));
	}

	static
	private void checkRemainingValues(Spliterator<Map<String, Object>> spliterator, int start){
		Consumer<Map<String, Object>> action = new Consumer<Map<String, Object>>(){

			private int index = start;


			@Override
			public void accept(Map<String, Object> row){
				assertEquals((int)this.index, row.get("A"));
				assertEquals((double)this.index, row.get("B"));

				this.index++;
			}
		};

		spliterator.forEachRemaining(action);

		assertFalse(spliterator.tryAdvance(row -> fail()));
	}
}