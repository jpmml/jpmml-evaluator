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
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.Function;
import org.jpmml.evaluator.FunctionException;
import org.jpmml.evaluator.PMMLException;

abstract
public class AbstractFunction implements Function {

	private String name = null;

	private List<String> aliases = null;


	public AbstractFunction(String name){
		this(name, null);
	}

	public AbstractFunction(String name, List<String> aliases){
		setName(Objects.requireNonNull(name));
		setAliases(aliases);
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

	protected FieldValue getArgument(List<FieldValue> arguments, int index){

		if(this instanceof MissingValueTolerant){
			return getOptionalArgument(arguments, index);
		}

		return getRequiredArgument(arguments, index);
	}

	protected FieldValue getOptionalArgument(List<FieldValue> arguments, int index){
		FieldValue argument = arguments.get(index);

		return argument;
	}

	protected FieldValue getRequiredArgument(List<FieldValue> arguments, int index){
		FieldValue argument = arguments.get(index);

		if(FieldValueUtil.isMissing(argument)){
			String alias = null;

			List<String> aliases = getAliases();
			if((aliases != null) && (index < aliases.size())){
				alias = aliases.get(index);
			} // End if

			if(alias != null){
				throw new FunctionException(this, "Missing " + PMMLException.formatKey(alias) + " value at position " + index);
			} else

			{
				throw new FunctionException(this, "Missing value at position " + index);
			}
		}

		return argument;
	}

	@Override
	public String getName(){
		return this.name;
	}

	private void setName(String name){
		this.name = name;
	}

	public List<String> getAliases(){
		return this.aliases;
	}

	private void setAliases(List<String> aliases){
		this.aliases = aliases;
	}
}