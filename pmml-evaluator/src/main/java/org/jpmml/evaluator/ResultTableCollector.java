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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

@IgnoreJRERequirement
public class ResultTableCollector extends TableCollector {

	private List<? extends ResultField> resultFields = null;

	private boolean decode = false;


	public ResultTableCollector(List<? extends ResultField> resultFields){
		this(resultFields, true);
	}

	public ResultTableCollector(List<? extends ResultField> resultFields, boolean decode){
		setResultFields(resultFields);
		setDecode(decode);
	}

	@Override
	protected Table createFinisherTable(int initialSize){
		List<? extends ResultField> resultFields = getResultFields();

		List<String> columns = resultFields.stream()
			.map(ResultField::getName)
			.collect(Collectors.toList());

		return new Table(columns, initialSize);
	}

	@Override
	protected Table.Row createFinisherRow(Table table){
		boolean decode = getDecode();

		Set<String> columns = new HashSet<>(table.getColumns());

		Table.Row result = table.new Row(0, -1){

			@Override
			public Object put(String key, Object value){

				if(!columns.contains(key)){
					return null;
				} // End if

				if(decode){
					value = EvaluatorUtil.decode(value);
				}

				return super.put(key, value);
			}
		};

		return result;
	}

	public List<? extends ResultField> getResultFields(){
		return this.resultFields;
	}

	public void setResultFields(List<? extends ResultField> resultFields){
		this.resultFields = Objects.requireNonNull(resultFields);
	}

	public boolean getDecode(){
		return this.decode;
	}

	private void setDecode(boolean decode){
		this.decode = decode;
	}
}