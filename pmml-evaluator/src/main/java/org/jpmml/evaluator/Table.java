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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.math3.stat.descriptive.UnivariateStatistic;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.descriptive.summary.Product;
import org.apache.commons.math3.stat.descriptive.summary.Sum;
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

		ensureSize(exceptions, numberOfRows);

		for(String column : columns){
			List<?> values = getValues(column);

			if(values == null){
				values = createList(numberOfRows);

				setValues(column, values);
			}

			ensureSize(values, numberOfRows);
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

	/**
	 * <p>
	 * Creates a row that can advance till the end of the table, but not beyond it.
	 * <p>
	 *
	 * @see Row#canAdvance()
	 */
	public Row createReaderRow(int origin){
		return new Row(origin, getNumberOfRows());
	}

	/**
	 * <p>
	 * Creates a row that can advance within the specified range.
	 * </p>
	 *
	 * @param origin The start line number.
	 * @param fence The end line number, exclusive.
	 *
	 * @see Row#canAdvance()
	 */
	public Row createReaderRow(int origin, int fence){
		return new Row(origin, fence);
	}

	/**
	 * <p>
	 * Creates a row that can advance indefinitely.
	 * </p>
	 *
	 * @see Row#canAdvance()
	 */
	public Row createWriterRow(int origin){
		return new Row(origin, -1);
	}

	@IgnoreJRERequirement
	public TableSpliterator spliterator(){
		return new TableSpliterator(this)
			.init();
	}

	@IgnoreJRERequirement
	public Stream<Row> stream(){
		return StreamSupport.stream(spliterator(), false);
	}

	@IgnoreJRERequirement
	public Stream<Row> parallelStream(){
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

		return get(exceptions, index);
	}

	public void setException(int index, Exception exception){
		List<Exception> exceptions = getExceptions();

		set(exceptions, index, exception);
	}

	public void clearExceptions(){
		List<Exception> exceptions = getExceptions();

		clear(exceptions);
	}

	@IgnoreJRERequirement
	@SuppressWarnings("unchecked")
	public void apply(Function<?, ?> function){
		Map<String, List<?>> columnValues = getValues();

		Collection<Map.Entry<String, List<?>>> entries = columnValues.entrySet();
		for(Map.Entry<String, List<?>> entry : entries){
			List<?> values = entry.getValue();

			transform((List<Object>)values, (Function<Object, Object>)function);
		}
	}

	@IgnoreJRERequirement
	@SuppressWarnings("unchecked")
	public void apply(String column, Function<?, ?> function){
		List<?> values = getValues(column);

		if(values != null){
			transform((List<Object>)values, (Function<Object, Object>)function);
		}
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

	static
	private <E> E get(List<E> values, int index){

		if(index < values.size()){
			return values.get(index);
		}

		return null;
	}

	static
	private <E> E set(List<E> values, int index, E value){

		if(index < values.size()){
			return values.set(index, value);
		} else

		{
			ensureSize(values, index);

			values.add(value);

			return null;
		}
	}

	static
	private <E> List<E> ensureSize(List<E> values, int size){

		while(values.size() < size){
			values.add(null);
		}

		return values;
	}

	static
	private <E> void clear(List<E> values){
		ListIterator<E> it = values.listIterator();

		while(it.hasNext()){
			E value = it.next();

			if(value != null){
				it.set(null);
			}
		}
	}

	@IgnoreJRERequirement
	static
	private <E> void transform(List<E> values, Function<E, E> function){
		ListIterator<E> it = values.listIterator();

		while(it.hasNext()){
			E value = it.next();

			value = function.apply(value);

			it.set(value);
		}
	}

	static
	private UnivariateStatistic getStatistic(String function){

		switch(function){
			case "avg":
				return new Mean();
			case "median":
				return new Median();
			case "product":
				return new Product();
			case "sum":
				return new Sum();
			case "stddev":
				return new StandardDeviation();
			default:
				throw new IllegalArgumentException(function);
		}
	}

	public class Row extends AbstractMap<String, Object> implements LaggableMap<String, Object>, AggregableMap<String, Object> {

		private int origin;

		private int fence;


		public Row(int origin, int fence){
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
				int index = origin;

				return Table.get(values, index);
			}

			return null;
		}

		@Override
		public Object getLagged(String key, int n, List<String> blockIndicatorKeys){
			int origin = getOrigin();

			List<?> values = getValues(key);
			if(values != null){

				if(blockIndicatorKeys.isEmpty()){
					int index = (origin - n);

					if(index < 0){
						return null;
					}

					return Table.get(values, index);
				} else

				{
					Map<String, ?> blockIndicatorMap = createBlockIndicator(origin, blockIndicatorKeys);

					int matches = 0;

					for(int index = (origin - 1); index > -1; index--){

						if(matchesBlockIndicator(index, blockIndicatorMap)){
							matches++;

							if(matches == n){
								return Table.get(values, index);
							}
						}
					}

					return null;
				}
			}

			return null;
		}

		@IgnoreJRERequirement
		@Override
		public Object getAggregated(String key, String function, int n, List<String> blockIndicatorKeys){
			int origin = getOrigin();

			List<?> values = getValues(key);
			if(values != null){
				List<Number> windowValues;

				if(blockIndicatorKeys.isEmpty()){
					int fromIndex = Math.max((origin - n), 0);
					int toIndex = origin;

					windowValues = (values.subList(fromIndex, toIndex)).stream()
						.filter(value -> value != null)
						.map(value -> TypeUtil.cast(Number.class, value))
						.collect(Collectors.toList());
				} else

				{
					Map<String, ?> blockIndicatorMap = createBlockIndicator(origin, blockIndicatorKeys);

					windowValues = new ArrayList<>();

					int matches = 0;

					for(int index = (origin - 1); index > -1; index--){

						if(matchesBlockIndicator(index, blockIndicatorMap)){
							matches++;

							Object value = Table.get(values, index);
							if(value != null){
								value = TypeUtil.cast(Number.class, value);

								windowValues.add((Number)value);
							} // End if

							if(matches == n){
								break;
							}
						}
					}
				} // End if

				if(windowValues.isEmpty()){
					return null;
				}

				switch(function){
					case "max":
						{
							return Collections.max((List)windowValues);
						}
					case "min":
						{
							return Collections.min((List)windowValues);
						}
					default:
						{
							UnivariateStatistic statistic = getStatistic(function);

							double[] doubleWindowValues = windowValues.stream()
								.mapToDouble(value -> value.doubleValue())
								.toArray();

							return statistic.evaluate(doubleWindowValues);
						}
				}
			}

			return null;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object put(String key, Object value){
			int origin = getOrigin();

			List<Object> values = (List<Object>)ensureValues(key);

			return Table.set(values, origin, value);
		}

		@Override
		public Object remove(Object key){
			int origin = getOrigin();

			List<?> values = getValues((String)key);
			if(values != null){
				return Table.set(values, origin, null);
			}

			return null;
		}

		@Override
		public Set<Map.Entry<String, Object>> entrySet(){
			int origin = getOrigin();

			Set<Map.Entry<String, Object>> result = new AbstractSet<>(){


				@Override
				public int size(){
					List<String> columns = getColumns();

					return columns.size();
				}

				@Override
				public Iterator<Map.Entry<String, Object>> iterator(){
					List<String> columns = getColumns();

					Iterator<Map.Entry<String, Object>> result = new Iterator<>(){

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
								Object value = Table.get(values, origin);

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

		private Map<String, Object> createBlockIndicator(int index, List<String> keys){
			Map<String, Object> result = new HashMap<>();

			for(int i = 0; i < keys.size(); i++){
				String key = keys.get(i);
				Object value;

				List<?> values = getValues(key);
				if(values != null){
					value = Table.get(values, index);
				} else

				{
					throw new IllegalArgumentException(key);
				}

				result.put(key, value);
			}

			return result;
		}

		private boolean matchesBlockIndicator(int index, Map<String, ?> map){

			if(map.isEmpty()){
				return true;
			}

			Collection<? extends Map.Entry<String, ?>> entries = map.entrySet();
			for(Map.Entry<String, ?> entry : entries){
				String key = entry.getKey();
				Object expectedValue = entry.getValue();

				List<?> values = getValues(key);
				if(values != null){
					Object actualValue = Table.get(values, index);

					if(!Objects.equals(expectedValue, actualValue)){
						return false;
					}
				} else

				{
					throw new IllegalArgumentException(key);
				}
			}

			return true;
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