/*
 * Copyright (c) 2018 Villu Ruusmann
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

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.jpmml.evaluator.FunctionException;

abstract
public class AbstractNumericFunction extends AbstractFunction {

	public AbstractNumericFunction(String name){
		super(name);
	}

	@Override
	protected void checkType(DataType dataType, OpType opType){
		super.checkType(dataType, opType);

		switch(dataType){
			case INTEGER:
			case FLOAT:
			case DOUBLE:
				break;
			default:
				throw new FunctionException(this, "Expected a numeric value, got " + dataType.value() + " value");
		}
	}
}