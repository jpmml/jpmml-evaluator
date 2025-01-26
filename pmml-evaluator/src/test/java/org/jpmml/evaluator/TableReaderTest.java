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

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.siegmar.fastcsv.reader.CsvReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TableReaderTest {

	@Test
	public void read() throws IOException {
		CsvReader.CsvReaderBuilder csvReaderBuilder = CsvReader.builder()
			.fieldSeparator(',');

		Table table;

		try(StringReader reader = new StringReader(TableReaderTest.string)){
			TableReader tableReader = new TableReader(csvReaderBuilder);

			table = tableReader.read(reader);

			assertEquals(-1, reader.read());
		}

		Table expectedTable = TableWriterTest.table;

		assertEquals(expectedTable.getColumns(), table.getColumns());

		assertEquals(expectedTable.getNumberOfRows(), table.getNumberOfRows());
		assertEquals(expectedTable.getNumberOfColumns(), table.getNumberOfColumns());

		Function<List<Object>, List<Object>> function = new Function<>(){

			@Override
			public List<Object> apply(List<Object> values){
				Function<Object, Object> function = new Function<>(){

					@Override
					public Object apply(Object value){

						if(("").equals(value)){
							return null;
						}

						return value;
					}
				};

				return Lists.transform(values, function);
			}
		};

		assertNotEquals(expectedTable.getValues(), table.getValues());
		assertEquals(expectedTable.getValues(), Maps.transformValues((Map)table.getValues(), function));
	}

	protected static String string = "A,B,C" + System.lineSeparator()
		+ "1,1.0," + System.lineSeparator()
		+ "2,,true" + System.lineSeparator()
		+ "3,3.0,false" + System.lineSeparator();
}