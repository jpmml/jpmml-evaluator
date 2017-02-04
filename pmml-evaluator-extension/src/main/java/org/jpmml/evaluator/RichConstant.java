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

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.dmg.pmml.Constant;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.jpmml.model.ReflectionUtil;

@XmlRootElement (
	name = "Constant"
)
public class RichConstant extends Constant implements HasParsedValue<RichConstant> {

	@XmlTransient
	private FieldValue parsedValue = null;


	public RichConstant(){
	}

	public RichConstant(Constant constant){
		ReflectionUtil.copyState(constant, this);
	}

	@Override
	public FieldName getField(){
		throw new UnsupportedOperationException();
	}

	@Override
	public String getValue(){
		return super.getValue();
	}

	@Override
	public RichConstant setValue(String value){
		return (RichConstant)super.setValue(value);
	}

	@Override
	public FieldValue getValue(DataType dataType, OpType opType){

		if((dataType != null) || (opType != null)){
			throw new IllegalArgumentException();
		} // End if

		if(this.parsedValue == null){
			this.parsedValue = FieldValueUtil.create(ExpressionUtil.getConstantDataType(this), null, getValue());
		}

		return this.parsedValue;
	}
}