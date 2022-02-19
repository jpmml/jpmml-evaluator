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

import java.util.List;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Value;
import org.jpmml.model.UnsupportedAttributeException;

public class TargetFieldUtil {

	private TargetFieldUtil(){
	}

	static
	public Value getValidValue(DataField dataField, Object value){

		if(value == null){
			return null;
		} // End if

		if(dataField.hasValues()){
			DataType dataType = dataField.requireDataType();

			value = TypeUtil.parseOrCast(dataType, value);

			List<Value> pmmlValues = dataField.getValues();
			for(int i = 0, max = pmmlValues.size(); i < max; i++){
				Value pmmlValue = pmmlValues.get(i);

				Object objectValue = pmmlValue.requireValue();

				Value.Property property = pmmlValue.getProperty();
				switch(property){
					case VALID:
						{
							boolean equals = TypeUtil.equals(dataType, value, objectValue);

							if(equals){
								return pmmlValue;
							}
						}
						break;
					case INVALID:
					case MISSING:
						break;
					default:
						throw new UnsupportedAttributeException(pmmlValue, property);
				}
			}
		}

		return null;
	}
}