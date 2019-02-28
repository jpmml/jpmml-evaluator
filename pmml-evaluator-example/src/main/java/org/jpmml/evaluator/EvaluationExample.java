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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import org.dmg.pmml.PMMLObject;
import org.jpmml.evaluator.visitors.ElementInternerBattery;
import org.jpmml.evaluator.visitors.ElementOptimizerBattery;
import org.jpmml.model.VisitorBattery;
import org.jpmml.model.visitors.AttributeInternerBattery;
import org.jpmml.model.visitors.AttributeOptimizerBattery;
import org.jpmml.model.visitors.ListFinalizerBattery;
import org.jpmml.model.visitors.LocatorNullifier;
import org.jpmml.model.visitors.MemoryMeasurer;

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
		names = {"--name"},
		description = "The name of the target model in a multi-model PMML file. If missing, the first model is targeted"
	)
	@ParameterOrder (
		value = 4
	)
	private String modelName = null;

	@Parameter (
		names = {"--separator"},
		description = "CSV cell separator character",
		converter = SeparatorConverter.class
	)
	@ParameterOrder (
		value = 5
	)
	private String separator = null;

	@Parameter (
		names = {"--missing-values"},
		description = "CSV missing value strings"
	)
	@ParameterOrder (
		value = 6
	)
	private List<String> missingValues = Arrays.asList("N/A", "NA");

	@Parameter (
		names = {"--sparse"},
		description = "Permit missing input field columns"
	)
	@ParameterOrder (
		value = 7
	)
	private boolean sparse = false;

	@Parameter (
		names = {"--copy-columns"},
		description = "Copy all columns from input CSV file to output CSV file",
		arity = 1
	)
	@ParameterOrder (
		value = 8
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
		names = {"--filter-output"},
		description = "Exclude non-final output fields",
		hidden = true
	)
	private boolean filterOutput = false;

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
		names = "--measure",
		description = "Measure PMML class model. Requires JPMML agent",
		hidden = true
	)
	private boolean measure = false;

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

		List<? extends Map<FieldName, ?>> inputRecords = BatchUtil.parseRecords(inputTable, createCellParser(this.missingValues.size() > 0 ? new HashSet<>(this.missingValues) : null));

		if(this.waitBeforeInit){
			waitForUserInput();
		}

		PMML pmml = readPMML(this.model);

		if(this.cacheBuilderSpec != null){
			CacheBuilderSpec cacheBuilderSpec = CacheBuilderSpec.parse(this.cacheBuilderSpec);

			CacheUtil.setCacheBuilderSpec(cacheBuilderSpec);
		}

		VisitorBattery visitorBattery = new VisitorBattery();

		if(this.intern){
			visitorBattery.add(LocatorNullifier.class);
		} // End if

		// Optimize first, intern second.
		// The goal is to intern optimized elements (keeps one copy), not optimize interned elements (expands one copy to multiple copies).
		if(this.optimize){
			visitorBattery.addAll(new AttributeOptimizerBattery());
			visitorBattery.addAll(new ElementOptimizerBattery());
		} // End if

		if(this.intern){
			visitorBattery.addAll(new AttributeInternerBattery());
			visitorBattery.addAll(new ElementInternerBattery());

			visitorBattery.addAll(new ListFinalizerBattery());
		}

		visitorBattery.applyTo(pmml);

		if(this.measure){
			MemoryMeasurer memoryMeasurer = new MemoryMeasurer();
			memoryMeasurer.applyTo(pmml);

			NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
			numberFormat.setGroupingUsed(true);

			long size = memoryMeasurer.getSize();
			System.out.println("Bytesize of the object graph: " + numberFormat.format(size));

			Set<Object> objects = memoryMeasurer.getObjects();

			long objectCount = objects.size();

			System.out.println("Number of distinct Java objects in the object graph: " + numberFormat.format(objectCount));

			long pmmlObjectCount = objects.stream()
				.filter(PMMLObject.class::isInstance)
				.count();

			System.out.println("\t" + "PMML class model objects: " + numberFormat.format(pmmlObjectCount));
			System.out.println("\t" + "Other objects: " + numberFormat.format(objectCount - pmmlObjectCount));
		}

		EvaluatorBuilder evaluatorBuilder = new ModelEvaluatorBuilder(pmml, this.modelName)
			.setModelEvaluatorFactory((ModelEvaluatorFactory)newInstance(this.modelEvaluatorFactoryClazz))
			.setValueFactoryFactory((ValueFactoryFactory)newInstance(this.valueFactoryFactoryClazz))
			.setOutputFilter(this.filterOutput ? OutputFilters.KEEP_FINAL_RESULTS : OutputFilters.KEEP_ALL);

		Evaluator evaluator = evaluatorBuilder.build();

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

			Sets.SetView<FieldName> missingInputFields = Sets.difference(new LinkedHashSet<>(Lists.transform(inputFields, InputField::getName)), inputRecord.keySet());
			if((missingInputFields.size() > 0) && !this.sparse){
				throw new IllegalArgumentException("Missing input field(s): " + missingInputFields);
			}

			Sets.SetView<FieldName> missingGroupFields = Sets.difference(new LinkedHashSet<>(Lists.transform(groupFields, InputField::getName)), inputRecord.keySet());
			if(missingGroupFields.size() > 0){
				throw new IllegalArgumentException("Missing group field(s): " + missingGroupFields);
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

						FieldValue value = inputField.prepare(inputRecord.get(name));

						arguments.put(name, value);
					}

					Map<FieldName, ?> results = evaluator.evaluate(arguments);

					outputRecords.add(results);
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

		outputTable.addAll(BatchUtil.formatRecords(outputRecords, new ArrayList<>(Lists.transform(resultFields, ResultField::getName)), createCellFormatter(this.missingValues.size() > 0 ? this.missingValues.get(0) : null)));

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