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

import java.io.*;
import java.util.*;

import javax.xml.transform.*;

import org.jpmml.manager.*;
import org.jpmml.model.*;

import org.dmg.pmml.*;

import com.google.common.collect.*;

import org.xml.sax.*;

public class BatchUtil {

	private BatchUtil(){
	}

	/**
	 * @return <code>true</code> If all evaluations succeeded, <code>false</code> otherwise.
	 */
	static
	public boolean evaluate(Batch batch) throws Exception {
		InputStream is = batch.getModel();

		Source source = ImportFilter.apply(new InputSource(is));

		PMML pmml = JAXBUtil.unmarshalPMML(source);

		PMMLManager pmmlManager = new PMMLManager(pmml);

		ModelManager<?> modelManager = pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());

		List<Map<FieldName, String>> input = CsvUtil.load(batch.getInput());
		List<Map<FieldName, String>> output = CsvUtil.load(batch.getOutput());

		Evaluator evaluator = (Evaluator)modelManager;

		List<Map<FieldName, Object>> table = Lists.newArrayList();

		List<FieldName> activeFields = evaluator.getActiveFields();
		List<FieldName> groupFields = evaluator.getGroupFields();
		List<FieldName> targetFields = evaluator.getTargetFields();

		List<FieldName> outputFields = evaluator.getOutputFields();

		List<FieldName> inputFields = Lists.newArrayList();
		inputFields.addAll(activeFields);
		inputFields.addAll(groupFields);

		for(int i = 0; i < input.size(); i++){
			Map<FieldName, String> inputRow = input.get(i);

			Map<FieldName, Object> arguments = Maps.newLinkedHashMap();

			for(FieldName inputField : inputFields){
				String inputCell = inputRow.get(inputField);

				Object inputValue = evaluator.prepare(inputField, inputCell);

				arguments.put(inputField, inputValue);
			}

			table.add(arguments);
		}

		if(groupFields.size() == 1){
			FieldName groupField = groupFields.get(0);

			table = EvaluatorUtil.groupRows(groupField, table);
		} else

		if(groupFields.size() > 1){
			throw new EvaluationException();
		} // End if

		if(output.isEmpty()){

			for(int i = 0; i < table.size(); i++){
				Map<FieldName, ?> arguments = table.get(i);

				evaluator.evaluate(arguments);
			}

			return true;
		} else

		{
			if(table.size() != output.size()){
				throw new EvaluationException();
			}

			boolean success = true;

			for(int i = 0; i < output.size(); i++){
				Map<FieldName, String> outputRow = output.get(i);

				Map<FieldName, ?> arguments = table.get(i);

				Map<FieldName, ?> result = evaluator.evaluate(arguments);

				for(FieldName targetField : targetFields){
					String outputCell = outputRow.get(targetField);

					Object targetValue = EvaluatorUtil.decode(result.get(targetField));

					success &= acceptable(outputCell, targetValue);
				}

				for(FieldName outputField : outputFields){
					String outputCell = outputRow.get(outputField);

					Object outputValue = result.get(outputField);

					success &= (outputCell != null ? acceptable(outputCell, outputValue) : acceptable(outputValue));
				}
			}

			return success;
		}
	}

	static
	private boolean acceptable(Object actual){
		return (actual != null);
	}

	static
	private boolean acceptable(String expected, Object actual){
		return VerificationUtil.acceptable(TypeUtil.parse(TypeUtil.getDataType(actual), expected), actual, BatchUtil.precision, BatchUtil.zeroThreshold);
	}

	// One part per million parts
	private static final double precision = 1d / (1000 * 1000);

	private static final double zeroThreshold = precision;
}