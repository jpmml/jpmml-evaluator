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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.model.PMMLUtil;

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

	@Override
	public Evaluator getEvaluator() throws Exception {
		PMML pmml = getPMML();

		ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();

		ModelEvaluator<?> modelEvaluator = modelEvaluatorFactory.newModelEvaluator(pmml);
		modelEvaluator.verify();

		return modelEvaluator;
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