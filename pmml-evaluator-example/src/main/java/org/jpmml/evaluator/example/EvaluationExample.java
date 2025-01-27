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
package org.jpmml.evaluator.example;

import java.io.Console;
import java.io.File;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.Timer;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLObject;
import org.jpmml.evaluator.CacheUtil;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.FieldNameSet;
import org.jpmml.evaluator.FunctionNameStack;
import org.jpmml.evaluator.HasGroupFields;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluatorBuilder;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.OutputFilters;
import org.jpmml.evaluator.Table;
import org.jpmml.evaluator.TableCollector;
import org.jpmml.evaluator.ValueFactoryFactory;
import org.jpmml.evaluator.visitors.AttributeFinalizerBattery;
import org.jpmml.evaluator.visitors.AttributeInternerBattery;
import org.jpmml.evaluator.visitors.AttributeOptimizerBattery;
import org.jpmml.evaluator.visitors.ElementFinalizerBattery;
import org.jpmml.evaluator.visitors.ElementInternerBattery;
import org.jpmml.evaluator.visitors.ElementOptimizerBattery;
import org.jpmml.model.visitors.LocatorNullifier;
import org.jpmml.model.visitors.MemoryMeasurer;
import org.jpmml.model.visitors.VisitorBattery;

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
	private char separator = ',';

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
		names = {"--catch-errors"},
		description = "Catch and process evaluation errors. If true, the main evaluation loop will run till completion",
		arity = 1
	)
	@ParameterOrder (
		value = 8
	)
	private boolean catchErrors = false;

	@Parameter (
		names = {"--copy-columns"},
		description = "Copy all columns from input CSV file to output CSV file",
		arity = 1
	)
	@ParameterOrder (
		value = 9
	)
	private boolean copyColumns = true;

	@Parameter (
		names = {"--error-column"},
		description = "The name of error column. This column is appended to output CSV file only in case of evaluation errors"
	)
	@ParameterOrder (
		value = 10
	)
	private String errorColumn = "_error";

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
		description = "The name of ModelEvaluatorFactory class",
		hidden = true
	)
	private String modelEvaluatorFactoryClazz = ModelEvaluatorFactory.class.getName();

	@Parameter (
		names = {"--valuefactoryfactory-class"},
		description = "The name of ValueFactoryFactory class",
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
		names = {"--safe"},
		description = "Guard against ill-fated derived field and function evaluations",
		hidden = true
	)
	private boolean safe = false;

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

		Function<String, String> cellParser = createCellParser(!this.missingValues.isEmpty() ? new HashSet<>(this.missingValues) : null);

		Table inputTable = readTable(this.input, this.separator);
		inputTable.apply(cellParser);

		List<String> inputColumns = inputTable.getColumns();

		if(this.waitBeforeInit){
			waitForUserInput();
		}

		PMML pmml = readPMML(this.model, true);

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
		} // End if

		if(this.optimize || this.intern){
			visitorBattery.addAll(new AttributeFinalizerBattery());
			visitorBattery.addAll(new ElementFinalizerBattery());
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

		ModelEvaluatorBuilder evaluatorBuilder = new ModelEvaluatorBuilder(pmml, this.modelName)
			.setModelEvaluatorFactory((ModelEvaluatorFactory)newInstance(this.modelEvaluatorFactoryClazz))
			.setValueFactoryFactory((ValueFactoryFactory)newInstance(this.valueFactoryFactoryClazz))
			.setOutputFilter(this.filterOutput ? OutputFilters.KEEP_FINAL_RESULTS : OutputFilters.KEEP_ALL);

		if(this.safe){
			evaluatorBuilder = evaluatorBuilder
				.setDerivedFieldGuard(new FieldNameSet(8))
				.setFunctionGuard(new FunctionNameStack(4));
		}

		Evaluator evaluator = evaluatorBuilder.build();

		// Perform self-testing
		evaluator.verify();

		List<InputField> inputFields = evaluator.getInputFields();
		List<InputField> groupFields = Collections.emptyList();

		if(evaluator instanceof HasGroupFields){
			HasGroupFields hasGroupfields = (HasGroupFields)evaluator;

			groupFields = hasGroupfields.getGroupFields();
		} // End if

		if(!inputColumns.isEmpty()){
			Sets.SetView<String> missingInputFields = Sets.difference(new LinkedHashSet<>(Lists.transform(inputFields, InputField::getName)), new LinkedHashSet<>(inputColumns));
			if(!missingInputFields.isEmpty() && !this.sparse){
				throw new IllegalArgumentException("Missing input field(s): " + missingInputFields);
			}

			Sets.SetView<String> missingGroupFields = Sets.difference(new LinkedHashSet<>(Lists.transform(groupFields, InputField::getName)), new LinkedHashSet<>(inputColumns));
			if(!missingGroupFields.isEmpty()){
				throw new IllegalArgumentException("Missing group field(s): " + missingGroupFields);
			}
		} // End if

		if(evaluator instanceof HasGroupFields){
			HasGroupFields hasGroupFields = (HasGroupFields)evaluator;

			inputTable = EvaluatorUtil.groupRows(hasGroupFields, inputTable);
		}

		Timer timer = new Timer(new SlidingWindowReservoir(this.loop));

		metricRegistry.register("main", timer);

		if(this.waitBeforeLoop){
			waitForUserInput();
		}

		Table outputTable = new Table(0);

		Function<Table.Row, Object> function = new Function<Table.Row, Object>(){

			@Override
			public Object apply(Table.Row arguments){

				try {
					Map<String, ?> results = evaluator.evaluate(arguments);

					return results;
				} catch(Exception e){

					if(!EvaluationExample.this.catchErrors){
						throw e;
					}

					return e;
				}
			}
		};

		for(int i = 0; i < this.loop; i++){
			Timer.Context context = timer.time();

			try {
				outputTable = inputTable.stream()
					.map(function)
					.collect(new TableCollector());
			} finally {
				context.close();
			}
		}

		if(this.waitAfterLoop){
			waitForUserInput();
		} // End if

		if(outputTable.hasExceptions()){

			if(this.errorColumn != null){
				outputTable.setValues(this.errorColumn, outputTable.getExceptions());
			}
		} // End if

		if((inputTable.getNumberOfRows() == outputTable.getNumberOfRows()) && this.copyColumns){
			Map<String, List<?>> outputColumnValues = outputTable.getValues();

			Collection<? extends Map.Entry<String, List<?>>> entries = outputColumnValues.entrySet();
			for(Map.Entry<String, List<?>> entry : entries){
				inputTable.setValues(entry.getKey(), entry.getValue());
			}

			outputTable = inputTable;
		}

		Function<Object, String> cellFormatter = createCellFormatter(!this.missingValues.isEmpty() ? this.missingValues.get(0) : null);

		outputTable.apply(cellFormatter);

		writeTable(outputTable, this.output, this.separator);

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