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
package org.jpmml.evaluator;

import java.util.LinkedHashMap;
import java.util.Map;

public class FunctionRegistry {

	private FunctionRegistry(){
	}

	static
	public Function getFunction(String name){
		Function function = FunctionRegistry.functions.get(name);
		if(function == null){
			function = loadJavaFunction(name);
		}

		return function;
	}

	static
	public void putFunctions(Function... functions){

		for(Function function : functions){
			putFunction(function);
		}
	}

	static
	public void putFunction(Function function){
		putFunction(function.getName(), function);
	}

	static
	public void putFunction(String name, Function function){
		FunctionRegistry.functions.put(name, function);
	}

	static
	private Function loadJavaFunction(String name){
		Class<?> clazz;

		try {
			ClassLoader classLoader = (Thread.currentThread()).getContextClassLoader();
			if(classLoader == null){
				classLoader = (FunctionRegistry.class).getClassLoader();
			}

			clazz = classLoader.loadClass(name);
		} catch(ClassNotFoundException cnfe){
			return null;
		}

		if(!(Function.class).isAssignableFrom(clazz)){
			return null;
		}

		Function function;

		try {
			function = (Function)clazz.newInstance();
		} catch(Exception e){
			throw new EvaluationException();
		}

		return function;
	}

	private static final Map<String, Function> functions = new LinkedHashMap<>();

	static {
		putFunctions(Functions.PLUS, Functions.MINUS, Functions.MULTIPLY, Functions.DIVIDE);
		putFunctions(Functions.MIN, Functions.MAX, Functions.AVG, Functions.SUM, Functions.PRODUCT);
		putFunctions(Functions.LOG10, Functions.LN, Functions.EXP, Functions.SQRT, Functions.ABS, Functions.POW, Functions.THRESHOLD, Functions.FLOOR, Functions.CEIL, Functions.ROUND);
		putFunctions(Functions.IS_MISSING, Functions.IS_NOT_MISSING);
		putFunctions(Functions.EQUAL, Functions.NOT_EQUAL);
		putFunctions(Functions.LESS_THAN, Functions.LESS_OR_EQUAL, Functions.GREATER_THAN, Functions.GREATER_OR_EQUAL);
		putFunctions(Functions.AND, Functions.OR);
		putFunctions(Functions.NOT);
		putFunctions(Functions.IS_IN, Functions.IS_NOT_IN);
		putFunctions(Functions.IF);
		putFunctions(Functions.UPPERCASE, Functions.LOWERCASE, Functions.SUBSTRING, Functions.TRIM_BLANKS);
		putFunctions(Functions.CONCAT);
		putFunctions(Functions.REPLACE, Functions.MATCHES);
		putFunctions(Functions.FORMAT_NUMBER, Functions.FORMAT_DATETIME);
		putFunctions(Functions.DATE_DAYS_SINCE_YEAR, Functions.DATE_SECONDS_SINCE_MIDNIGHT, Functions.DATE_SECONDS_SINCE_YEAR);
	}
}