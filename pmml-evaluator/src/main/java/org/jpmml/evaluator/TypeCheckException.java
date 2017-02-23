/*
 * Copyright (c) 2013 Villu Ruusmann
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

import org.dmg.pmml.DataType;
import org.dmg.pmml.Field;

/**
 * <p>
 * Thrown to indicate a violation of PMML type system.
 * This exception class can be regarded as the PMML equivalent of {@link ClassCastException}.
 * </p>
 */
public class TypeCheckException extends EvaluationException {

	public TypeCheckException(Field field, Object value){
		this(field.getDataType(), value);
	}

	public TypeCheckException(DataType expected, Object value){
		this(formatDataType(expected), formatDataType(getDataType(value)), value);
	}

	public TypeCheckException(Class<?> expected, Object value){
		this(formatClass(expected), formatClass(getClass(value)), value);
	}

	private TypeCheckException(String expected, String actual, Object value){
		super(formatMessage(expected, actual, value));
	}

	static
	private String formatMessage(String expected, String actual, Object value){
		String message = "Expected " + expected + ", but got " + actual;

		if(value != null){
			message += (" (" + String.valueOf(value) + ")");
		}

		return message;
	}

	static
	private DataType getDataType(Object value){

		if(value != null){

			try {
				return TypeUtil.getDataType(value);
			} catch(EvaluationException ee){
				// Ignored
			}
		}

		return null;
	}

	static
	private String formatDataType(DataType dataType){
		return String.valueOf(dataType);
	}

	static
	private Class<?> getClass(Object value){

		if(value != null){
			return value.getClass();
		}

		return null;
	}

	static
	private String formatClass(Class<?> clazz){
		return String.valueOf(clazz != null ? clazz.getName() : null);
	}
}