/*
 * Copyright (c) 2017 Villu Ruusmann
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
package org.jpmml.evaluator.java;

import java.util.Objects;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValues;
import org.jpmml.evaluator.MissingValueException;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.mining.MiningModelEvaluationContext;

public class JavaModelEvaluationContext extends ModelEvaluationContext {

	public JavaModelEvaluationContext(JavaModelEvaluator javaModelEvaluator){
		super(javaModelEvaluator);
	}

	public JavaModelEvaluationContext(MiningModelEvaluationContext parent, JavaModelEvaluator javaModelEvaluator){
		super(parent, javaModelEvaluator);
	}

	@Override
	public JavaModelEvaluator getModelEvaluator(){
		return (JavaModelEvaluator)super.getModelEvaluator();
	}

	/**
	 * @throws MissingValueException
	 */
	public FieldValue evaluateRequired(FieldName name){
		FieldValue value = evaluate(name);

		if(Objects.equals(FieldValues.MISSING_VALUE, value)){
			throw new MissingValueException(name);
		}

		return value;
	}

	/**
	 * @throws MissingValueException
	 */
	public FieldValue evaluateRequired(int index){
		FieldValue value = evaluate(index);

		if(Objects.equals(FieldValues.MISSING_VALUE, value)){
			throw new MissingValueException();
		}

		return value;
	}
}