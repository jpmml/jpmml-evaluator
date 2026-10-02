/*
 * Copyright (c) 2021 Villu Ruusmann
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

import java.util.Objects;

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;

/**
 * <p>
 * An unnamed local data field.
 * </p>
 */
public class DefaultLocalDataField extends LocalDataField {

	public DefaultLocalDataField(OpType opType, DataType dataType){
		super(Evaluator.DEFAULT_TARGET_NAME, opType, dataType);
	}

	@Override
	public String requireName(){
		return getName();
	}

	@Override
	public String getName(){
		return super.getName();
	}

	@Override
	public DefaultLocalDataField setName(String name){

		if(!Objects.equals(name, Evaluator.DEFAULT_TARGET_NAME)){
			throw new IllegalArgumentException();
		}

		return (DefaultLocalDataField)super.setName(name);
	}

	public static final DefaultLocalDataField CONTINUOUS_FLOAT = new DefaultLocalDataField(OpType.CONTINUOUS, DataType.FLOAT);
	public static final DefaultLocalDataField CONTINUOUS_DOUBLE = new DefaultLocalDataField(OpType.CONTINUOUS, DataType.DOUBLE);
	public static final DefaultLocalDataField CATEGORICAL_STRING = new DefaultLocalDataField(OpType.CATEGORICAL, DataType.STRING);
}