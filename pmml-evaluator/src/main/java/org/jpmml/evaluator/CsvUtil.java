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
package org.jpmml.evaluator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

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
	private String getSeparator(String line){
		String[] separators = {"\t", ";", ","};

		for(String separator : separators){
			String[] cells = line.split(separator);

			if(cells.length > 1){
				return separator;
			}
		}

		throw new IllegalArgumentException();
	}

	static
	public class Table extends ArrayList<List<String>> {

		private String separator = null;


		public Table(){
			super(1024);
		}

		public String getSeparator(){
			return this.separator;
		}

		public void setSeparator(String separator){
			this.separator = separator;
		}
	}
}