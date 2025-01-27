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
package org.jpmml.evaluator.example;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.beust.jcommander.Parameter;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.Model;
import org.dmg.pmml.ModelVerification;
import org.dmg.pmml.NamespacePrefixes;
import org.dmg.pmml.NamespaceURIs;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Row;
import org.dmg.pmml.VerificationField;
import org.dmg.pmml.VerificationFields;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorBuilder;
import org.jpmml.evaluator.Table;
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
	private char separator = ',';

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

		ModelEvaluatorBuilder modelEvaluatorBuilder = new ModelEvaluatorBuilder(pmml);

		ModelEvaluator<?> modelEvaluator = modelEvaluatorBuilder.build();

		Model model = modelEvaluator.getModel();

		ModelVerification modelVerification = model.getModelVerification();
		if(modelVerification != null){
			throw new IllegalArgumentException("Model verification data is already defined");
		}

		Function<String, String> cellParser = createCellParser(new HashSet<>(this.missingValues));

		Table table = readTable(this.verification, this.separator);
		table.apply(cellParser);

		List<String> columns = table.getColumns();

		modelVerification = new ModelVerification();

		List<String> tagNames = new ArrayList<>();

		VerificationFields verificationFields = new VerificationFields();

		header:
		{
			for(int j = 0; j < columns.size(); j++){
				String fieldName = columns.get(j);

				MiningField miningField = modelEvaluator.getMiningField(fieldName);
				if(miningField == null){
					OutputField outputField = modelEvaluator.getOutputField(fieldName);

					if(outputField == null){
						tagNames.add(null);

						continue;
					}
				}

				VerificationField verificationField = new VerificationField(fieldName);

				if(fieldName.contains(" ")){
					verificationField.setColumn(fieldName.replace(" ", "_x0020_"));

					tagNames.add(verificationField.getColumn());
				} else

				{
					tagNames.add(fieldName);
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

		Table.Row results = table.new Row(0);

		body:
		for(int i = 0, max = table.getNumberOfRows(); i < max; i++){
			Row row = new Row();

			for(int j = 0; j < columns.size(); j++){
				String fieldName = columns.get(j);
				String tagName = tagNames.get(j);

				if(tagName == null){
					continue;
				}

				String value = (String)results.get(fieldName);
				if(value != null){
					value = cellParser.apply(value);
				} // End if

				if(value == null){
					continue;
				}

				Element element = document.createElementNS(NamespaceURIs.JPMML_INLINETABLE, (NamespacePrefixes.JPMML_INLINETABLE + ":" + tagName));
				element.setTextContent(value);

				row.addContent(element);
			}

			inlineTable.addRows(row);

			results.advance();
		}

		modelVerification.setInlineTable(inlineTable);

		model.setModelVerification(modelVerification);

		writePMML(pmml, this.model);
	}
}