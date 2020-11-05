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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;

/**
 * <p>
 * A registry of Java user-defined functions (Java UDFs).
 * </p>
 */
public class FunctionRegistry {

	private FunctionRegistry(){
	}

	/**
	 * <p>
	 * Gets a function for a name.
	 * <p>
	 *
	 * <p>
	 * First, if the name is registered with a singleton function instance, returns that instance.
	 * After that, if the name represents a {@link Function} class on the application classpath, loads it, and returns a newly created instance.
	 * </p>
	 */
	static
	public Function getFunction(String name){

		if(name == null){
			return null;
		} // End if

		if(FunctionRegistry.pmmlFunctions.containsKey(name)){
			Function function = FunctionRegistry.pmmlFunctions.get(name);

			return function;
		} // End if

		if(FunctionRegistry.userDefinedFunctions.containsKey(name)){
			Function function = FunctionRegistry.userDefinedFunctions.get(name);

			return function;
		}

		Class<?> functionClazz;

		if(FunctionRegistry.userDefinedFunctionClazzes.containsKey(name)){
			functionClazz = FunctionRegistry.userDefinedFunctionClazzes.get(name);
		} else

		{
			functionClazz = loadFunctionClass(name);

			FunctionRegistry.userDefinedFunctionClazzes.put(name, functionClazz);
		} // End if

		if(functionClazz != null){
			Function function;

			try {
				function = (Function)functionClazz.newInstance();
			} catch(IllegalAccessException | InstantiationException | ExceptionInInitializerError e){
				throw new EvaluationException("Function class " + PMMLException.formatKey(functionClazz.getName()) + " could not be instantiated")
					.initCause(e);
			}

			return function;
		}

		return null;
	}

	/**
	 * <p>
	 * Registers a function by its default name.
	 * </p>
	 */
	static
	public void putFunction(Function function){
		putFunction(function.getName(), function);
	}

	/**
	 * <p>
	 * Registers a function by a name other than its default name.
	 * </p>
	 */
	static
	public void putFunction(String name, Function function){
		FunctionRegistry.userDefinedFunctions.put(Objects.requireNonNull(name), function);
	}

	/**
	 * <p>
	 * Registers a function class.
	 * </p>
	 */
	static
	public void putFunction(String name, Class<? extends Function> functionClazz){
		FunctionRegistry.userDefinedFunctionClazzes.put(Objects.requireNonNull(name), checkClass(functionClazz));
	}

	static
	public void removeFunction(String name){
		FunctionRegistry.userDefinedFunctions.remove(name);
		FunctionRegistry.userDefinedFunctionClazzes.remove(name);
	}

	static
	private Class<?> loadFunctionClass(String name){
		Class<?> clazz;

		try {
			Thread thread = Thread.currentThread();

			ClassLoader classLoader = thread.getContextClassLoader();
			if(classLoader == null){
				classLoader = (FunctionRegistry.class).getClassLoader();
			}

			clazz = classLoader.loadClass(name);
		} catch(ClassNotFoundException cnfe){
			return null;
		}

		return checkClass(clazz);
	}

	static
	private Class<?> checkClass(Class<?> clazz){

		if(!(Function.class).isAssignableFrom(clazz)){
			throw new TypeCheckException(Function.class, clazz);
		}

		return clazz;
	}

	private static final Map<String, Function> pmmlFunctions;

	static {
		ImmutableMap.Builder<String, Function> builder = new ImmutableMap.Builder<>();

		List<? extends Function> functions = Arrays.asList(
			Functions.ADD, Functions.SUBTRACT, Functions.MULTIPLY, Functions.DIVIDE,
			Functions.MIN, Functions.MAX, Functions.AVG, Functions.SUM, Functions.PRODUCT,
			Functions.LOG10, Functions.LN, Functions.EXP, Functions.SQRT, Functions.ABS, Functions.POW, Functions.THRESHOLD, Functions.FLOOR, Functions.CEIL, Functions.ROUND, Functions.MODULO,
			Functions.IS_MISSING, Functions.IS_NOT_MISSING, Functions.IS_VALID, Functions.IS_NOT_VALID,
			Functions.EQUAL, Functions.NOT_EQUAL,
			Functions.LESS_THAN, Functions.LESS_OR_EQUAL, Functions.GREATER_THAN, Functions.GREATER_OR_EQUAL,
			Functions.AND, Functions.OR,
			Functions.NOT,
			Functions.IS_IN, Functions.IS_NOT_IN,
			Functions.IF,
			Functions.UPPERCASE, Functions.LOWERCASE, Functions.STRING_LENGTH, Functions.SUBSTRING, Functions.TRIM_BLANKS,
			Functions.CONCAT,
			Functions.REPLACE, Functions.MATCHES,
			Functions.FORMAT_NUMBER, Functions.FORMAT_DATETIME,
			Functions.DATE_DAYS_SINCE_YEAR, Functions.DATE_SECONDS_SINCE_MIDNIGHT, Functions.DATE_SECONDS_SINCE_YEAR,
			Functions.EXPM1, Functions.HYPOT, Functions.LN1P, Functions.RINT,
			Functions.SIN, Functions.ASIN, Functions.SINH, Functions.COS, Functions.ACOS, Functions.COSH, Functions.TAN, Functions.ATAN, Functions.TANH
		);

		for(Function function : functions){
			builder.put(function.getName(), function);
		}

		List<? extends Function> extensionFunctions = Arrays.asList(
			Functions.ATAN2
		);

		for(Function extensionFunction : extensionFunctions){
			builder.put(extensionFunction.getName(), extensionFunction);
		}

		pmmlFunctions = builder.build();
	}

	private static final Map<String, Function> userDefinedFunctions = new LinkedHashMap<>();

	private static final Map<String, Class<?>> userDefinedFunctionClazzes = new LinkedHashMap<>();
}