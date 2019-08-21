/*
 * Copyright (c) 2019 Villu Ruusmann
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

import java.util.Iterator;
import java.util.List;

public class JavaOperationUtil {

	private JavaOperationUtil(){
	}

	static
	public <V> V evaluate(V value, List<? extends JavaOperation<V>> javaOperations, EvaluationContext context){

		for(Iterator<? extends JavaOperation<V>> it = javaOperations.iterator(); value != null && it.hasNext(); ){
			JavaOperation<V> javaOperation = it.next();

			value = javaOperation.evaluate(value, context);
		}

		return value;
	}
}