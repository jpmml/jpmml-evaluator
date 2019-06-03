/*
 * Copyright (c) 2016 Villu Ruusmann
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
package org.jpmml.evaluator.functions;

import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.NaNResultException;
import org.jpmml.evaluator.TypeInfos;

abstract
public class TrigonometricFunction extends UnaryMathFunction {

	public TrigonometricFunction(String name){
		super(name);
	}

	@Override
	abstract
	public Double evaluate(Number value);

	/**
	 * @param value Angle in radians.
	 */
	@Override
	public FieldValue evaluate(FieldValue value){
		Double result = evaluate(value.asNumber());
		if(result.isNaN()){
			throw new NaNResultException();
		}

		return FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, result);
	}
}