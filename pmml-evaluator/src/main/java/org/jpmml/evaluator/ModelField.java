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
package org.jpmml.evaluator;

import java.io.Serializable;
import java.util.Objects;

import org.dmg.pmml.DataType;
import org.dmg.pmml.Field;
import org.dmg.pmml.OpType;
import org.jpmml.model.ToStringHelper;

/**
 * <p>
 * A common superclass for all model fields.
 * </p>
 */
abstract
public class ModelField implements Serializable {

	private Field<?> field = null;


	ModelField(){
	}

	public ModelField(Field<?> field){
		setField(field);
	}

	public String getName(){
		Field<?> field = getField();

		return field.requireName();
	}

	public String getDisplayName(){
		Field<?> field = getField();

		return field.getDisplayName();
	}

	public OpType getOpType(){
		Field<?> field = getField();

		return field.getOpType();
	}

	public DataType getDataType(){
		Field<?> field = getField();

		return field.getDataType();
	}

	@Override
	public String toString(){
		ToStringHelper helper = toStringHelper();

		return helper.toString();
	}

	protected ToStringHelper toStringHelper(){
		ToStringHelper helper = new ToStringHelper(this)
			.add("name", getName())
			.add("displayName", getDisplayName())
			.add("opType", getOpType())
			.add("dataType", getDataType());

		return helper;
	}

	/**
	 * @return The backing {@link Field} element.
	 */
	public Field<?> getField(){
		return this.field;
	}

	private void setField(Field<?> field){
		this.field = Objects.requireNonNull(field);
	}
}
