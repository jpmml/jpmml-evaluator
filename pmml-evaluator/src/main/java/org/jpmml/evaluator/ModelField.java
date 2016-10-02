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

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;

/**
 * <p>
 * A common superclass for all model fields.
 * </p>
 */
abstract
public class ModelField implements Serializable {

	abstract
	public FieldName getName();

	abstract
	public DataType getDataType();

	abstract
	public OpType getOpType();

	@Override
	public String toString(){
		ToStringHelper helper = toStringHelper();

		return helper.toString();
	}

	protected ToStringHelper toStringHelper(){
		ToStringHelper helper = Objects.toStringHelper(this)
			.add("name", getName())
			.add("dataType", getDataType())
			.add("opType", getOpType());

		return helper;
	}
}