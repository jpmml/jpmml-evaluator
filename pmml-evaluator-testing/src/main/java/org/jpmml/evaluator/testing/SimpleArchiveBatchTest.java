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

import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.jpmml.evaluator.ResultField;

public class SimpleArchiveBatchTest extends ArchiveBatchTest {

	public SimpleArchiveBatchTest(Equivalence<Object> equivalence){
		super(equivalence);
	}

	@Override
	public SimpleArchiveBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		SimpleArchiveBatch result = new SimpleArchiveBatch(algorithm, dataset, columnFilter, equivalence){

			@Override
			public SimpleArchiveBatchTest getArchiveBatchTest(){
				return SimpleArchiveBatchTest.this;
			}
		};

		return result;
	}
}