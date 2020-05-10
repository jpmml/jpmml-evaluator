/*
 * Copyright (c) 2013 Villu Ruusmann
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
package org.jpmml.evaluator.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.HasGroupFields;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.TargetField;

public class BatchUtil {

	private BatchUtil(){
	}

	static
	public List<Conflict> evaluate(Batch batch) throws Exception {
		Evaluator evaluator = batch.getEvaluator();

		List<? extends Map<FieldName, ?>> input = batch.getInput();
		List<? extends Map<FieldName, ?>> output = batch.getOutput();

		if(evaluator instanceof HasGroupFields){
			HasGroupFields hasGroupFields = (HasGroupFields)evaluator;

			input = EvaluatorUtil.groupRows(hasGroupFields, input);
		} // End if

		if(input.size() != output.size()){
			throw new IllegalArgumentException("Expected the same number of data rows, got " + input.size() + " input data rows and " + output.size() + " expected output data rows");
		}

		Predicate<ResultField> predicate = batch.getPredicate();

		Set<FieldName> names = new LinkedHashSet<>();

		List<TargetField> targetFields = evaluator.getTargetFields();
		for(TargetField targetField : targetFields){

			if(targetField.isSynthetic()){
				continue;
			} // End if

			if(predicate.test(targetField)){
				names.add(targetField.getName());
			}
		}

		List<OutputField> outputFields = evaluator.getOutputFields();
		for(OutputField outputField : outputFields){

			if(predicate.test(outputField)){
				names.add(outputField.getName());
			}
		}

		Equivalence<Object> equivalence = batch.getEquivalence();

		List<Conflict> conflicts = new ArrayList<>();

		for(int i = 0; i < input.size(); i++){
			Map<FieldName, ?> arguments = input.get(i);

			Map<FieldName, ?> expectedResults = output.get(i);
			expectedResults = Maps.filterKeys(expectedResults, names::contains);

			try {
				Map<FieldName, ?> actualResults = evaluator.evaluate(arguments);
				actualResults = Maps.filterKeys(actualResults, names::contains);

				MapDifference<FieldName, ?> difference = Maps.<FieldName, Object>difference(expectedResults, actualResults, equivalence);
				if(!difference.areEqual()){
					Conflict conflict = new Conflict(i, arguments, difference);

					conflicts.add(conflict);
				}
			} catch(Exception e){
				Conflict conflict = new Conflict(i, arguments, e);

				conflicts.add(conflict);
			}
		}

		return conflicts;
	}

	static
	public List<Map<FieldName, String>> parseRecords(List<List<String>> table, Function<String, String> function){
		List<Map<FieldName, String>> records = new ArrayList<>(table.size() - 1);

		List<String> headerRow = table.get(0);

		Set<String> uniqueHeaderRow = new LinkedHashSet<>(headerRow);
		if(uniqueHeaderRow.size() < headerRow.size()){
			Set<String> duplicateHeaderCells = new LinkedHashSet<>();

			for(int column = 0; column < headerRow.size(); column++){
				String headerCell = headerRow.get(column);

				if(Collections.frequency(headerRow, headerCell) != 1){
					duplicateHeaderCells.add(headerCell);
				}
			}

			if(!duplicateHeaderCells.isEmpty()){
				throw new IllegalArgumentException("Expected unique cell names, got non-unique cell name(s) " + duplicateHeaderCells);
			}
		}

		for(int row = 1; row < table.size(); row++){
			List<String> bodyRow = table.get(row);

			if(headerRow.size() != bodyRow.size()){
				throw new IllegalArgumentException("Expected " + headerRow.size() + " cells, got " + bodyRow.size() + " cells (data record " + (row - 1) + ")");
			}

			Map<FieldName, String> record = new LinkedHashMap<>();

			for(int column = 0; column < headerRow.size(); column++){
				FieldName name = FieldName.create(headerRow.get(column));
				String value = function.apply(bodyRow.get(column));

				record.put(name, value);
			}

			records.add(record);
		}

		return records;
	}

	static
	public List<List<String>> formatRecords(List<Map<FieldName, ?>> records, List<FieldName> names, Function<Object, String> function){
		List<List<String>> table = new ArrayList<>(1 + records.size());

		List<String> headerRow = new ArrayList<>(names.size());

		for(FieldName name : names){
			headerRow.add(name != null ? name.getValue() : "(null)");
		}

		table.add(headerRow);

		for(Map<FieldName, ?> record : records){
			List<String> bodyRow = new ArrayList<>(names.size());

			for(FieldName name : names){
				bodyRow.add(function.apply(record.get(name)));
			}

			table.add(bodyRow);
		}

		return table;
	}
}
