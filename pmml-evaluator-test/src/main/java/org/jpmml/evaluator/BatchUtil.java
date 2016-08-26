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
package org.jpmml.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.dmg.pmml.FieldName;

public class BatchUtil {

	private BatchUtil(){
	}

	static
	public List<Conflict> evaluate(Batch batch, Set<FieldName> ignoredFields, final double precision, final double zeroThreshold) throws Exception {
		Evaluator evaluator = batch.getEvaluator();

		List<? extends Map<FieldName, ?>> input = batch.getInput();
		List<? extends Map<FieldName, ?>> output = batch.getOutput();

		List<InputField> groupFields = evaluator.getGroupFields();

		if(groupFields.size() == 1){
			InputField groupField = groupFields.get(0);

			input = EvaluatorUtil.groupRows(groupField.getName(), input);
		} else

		if(groupFields.size() > 1){
			throw new EvaluationException();
		}

		Equivalence<Object> equivalence = new Equivalence<Object>(){

			@Override
			public boolean doEquivalent(Object expected, Object actual){
				actual = EvaluatorUtil.decode(actual);

				return VerificationUtil.acceptable(TypeUtil.parseOrCast(TypeUtil.getDataType(actual), expected), actual, precision, zeroThreshold);
			}

			@Override
			public int doHash(Object object){
				return object.hashCode();
			}
		};

		if(output.size() > 0){

			if(input.size() != output.size()){
				throw new EvaluationException();
			}

			List<Conflict> conflicts = new ArrayList<>();

			for(int i = 0; i < input.size(); i++){
				Map<FieldName, ?> arguments = input.get(i);

				Map<FieldName, ?> result = evaluator.evaluate(arguments);

				if(result.containsKey(Consumer.DEFAULT_TARGET_NAME)){
					result = new LinkedHashMap<>(result);

					result.remove(Consumer.DEFAULT_TARGET_NAME);
				} // End if

				if(ignoredFields != null && ignoredFields.size() > 0){
					result = new LinkedHashMap<>(result);

					Set<FieldName> fields = result.keySet();
					fields.removeAll(ignoredFields);
				}

				MapDifference<FieldName, ?> difference = Maps.<FieldName, Object>difference(output.get(i), result, equivalence);
				if(!difference.areEqual()){
					Conflict conflict = new Conflict(i, arguments, difference);

					conflicts.add(conflict);
				}
			}

			return conflicts;
		} else

		{
			for(int i = 0; i < input.size(); i++){
				Map<FieldName, ?> arguments = input.get(i);

				evaluator.evaluate(arguments);
			}

			return Collections.emptyList();
		}
	}

	/**
	 * <p>
	 * Evaluates the model using empty arguments.
	 * </p>
	 *
	 * @return The value of the target field.
	 */
	static
	public Object evaluateDefault(Batch batch) throws Exception {
		Evaluator evaluator = batch.getEvaluator();

		List<TargetField> targetFields = evaluator.getTargetFields();
		if(targetFields.size() != 1){
			throw new EvaluationException();
		}

		TargetField targetField = targetFields.get(0);

		Map<FieldName, ?> arguments = Collections.emptyMap();

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		return result.get(targetField.getName());
	}

	static
	public List<Map<FieldName, String>> parseRecords(List<List<String>> table, Function<String, String> function){
		List<Map<FieldName, String>> records = new ArrayList<>(table.size() - 1);

		List<String> headerRow = table.get(0);

		Set<String> uniqueHeaderRow = new LinkedHashSet<>(headerRow);

		if(uniqueHeaderRow.size() < headerRow.size()){
			List<String> duplicateCells = new ArrayList<>();

			for(int j = 0; j < headerRow.size(); j++){
				String cell = headerRow.get(j);

				if(Collections.frequency(headerRow, cell) != 1){
					duplicateCells.add(cell);
				}
			}

			throw new IllegalArgumentException("Expected unique cell names, but got non-unique cell name(s) " + duplicateCells);
		}

		for(int i = 1; i < table.size(); i++){
			List<String> bodyRow = table.get(i);

			if(headerRow.size() != bodyRow.size()){
				throw new IllegalArgumentException("Expected " + headerRow.size() + " cells, but got " + bodyRow.size() + " cells (data record " + (i - 1) + ")");
			}

			Map<FieldName, String> record = new LinkedHashMap<>();

			for(int j = 0; j < headerRow.size(); j++){
				record.put(FieldName.create(headerRow.get(j)), function.apply(bodyRow.get(j)));
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