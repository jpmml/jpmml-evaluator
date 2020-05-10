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
package org.jpmml.evaluator.testing;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.dmg.pmml.FieldName;

abstract
public class IntegrationTest extends BatchTest {

	private Equivalence<Object> equivalence = null;


	public IntegrationTest(Equivalence<Object> equivalence){
		setEquivalence(Objects.requireNonNull(equivalence));
	}

	public void evaluate(String name, String dataset) throws Exception {
		evaluate(name, dataset, null, null);
	}

	public void evaluate(String name, String dataset, Predicate<FieldName> predicate) throws Exception {
		evaluate(name, dataset, predicate, null);
	}

	public void evaluate(String name, String dataset, Equivalence<Object> equivalence) throws Exception {
		evaluate(name, dataset, null, equivalence);
	}

	public void evaluate(String name, String dataset, Predicate<FieldName> predicate, Equivalence<Object> equivalence) throws Exception {

		if(predicate == null){
			predicate = (x -> true);
		} // End if

		if(equivalence == null){
			equivalence = getEquivalence();
		}

		try(Batch batch = createBatch(name, dataset, predicate, equivalence)){
			evaluate(batch);
		}
	}

	protected Batch createBatch(String name, String dataset, Predicate<FieldName> predicate, Equivalence<Object> equivalence){
		Batch result = new IntegrationTestBatch(name, dataset, predicate, equivalence){

			@Override
			public IntegrationTest getIntegrationTest(){
				return IntegrationTest.this;
			}
		};

		return result;
	}

	public Equivalence<Object> getEquivalence(){
		return this.equivalence;
	}

	private void setEquivalence(Equivalence<Object> equivalence){
		this.equivalence = equivalence;
	}

	static
	public Predicate<FieldName> excludeFields(FieldName... names){
		return excludeFields(Arrays.asList(names));
	}

	static
	public Predicate<FieldName> excludeFields(Collection<FieldName> names){
		Predicate<FieldName> predicate = new Predicate<FieldName>(){

			@Override
			public boolean test(FieldName name){
				return !names.contains(name);
			}
		};

		return predicate;
	}
}