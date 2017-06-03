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

import com.google.common.base.Equivalence;

import static org.junit.Assert.fail;

abstract
public class BatchTest {

	private Equivalence<Object> equivalence = null;


	public BatchTest(Equivalence<Object> equivalence){
		setEquivalence(equivalence);
	}

	public void evaluate(Batch batch, Equivalence<Object> equivalence) throws Exception {

		if(equivalence == null){
			equivalence = getEquivalence();
		}

		List<Conflict> conflicts = BatchUtil.evaluate(batch, equivalence);
		if(conflicts.size() > 0){

			for(Conflict conflict : conflicts){
				log(conflict);
			}

			fail("Found " + conflicts.size() + " conflict(s)");
		}
	}

	public void log(Conflict conflict){
		System.err.println(conflict);
	}

	public Equivalence<Object> getEquivalence(){
		return this.equivalence;
	}

	private void setEquivalence(Equivalence<Object> equivalence){

		if(equivalence == null){
			throw new IllegalArgumentException();
		}

		this.equivalence = equivalence;
	}
}