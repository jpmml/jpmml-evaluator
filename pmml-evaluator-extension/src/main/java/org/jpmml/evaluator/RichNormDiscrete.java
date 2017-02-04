/*
 * Copyright (c) 2015 Villu Ruusmann
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

import org.dmg.pmml.DataType;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.OpType;
import org.jpmml.model.ReflectionUtil;

@XmlRootElement (
	name = "NormDiscrete"
)
public class RichNormDiscrete extends NormDiscrete implements HasParsedValue<NormDiscrete> {

	@XmlTransient
	private FieldValue parsedValue = null;


	public RichNormDiscrete(){
	}

	public RichNormDiscrete(NormDiscrete normDiscrete){
		ReflectionUtil.copyState(normDiscrete, this);
	}

	@Override
	public FieldValue getValue(DataType dataType, OpType opType){

		if(this.parsedValue == null){
			this.parsedValue = FieldValueUtil.create(dataType, opType, getValue());
		}

		return this.parsedValue;
	}
}