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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class TableWriter extends AbstractMap<String, Object> {

	private Table table = null;

	private int position = -1;


	public TableWriter(Table table){
		setTable(table);
	}

	public void next(){
		int position = getPosition();

		setPosition(position + 1);
	}

	@Override
	public Object get(Object key){
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Map.Entry<String, Object>> entrySet(){
		throw new UnsupportedOperationException();
	}

	public void put(Exception exception){
		Table table = getTable();
		int position = ensurePosition();

		table.setException(position, exception);
	}

	@Override
	public Object put(String key, Object value){
		Table table = getTable();
		int position = ensurePosition();

		@SuppressWarnings("unchecked")
		List<Object> values = (List<Object>)table.getValues(key);
		if(values == null){
			table.addColumn(key);

			values = new ArrayList<>();

			table.setValues(key, values);
		}

		return TableUtil.set(values, position, value);
	}

	protected int ensurePosition(){
		int position = getPosition();

		if(position < 0){
			throw new IllegalStateException();
		}

		return position;
	}

	public Table getTable(){
		return this.table;
	}

	private void setTable(Table table){
		this.table = Objects.requireNonNull(table);
	}

	int getPosition(){
		return this.position;
	}

	void setPosition(int position){
		this.position = position;
	}
}