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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;

import com.beust.jcommander.Parameter;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.manager.PMMLManager;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.xml.sax.InputSource;

public class CsvEvaluationExample extends Example {

	@Parameter (
		names = {"--model"},
		description = "PMML file",
		required = true
	)
	private File model = null;

	@Parameter (
		names = {"--input"},
		description = "Input CSV file",
		required = true
	)
	private File input = null;

	@Parameter (
		names = {"--output"},
		description = "Output CSV file",
		required = true
	)
	private File output = null;

	@Parameter (
		names = {"--separator"},
		description = "CSV cell separator character"
	)
	private String separator = null;


	static
	public void main(String... args) throws Exception {
		execute(CsvEvaluationExample.class, args);
	}

	@Override
	public void execute() throws Exception {
		PMML pmml;

		InputStream is = new FileInputStream(this.model);

		try {
			Source source = ImportFilter.apply(new InputSource(is));

			pmml = JAXBUtil.unmarshalPMML(source);
		} finally {
			is.close();
		}

		PMMLManager pmmlManager = new PMMLManager(pmml);

		Evaluator evaluator = (Evaluator)pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());

		CsvUtil.Table inputTable = CsvUtil.readTable(this.input, this.separator);

		List<Map<FieldName, FieldValue>> argumentsList = prepareAll(evaluator, inputTable);

		List<Map<FieldName, ?>> resultList = evaluateAll(evaluator, argumentsList);

		// Check if the input table and the output table have equal number of rows
		boolean copyCells = (argumentsList.size() == (inputTable.size() - 1));

		CsvUtil.Table outputTable = new CsvUtil.Table();
		outputTable.setSeparator(inputTable.getSeparator());

		List<FieldName> fields = new ArrayList<FieldName>();
		fields.addAll(evaluator.getTargetFields());
		fields.addAll(evaluator.getOutputFields());

		header:
		{
			List<String> headerRow = new ArrayList<String>();

			if(copyCells){
				headerRow.addAll(inputTable.get(0));
			}

			for(FieldName field : fields){
				headerRow.add(field.getValue());
			}

			outputTable.add(headerRow);
		}

		body:
		for(int i = 0; i < resultList.size(); i++){
			List<String> bodyRow = new ArrayList<String>();

			if(copyCells){
				bodyRow.addAll(inputTable.get(i + 1));
			}

			Map<FieldName, ?> result = resultList.get(i);

			for(FieldName field : fields){
				Object value = EvaluatorUtil.decode(result.get(field));

				bodyRow.add(String.valueOf(value));
			}

			outputTable.add(bodyRow);
		}

		CsvUtil.writeTable(outputTable, this.output);
	}

	static
	private List<Map<FieldName, FieldValue>> prepareAll(Evaluator evaluator, CsvUtil.Table table){
		List<FieldName> names = new ArrayList<FieldName>();

		List<FieldName> activeFields = evaluator.getActiveFields();
		List<FieldName> groupFields = evaluator.getGroupFields();

		header:
		{
			List<String> headerRow = table.get(0);
			for(int column = 0; column < headerRow.size(); column++){
				FieldName field = FieldName.create(headerRow.get(column));

				if(!(activeFields.contains(field) || groupFields.contains(field))){
					field = null;
				}

				names.add(field);
			}
		}

		List<Map<FieldName, Object>> stringRows = new ArrayList<Map<FieldName, Object>>();

		body:
		for(int row = 1; row < table.size(); row++){
			List<String> bodyRow = table.get(row);

			Map<FieldName, Object> stringRow = new LinkedHashMap<FieldName, Object>();

			for(int column = 0; column < bodyRow.size(); column++){
				FieldName name = names.get(column);
				if(name == null){
					continue;
				}

				String value = bodyRow.get(column);
				if(("").equals(value) || ("NA").equals(value) || ("N/A").equals(value)){
					value = null;
				}

				stringRow.put(name, value);
			}

			stringRows.add(stringRow);
		}

		if(groupFields.size() == 1){
			FieldName groupField = groupFields.get(0);

			stringRows = EvaluatorUtil.groupRows(groupField, stringRows);
		} else

		if(groupFields.size() > 1){
			throw new EvaluationException();
		}

		List<Map<FieldName, FieldValue>> fieldValueRows = new ArrayList<Map<FieldName, FieldValue>>();

		for(Map<FieldName, Object> stringRow : stringRows){
			Map<FieldName, FieldValue> fieldValueRow = new LinkedHashMap<FieldName, FieldValue>();

			Collection<Map.Entry<FieldName, Object>> entries = stringRow.entrySet();
			for(Map.Entry<FieldName, Object> entry : entries){
				FieldName name = entry.getKey();
				FieldValue value = EvaluatorUtil.prepare(evaluator, name, entry.getValue());

				fieldValueRow.put(name, value);
			}

			fieldValueRows.add(fieldValueRow);
		}

		return fieldValueRows;
	}

	static
	private List<Map<FieldName, ?>> evaluateAll(Evaluator evaluator, List<Map<FieldName, FieldValue>> argumentsList){
		List<Map<FieldName, ?>> resultList = new ArrayList<Map<FieldName, ?>>();

		for(Map<FieldName, FieldValue> arguments : argumentsList){
			Map<FieldName, ?> result = evaluator.evaluate(arguments);

			resultList.add(result);
		}

		return resultList;
	}
}