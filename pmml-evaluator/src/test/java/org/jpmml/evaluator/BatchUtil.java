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

import java.util.*;

import com.google.common.base.*;
import com.google.common.collect.*;

import org.dmg.pmml.*;

public class BatchUtil {

	private BatchUtil(){
	}

	/**
	 * @see #evaluate(Batch, double, double)
	 */
	static
	public boolean evaluate(Batch batch) throws Exception {
		return evaluate(batch, BatchUtil.precision, BatchUtil.zeroThreshold);
	}

	/**
	 * Evaluates the model using arguments from the specified CSV resource.
	 *
	 * @return <code>true</code> If there were no differences between expected and actual results, <code>false</code> otherwise.
	 */
	static
	public boolean evaluate(Batch batch, double precision, double zeroThreshold) throws Exception {
		List<MapDifference<FieldName, ?>> differences = difference(batch, precision, zeroThreshold);

		return Iterables.isEmpty(differences);
	}

	/**
	 * Evaluates the model using empty arguments.
	 *
	 * @return The value of the target field.
	 */
	static
	public Object evaluateDefault(Batch batch) throws Exception {
		Evaluator evaluator = PMMLUtil.createModelEvaluator(batch.getModel());

		Map<FieldName, ?> arguments = Collections.emptyMap();

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		return result.get(evaluator.getTargetField());
	}

	static
	public List<MapDifference<FieldName, ?>> difference(Batch batch, final double precision, final double zeroThreshold) throws Exception {
		List<Map<FieldName, String>> input = CsvUtil.load(batch.getInput());
		List<Map<FieldName, String>> output = CsvUtil.load(batch.getOutput());

		Evaluator evaluator = PMMLUtil.createModelEvaluator(batch.getModel());

		List<FieldName> activeFields = evaluator.getActiveFields();
		List<FieldName> groupFields = evaluator.getGroupFields();
		List<FieldName> targetFields = evaluator.getTargetFields();
		List<FieldName> outputFields = evaluator.getOutputFields();

		List<? extends Map<FieldName, ?>> tableInput = input;

		if(groupFields.size() == 1){
			FieldName groupField = groupFields.get(0);

			tableInput = EvaluatorUtil.groupRows(groupField, input);
		} else

		if(groupFields.size() > 1){
			throw new EvaluationException();
		}

		List<Map<FieldName, FieldValue>> table = Lists.newArrayList();

		List<FieldName> argumentFields = Lists.newArrayList(Iterables.concat(activeFields, groupFields));

		for(int i = 0; i < tableInput.size(); i++){
			Map<FieldName, ?> inputRow = tableInput.get(i);

			Map<FieldName, FieldValue> arguments = Maps.newLinkedHashMap();

			for(FieldName argumentField : argumentFields){
				Object inputCell = inputRow.get(argumentField);

				FieldValue value = EvaluatorUtil.prepare(evaluator, argumentField, inputCell);

				arguments.put(argumentField, value);
			}

			table.add(arguments);
		}

		Equivalence<Object> equivalence = new Equivalence<Object>(){

			@Override
			public boolean doEquivalent(Object expected, Object actual){
				actual = EvaluatorUtil.decode(actual);

				if("NA".equals(expected) || "N/A".equals(expected)){
					return true;
				}

				return VerificationUtil.acceptable(TypeUtil.parseOrCast(TypeUtil.getDataType(actual), expected), actual, precision, zeroThreshold);
			}

			@Override
			public int doHash(Object object){
				return object.hashCode();
			}
		};

		if(output.size() > 0){

			if(table.size() != output.size()){
				throw new EvaluationException();
			}

			List<MapDifference<FieldName, ?>> differences = Lists.newArrayList();

			for(int i = 0; i < output.size(); i++){
				Map<FieldName, String> outputRow = output.get(i);

				Map<FieldName, ?> arguments = table.get(i);

				Map<FieldName, ?> result = evaluator.evaluate(arguments);

				// Delete the synthetic target field
				if(targetFields.size() == 0){
					result = Maps.newLinkedHashMap(result);

					result.remove(evaluator.getTargetField());
				}

				MapDifference<FieldName, Object> difference = Maps.<FieldName, Object>difference(outputRow, result, equivalence);
				if(!difference.areEqual()){
					differences.add(difference);
				}
			}

			return differences;
		} else

		{
			for(int i = 0; i < table.size(); i++){
				Map<FieldName, ?> arguments = table.get(i);

				evaluator.evaluate(arguments);
			}

			return Collections.emptyList();
		}
	}

	// One part per million parts
	private static final double precision = 1d / (1000 * 1000);

	private static final double zeroThreshold = precision;
}