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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Objects;

import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;

public class TableWriter {

	private CsvWriter.CsvWriterBuilder csvWriterBuilder = null;


	public TableWriter(char separator){
		this(createCsvWriterBuilder(separator));
	}

	public TableWriter(CsvWriter.CsvWriterBuilder csvWriterBuilder){
		setCsvWriterBuilder(csvWriterBuilder);
	}

	public void write(Table table, OutputStream os) throws IOException {
		write(table, new OutputStreamWriter(os, "UTF-8"));
	}

	public void write(Table table, Writer writer) throws IOException {
		CsvWriter.CsvWriterBuilder csvWriterBuilder = getCsvWriterBuilder();

		FilterWriter safeWriter = new FilterWriter(writer){

			@Override
			public void close() throws IOException {
				super.flush();
			}
		};

		try(CsvWriter csvWriter = csvWriterBuilder.build(safeWriter)){
			List<String> columns = table.getColumns();

			csvWriter.writeRecord(columns);

			int numberOfRows = table.getNumberOfRows();
			if(numberOfRows > 0){
				Table.Row row = table.createWriterRow(0);

				for(int i = 0; i < numberOfRows; i++){
					Iterable<String> cells = (Iterable)row.values();

					csvWriter.writeRecord(cells);

					row.advance();
				}
			}
		}
	}

	public CsvWriter.CsvWriterBuilder getCsvWriterBuilder(){
		return this.csvWriterBuilder;
	}

	private void setCsvWriterBuilder(CsvWriter.CsvWriterBuilder csvWriterBuilder){
		this.csvWriterBuilder = Objects.requireNonNull(csvWriterBuilder);
	}

	static
	public CsvWriter.CsvWriterBuilder createCsvWriterBuilder(char separator){
		CsvWriter.CsvWriterBuilder csvWriterBuilder = CsvWriter.builder()
			.lineDelimiter(LineDelimiter.PLATFORM)
			.fieldSeparator(separator);

		return csvWriterBuilder;
	}
}