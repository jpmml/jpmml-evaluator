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

import org.dmg.pmml.*;

public class TypeCheckException extends EvaluationException {

	public TypeCheckException(DataType expected, FieldValue value){
		this(expected, FieldValueUtil.getValue(value));
	}

	public TypeCheckException(DataType expected, Object value){
		super(formatMessage(expected, (value != null ? TypeUtil.getDataType(value) : null), value));
	}

	public TypeCheckException(Class<?> expected, FieldValue value){
		this(expected, FieldValueUtil.getValue(value));
	}

	public TypeCheckException(Class<?> expected, Object value){
		super(formatMessage(expected, (value != null ? value.getClass() : null), value));
	}

	static
	private String formatMessage(DataType expected, DataType actual, Object value){
		String message = "Expected: " + expected + ", actual: " + (actual != null ? actual : "null");

		if(value != null){
			message += (" (" + String.valueOf(value) + ")");
		}

		return message;
	}

	static
	private String formatMessage(Class<?> expected, Class<?> actual, Object value){
		String message = "Expected: " + expected.getName() + ", actual: " + (actual != null ? actual.getName() : "null");

		if(value != null){
			message += (" (" + String.valueOf(value) + ")");
		}

		return message;
	}
}