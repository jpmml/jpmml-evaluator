/*
 * Copyright (c) 2014 Villu Ruusmann
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.beust.jcommander.Parameter;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.Model;
import org.dmg.pmml.ModelVerification;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Row;
import org.dmg.pmml.VerificationField;
import org.dmg.pmml.VerificationFields;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EnhancementExample extends Example {

	@Parameter (
		names = {"--model"},
		description = "PMML file",
		required = true
	)
	@ParameterOrder (
		value = 1
	)
	private File model = null;

	@Parameter (
		names = {"--verification"},
		description = "Verification CSV file. Verification data is a combination of input and expected output data",
		required = true
	)
	@ParameterOrder (
		value = 2
	)
	private File verification = null;

	@Parameter (
		names = {"--separator"},
		description = "CSV cell separator character",
		converter = SeparatorConverter.class
	)
	@ParameterOrder (
		value = 3
	)
	private String separator = null;

	@Parameter (
		names = {"--missing-values"},
		description = "CSV missing value strings"
	)
	@ParameterOrder (
		value = 4
	)
	private List<String> missingValues = Arrays.asList("N/A", "NA");


	static
	public void main(String... args) throws Exception {
		execute(EnhancementExample.class, args);
	}

	@Override
	public void execute() throws Exception {
		PMML pmml = readPMML(this.model);

		CsvUtil.Table table = readTable(this.verification, this.separator);

		ModelEvaluatorBuilder modelEvaluatorBuilder = new ModelEvaluatorBuilder(pmml);

		ModelEvaluator<?> modelEvaluator = modelEvaluatorBuilder.build();

		Model model = modelEvaluator.getModel();

		ModelVerification modelVerification = model.getModelVerification();
		if(modelVerification != null){
			throw new IllegalArgumentException("Model verification data is already defined");
		}

		java.util.function.Function<String, String> cellParser = createCellParser(new HashSet<>(this.missingValues));

		modelVerification = new ModelVerification();

		List<String> tagNames = new ArrayList<>();

		VerificationFields verificationFields = new VerificationFields();

		header:
		{
			List<String> headerRow = table.get(0);

			for(int column = 0; column < headerRow.size(); column++){
				String field = headerRow.get(column);

				FieldName name = FieldName.create(field);

				MiningField miningField = modelEvaluator.getMiningField(name);
				if(miningField == null){
					OutputField outputField = modelEvaluator.getOutputField(name);

					if(outputField == null){
						tagNames.add(null);

						continue;
					}
				}

				VerificationField verificationField = new VerificationField(name);

				if(field.contains(" ")){
					verificationField.setColumn(field.replace(" ", "_x0020_"));

					tagNames.add(verificationField.getColumn());
				} else

				{
					tagNames.add(field);
				}

				verificationFields.addVerificationFields(verificationField);
			}
		}

		modelVerification.setVerificationFields(verificationFields);

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);

		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

		Document document = documentBuilder.newDocument();

		InlineTable inlineTable = new InlineTable();

		body:
		for(int i = 1; i < table.size(); i++){
			List<String> bodyRow = table.get(i);

			Row row = new Row();

			for(int column = 0; column < bodyRow.size(); column++){
				String tagName = tagNames.get(column);

				if(tagName == null){
					continue;
				}

				String value = bodyRow.get(column);
				if(value != null){
					value = cellParser.apply(value);
				} // End if

				if(value == null){
					continue;
				}

				Element element = document.createElementNS("http://jpmml.org/jpmml-model/InlineTable", ("data:" + tagName));
				element.setTextContent(value);

				row.addContent(element);
			}

			inlineTable.addRows(row);
		}

		modelVerification.setInlineTable(inlineTable);

		model.setModelVerification(modelVerification);

		writePMML(pmml, this.model);
	}
}