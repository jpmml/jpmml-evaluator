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

	private String name = null;


	ModelField(){
	}

	public ModelField(Field<?> field){
		setField(field);
	}

	/**
	 * <p>
	 * The name of this model field in user application space.
	 * </p>
	 *
	 * @see #getFieldName()
	 */
	public String getName(){

		if(this.name == null){
			return getFieldName();
		}

		return this.name;
	}

	/**
	 * <p>
	 * Soft-renames this model field.
	 * </p>
	 *
	 * @param name The new name of the model field.
	 * Use <code>null</code> to restore the origial name of the model field.
	 */
	void setName(String name){
		this.name = name;
	}

	/**
	 * <p>
	 * The name of this model field in PMML space.
	 * </p>
	 */
	public String getFieldName(){
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
			.add("fieldName", getFieldName())
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
