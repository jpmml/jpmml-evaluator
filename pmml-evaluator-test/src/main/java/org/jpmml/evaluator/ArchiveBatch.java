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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.visitors.DefaultModelEvaluatorBattery;
import org.jpmml.model.PMMLUtil;
import org.jpmml.model.visitors.VisitorBattery;

abstract
public class ArchiveBatch implements Batch {

	private String name = null;

	private String dataset = null;

	private Predicate<FieldName> predicate = null;


	public ArchiveBatch(String name, String dataset, Predicate<FieldName> predicate){
		setName(name);
		setDataset(dataset);
		setPredicate(predicate);
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
	public List<Map<FieldName, String>> getInput() throws IOException {
		return loadRecords("/csv/" + getDataset() + ".csv");
	}

	@Override
	public List<Map<FieldName, String>> getOutput() throws IOException {
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

	protected List<Map<FieldName, String>> loadRecords(String path) throws IOException {
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

	public String getName(){
		return this.name;
	}

	private void setName(String name){
		this.name = name;
	}

	public String getDataset(){
		return this.dataset;
	}

	private void setDataset(String dataset){
		this.dataset = dataset;
	}

	@Override
	public Predicate<FieldName> getPredicate(){
		return this.predicate;
	}

	private void setPredicate(Predicate<FieldName> predicate){

		if(predicate == null){
			throw new IllegalArgumentException();
		}

		this.predicate = predicate;
	}
}