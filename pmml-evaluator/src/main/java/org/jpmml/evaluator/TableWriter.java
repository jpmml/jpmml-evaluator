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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Objects;

import com.google.common.base.Joiner;

public class TableWriter {

	private String separator = null;


	public TableWriter(String separator){
		setSeparator(separator);
	}

	public void write(Table table, OutputStream os) throws IOException {
		write(table, new OutputStreamWriter(os, "UTF-8"));
	}

	public void write(Table table, Writer writer) throws IOException {
		String separator = getSeparator();

		try(BufferedWriter bufferedWriter = new BufferedWriter(writer)){
			Joiner joiner = Joiner.on(separator);

			Collection<?> columns = table.getColumns();

			writer.write(joiner.join(columns));

			int numberOfRows = table.getNumberOfRows();
			if(numberOfRows > 0){
				Table.Row row = table.new Row(0);

				for(int i = 0; i < numberOfRows; i++){
					writer.write('\n');

					Collection<Object> cells = row.values();

					writer.write(joiner.join(cells));

					row.advance();
				}
			}
		}
	}

	public String getSeparator(){
		return this.separator;
	}

	private void setSeparator(String separator){
		this.separator = Objects.requireNonNull(separator);
	}
}