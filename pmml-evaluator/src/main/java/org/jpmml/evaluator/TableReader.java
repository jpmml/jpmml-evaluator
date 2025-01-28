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
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import org.jpmml.model.ReflectionUtil;

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

			Table.Row row = table.createWriterRow(0);

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

	public char getSeparator(){
		CsvReader.CsvReaderBuilder csvReaderBuilder = getCsvReaderBuilder();

		return getSeparator(csvReaderBuilder);
	}

	public CsvReader.CsvReaderBuilder getCsvReaderBuilder(){
		return this.csvReaderBuilder;
	}

	private void setCsvReaderBuilder(CsvReader.CsvReaderBuilder csvReaderBuilder){
		this.csvReaderBuilder = Objects.requireNonNull(csvReaderBuilder);
	}

	static
	public char getSeparator(CsvReader.CsvReaderBuilder csvReaderBuilder){

		try {
			Field fieldSeparatorField = CsvReader.CsvReaderBuilder.class.getDeclaredField("fieldSeparator");

			return ReflectionUtil.getFieldValue(fieldSeparatorField, csvReaderBuilder);
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}

	static
	public CsvReader.CsvReaderBuilder detectCsvReaderBuilder(Reader reader, int numberOfLines) throws IOException {

		if(!reader.markSupported()){
			throw new IllegalArgumentException();
		}

		FilterReader safeReader = new FilterReader(reader){

			@Override
			public void close(){
			}
		};

		CsvReader.CsvReaderBuilder result = null;

		// The detected separator must find at least two cells
		int resultFieldCount = 1;

		char[] separators = {',', ';', '\t', '|'};
		for(char separator : separators){
			CsvReader.CsvReaderBuilder csvReaderBuilder = createCsvReaderBuilder(separator);

			int fieldCount = -1;

			// Assume up to 256 cells per line, 8 characters per cell
			safeReader.mark(numberOfLines * 256 * 8);

			try(CsvReader csvReader = csvReaderBuilder.ofCsvRecord(safeReader)){
				Iterator<CsvRecord> it = csvReader.iterator();

				for(int i = 0, max = numberOfLines; (i < max) && it.hasNext(); i++){
					CsvRecord csvRecord = it.next();

					fieldCount = csvRecord.getFieldCount();
				}
			} finally {
				safeReader.reset();
			}

			if(fieldCount > resultFieldCount){
				result = csvReaderBuilder;

				resultFieldCount = fieldCount;
			}
		}

		return result;
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