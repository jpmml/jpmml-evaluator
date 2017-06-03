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

import java.util.Arrays;

import com.google.common.base.Equivalence;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.dmg.pmml.FieldName;

abstract
public class IntegrationTest extends BatchTest {

	public IntegrationTest(Equivalence<Object> equivalence){
		super(equivalence);
	}

	public void evaluate(String name, String dataset) throws Exception {
		evaluate(name, dataset, Predicates.<FieldName>alwaysTrue(), null);
	}

	public void evaluate(String name, String dataset, Equivalence<Object> equivalence) throws Exception {
		evaluate(name, dataset, Predicates.<FieldName>alwaysTrue(), equivalence);
	}

	public void evaluate(String name, String dataset, Predicate<FieldName> predicate) throws Exception {
		evaluate(name, dataset, predicate, null);
	}

	public void evaluate(String name, String dataset, Predicate<FieldName> predicate, Equivalence<Object> equivalence) throws Exception {

		try(Batch batch = createBatch(name, dataset, predicate)){
			evaluate(batch, equivalence);
		}
	}

	protected Batch createBatch(String name, String dataset, Predicate<FieldName> predicate){
		Batch result = new IntegrationTestBatch(name, dataset, predicate){

			@Override
			public IntegrationTest getIntegrationTest(){
				return IntegrationTest.this;
			}
		};

		return result;
	}

	static
	public Predicate<FieldName> excludeFields(FieldName... names){
		return Predicates.not(Predicates.in(Arrays.asList(names)));
	}
}