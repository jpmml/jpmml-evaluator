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
package org.jpmml.evaluator.example;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.beust.jcommander.Parameter;
import com.google.common.base.Equivalence;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorBuilder;
import org.jpmml.evaluator.ModelEvaluatorBuilder;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.Table;
import org.jpmml.evaluator.testing.Batch;
import org.jpmml.evaluator.testing.BatchUtil;
import org.jpmml.evaluator.testing.Conflict;
import org.jpmml.evaluator.testing.PMMLEquivalence;

public class TestingExample extends Example {

	@Parameter (
		names = {"--model"},
		description = "Model PMML file",
		required = true,
		order = 1
	)
	private File model = null;

	@Parameter (
		names = {"--input"},
		description = "Input CSV file",
		required = true,
		order = 2
	)
	private File input = null;

	@Parameter (
		names = {"--expected-output"},
		description = "Expected output CSV file",
		required = true,
		order = 3
	)
	private File output = null;

	@Parameter (
		names = {"--separator"},
		description = "CSV cell separator character",
		converter = SeparatorConverter.class,
		order = 4
	)
	private char separator = ',';

	@Parameter (
		names = {"--missing-values"},
		description = "CSV missing value strings",
		order = 5
	)
	private List<String> missingValues = Arrays.asList("N/A", "NA");

	@Parameter (
		names = {"--ignored-fields"},
		description = "Ignored Model fields",
		order = 6
	)
	private List<String> ignoredFields = new ArrayList<>();

	@Parameter (
		names = {"--precision"},
		description = "Relative error",
		order = 7
	)
	private double precision = 1e-9;

	@Parameter (
		names = {"--zero-threshold"},
		description = "Absolute error near zero",
		order = 8
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
		PMML pmml = readPMML(this.model, true);

		EvaluatorBuilder evaluatorBuilder = new ModelEvaluatorBuilder(pmml)
			.setModelEvaluatorFactory((ModelEvaluatorFactory)newInstance(this.modelEvaluatorFactoryClazz));

		Evaluator evaluator = evaluatorBuilder.build();

		// Perform self-testing
		evaluator.verify();

		Function<String, String> cellParser = createCellParser(new HashSet<>(this.missingValues));

		Table inputTable = readTable(this.input, this.separator);
		inputTable.apply(cellParser);

		Table outputTable = readTable(this.output, this.separator);
		outputTable.apply(cellParser);

		Predicate<ResultField> columnFilter;

		if(this.ignoredFields != null && !this.ignoredFields.isEmpty()){
			columnFilter = (ResultField resultField) -> !this.ignoredFields.contains(resultField.getName());
		} else

		{
			columnFilter = (ResultField resultField) -> true;
		}

		Equivalence<Object> equivalence = new PMMLEquivalence(this.precision, this.zeroThreshold);

		List<Conflict> conflicts;

		try(Batch batch = createBatch(evaluator, inputTable, outputTable, columnFilter, equivalence)){
			conflicts = BatchUtil.evaluate(batch);
		}

		for(Conflict conflict : conflicts){
			System.err.println(conflict);
		}
	}

	static
	private Batch createBatch(Evaluator evaluator, Table input, Table output, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		Batch batch = new Batch(){

			@Override
			public Evaluator getEvaluator(){
				return evaluator;
			}

			@Override
			public Table getInput(){
				return input;
			}

			@Override
			public Table getOutput(){
				return output;
			}

			@Override
			public Predicate<ResultField> getColumnFilter(){
				return columnFilter;
			}

			@Override
			public Equivalence<Object> getEquivalence(){
				return equivalence;
			}

			@Override
			public void close(){
			}
		};

		return batch;
	}
}