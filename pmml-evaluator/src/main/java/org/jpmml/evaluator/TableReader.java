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

import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

public class TableReader {

	private CsvReader.CsvReaderBuilder csvReaderBuilder = null;


	public TableReader(char separator){
		this(createCsvReaderBuilder(separator));
	}

	public TableReader(CsvReader.CsvReaderBuilder csvReaderBuilder){
		setCsvReaderBuilder(csvReaderBuilder);
	}

	public Table read(InputStream is) throws IOException {
		return read(new InputStreamReader(is, "UTF-8"));
	}

	public Table read(Reader reader) throws IOException {
		CsvReader.CsvReaderBuilder csvReaderBuilder = getCsvReaderBuilder();

		FilterReader safeReader = new FilterReader(reader){

			@Override
			public void close(){
			}
		};

		Table table = new Table(1024);

		try(CsvReader<NamedCsvRecord> csvReader = csvReaderBuilder.ofNamedCsvRecord(safeReader)){
			List<String> columns = null;

			Table.Row row = table.new Row(0);

			for(Iterator<NamedCsvRecord> it = csvReader.iterator(); it.hasNext(); ){
				NamedCsvRecord csvRecord = it.next();

				if(columns == null){
					columns = initColumns(table, csvRecord);
				}

				row.putAll(csvRecord.getFieldsAsMap());

				row.advance();
			}
		}

		table.canonicalize();

		return table;
	}

	public CsvReader.CsvReaderBuilder getCsvReaderBuilder(){
		return this.csvReaderBuilder;
	}

	private void setCsvReaderBuilder(CsvReader.CsvReaderBuilder csvReaderBuilder){
		this.csvReaderBuilder = Objects.requireNonNull(csvReaderBuilder);
	}

	static
	public CsvReader.CsvReaderBuilder createCsvReaderBuilder(char separator){
		CsvReader.CsvReaderBuilder csvReaderBuilder = CsvReader.builder()
			.fieldSeparator(separator);

		// Activate strict(er) parsing mode
		csvReaderBuilder = csvReaderBuilder
			.acceptCharsAfterQuotes(false)
			.ignoreDifferentFieldCount(false);

		return csvReaderBuilder;
	}

	static
	private List<String> initColumns(Table table, NamedCsvRecord csvRecord){
		List<String> columns = csvRecord.getHeader();

		Set<String> duplicateColumns = new LinkedHashSet<>();

		for(String column : columns){
			boolean duplicate = !table.addColumn(column);

			if(duplicate){
				duplicateColumns.add(column);
			}
		}

		if(!duplicateColumns.isEmpty()){
			throw new IllegalArgumentException("Expected unique column names, got non-unique column name(s) " + duplicateColumns);
		}

		return columns;
	}
}