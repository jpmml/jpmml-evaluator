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
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.Function;
import org.jpmml.evaluator.FunctionException;

abstract
public class AbstractFunction implements Function {

	private String name = null;


	public AbstractFunction(String name){
		setName(Objects.requireNonNull(name));
	}

	protected void checkArguments(List<FieldValue> arguments, int size){
		checkArguments(arguments, size, false);
	}

	/**
	 * <p>
	 * Validates arguments for a function that has a fixed number of formal parameters.
	 * </p>
	 *
	 * @param size The number of arguments.
	 * @param allowNulls <code>true</code> if missing arguments are permitted, <code>false</code> otherwise.
	 *
	 * @throws FunctionException If the validation fails.
	 */
	protected void checkArguments(List<FieldValue> arguments, int size, boolean allowNulls){

		if(arguments.size() != size){
			throw new FunctionException(this, "Expected " + size + " arguments, but got " + arguments.size() + " arguments");
		} // End if

		if(!allowNulls && arguments.contains(null)){
			throw new FunctionException(this, "Missing arguments");
		}
	}

	protected void checkVariableArguments(List<FieldValue> arguments, int minSize){
		checkVariableArguments(arguments, minSize, false);
	}

	/**
	 * <p>
	 * Validates arguments for a function that has a variable number ("<code>n</code> or more") of formal parameters.
	 * </p>
	 *
	 * @param minSize The minimum number of arguments.
	 * @param allowNulls <code>true</code> if missing arguments are allowed, <code>false</code> otherwise.
	 *
	 * @throws FunctionException If the validation fails.
	 */
	protected void checkVariableArguments(List<FieldValue> arguments, int minSize, boolean allowNulls){

		if(arguments.size() < minSize){
			throw new FunctionException(this, "Expected " + minSize + " or more arguments, but got " + arguments.size() + " arguments");
		} // End if

		if(!allowNulls && arguments.contains(null)){
			throw new FunctionException(this, "Missing arguments");
		}
	}

	@Override
	public String getName(){
		return this.name;
	}

	private void setName(String name){
		this.name = name;
	}

	static
	protected DataType integerToDouble(DataType dataType){

		switch(dataType){
			case INTEGER:
				return DataType.DOUBLE;
			default:
				break;
		}

		return dataType;
	}
}