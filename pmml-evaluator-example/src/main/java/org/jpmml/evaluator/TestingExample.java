/*
 * Copyright (c) 2016 Villu Ruusmann
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
import java.util.Map;
import java.util.function.Predicate;

import com.beust.jcommander.Parameter;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;

public class TestingExample extends Example {

	@Parameter (
		names = {"--model"},
		description = "Model PMML file",
		required = true
	)
	@ParameterOrder (
		value = 1
	)
	private File model = null;

	@Parameter (
		names = {"--input"},
		description = "Input CSV file",
		required = true
	)
	@ParameterOrder (
		value = 2
	)
	private File input = null;

	@Parameter (
		names = {"--expected-output"},
		description = "Expected output CSV file",
		required = true
	)
	@ParameterOrder (
		value = 3
	)
	private File output = null;

	@Parameter (
		names = {"--separator"},
		description = "CSV cell separator character",
		converter = SeparatorConverter.class
	)
	@ParameterOrder (
		value = 4
	)
	private String separator = null;

	@Parameter (
		names = {"--missing-values"},
		description = "CSV missing value strings"
	)
	@ParameterOrder (
		value = 5
	)
	private List<String> missingValues = Arrays.asList("N/A", "NA");

	@Parameter (
		names = {"--ignored-fields"},
		description = "Ignored Model fields",
		converter = FieldNameConverter.class
	)
	@ParameterOrder (
		value = 6
	)
	private List<FieldName> ignoredFields = new ArrayList<>();

	@Parameter (
		names = {"--precision"},
		description = "Relative error"
	)
	@ParameterOrder (
		value = 7
	)
	private double precision = 1e-9;

	@Parameter (
		names = {"--zero-threshold"},
		description = "Absolute error near zero"
	)
	@ParameterOrder (
		value = 8
	)
	private double zeroThreshold = 1e-9;

	@Parameter (
		names = {"--factory-class", "--modelevaluatorfactory-class"},
		description = "Name of ModelEvaluatorFactory class",
		hidden = true
	)
	private String modelEvaluatorFactoryClazz = ModelEvaluatorFactory.class.getName();


	static
	public void main(String... args) throws Exception {
		execute(TestingExample.class, args);
	}

	@Override
	public void execute() throws Exception {
		PMML pmml = readPMML(this.model);

		CsvUtil.Table inputTable = readTable(this.input, this.separator);

		CsvUtil.Table outputTable = readTable(this.output, this.separator);

		EvaluatorBuilder evaluatorBuilder = new ModelEvaluatorBuilder(pmml)
			.setModelEvaluatorFactory((ModelEvaluatorFactory)newInstance(this.modelEvaluatorFactoryClazz));

		Evaluator evaluator = evaluatorBuilder.build();

		// Perform self-testing
		evaluator.verify();

		java.util.function.Function<String, String> cellParser = createCellParser(new HashSet<>(this.missingValues));

		List<? extends Map<FieldName, ?>> inputRecords = BatchUtil.parseRecords(inputTable, cellParser);

		List<? extends Map<FieldName, ?>> outputRecords = BatchUtil.parseRecords(outputTable, cellParser);

		Predicate<FieldName> predicate;

		if(this.ignoredFields != null && this.ignoredFields.size() > 0){
			predicate = (FieldName name) -> !this.ignoredFields.contains(name);
		} else

		{
			predicate = (FieldName name) -> true;
		}

		List<Conflict> conflicts;

		try(Batch batch = createBatch(evaluator, inputRecords, outputRecords, predicate)){
			conflicts = BatchUtil.evaluate(batch, new PMMLEquivalence(this.precision, this.zeroThreshold));
		}

		for(Conflict conflict : conflicts){
			System.err.println(conflict);
		}
	}

	static
	private Batch createBatch(Evaluator evaluator, List<? extends Map<FieldName, ?>> input, List<? extends Map<FieldName, ?>> output, Predicate<FieldName> predicate){
		Batch batch = new Batch(){

			@Override
			public Evaluator getEvaluator(){
				return evaluator;
			}

			@Override
			public List<? extends Map<FieldName, ?>> getInput(){
				return input;
			}

			@Override
			public List<? extends Map<FieldName, ?>> getOutput(){
				return output;
			}

			@Override
			public Predicate<FieldName> getPredicate(){
				return predicate;
			}

			@Override
			public void close(){
			}
		};

		return batch;
	}
}