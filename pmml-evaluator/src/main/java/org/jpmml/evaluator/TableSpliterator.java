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

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

@IgnoreJRERequirement
public class TableSpliterator implements Spliterator<Map<String, Object>> {

	private Table table = null;

	private int origin;

	private int fence;


	public TableSpliterator(Table table){
		setTable(table);

		setOrigin(0);
		setFence(table.getNumberOfRows());
	}

	public TableSpliterator(Table table, int origin, int fence){
		setTable(table);

		setOrigin(origin);
		setFence(fence);
	}

	@Override
	public int characteristics(){
		return (Spliterator.IMMUTABLE | Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.SIZED | Spliterator.SUBSIZED);
	}

	@Override
	public long estimateSize(){
		int origin = getOrigin();
		int fence = getFence();

		return (fence - origin);
	}

	@Override
	public boolean tryAdvance(Consumer<? super Map<String, Object>> action){
		int origin = getOrigin();
		int fence = getFence();

		if(origin < fence){
			Row row = new Row(origin);

			action.accept(row);

			setOrigin(origin + 1);

			return true;
		}

		return false;
	}

	@Override
	public void forEachRemaining(Consumer<? super Map<String, Object>> action){
		int origin = getOrigin();
		int fence = getFence();

		Row row = new Row();

		while(origin < fence){
			row.setIndex(origin);

			action.accept(row);

			origin += 1;
		}

		setOrigin(origin);
	}

	@Override
	public TableSpliterator trySplit(){
		Table table = getTable();
		int origin = getOrigin();
		int fence = getFence();

		int mid = (origin + fence) >>> 1;
		if(origin < mid){
			setOrigin(mid);

			return new TableSpliterator(table, origin, mid);
		}

		return null;
	}

	public Table getTable(){
		return this.table;
	}

	private void setTable(Table table){
		this.table = Objects.requireNonNull(table);
	}

	int getOrigin(){
		return this.origin;
	}

	private void setOrigin(int origin){
		this.origin = origin;
	}

	int getFence(){
		return this.fence;
	}

	private void setFence(int fence){
		this.fence = fence;
	}

	class Row extends AbstractMap<String, Object> {

		private int index;


		Row(){
			this(-1);
		}

		Row(int index){
			setIndex(index);
		}

		@Override
		public Object get(Object key){
			Table table = getTable();
			int index = getIndex();

			List<?> values = table.getValues((String)key);
			if(values != null){
				return values.get(index);
			}

			return null;
		}

		@Override
		public Set<Map.Entry<String, Object>> entrySet(){
			throw new UnsupportedOperationException();
		}

		Table getTable(){
			return TableSpliterator.this.getTable();
		}

		int getIndex(){
			return this.index;
		}

		void setIndex(int index){
			this.index = index;
		}
	}
}