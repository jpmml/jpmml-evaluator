/*
 * Copyright (c) 2017 Villu Ruusmann
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

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

abstract
public class Report {

	abstract
	public Report copy();

	abstract
	public void add(Entry entry);

	abstract
	public List<Entry> getEntries();

	@Override
	public String toString(){
		ToStringHelper helper = Objects.toStringHelper(this)
			.add("entries", getEntries());

		return helper.toString();
	}

	public boolean hasEntries(){
		List<Entry> entries = getEntries();

		return (entries.size() > 0);
	}

	Entry headEntry(){
		List<Entry> entries = getEntries();

		if(entries.size() < 1){
			throw new IllegalStateException();
		}

		return entries.get(0);
	}

	Entry tailEntry(){
		List<Entry> entries = getEntries();

		if(entries.size() < 1){
			throw new IllegalStateException();
		}

		return entries.get(entries.size() - 1);
	}

	static
	public class Entry {

		private String expression = null;

		private Number value = null;


		public Entry(String expression, Number value){
			setExpression(expression);
			setValue(value);
		}

		@Override
		public String toString(){
			ToStringHelper helper = Objects.toStringHelper(this)
				.add("expression", getExpression())
				.add("value", getValue());

			return helper.toString();
		}

		public String getExpression(){
			return this.expression;
		}

		private void setExpression(String expression){
			this.expression = expression;
		}

		public Number getValue(){
			return this.value;
		}

		private void setValue(Number value){
			this.value = value;
		}
	}
}