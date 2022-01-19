/*
 * Copyright (c) 2013 Villu Ruusmann
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
package org.jpmml.evaluator.testing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class CsvUtil {

	private CsvUtil(){
	}

	static
	public Table readTable(InputStream is) throws IOException {
		return readTable(is, null);
	}

	static
	public Table readTable(InputStream is, String separator) throws IOException {
		Table table = new Table();

		try(BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))){
			Splitter splitter = null;

			while(true){
				String line = reader.readLine();

				if(line == null || (line.trim()).equals("")){
					break;
				} // End if

				if(separator == null){
					separator = getSeparator(line);
				} // End if

				if(splitter == null){
					splitter = Splitter.on(separator);
				}

				List<String> row = Lists.newArrayList(splitter.split(line));

				table.add(row);
			}
		}

		table.setSeparator(separator);

		return table;
	}

	static
	public void writeTable(Table table, OutputStream os) throws IOException {

		try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"))){
			Joiner joiner = Joiner.on(table.getSeparator());

			for(int i = 0; i < table.size(); i++){
				List<String> row = table.get(i);

				if(i > 0){
					writer.write('\n');
				}

				writer.write(joiner.join(row));
			}
		}
	}

	static
	public <V> List<Map<String, V>> toRecords(Table table, Function<String, V> parseFunction){
		List<Map<String, V>> records = new ArrayList<>(table.size() - 1);

		List<String> headerRow = table.get(0);

		Set<String> uniqueHeaderRow = new LinkedHashSet<>(headerRow);
		if(uniqueHeaderRow.size() < headerRow.size()){
			Set<String> duplicateHeaderCells = new LinkedHashSet<>();

			for(int column = 0; column < headerRow.size(); column++){
				String headerCell = headerRow.get(column);

				if(Collections.frequency(headerRow, headerCell) != 1){
					duplicateHeaderCells.add(headerCell);
				}
			}

			if(!duplicateHeaderCells.isEmpty()){
				throw new IllegalArgumentException("Expected unique cell names, got non-unique cell name(s) " + duplicateHeaderCells);
			}
		}

		for(int row = 1; row < table.size(); row++){
			List<String> bodyRow = table.get(row);

			if(headerRow.size() != bodyRow.size()){
				throw new IllegalArgumentException("Expected " + headerRow.size() + " cells, got " + bodyRow.size() + " cells (data record " + (row - 1) + ")");
			}

			Map<String, V> record = new LinkedHashMap<>();

			for(int column = 0; column < headerRow.size(); column++){
				String fieldName = headerRow.get(column);
				V value = parseFunction.apply(bodyRow.get(column));

				record.put(fieldName, value);
			}

			records.add(record);
		}

		return records;
	}

	static
	public Table fromRecords(String separator, List<String> fieldNames, List<? extends Map<String, ?>> records, Function<Object, String> formatFunction){
		Table table = new Table(1 + records.size());
		table.setSeparator(separator);

		List<String> headerRow = new ArrayList<>(fieldNames.size());

		for(String fieldName : fieldNames){
			headerRow.add(fieldName != null ? fieldName : "(null)");
		}

		table.add(headerRow);

		for(Map<String, ?> record : records){
			List<String> bodyRow = new ArrayList<>(fieldNames.size());

			for(String fieldName : fieldNames){
				Object value = record.get(fieldName);

				bodyRow.add(formatFunction.apply(value));
			}

			table.add(bodyRow);
		}

		return table;
	}

	static
	private String getSeparator(String line){
		String[] separators = {"\t", ";", ","};

		for(String separator : separators){
			String[] cells = line.split(separator);

			if(cells.length > 1){
				return separator;
			}
		}

		throw new IllegalArgumentException("Missing CSV separator");
	}

	static
	public class Table extends ArrayList<List<String>> {

		private String separator = null;


		public Table(){
			this(1024);
		}

		public Table(int initialCapacity){
			super(initialCapacity);
		}

		public String getSeparator(){
			return this.separator;
		}

		public void setSeparator(String separator){
			this.separator = separator;
		}
	}
}