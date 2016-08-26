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

import java.util.Objects;

import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;

public class OutputField extends ResultField {

	private org.dmg.pmml.OutputField outputField = null;


	public OutputField(org.dmg.pmml.OutputField outputField){
		setOutputField(Objects.requireNonNull(outputField));
	}

	@Override
	public FieldName getName(){
		org.dmg.pmml.OutputField outputField = getOutputField();

		return outputField.getName();
	}

	@Override
	public DataType getDataType(){
		org.dmg.pmml.OutputField outputField = getOutputField();

		return outputField.getDataType();
	}

	@Override
	public OpType getOpType(){
		org.dmg.pmml.OutputField outputField = getOutputField();

		return outputField.getOpType();
	}

	public org.dmg.pmml.OutputField getOutputField(){
		return this.outputField;
	}

	private void setOutputField(org.dmg.pmml.OutputField outputField){
		this.outputField = outputField;
	}
}