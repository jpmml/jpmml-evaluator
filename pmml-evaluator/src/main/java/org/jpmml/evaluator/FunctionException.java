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


/**
 * <p>
 * Thrown to indicate an incorrect function invocation.
 * </p>
 *
 * @see Function
 */
public class FunctionException extends EvaluationException {

	private Function function = null;


	public FunctionException(Function function, String message){
		super(message);

		setFunction(function);
	}

	public Function getFunction(){
		return this.function;
	}

	private void setFunction(Function function){
		this.function = function;
	}
}