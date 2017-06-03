/*
 * Copyright (c) 2015 Villu Ruusmann
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

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import org.dmg.pmml.FieldName;

public class FilterBatch implements Batch {

	private Batch batch = null;


	public FilterBatch(Batch batch){
		setBatch(batch);
	}

	@Override
	public Evaluator getEvaluator() throws Exception {
		Batch batch = getBatch();

		return batch.getEvaluator();
	}

	@Override
	public List<? extends Map<FieldName, ?>> getInput() throws Exception {
		Batch batch = getBatch();

		return batch.getInput();
	}

	@Override
	public List<? extends Map<FieldName, ?>> getOutput() throws Exception {
		Batch batch = getBatch();

		return batch.getOutput();
	}

	@Override
	public Predicate<FieldName> getPredicate(){
		Batch batch = getBatch();

		return batch.getPredicate();
	}

	@Override
	public void close() throws Exception {
		Batch batch = getBatch();

		batch.close();
	}

	public Batch getBatch(){
		return this.batch;
	}

	private void setBatch(Batch batch){
		this.batch = batch;
	}
}