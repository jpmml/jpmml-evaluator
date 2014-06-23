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

import java.util.List;

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;

public class OrdinalValue extends FieldValue {

	private List<?> ordering = null;


	public OrdinalValue(DataType dataType, Object value){
		super(dataType, value);
	}

	@Override
	public OpType getOpType(){
		return OpType.ORDINAL;
	}

	@Override
	public int compareToString(String string){
		List<?> ordering = getOrdering();
		if(ordering == null){
			return super.compareToString(string);
		}

		return compare(ordering, getValue(), parseValue(string));
	}

	@Override
	public int compareToValue(FieldValue value){
		List<?> ordering = getOrdering();
		if(ordering == null){
			return super.compareToValue(value);
		}

		return compare(ordering, getValue(), value.getValue());
	}

	public List<?> getOrdering(){
		return this.ordering;
	}

	public void setOrdering(List<?> ordering){
		this.ordering = ordering;
	}

	static
	private int compare(List<?> ordering, Object left, Object right){
		int leftIndex = ordering.indexOf(left);
		int rightIndex = ordering.indexOf(right);

		if((leftIndex | rightIndex) < 0){
			throw new EvaluationException();
		}

		return (leftIndex - rightIndex);
	}
}