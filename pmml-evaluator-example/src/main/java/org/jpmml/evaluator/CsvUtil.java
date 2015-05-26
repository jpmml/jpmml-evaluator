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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvUtil {

	private CsvUtil(){
	}

	static
	public Table readTable(File file) throws IOException {
		return readTable(file, null);
	}

	static
	public Table readTable(File file, String separator) throws IOException {
		Table table = new Table();

		BufferedReader reader = new BufferedReader(new FileReader(file));

		try {
			while(true){
				String line = reader.readLine();
				if(line == null){
					break;
				} // End if

				if((line.trim()).equals("")){
					break;
				} // End if

				if(separator == null){
					separator = getSeparator(line);
				}

				table.add(parseLine(line, separator));
			}
		} finally {
			reader.close();
		}

		table.setSeparator(separator);

		return table;
	}

	static
	public void writeTable(Table table, File file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		try {
			String terminator = "";

			for(List<String> row : table){
				StringBuilder sb = new StringBuilder();

				sb.append(terminator);
				terminator = "\n";

				String separator = "";

				for(int i = 0; i < row.size(); i++){
					sb.append(separator);
					separator = table.getSeparator();

					sb.append(row.get(i));
				}

				writer.write(sb.toString());
			}

			writer.flush();
		} finally {
			writer.close();
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
	public List<String> parseLine(String line, String separator){
		List<String> result = new ArrayList<>();

		String[] cells = line.split(separator);
		for(String cell : cells){

			// Remove quotation marks, if any
			cell = stripQuotes(cell, "\"");
			cell = stripQuotes(cell, "\'");

			// Standardize decimal marks to Full Stop (US)
			if(!(",").equals(separator)){
				cell = cell.replace(',', '.');
			}

			result.add(cell);
		}

		return result;
	}

	static
	private String stripQuotes(String string, String quote){

		if(string.startsWith(quote) && string.endsWith(quote)){
			string = string.substring(quote.length(), string.length() - quote.length());
		}

		return string;
	}

	static
	public class Table extends ArrayList<List<String>> {

		private String separator = null;


		public String getSeparator(){
			return this.separator;
		}

		public void setSeparator(String separator){
			this.separator = separator;
		}
	}
}