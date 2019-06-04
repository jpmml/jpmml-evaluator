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
package org.jpmml.evaluator.functions;

import java.util.List;

import org.jpmml.evaluator.FieldValue;

abstract
public class TernaryFunction extends AbstractFunction {

	public TernaryFunction(String name){
		super(name);
	}

	public TernaryFunction(String name, List<String> aliases){
		super(name, aliases);
	}

	abstract
	public FieldValue evaluate(FieldValue first, FieldValue second, FieldValue third);

	@Override
	public FieldValue evaluate(List<FieldValue> arguments){
		checkFixedArityArguments(arguments, 3);

		return evaluate(getArgument(arguments, 0), getArgument(arguments, 1), getArgument(arguments, 2));
	}
}