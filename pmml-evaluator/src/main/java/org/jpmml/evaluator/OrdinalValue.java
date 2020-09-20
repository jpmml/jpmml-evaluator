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

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.jpmml.model.ToStringHelper;

public class OrdinalValue extends DiscreteValue {

	private List<?> ordering = null;


	OrdinalValue(){
	}

	OrdinalValue(DataType dataType, List<?> ordering, Object value){
		super(dataType, value);

		setOrdering(ordering);
	}

	@Override
	public OpType getOpType(){
		return OpType.ORDINAL;
	}

	@Override
	public int compareToValue(Object value){
		List<?> ordering = getOrdering();
		if(ordering == null){
			return super.compareToValue(value);
		}

		return compare(ordering, getValue(), TypeUtil.parseOrCast(getDataType(), value));
	}

	@Override
	public int compareToValue(FieldValue value){
		List<?> ordering = getOrdering();
		if(ordering == null){
			return super.compareToValue(value);
		}

		return compare(ordering, getValue(), value.getValue(getDataType()));
	}

	@Override
	public int hashCode(){
		List<?> ordering = getOrdering();
		if(ordering == null){
			return super.hashCode();
		}

		return (31 * super.hashCode()) + ordering.hashCode();
	}

	@Override
	public boolean equals(Object object){

		if(object instanceof OrdinalValue){
			OrdinalValue that = (OrdinalValue)object;

			return super.equals(object) && Objects.equals(this.getOrdering(), that.getOrdering());
		}

		return false;
	}

	@Override
	protected ToStringHelper toStringHelper(){
		ToStringHelper helper = super.toStringHelper()
			.add("ordering", getOrdering());

		return helper;
	}

	@Override
	public List<?> getOrdering(){
		return this.ordering;
	}

	private void setOrdering(List<?> ordering){
		this.ordering = ordering;
	}

	static
	public FieldValue create(DataType dataType, List<?> ordering, Object value){

		if(ordering != null && ordering.isEmpty()){
			ordering = null;
		} // End if

		if(value instanceof Collection){
			return new CollectionValue(dataType, OpType.ORDINAL, ordering, (Collection<?>)value);
		}

		switch(dataType){
			case STRING:
				return new OrdinalString(ordering, value);
			default:
				return new OrdinalValue(dataType, ordering, value);
		}
	}

	static
	private int compare(List<?> ordering, Object left, Object right){
		int leftIndex = ordering.indexOf(left);
		int rightIndex = ordering.indexOf(right);

		if((leftIndex | rightIndex) < 0){
			throw new EvaluationException("Values " + PMMLException.formatValue(left) + " and " + PMMLException.formatValue(right) + " cannot be ordered");
		}

		return (leftIndex - rightIndex);
	}

	static
	private class OrdinalString extends OrdinalValue {

		private OrdinalString(){
		}

		OrdinalString(List<?> ordering, Object value){
			super(DataType.STRING, ordering, value);
		}

		@Override
		public boolean equalsValue(Object value){

			if(value instanceof String){
				return (asString()).equals(value);
			}

			return super.equalsValue(value);
		}

		@Override
		public String asString(){
			return (String)getValue();
		}
	}
}