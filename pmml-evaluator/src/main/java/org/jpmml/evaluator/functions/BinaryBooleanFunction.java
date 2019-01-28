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
public class BinaryBooleanFunction extends AbstractFunction {

	public BinaryBooleanFunction(String name){
		super(name);
	}

	abstract
	public Boolean evaluate(Boolean left, Boolean right);

	@Override
	public FieldValue evaluate(List<FieldValue> arguments){
		checkVariableArityArguments(arguments, 2);

		Boolean result = getRequiredArgument(arguments, 0).asBoolean();

		for(int i = 1; i < arguments.size(); i++){
			result = evaluate(result, getRequiredArgument(arguments, i).asBoolean());
		}

		return FieldValueUtil.create(TypeInfos.CATEGORICAL_BOOLEAN, result);
	}
}