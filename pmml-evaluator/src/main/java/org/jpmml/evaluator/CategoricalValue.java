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
import org.dmg.pmml.OpType;

public class CategoricalValue extends FieldValue {

	public CategoricalValue(DataType dataType, Object value){
		super(dataType, value);
	}

	@Override
	public OpType getOpType(){
		return OpType.CATEGORICAL;
	}

	@Override
	public int compareToString(String string){
		DataType dataType = getDataType();

		if((DataType.BOOLEAN).equals(dataType)){
			Object value;

			try {
				value = TypeUtil.parse(DataType.DOUBLE, string);
			} catch(NumberFormatException nfe){
				throw new TypeCheckException(DataType.DOUBLE, string);
			}

			return TypeUtil.compare(DataType.DOUBLE, asBoolean(), value);
		}

		throw new EvaluationException();
	}

	@Override
	public int compareToValue(FieldValue that){
		DataType dataType = getDataType();

		if((DataType.BOOLEAN).equals(dataType)){
			Object value;

			try {
				value = that.asNumber();
			} catch(TypeCheckException tce){
				throw new TypeCheckException(DataType.DOUBLE, that.getValue());
			}

			return TypeUtil.compare(DataType.DOUBLE, asBoolean(), value);
		}

		throw new EvaluationException();
	}
}