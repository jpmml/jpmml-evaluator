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
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.Table;
import org.jpmml.evaluator.TableReader;

abstract
public class ArchiveBatch implements Batch {

	private String algorithm = null;

	private String dataset = null;

	private Predicate<ResultField> columnFilter = null;

	private Equivalence<Object> equivalence = null;


	public ArchiveBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		setAlgorithm(algorithm);
		setDataset(dataset);
		setColumnFilter(columnFilter);
		setEquivalence(equivalence);
	}

	abstract
	public InputStream open(String path) throws IOException;

	public String getInputCsvPath(){
		return "/csv/" + getDataset() + ".csv";
	}

	@Override
	public Table getInput() throws IOException {
		return loadRecords(getInputCsvPath());
	}

	public String getOutputCsvPath(){
		return "/csv/" + (getAlgorithm() + getDataset()) + ".csv";
	}

	@Override
	public Table getOutput() throws IOException {
		return loadRecords(getOutputCsvPath());
	}

	@Override
	public void close() throws Exception {
	}

	protected Table loadRecords(String path) throws IOException {
		TableReader tableReader = createTableReader();

		Table table;

		try(InputStream is = open(path)){
			table = tableReader.read(is);
		}

		Function<String, String> function = new Function<>(){

			@Override
			public String apply(String string){

				if(("N/A").equals(string) || ("NA").equals(string)){
					return null;
				}

				return string;
			}
		};

		table.apply(function);

		return table;
	}

	protected char getSeparator(){
		return ',';
	}

	protected TableReader createTableReader(){
		TableReader tableReader = new TableReader(getSeparator());

		return tableReader;
	}

	public String getAlgorithm(){
		return this.algorithm;
	}

	private void setAlgorithm(String algorithm){
		this.algorithm = Objects.requireNonNull(algorithm);
	}

	public String getDataset(){
		return this.dataset;
	}

	private void setDataset(String dataset){
		this.dataset = Objects.requireNonNull(dataset);
	}

	@Override
	public Predicate<ResultField> getColumnFilter(){
		return this.columnFilter;
	}

	private void setColumnFilter(Predicate<ResultField> columnFilter){
		this.columnFilter = Objects.requireNonNull(columnFilter);
	}

	@Override
	public Equivalence<Object> getEquivalence(){
		return this.equivalence;
	}

	private void setEquivalence(Equivalence<Object> equivalence){
		this.equivalence = Objects.requireNonNull(equivalence);
	}
}