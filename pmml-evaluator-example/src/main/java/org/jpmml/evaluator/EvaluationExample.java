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
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.jpmml.evaluator.visitors.PredicateParser;

public class EvaluationExample extends Example {

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

	@Parameter (
		names = {"--wait-before"},
		description = "Pause before starting the work",
		hidden = true
	)
	private boolean waitBefore = false;

	@Parameter (
		names = {"--wait-after"},
		description = "Pause after completing the work",
		hidden = true
	)
	private boolean waitAfter = false;

	@Parameter (
		names = "--loop",
		description = "The number of repetitions",
		hidden = true,
		validateWith = PositiveInteger.class
	)
	private int loop = 1;

	@Parameter (
		names = "--optimize",
		description = "Optimize PMML class model",
		hidden = true
	)
	private boolean optimize = false;


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

		Function<String, String> parseFunction = new Function<String, String>(){

			@Override
			public String apply(String string){

				if(("").equals(string) || ("N/A").equals(string) || ("NA").equals(string)){
					return null;
				}

				// Remove leading and trailing quotation marks
				string = stripQuotes(string, '\"');
				string = stripQuotes(string, '\"');

				// Standardize European-style decimal marks (',') to US-style decimal marks ('.')
				if(string.indexOf(',') > -1){
					String usString = string.replace(',', '.');

					try {
						Double.parseDouble(usString);

						string = usString;
					} catch(NumberFormatException nfe){
						// Ignored
					}
				}

				return string;
			}

			private String stripQuotes(String string, char quoteChar){

				if(string.length() > 1 && ((string.charAt(0) == quoteChar) && (string.charAt(string.length() - 1) == quoteChar))){
					return string.substring(1, string.length() - 1);
				}

				return string;
			}
		};

		List<? extends Map<FieldName, ?>> inputRecords = BatchUtil.parseRecords(inputTable, parseFunction);

		if(this.waitBefore){
			waitForUserInput();
		}

		PMML pmml = readPMML(this.model);

		if(this.optimize){
			List<? extends Visitor> optimizers = Arrays.asList(new PredicateParser());

			for(Visitor optimizer : optimizers){
				optimizer.applyTo(pmml);
			}
		}

		ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();

		Evaluator evaluator = modelEvaluatorFactory.newModelManager(pmml);

		// Perform self-testing
		evaluator.verify();

		List<FieldName> activeFields = evaluator.getActiveFields();
		List<FieldName> groupFields = evaluator.getGroupFields();

		if(inputRecords.size() > 0){
			Map<FieldName, ?> inputRecord = inputRecords.get(0);

			Sets.SetView<FieldName> missingActiveFields = Sets.difference(new LinkedHashSet<>(activeFields), inputRecord.keySet());
			if(missingActiveFields.size() > 0){
				throw new IllegalArgumentException("Missing active field(s): " + missingActiveFields.toString());
			}

			Sets.SetView<FieldName> missingGroupFields = Sets.difference(new LinkedHashSet<>(groupFields), inputRecord.keySet());
			if(missingGroupFields.size() > 0){
				throw new IllegalArgumentException("Missing group field(s): " + missingGroupFields.toString());
			}
		}

		if(groupFields.size() == 1){
			FieldName groupField = groupFields.get(0);

			inputRecords = EvaluatorUtil.groupRows(groupField, inputRecords);
		} else

		if(groupFields.size() > 1){
			throw new EvaluationException();
		}

		List<Map<FieldName, ?>> outputRecords = new ArrayList<>();

		Timer timer = new Timer(new SlidingWindowReservoir(this.loop));

		metricRegistry.register("main", timer);

		int epoch = 0;

		do {
			Timer.Context context = timer.time();

			try {
				for(Map<FieldName, ?> inputRecord : inputRecords){
					Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

					for(FieldName activeField : activeFields){
						FieldValue activeValue = EvaluatorUtil.prepare(evaluator, activeField, inputRecord.get(activeField));

						arguments.put(activeField, activeValue);
					}

					Map<FieldName, ?> result = evaluator.evaluate(arguments);

					outputRecords.add(result);
				}
			} finally {
				context.close();
			}

			epoch++;
		} while(epoch < this.loop);

		if(this.waitAfter){
			waitForUserInput();
		}

		List<FieldName> targetFields = evaluator.getTargetFields();
		List<FieldName> outputFields = evaluator.getOutputFields();

		Function<Object, String> formatFunction = new Function<Object, String>(){

			@Override
			public String apply(Object object){
				object = EvaluatorUtil.decode(object);

				if(object == null){
					return "N/A";
				}

				return object.toString();
			}
		};

		CsvUtil.Table outputTable = new CsvUtil.Table();
		outputTable.setSeparator(inputTable.getSeparator());
		outputTable.addAll(BatchUtil.formatRecords(outputRecords, Lists.newArrayList(Iterables.concat(targetFields, outputFields)), formatFunction));

		if(inputTable.size() == outputTable.size()){

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