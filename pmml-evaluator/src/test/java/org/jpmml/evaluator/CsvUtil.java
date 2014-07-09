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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.dmg.pmml.FieldName;

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

			List<String> headerCells = parseLine(headerLine);

			while(true){
				String bodyLine = reader.readLine();
				if(bodyLine == null){
					break;
				}

				Map<FieldName, String> row = Maps.newLinkedHashMap();

				List<String> bodyCells = parseLine(bodyLine);

				// Must be of equal length
				if(headerCells.size() != bodyCells.size()){
					throw new RuntimeException();
				}

				for(int i = 0; i < headerCells.size(); i++){
					row.put(FieldName.create(headerCells.get(i)), bodyCells.get(i));
				}

				table.add(row);
			}
		} finally {
			reader.close();
		}

		return table;
	}

	static
	public List<String> parseLine(String line){
		List<String> cells = Arrays.asList(line.split(","));

		Function<String, String> function = new Function<String, String>(){

			@Override
			public String apply(String cell){

				if("NA".equals(cell) || "N/A".equals(cell)){
					return null;
				}

				return cell;
			}
		};

		return Lists.transform(cells, function);
	}
}