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
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public class TableReader extends AbstractMap<String, Object> implements Iterator<Map<String, Object>> {

	private Table table = null;

	private int position = -1;

	private int maxPosition = -1;


	public TableReader(Table table){
		setTable(table);
	}

	@Override
	public boolean hasNext(){
		int position = getPosition();
		int maxPosition = getMaxPosition();

		return position < maxPosition;
	}

	@Override
	public Map<String, Object> next(){
		int position = getPosition();
		int maxPosition = getMaxPosition();

		if(position < maxPosition){
			setPosition(position + 1);
		} else

		{
			throw new NoSuchElementException();
		}

		return this;
	}

	@Override
	public Object get(Object key){
		Table table = getTable();
		int position = ensurePosition();

		List<?> values = table.getValues((String)key);
		if(values != null){
			return values.get(position);
		}

		return null;
	}

	@Override
	public Set<Map.Entry<String, Object>> entrySet(){
		Table table = getTable();
		int position = ensurePosition();

		Set<Map.Entry<String, Object>> result = new AbstractSet<Map.Entry<String, Object>>(){


			@Override
			public int size(){
				List<String> columns = table.getColumns();

				return columns.size();
			}

			@Override
			public Iterator<Map.Entry<String, Object>> iterator(){
				List<String> columns = table.getColumns();

				Iterator<Map.Entry<String, Object>> result = new Iterator<Map.Entry<String, Object>>(){

					private Iterator<String> it = columns.iterator();


					@Override
					public boolean hasNext(){
						return this.it.hasNext();
					}

					@Override
					public Map.Entry<String, Object> next(){
						String column = this.it.next();

						List<?> values = table.getValues(column);
						if(values != null){
							Object value = values.get(position);

							return new SimpleEntry<>(column, value);
						}

						return new SimpleEntry<>(column, null);
					}
				};

				return result;
			}
		};

		return result;
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

	void setTable(Table table){
		this.table = Objects.requireNonNull(table);

		setPosition(-1);
		setMaxPosition(table.getNumberOfRows() - 1);
	}

	int getPosition(){
		return this.position;
	}

	void setPosition(int position){
		this.position = position;
	}

	int getMaxPosition(){
		return this.maxPosition;
	}

	void setMaxPosition(int maxPosition){
		this.maxPosition = maxPosition;
	}
}