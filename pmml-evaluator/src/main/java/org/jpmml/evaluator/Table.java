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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

public class Table {

	private List<String> columns = null;

	private int initialCapacity = 0;

	private List<Exception> exceptions = null;

	private Map<String, List<?>> values = new HashMap<>();


	public Table(int initialCapacity){
		this(new ArrayList<>(), initialCapacity);
	}

	public Table(List<String> columns, int initialCapacity){
		setColumns(columns);
		setInitialCapacity(initialCapacity);
		setExceptions(createList());
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
				values = createList(numberOfRows);

				setValues(column, values);
			}

			TableUtil.ensureSize(values, numberOfRows);
		}
	}

	protected boolean ensureColumn(String column){
		List<String> columns = getColumns();

		if(!columns.contains(column)){
			columns.add(column);

			return true;
		}

		return false;
	}

	public boolean addColumn(String column){
		return ensureColumn(column);
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

	@IgnoreJRERequirement
	public TableSpliterator spliterator(){
		return new TableSpliterator(this)
			.init();
	}

	@IgnoreJRERequirement
	public Stream<Map<String, Object>> stream(){
		return StreamSupport.stream(spliterator(), false);
	}

	@IgnoreJRERequirement
	public Stream<Map<String, Object>> parallelStream(){
		return StreamSupport.stream(spliterator(), true);
	}

	public boolean hasExceptions(){
		List<Exception> exceptions = getExceptions();

		for(int i = 0; i < exceptions.size(); i++){
			Exception exception = exceptions.get(i);

			if(exception != null){
				return true;
			}
		}

		return false;
	}

	public Exception getException(int index){
		List<Exception> exceptions = getExceptions();

		return TableUtil.get(exceptions, index);
	}

	public void setException(int index, Exception exception){
		List<Exception> exceptions = getExceptions();

		TableUtil.set(exceptions, index, exception);
	}

	protected List<?> ensureValues(String column){
		Map<String, List<?>> columnValues = getValues();

		List<?> values = columnValues.get(column);
		if(values == null){
			ensureColumn(column);

			values = createList();

			columnValues.put(column, values);
		}

		return values;
	}

	public List<?> getValues(String column){
		Map<String, List<?>> columnValues = getValues();

		return columnValues.get(column);
	}

	public void setValues(String column, List<?> values){
		Map<String, List<?>> columnValues = getValues();

		ensureColumn(column);

		columnValues.put(column, values);
	}

	private <E> List<E> createList(){
		return createList(getInitialCapacity());
	}

	private <E> List<E> createList(int initialCapacity){
		return new ArrayList<>(initialCapacity);
	}

	public List<String> getColumns(){
		return this.columns;
	}

	private void setColumns(List<String> columns){
		this.columns = Objects.requireNonNull(columns);
	}

	public int getInitialCapacity(){
		return this.initialCapacity;
	}

	private void setInitialCapacity(int initialCapacity){
		this.initialCapacity = initialCapacity;
	}

	public List<Exception> getExceptions(){
		return this.exceptions;
	}

	private void setExceptions(List<Exception> exceptions){
		this.exceptions = Objects.requireNonNull(exceptions);
	}

	public Map<String, List<?>> getValues(){
		return this.values;
	}

	public class Row extends AbstractMap<String, Object> {

		private int origin;

		private int fence;


		protected Row(int origin){
			this(origin, -1);
		}

		protected Row(int origin, int fence){
			setOrigin(origin);
			setFence(fence);
		}

		public int estimateAdvances(){
			int origin = getOrigin();
			int fence = getFence();

			if(fence < 0){
				throw new IllegalStateException();
			}

			return (fence - origin);
		}

		public boolean canAdvance(){
			int origin = getOrigin();
			int fence = getFence();

			if(fence < 0){
				return true;
			}

			return (origin < fence);
		}

		public void advance(){
			int origin = getOrigin();

			setOrigin(origin + 1);
		}

		public Exception getException(){
			int origin = getOrigin();

			return Table.this.getException(origin);
		}

		public void setException(Exception exception){
			int origin = getOrigin();

			Table.this.setException(origin, exception);
		}

		@Override
		public Object get(Object key){
			int origin = getOrigin();

			List<?> values = getValues((String)key);
			if(values != null){
				return values.get(origin);
			}

			return null;
		}

		@Override
		public Object put(String key, Object value){
			int origin = getOrigin();

			@SuppressWarnings("unchecked")
			List<Object> values = (List<Object>)ensureValues(key);

			return TableUtil.set(values, origin, value);
		}

		@Override
		public Set<Map.Entry<String, Object>> entrySet(){
			int origin = getOrigin();

			Set<Map.Entry<String, Object>> result = new AbstractSet<Map.Entry<String, Object>>(){


				@Override
				public int size(){
					List<String> columns = getColumns();

					return columns.size();
				}

				@Override
				public Iterator<Map.Entry<String, Object>> iterator(){
					List<String> columns = getColumns();

					Iterator<Map.Entry<String, Object>> result = new Iterator<Map.Entry<String, Object>>(){

						private Iterator<String> it = columns.iterator();


						@Override
						public boolean hasNext(){
							return this.it.hasNext();
						}

						@Override
						public Map.Entry<String, Object> next(){
							String column = this.it.next();

							List<?> values = getValues(column);
							if(values != null){
								Object value = TableUtil.get(values, origin);

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

		public Table getTable(){
			return Table.this;
		}

		int getOrigin(){
			return this.origin;
		}

		void setOrigin(int origin){
			this.origin = origin;
		}

		int getFence(){
			return this.fence;
		}

		void setFence(int fence){
			this.fence = fence;
		}
	}
}