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
import java.util.Objects;

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
		Function function = FunctionRegistry.functions.get(name);
		if(function == null){
			function = loadJavaFunction(name);
		}

		return function;
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
		FunctionRegistry.functions.put(Objects.requireNonNull(name), function);
	}

	static
	public Function removeFunction(String name){
		return FunctionRegistry.functions.remove(name);
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
			throw new TypeCheckException(Function.class, clazz);
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
}