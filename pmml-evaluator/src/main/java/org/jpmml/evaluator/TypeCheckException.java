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

/**
 * <p>
 * Thrown to indicate a violation of PMML type system.
 * This exception class can be regarded as the PMML equivalent of {@link ClassCastException}.
 * </p>
 */
public class TypeCheckException extends EvaluationException {

	public TypeCheckException(String message){
		super(message);
	}

	public TypeCheckException(DataType expected, Object actual){
		this(formatMessage(formatDataType(expected), formatDataType(getDataType(actual)), actual));
	}

	public TypeCheckException(Class<?> expected, Object actual){
		this(formatMessage(formatClass(expected), formatClass(getClass(actual)), actual));
	}

	static
	private String formatMessage(String expected, String actual, Object value){
		return "Expected " + (expected + " value") + ", got " + (value != null ? (actual + " value") : "missing value (null)");
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
		return (dataType != null ? dataType.value() : null);
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
		return (clazz != null ? clazz.getName() : null);
	}
}
