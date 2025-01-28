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

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

@IgnoreJRERequirement
public class TableSpliterator implements Spliterator<Table.Row> {

	private Table table = null;

	private Table.Row row = null;


	public TableSpliterator(Table table){
		setTable(table);
	}

	public TableSpliterator init(){
		Table table = getTable();

		setRow(table.createReaderRow(0));

		return this;
	}

	public TableSpliterator init(int origin, int fence){
		Table table = getTable();

		setRow(table.createReaderRow(origin, fence));

		return this;
	}

	@Override
	public int characteristics(){
		return (Spliterator.IMMUTABLE | Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.SIZED | Spliterator.SUBSIZED);
	}

	@Override
	public long estimateSize(){
		Table.Row row = ensureRow();

		return row.estimateAdvances();
	}

	@Override
	public boolean tryAdvance(Consumer<? super Table.Row> action){
		Table.Row row = ensureRow();

		if(row.canAdvance()){
			action.accept(row);

			row.advance();

			return true;
		}

		return false;
	}

	@Override
	public void forEachRemaining(Consumer<? super Table.Row> action){
		Table.Row row = ensureRow();

		while(row.canAdvance()){
			action.accept(row);

			row.advance();
		}
	}

	@Override
	public TableSpliterator trySplit(){
		Table.Row row = ensureRow();

		int origin = row.getOrigin();
		int fence = row.getFence();

		int mid = (origin + fence) >>> 1;
		if(origin < mid){
			Table table = getTable();

			row.setOrigin(mid);

			return new TableSpliterator(table)
				.init(origin, mid);
		}

		return null;
	}

	private Table.Row ensureRow(){
		Table.Row row = getRow();

		if(row == null){
			throw new IllegalStateException();
		}

		return row;
	}

	public Table getTable(){
		return this.table;
	}

	private void setTable(Table table){
		this.table = Objects.requireNonNull(table);
	}

	private Table.Row getRow(){
		return this.row;
	}

	private void setRow(Table.Row row){
		this.row = Objects.requireNonNull(row);
	}
}