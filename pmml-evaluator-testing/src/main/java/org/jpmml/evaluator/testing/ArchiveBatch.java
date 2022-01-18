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
package org.jpmml.evaluator.testing;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorBuilder;
import org.jpmml.evaluator.FieldNameSet;
import org.jpmml.evaluator.FunctionNameStack;
import org.jpmml.evaluator.ModelEvaluatorBuilder;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.visitors.DefaultModelEvaluatorBattery;
import org.jpmml.model.PMMLUtil;
import org.jpmml.model.visitors.VisitorBattery;

abstract
public class ArchiveBatch extends AbstractBatch {

	public ArchiveBatch(String name, String dataset, Predicate<ResultField> predicate, Equivalence<Object> equivalence){
		super(name, dataset, predicate, equivalence);
	}

	abstract
	public InputStream open(String path);

	public EvaluatorBuilder getEvaluatorBuilder() throws Exception {
		PMML pmml = getPMML();

		VisitorBattery visitorBattery = new DefaultModelEvaluatorBattery();
		visitorBattery.applyTo(pmml);

		EvaluatorBuilder evaluatorBuilder = new ModelEvaluatorBuilder(pmml)
			.setDerivedFieldGuard(new FieldNameSet(8))
			.setFunctionGuard(new FunctionNameStack(4));

		return evaluatorBuilder;
	}

	@Override
	public Evaluator getEvaluator() throws Exception {
		EvaluatorBuilder evaluatorBuilder = getEvaluatorBuilder();

		Evaluator evaluator = evaluatorBuilder.build();

		evaluator.verify();

		return evaluator;
	}

	public PMML getPMML() throws Exception {
		return loadPMML("/pmml/" + (getName() + getDataset()) + ".pmml");
	}

	@Override
	public List<? extends Map<String, ?>> getInput() throws IOException {
		return loadRecords("/csv/" + getDataset() + ".csv");
	}

	@Override
	public List<? extends Map<String, ?>> getOutput() throws IOException {
		return loadRecords("/csv/" + (getName() + getDataset()) + ".csv");
	}

	@Override
	public void close() throws Exception {
	}

	protected PMML loadPMML(String path) throws Exception {

		try(InputStream is = open(path)){
			return PMMLUtil.unmarshal(is);
		}
	}

	protected List<Map<String, String>> loadRecords(String path) throws IOException {
		List<List<String>> table;

		try(InputStream is = open(path)){
			table = CsvUtil.readTable(is, ",");
		}

		Function<String, String> function = new Function<String, String>(){

			@Override
			public String apply(String string){

				if(("N/A").equals(string) || ("NA").equals(string)){
					return null;
				}

				return string;
			}
		};

		return BatchUtil.parseRecords(table, function);
	}
}