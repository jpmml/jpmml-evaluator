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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

@IgnoreJRERequirement
public class ColumnCollector implements Collector<Object, List<Object>, Table> {

	private Table table = null;

	private String column = null;


	public ColumnCollector(Table table, String column){
		setTable(table);
		setColumn(column);
	}

	@Override
	public Set<Collector.Characteristics> characteristics(){
		return Collections.emptySet();
	}

	@Override
	public Supplier<List<Object>> supplier(){
		return () -> {
			return new ArrayList<>();
		};
	}

	@Override
	public BiConsumer<List<Object>, Object> accumulator(){
		return (elements, element) -> {
			elements.add(element);
		};
	}

	@Override
	public BinaryOperator<List<Object>> combiner(){
		return (left, right) -> {
			left.addAll(right);

			return left;
		};
	}

	@Override
	public Function<List<Object>, Table> finisher(){
		return (elements) -> {
			Table table = getTable();
			String column = getColumn();

			if((table.getNumberOfColumns() > 0) && (elements.size() != table.getNumberOfRows())){
				throw new IllegalArgumentException();
			}

			List<?> prevValues = table.getValues(column);

			Table.Row row = null;

			for(Object element : elements){

				if(row == null){
					row = createFinisherRow(table);
				} // End if

				if(element instanceof Exception){
					Exception exception = (Exception)element;

					row.setException(exception);

					if(prevValues != null){
						row.remove(column);
					}
				} else

				{
					row.put(column, element);
				}

				row.advance();
			}

			table.canonicalize();

			return table;
		};
	}

	protected Table.Row createFinisherRow(Table table){
		return table.createWriterRow(0);
	}

	public Table getTable(){
		return this.table;
	}

	private void setTable(Table table){
		this.table = Objects.requireNonNull(table);
	}

	public String getColumn(){
		return this.column;
	}

	private void setColumn(String column){
		this.column = column;
	}
}