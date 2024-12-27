/*
 * Copyright (c) 2024 Villu Ruusmann
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Table {

	private List<String> columns = null;

	private List<Exception> exceptions = new ArrayList<>();

	private Map<String, List<?>> values = new HashMap<>();


	public Table(){
		this(new ArrayList<>());
	}

	public Table(List<String> columns){
		setColumns(columns);
	}

	public int getNumberOfRows(){
		List<Exception> exceptions = getExceptions();

		int result = exceptions.size();

		Map<String, List<?>> columnValues = getValues();

		Collection<Map.Entry<String, List<?>>> entries = columnValues.entrySet();
		for(Map.Entry<String, List<?>> entry : entries){
			List<?> values = entry.getValue();

			result = Math.max(result, values.size());
		}

		return result;
	}

	public int getNumberOfColumns(){
		List<String> columns = getColumns();

		return columns.size();
	}

	public void canonicalize(){
		List<String> columns = getColumns();

		int numberOfRows = getNumberOfRows();

		List<Exception> exceptions = getExceptions();

		TableUtil.ensureSize(exceptions, numberOfRows);

		for(String column : columns){
			List<?> values = getValues(column);

			if(values == null){
				values = new ArrayList<>(numberOfRows);

				setValues(column, values);
			}

			TableUtil.ensureSize(values, numberOfRows);
		}
	}

	public boolean addColumn(String column){
		List<String> columns = getColumns();

		if(!columns.contains(column)){
			columns.add(column);

			return true;
		}

		return false;
	}

	public boolean removeColumn(String column){
		List<String> columns = getColumns();

		boolean result = columns.remove(column);
		if(result){
			Map<String, List<?>> columnValues = getValues();

			columnValues.remove(column);
		}

		return result;
	}

	public Exception getException(int index){
		List<Exception> exceptions = getExceptions();

		return TableUtil.get(exceptions, index);
	}

	public void setException(int index, Exception exception){
		List<Exception> exceptions = getExceptions();

		TableUtil.set(exceptions, index, exception);
	}

	public List<?> getValues(String column){
		Map<String, List<?>> columnValues = getValues();

		return columnValues.get(column);
	}

	public void setValues(String column, List<?> values){
		Map<String, List<?>> columnValues = getValues();

		columnValues.put(column, values);
	}

	public List<String> getColumns(){
		return this.columns;
	}

	void setColumns(List<String> columns){
		this.columns = Objects.requireNonNull(columns);
	}

	public List<Exception> getExceptions(){
		return this.exceptions;
	}

	public Map<String, List<?>> getValues(){
		return this.values;
	}
}