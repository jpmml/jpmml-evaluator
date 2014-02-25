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

import java.io.*;
import java.util.*;

import org.dmg.pmml.*;

import com.google.common.collect.*;

public class CsvUtil {

	private CsvUtil(){
	}

	static
	public List<Map<FieldName, String>> load(InputStream is) throws IOException {
		List<Map<FieldName, String>> table = Lists.newArrayList();

		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "US-ASCII"));

		table:
		try {
			String headerLine = reader.readLine();
			if(headerLine == null){
				break table;
			}

			List<FieldName> keys = Lists.newArrayList();

			List<String> headerCells = parseLine(headerLine);
			for(int i = 0; i < headerCells.size(); i++){
				keys.add(FieldName.create(headerCells.get(i)));
			}

			while(true){
				String bodyLine = reader.readLine();
				if(bodyLine == null){
					break;
				}

				Map<FieldName, String> row = Maps.newLinkedHashMap();

				List<String> bodyCells = parseLine(bodyLine);

				// Must be of equal length
				if(bodyCells.size() != headerCells.size()){
					throw new RuntimeException();
				}

				for(int i = 0; i < bodyCells.size(); i++){
					row.put(keys.get(i), bodyCells.get(i));
				}

				table.add(row);
			}
		} finally {
			reader.close();
		}

		return table;
	}

	static
	private List<String> parseLine(String line){
		return Arrays.asList(line.split(","));
	}
}