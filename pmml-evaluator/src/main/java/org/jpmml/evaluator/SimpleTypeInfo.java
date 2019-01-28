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
package org.jpmml.evaluator;

import java.util.List;
import java.util.Objects;

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;

public class SimpleTypeInfo implements TypeInfo {

	private DataType dataType = null;

	private OpType opType = null;

	private List<?> ordering = null;


	public SimpleTypeInfo(DataType dataType, OpType opType){
		this(dataType, opType, null);
	}

	public SimpleTypeInfo(DataType dataType, OpType opType, List<?> ordering){
		setDataType(Objects.requireNonNull(dataType));
		setOpType(Objects.requireNonNull(opType));
		setOrdering(ordering);
	}

	@Override
	public DataType getDataType(){
		return this.dataType;
	}

	private void setDataType(DataType dataType){
		this.dataType = dataType;
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
}