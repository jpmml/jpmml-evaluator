/*
 * Copyright (c) 2022 Villu Ruusmann
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

import java.util.List;

public class InvalidArgumentListException extends ApplyException {

	public InvalidArgumentListException(String function, String message){
		super(function, message);
	}

	static
	public String formatFixedArityMessage(String function, int arity, List<FieldValue> arguments){
		return "Function " + EvaluationException.formatName(function) + " expects " + arity + " values, got " + arguments.size() + " values";
	}

	static
	public String formatVariableArityMessage(String function, int minArity, List<FieldValue> arguments){
		return "Function " + EvaluationException.formatName(function) + " expects " + minArity + " or more values, got " + arguments.size() + " values";
	}

	static
	public String formatVariableArityMessage(String function, int minArity, int maxArity, List<FieldValue> arguments){
		return "Function " + EvaluationException.formatName(function) + " expects " + minArity + " to " + maxArity + " values, got " + arguments.size() + " values";
	}
}