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
import java.util.Objects;

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.FieldValues;

abstract
public class ValueListFunction extends AbstractFunction {

	public ValueListFunction(String name){
		super(name);
	}

	abstract
	public Boolean evaluate(boolean isIn);

	@Override
	public FieldValue evaluate(List<FieldValue> arguments){
		checkVariableArityArguments(arguments, 2);

		return evaluate(getOptionalArgument(arguments, 0), arguments.subList(1, arguments.size()));
	}

	private FieldValue evaluate(FieldValue value, List<FieldValue> values){
		Boolean result;

		if(Objects.equals(FieldValues.MISSING_VALUE, value)){
			result = evaluate(values.contains(FieldValues.MISSING_VALUE));
		} else

		{
			result = evaluate(value.indexIn(values) > -1);
		}

		return FieldValueUtil.create(DataType.BOOLEAN, OpType.CATEGORICAL, result);
	}
}