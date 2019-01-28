/*
 * Copyright (c) 2014 Villu Ruusmann
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
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.TypeInfos;

abstract
public class ComparisonFunction extends AbstractFunction {

	public ComparisonFunction(String name){
		super(name);
	}

	abstract
	public Boolean evaluate(int order);

	@Override
	public FieldValue evaluate(List<FieldValue> arguments){
		checkFixedArityArguments(arguments, 2);

		return evaluate(getRequiredArgument(arguments, 0), getRequiredArgument(arguments, 1));
	}

	private FieldValue evaluate(FieldValue left, FieldValue right){
		Boolean result = evaluate((left).compareToValue(right));

		return FieldValueUtil.create(TypeInfos.CATEGORICAL_BOOLEAN, result);
	}
}