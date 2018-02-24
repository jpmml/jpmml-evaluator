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

import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValues;
import org.jpmml.evaluator.Function;
import org.jpmml.evaluator.FunctionException;

abstract
public class AbstractFunction implements Function {

	private String name = null;


	public AbstractFunction(String name){
		setName(Objects.requireNonNull(name));
	}

	protected void checkFixedArityArguments(List<FieldValue> arguments, int arity){

		if(arguments.size() != arity){
			throw new FunctionException(this, "Expected " + arity + " values, got " + arguments.size() + " values");
		}
	}

	protected void checkVariableArityArguments(List<FieldValue> arguments, int minArity){

		if(arguments.size() < minArity){
			throw new FunctionException(this, "Expected " + minArity + " or more values, got " + arguments.size() + " values");
		}
	}

	protected void checkVariableArityArguments(List<FieldValue> arguments, int minArity, int maxArity){

		if(arguments.size() < minArity || arguments.size() > maxArity){
			throw new FunctionException(this, "Expected " + minArity + " to " + maxArity + " values, got " + arguments.size() + " values");
 		}
 	}

	protected FieldValue getOptionalArgument(List<FieldValue> arguments, int index){
		return getOptionalArgument(arguments, index, null);
	}

	protected FieldValue getOptionalArgument(List<FieldValue> arguments, int index, String alias){
		FieldValue argument = arguments.get(index);

		return checkArgument(argument, index, alias);
	}

	protected FieldValue getRequiredArgument(List<FieldValue> arguments, int index){
		return getRequiredArgument(arguments, index, null);
	}

	protected FieldValue getRequiredArgument(List<FieldValue> arguments, int index, String alias){
		FieldValue argument = arguments.get(index);

		if(Objects.equals(FieldValues.MISSING_VALUE, argument)){

			if(alias != null){
				throw new FunctionException(this, "Missing \'" + alias + "\' value at position " + index);
			} else

			{
				throw new FunctionException(this, "Missing value at position " + index);
			}
		}

		return checkArgument(argument, index, alias);
	}

	protected FieldValue checkArgument(FieldValue argument, int index, String alias){
		return argument;
	}

	@Override
	public String getName(){
		return this.name;
	}

	private void setName(String name){
		this.name = name;
	}
}