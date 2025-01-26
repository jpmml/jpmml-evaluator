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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class TableReader {

	private String separator = null;


	public TableReader(){
		this(null);
	}

	public TableReader(String separator){
		setSeparator(separator);
	}

	public Table read(InputStream is) throws IOException {
		return read(new InputStreamReader(is, "UTF-8"));
	}

	public Table read(Reader reader) throws IOException {
		String separator = getSeparator();

		Table table = new Table(1024);

		try(BufferedReader bufferedReader = new BufferedReader(reader)){
			Splitter splitter = null;

			List<String> columns = null;

			Table.Row row = null;

			while(true){
				String line = bufferedReader.readLine();

				if((line == null) || (line.trim()).equals("")){
					break;
				} // End if

				if(splitter == null){

					if(separator == null){
						separator = detectSeparator(line);

						setSeparator(separator);
					}

					splitter = Splitter.on(separator);
				}

				List<String> cells = Lists.newArrayList(splitter.split(line));

				if(columns == null){
					Set<String> uniqueCells = new LinkedHashSet<>(cells);

					if(uniqueCells.size() < cells.size()){
						throw new IllegalArgumentException("Expected unique column names, got non-unique column names");
					}

					columns = cells;

					continue;
				} // End if

				if(cells.size() != columns.size()){
					throw new IllegalArgumentException("Expected " + columns.size() + " cells, got " + cells.size() + " cells");
				} // End if

				if(row == null){
					row = table.new Row(0);
				} else

				{
					row.advance();
				}

				for(int i = 0; i < columns.size(); i++){
					String column = columns.get(i);
					Object cell = cells.get(i);

					row.put(column, cell);
				}
			}
		}

		table.canonicalize();

		return table;
	}

	public String getSeparator(){
		return this.separator;
	}

	private void setSeparator(String separator){
		this.separator = separator;
	}

	static
	private String detectSeparator(String line){
		String[] separators = {"\t", ";", ","};

		for(String separator : separators){
			String[] cells = line.split(separator);

			if(cells.length > 1){
				return separator;
			}
		}

		throw new IllegalArgumentException("Missing CSV separator");
	}
}