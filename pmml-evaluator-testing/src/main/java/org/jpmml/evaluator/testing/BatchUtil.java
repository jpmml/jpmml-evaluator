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
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorFunction;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.HasGroupFields;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.Table;
import org.jpmml.evaluator.TableCollector;
import org.jpmml.evaluator.TargetField;

public class BatchUtil {

	private BatchUtil(){
	}

	static
	public List<Conflict> evaluate(Batch batch) throws Exception {
		Evaluator evaluator = batch.getEvaluator();

		Table input = batch.getInput();

		if(input.getNumberOfRows() == 0){
			return Collections.emptyList();
		} // End if

		if(evaluator instanceof HasGroupFields){
			HasGroupFields hasGroupFields = (HasGroupFields)evaluator;

			input = EvaluatorUtil.groupRows(hasGroupFields, input);
		}

		Table expectedOutput = batch.getOutput();

		if(input.getNumberOfRows() != expectedOutput.getNumberOfRows()){
			throw new IllegalArgumentException("Expected the same number of data rows, got " + input.getNumberOfRows() + " input data rows and " + expectedOutput.getNumberOfRows() + " expected output data rows");
		}

		Set<String> columns = collectResultColumns(evaluator, batch.getColumnFilter());

		Equivalence<Object> equivalence = batch.getEquivalence();

		Table actualOutput = input.stream()
			.map(new EvaluatorFunction(evaluator))
			.collect(new TableCollector());

		if(expectedOutput.getNumberOfRows() != actualOutput.getNumberOfRows()){
			throw new IllegalArgumentException("Expected the same number of output data rows, got " + expectedOutput.getNumberOfRows() + " expected output data rows and " + actualOutput.getNumberOfRows() + " actual output data rows");
		}

		List<Conflict> conflicts = new ArrayList<>();

		Table.Row arguments = input.createReaderRow(0);
		Table.Row expectedResults = expectedOutput.createReaderRow(0);
		Table.Row actualResults = actualOutput.createReaderRow(0);

		for(int i = 0, max = expectedOutput.getNumberOfRows(); i < max; i++){
			Exception exception = actualResults.getException();

			if(exception != null){
				Conflict conflict = new Conflict(i, new LinkedHashMap<>(arguments), exception);

				conflicts.add(conflict);
			} else

			{
				MapDifference<String, ?> difference = filteredDifference(expectedResults, actualResults, columns, equivalence);
				if(!difference.areEqual()){
					Conflict conflict = new Conflict(i, new LinkedHashMap<>(arguments), difference);

					conflicts.add(conflict);
				}
			}

			arguments.advance();
			expectedResults.advance();
			actualResults.advance();
		}

		return conflicts;
	}

	static
	public List<Conflict> evaluateSingleton(Batch batch, Function<Map<String, ?>, Table> resultsExpander) throws Exception {
		Evaluator evaluator = batch.getEvaluator();

		Table input = batch.getInput();

		if(evaluator instanceof HasGroupFields){
			HasGroupFields hasGroupFields = (HasGroupFields)evaluator;

			input = EvaluatorUtil.groupRows(hasGroupFields, input);
		} // End if

		if(input.getNumberOfRows() != 1){
			throw new IllegalArgumentException("Expected exactly one input data row, got " + input.getNumberOfRows() + " input data rows");
		}

		Table expectedOutput = batch.getOutput();

		Set<String> columns = collectResultColumns(evaluator, batch.getColumnFilter());

		Equivalence<Object> equivalence = batch.getEquivalence();

		int i = 0;

		Map<String, ?> arguments = input.createReaderRow(i);
		Map<String, ?> results;

		try {
			results = evaluator.evaluate(arguments);
		} catch(Exception e){
			Conflict conflict = new Conflict(i, new LinkedHashMap<>(arguments), e);

			return Collections.singletonList(conflict);
		}

		Table actualOutput = resultsExpander.apply(results);

		if(expectedOutput.getNumberOfRows() != actualOutput.getNumberOfRows()){
			throw new IllegalArgumentException("Expected the same number of output data rows, got " + expectedOutput.getNumberOfRows() + " expected output data rows and " + actualOutput.getNumberOfRows() + " actual output data rows");
		}

		List<Conflict> conflicts = new ArrayList<>();

		Table.Row expectedResults = expectedOutput.createReaderRow(i);
		Table.Row actualResults = actualOutput.createReaderRow(i);

		for(int j = 0, max = expectedOutput.getNumberOfRows(); j < max; j++){
			MapDifference<String, Object> difference = filteredDifference(expectedResults, actualResults, columns, equivalence);

			if(!difference.areEqual()){
				Conflict conflict = new Conflict(i + "/" + j, new LinkedHashMap<>(arguments), difference);

				conflicts.add(conflict);
			}

			expectedResults.advance();
			actualResults.advance();
		}

		return conflicts;
	}

	static
	private <K> MapDifference<K, Object> filteredDifference(Map<K, ?> expectedResults, Map<K, ?> actualResults, Set<K> columns, Equivalence<Object> equivalence){
		Map<K, ?> filteredExpectedResults = Maps.filterKeys(expectedResults, columns::contains);
		Map<K, ?> filteredActualResults = Maps.filterKeys(actualResults, columns::contains);

		return Maps.difference(filteredExpectedResults, filteredActualResults, equivalence);
	}

	static
	private Set<String> collectResultColumns(Evaluator evaluator, Predicate<ResultField> columnFilter){
		Set<String> result = new LinkedHashSet<>();

		List<TargetField> targetFields = evaluator.getTargetFields();
		for(TargetField targetField : targetFields){

			if(targetField.isSynthetic()){
				continue;
			} // End if

			if(columnFilter.test(targetField)){
				result.add(targetField.getName());
			}
		}

		List<OutputField> outputFields = evaluator.getOutputFields();
		for(OutputField outputField : outputFields){

			if(columnFilter.test(outputField)){
				result.add(outputField.getName());
			}
		}

		return result;
	}
}
