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

public class OperationUtil {

	private OperationUtil(){
	}

	static
	public <V extends Number> Value<V> evaluate(Value<V> value, List<? extends Operation<V>> operations, EvaluationContext context){

		for(Iterator<? extends Operation<V>> it = operations.iterator(); value != null && it.hasNext(); ){
			Operation<V> operation = it.next();

			value = operation.evaluate(value, context);
		}

		return value;
	}
}