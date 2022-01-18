/*
 * Copyright (c) 2022 Villu Ruusmann
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

import java.util.Objects;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.jpmml.evaluator.ResultField;

abstract
public class AbstractBatch implements Batch {

	private String name = null;

	private String dataset = null;

	private Predicate<ResultField> columnFilter = null;

	private Equivalence<Object> equivalence = null;


	public AbstractBatch(String name, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		setName(name);
		setDataset(dataset);
		setColumnFilter(columnFilter);
		setEquivalence(equivalence);
	}

	public String getName(){
		return this.name;
	}

	private void setName(String name){
		this.name = Objects.requireNonNull(name);
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