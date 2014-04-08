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

import java.util.*;

import org.jpmml.evaluator.*;
import org.jpmml.evaluator.FieldValue;

import org.dmg.pmml.*;

import static com.google.common.base.Preconditions.*;

abstract
public class AbstractFunction implements Function {

	private String name = null;


	public AbstractFunction(String name){
		setName(name);
	}

	protected void checkArguments(List<FieldValue> arguments, int size){
		checkArguments(arguments, size, false);
	}

	protected void checkArguments(List<FieldValue> arguments, int size, boolean allowNulls){

		if(arguments.size() != size){
			throw new FunctionException(getName(), "Expected " + size + " arguments, but got " + arguments.size() + " arguments");
		} // End if

		if(!allowNulls && arguments.contains(null)){
			throw new FunctionException(getName(), "Missing arguments");
		}
	}

	protected void checkVariableArguments(List<FieldValue> arguments, int size){
		checkVariableArguments(arguments, size, false);
	}

	protected void checkVariableArguments(List<FieldValue> arguments, int size, boolean allowNulls){

		if(arguments.size() < size){
			throw new FunctionException(getName(), "Expected " + size + " or more arguments, but got " + arguments.size() + " arguments");
		} // End if

		if(!allowNulls && arguments.contains(null)){
			throw new FunctionException(getName(), "Missing arguments");
		}
	}

	@Override
	public String getName(){
		return this.name;
	}

	private void setName(String name){
		this.name = checkNotNull(name);
	}

	static
	protected Number cast(DataType dataType, Number number){

		switch(dataType){
			case INTEGER:
				if(number instanceof Integer){
					return number;
				}
				return Integer.valueOf(number.intValue());
			case FLOAT:
				if(number instanceof Float){
					return number;
				}
				return Float.valueOf(number.floatValue());
			case DOUBLE:
				if(number instanceof Double){
					return number;
				}
				return Double.valueOf(number.doubleValue());
			default:
				break;
		}

		throw new EvaluationException();
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