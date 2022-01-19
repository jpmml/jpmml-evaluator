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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
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

		List<? extends Map<String, ?>> input = batch.getInput();
		List<? extends Map<String, ?>> output = batch.getOutput();

		if(evaluator instanceof HasGroupFields){
			HasGroupFields hasGroupFields = (HasGroupFields)evaluator;

			input = EvaluatorUtil.groupRows(hasGroupFields, input);
		} // End if

		if(input.size() != output.size()){
			throw new IllegalArgumentException("Expected the same number of data rows, got " + input.size() + " input data rows and " + output.size() + " expected output data rows");
		}

		Predicate<ResultField> columnFilter = batch.getColumnFilter();

		Set<String> names = new LinkedHashSet<>();

		List<TargetField> targetFields = evaluator.getTargetFields();
		for(TargetField targetField : targetFields){

			if(targetField.isSynthetic()){
				continue;
			} // End if

			if(columnFilter.test(targetField)){
				names.add(targetField.getName());
			}
		}

		List<OutputField> outputFields = evaluator.getOutputFields();
		for(OutputField outputField : outputFields){

			if(columnFilter.test(outputField)){
				names.add(outputField.getName());
			}
		}

		Equivalence<Object> equivalence = batch.getEquivalence();

		List<Conflict> conflicts = new ArrayList<>();

		for(int i = 0; i < input.size(); i++){
			Map<String, ?> arguments = input.get(i);

			Map<String, ?> expectedResults = output.get(i);
			expectedResults = Maps.filterKeys(expectedResults, names::contains);

			try {
				Map<String, ?> actualResults = evaluator.evaluate(arguments);
				actualResults = Maps.filterKeys(actualResults, names::contains);

				MapDifference<String, ?> difference = Maps.<String, Object>difference(expectedResults, actualResults, equivalence);
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
}
