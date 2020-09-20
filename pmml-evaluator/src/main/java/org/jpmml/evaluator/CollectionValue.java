/*
 * Copyright (c) 2019 Villu Ruusmann
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

public class CollectionValue extends FieldValue {

	private OpType opType = null;

	private List<?> ordering = null;


	private CollectionValue(){
	}

	CollectionValue(DataType dataType, OpType opType, Collection<?> value){
		this(dataType, opType, null, value);
	}

	CollectionValue(DataType dataType, OpType opType, List<?> ordering, Collection<?> value){
		super(dataType, value);

		setOpType(Objects.requireNonNull(opType));

		switch(opType){
			case CONTINUOUS:
			case CATEGORICAL:
				if(ordering != null){
					throw new IllegalArgumentException();
				}
				break;
			case ORDINAL:
				setOrdering(ordering);
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public boolean isValid(){
		throw new EvaluationException("Collection value cannot be queried for validity");
	}

	@Override
	public int compareToValue(Object value){
		throw new EvaluationException("Collection value cannot be used in comparison operations");
	}

	@Override
	public int compareToValue(FieldValue value){
		throw new EvaluationException("Collection value cannot be used in comparison operations");
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

		if(object instanceof CollectionValue){
			CollectionValue that = (CollectionValue)object;

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
	public OpType getOpType(){
		return this.opType;
	}

	private void setOpType(OpType opType){
		this.opType = opType;
	}

	@Override
	public List<?> getOrdering(){
		return this.ordering;
	}

	private void setOrdering(List<?> ordering){
		this.ordering = ordering;
	}

	@Override
	public Collection<?> getValue(){
		return (Collection<?>)super.getValue();
	}
}