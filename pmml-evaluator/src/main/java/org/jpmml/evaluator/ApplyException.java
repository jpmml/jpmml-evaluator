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

import java.util.Objects;

import org.dmg.pmml.Apply;
import org.dmg.pmml.PMMLObject;

/**
 * <p>
 * Thrown to indicate an incorrect function call.
 * </p>
 *
 * @see Apply
 */
public class ApplyException extends EvaluationException {

	private String function = null;


	public ApplyException(String function, String message){
		super(message);

		setFunction(function);
	}

	public ApplyException(String function, String message, Apply apply){
		super(message, apply);

		setFunction(function);
	}

	@Override
	public Apply getContext(){
		return (Apply)super.getContext();
	}

	@Override
	public ApplyException ensureContext(PMMLObject parentContext){

		if(!(parentContext instanceof Apply)){
			throw new IllegalArgumentException();
		}

		return (ApplyException)super.ensureContext(parentContext);
	}

	public String getFunction(){
		return this.function;
	}

	private void setFunction(String function){
		this.function = Objects.requireNonNull(function);
	}
}