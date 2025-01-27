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
import java.io.StringWriter;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TableWriterTest {

	@Test
	public void write() throws IOException {
		String string;

		try(StringWriter writer = new StringWriter()){
			TableWriter tableWriter = new TableWriter(',');

			tableWriter.write(TableWriterTest.table, writer);

			string = writer.toString();
		}

		String expectedString = TableReaderTest.string;

		assertEquals(expectedString, string);
	}

	protected static Table table;

	static {
		Table table = new Table(3);
		table.setValues("A", Arrays.asList("1", "2", "3"));
		table.setValues("B", Arrays.asList("1.0", null, "3.0"));
		table.setValues("C", Arrays.asList(null, "true", "false"));

		TableWriterTest.table = table;
	}
}