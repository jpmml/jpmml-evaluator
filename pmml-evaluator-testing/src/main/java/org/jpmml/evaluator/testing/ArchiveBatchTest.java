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
public class ArchiveBatchTest extends BatchTest {

	private Equivalence<Object> equivalence = null;


	public ArchiveBatchTest(Equivalence<Object> equivalence){
		setEquivalence(equivalence);
	}

	abstract
	public ArchiveBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence);

	public void evaluate(String algorithm, String dataset) throws Exception {
		evaluate(algorithm, dataset, null, null);
	}

	public void evaluate(String algorithm, String dataset, Predicate<ResultField> columnFilter) throws Exception {
		evaluate(algorithm, dataset, columnFilter, null);
	}

	public void evaluate(String algorithm, String dataset, Equivalence<Object> equivalence) throws Exception {
		evaluate(algorithm, dataset, null, equivalence);
	}

	public void evaluate(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence) throws Exception {

		if(columnFilter == null){
			columnFilter = (resultField -> true);
		} // End if

		if(equivalence == null){
			equivalence = getEquivalence();
		}

		try(Batch batch = createBatch(algorithm, dataset, columnFilter, equivalence)){
			evaluate(batch);
		}
	}

	public Equivalence<Object> getEquivalence(){
		return this.equivalence;
	}

	private void setEquivalence(Equivalence<Object> equivalence){
		this.equivalence = Objects.requireNonNull(equivalence);
	}
}