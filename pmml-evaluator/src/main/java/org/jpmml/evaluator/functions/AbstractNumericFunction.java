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

import java.util.Objects;

import org.dmg.pmml.DataType;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValues;
import org.jpmml.evaluator.FunctionException;

abstract
public class AbstractNumericFunction extends AbstractFunction {

	public AbstractNumericFunction(String name){
		super(name);
	}

	@Override
	protected FieldValue checkArgument(FieldValue argument, int index, String alias){

		if(Objects.equals(FieldValues.MISSING_VALUE, argument)){
			return argument;
		}

		DataType dataType = argument.getDataType();
		switch(dataType){
			case INTEGER:
			case FLOAT:
			case DOUBLE:
				break;
			default:
				if(alias != null){
					throw new FunctionException(this, "Expected a numeric \'" + alias + "\' value at position " + index + ", got " + dataType.value() + " value");
				} else

				{
					throw new FunctionException(this, "Expected a numeric value at position " + index + ", got " + dataType.value() + " value");
				}
		}

		return argument;
	}
}