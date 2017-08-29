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

import java.io.Console;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.Timer;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.jpmml.evaluator.visitors.ExpressionOptimizer;
import org.jpmml.evaluator.visitors.FieldOptimizer;
import org.jpmml.evaluator.visitors.GeneralRegressionModelOptimizer;
import org.jpmml.evaluator.visitors.MiningFieldInterner;
import org.jpmml.evaluator.visitors.NaiveBayesModelOptimizer;
import org.jpmml.evaluator.visitors.PredicateInterner;
import org.jpmml.evaluator.visitors.PredicateOptimizer;
import org.jpmml.evaluator.visitors.RegressionModelOptimizer;
import org.jpmml.evaluator.visitors.ScoreDistributionInterner;
import org.jpmml.model.visitors.ArrayListOptimizer;
import org.jpmml.model.visitors.ArrayListTransformer;
import org.jpmml.model.visitors.DoubleInterner;
import org.jpmml.model.visitors.IntegerInterner;
import org.jpmml.model.visitors.StringInterner;

public class EvaluationExample extends Example {

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
		names = {"--output"},
		description = "Output CSV file",
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
		names = {"--sparse"},
		description = "Permit missing input field columns"
	)
	@ParameterOrder (
		value = 5
	)
	private boolean sparse = false;

	@Parameter (
		names = {"--copy-columns"},
		description = "Copy all columns from input CSV file to output CSV file",
		arity = 1
	)
	@ParameterOrder (
		value = 6
	)
	private boolean copyColumns = true;

	@Parameter (
		names = {"--wait-before-init"},
		description = "Pause before initializing the JPMML stack",
		hidden = true
	)
	private boolean waitBeforeInit = false;

	@Parameter (
		names = "--cache-builder-spec",
		description = "CacheBuilder configuration",
		hidden = true
	)
	private String cacheBuilderSpec = null;

	@Parameter (
		names = {"--factory-class", "--modelevaluatorfactory-class"},
		description = "Name of ModelEvaluatorFactory class",
		hidden = true
	)
	private String modelEvaluatorFactoryClazz = ModelEvaluatorFactory.class.getName();

	@Parameter (
		names = {"--valuefactoryfactory-class"},
		description = "Name of ValueFactoryFactory class",
		hidden = true
	)
	private String valueFactoryFactoryClazz = ValueFactoryFactory.class.getName();


	@Parameter (
		names = "--optimize",
		description = "Optimize PMML class model",
		hidden = true
	)
	private boolean optimize = false;

	@Parameter (
		names = "--intern",
		description = "Intern PMML class model",
		hidden = true
	)
	private boolean intern = false;

	@Parameter (
		names = "--loop",
		description = "The number of repetitions",
		hidden = true,
		validateWith = PositiveInteger.class
	)
	private int loop = 1;

	@Parameter (
		names = {"--wait-before-loop"},
		description = "Pause before entering the main evaluation loop",
		hidden = true
	)
	private boolean waitBeforeLoop = false;

	@Parameter (
		names = {"--wait-after-loop"},
		description = "Pause after exiting the main evaluation loop",
		hidden = true
	)
	private boolean waitAfterLoop = false;


	static
	public void main(String... args) throws Exception {
		execute(EvaluationExample.class, args);
	}

	@Override
	public void execute() throws Exception {
		MetricRegistry metricRegistry = new MetricRegistry();

		ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry)
			.convertRatesTo(TimeUnit.SECONDS)
			.convertDurationsTo(TimeUnit.MILLISECONDS)
			.build();

		CsvUtil.Table inputTable = readTable(this.input, this.separator);

		List<? extends Map<FieldName, ?>> inputRecords = BatchUtil.parseRecords(inputTable, Example.CSV_PARSER);

		if(this.waitBeforeInit){
			waitForUserInput();
		}

		PMML pmml = readPMML(this.model);

		if(this.cacheBuilderSpec != null){
			CacheBuilderSpec cacheBuilderSpec = CacheBuilderSpec.parse(this.cacheBuilderSpec);

			CacheUtil.setCacheBuilderSpec(cacheBuilderSpec);
		} // End if

		if(this.optimize){
			List<? extends Visitor> optimizers = Arrays.asList(new ArrayListOptimizer(), new ExpressionOptimizer(), new FieldOptimizer(), new PredicateOptimizer(), new GeneralRegressionModelOptimizer(), new NaiveBayesModelOptimizer(), new RegressionModelOptimizer());

			for(Visitor optimizer : optimizers){
				optimizer.applyTo(pmml);
			}
		} // End if

		// Optimize first, intern second.
		// The goal is to intern optimized elements (keeps one copy), not optimize interned elements (expands one copy to multiple copies).
		if(this.intern){
			List<? extends Visitor> interners = Arrays.asList(new DoubleInterner(), new IntegerInterner(), new StringInterner(), new MiningFieldInterner(), new PredicateInterner(), new ScoreDistributionInterner());

			for(Visitor interner : interners){
				interner.applyTo(pmml);
			}
		} // End if

		if(this.optimize || this.intern){
			Visitor transformer = new ArrayListTransformer();

			transformer.applyTo(pmml);
		}

		ModelEvaluatorFactory modelEvaluatorFactory = (ModelEvaluatorFactory)newInstance(Class.forName(this.modelEvaluatorFactoryClazz));

		ValueFactoryFactory valueFactoryFactory = (ValueFactoryFactory)newInstance(Class.forName(this.valueFactoryFactoryClazz));
		modelEvaluatorFactory.setValueFactoryFactory(valueFactoryFactory);

		Evaluator evaluator = modelEvaluatorFactory.newModelEvaluator(pmml);

		// Perform self-testing
		evaluator.verify();

		List<InputField> inputFields = evaluator.getInputFields();
		List<InputField> groupFields = Collections.emptyList();

		if(evaluator instanceof HasGroupFields){
			HasGroupFields hasGroupfields = (HasGroupFields)evaluator;

			groupFields = hasGroupfields.getGroupFields();
		} // End if

		if(inputRecords.size() > 0){
			Map<FieldName, ?> inputRecord = inputRecords.get(0);

			Sets.SetView<FieldName> missingInputFields = Sets.difference(new LinkedHashSet<>(EvaluatorUtil.getNames(inputFields)), inputRecord.keySet());
			if((missingInputFields.size() > 0) && !this.sparse){
				throw new IllegalArgumentException("Missing input field(s): " + missingInputFields.toString());
			}

			Sets.SetView<FieldName> missingGroupFields = Sets.difference(new LinkedHashSet<>(EvaluatorUtil.getNames(groupFields)), inputRecord.keySet());
			if(missingGroupFields.size() > 0){
				throw new IllegalArgumentException("Missing group field(s): " + missingGroupFields.toString());
			}
		} // End if

		if(evaluator instanceof HasGroupFields){
			HasGroupFields hasGroupFields = (HasGroupFields)evaluator;

			inputRecords = EvaluatorUtil.groupRows(hasGroupFields, inputRecords);
		}

		List<Map<FieldName, ?>> outputRecords = new ArrayList<>(inputRecords.size());

		Timer timer = new Timer(new SlidingWindowReservoir(this.loop));

		metricRegistry.register("main", timer);

		if(this.waitBeforeLoop){
			waitForUserInput();
		}

		for(int i = 0; i < this.loop; i++){
			Timer.Context context = timer.time();

			try {
				outputRecords.clear();

				Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

				for(Map<FieldName, ?> inputRecord : inputRecords){
					arguments.clear();

					for(InputField inputField : inputFields){
						FieldName name = inputField.getName();

						FieldValue value = EvaluatorUtil.prepare(inputField, inputRecord.get(name));

						arguments.put(name, value);
					}

					Map<FieldName, ?> result = evaluator.evaluate(arguments);

					outputRecords.add(result);
				}
			} finally {
				context.close();
			}
		}

		if(this.waitAfterLoop){
			waitForUserInput();
		}

		List<TargetField> targetFields = evaluator.getTargetFields();
		List<OutputField> outputFields = evaluator.getOutputFields();

		List<? extends ResultField> resultFields = Lists.newArrayList(Iterables.concat(targetFields, outputFields));

		CsvUtil.Table outputTable = new CsvUtil.Table();
		outputTable.setSeparator(inputTable.getSeparator());

		outputTable.addAll(BatchUtil.formatRecords(outputRecords, EvaluatorUtil.getNames(resultFields), Example.CSV_FORMATTER));

		if((inputTable.size() == outputTable.size()) && this.copyColumns){

			for(int i = 0; i < inputTable.size(); i++){
				List<String> inputRow = inputTable.get(i);
				List<String> outputRow = outputTable.get(i);

				outputRow.addAll(0, inputRow);
			}
		}

		writeTable(outputTable, this.output);

		if(this.loop > 1){
			reporter.report();
		}

		reporter.close();
	}

	static
	private void waitForUserInput(){
		Console console = System.console();
		if(console == null){
			throw new IllegalStateException();
		}

		console.readLine("Press ENTER to continue");
	}
}