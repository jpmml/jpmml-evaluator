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

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.FieldValues;
import org.jpmml.evaluator.TypeUtil;
import org.jpmml.evaluator.UndefinedResultException;

abstract
public class ArithmeticFunction extends AbstractNumericFunction {

	public ArithmeticFunction(String name){
		super(name);
	}

	abstract
	public Number evaluate(Number left, Number right);

	@Override
	public FieldValue evaluate(List<FieldValue> arguments){
		checkFixedArityArguments(arguments, 2);

		return evaluate(getOptionalArgument(arguments, 0), getOptionalArgument(arguments, 1));
	}

	private FieldValue evaluate(FieldValue left, FieldValue right){

		// "If one of the input fields of a simple arithmetic function is a missing value, then the result evaluates to missing value"
		if(FieldValueUtil.isMissing(left) || FieldValueUtil.isMissing(right)){
			return FieldValues.MISSING_VALUE;
		}

		DataType dataType = TypeUtil.getCommonDataType(left.getDataType(), right.getDataType());

		Number result;

		try {
			result = evaluate(left.asNumber(), right.asNumber());
		} catch(ArithmeticException ae){
			throw new UndefinedResultException()
				.initCause(ae);
		}

		return FieldValueUtil.create(dataType, OpType.CONTINUOUS, result);
	}
}