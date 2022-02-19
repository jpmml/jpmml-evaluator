/*
 * Copyright (c) 2020 Villu Ruusmann
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import jakarta.xml.bind.annotation.XmlTransient;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.Value;
import org.jpmml.model.InvalidAttributeException;
import org.jpmml.model.ReflectionUtil;
import org.jpmml.model.UnsupportedAttributeException;

public class RichOutputField extends OutputField implements ValueStatusHolder {

	@XmlTransient
	private Map<?, Integer> valueMap = null;


	public RichOutputField(){
	}

	public RichOutputField(OutputField outputField){
		ReflectionUtil.copyState(outputField, this);
	}

	@Override
	public Map<?, Integer> getMap(){

		if(this.valueMap == null){
			this.valueMap = ImmutableMap.copyOf(parseValues());
		}

		return this.valueMap;
	}

	@Override
	public boolean hasValidValues(){
		return hasValues();
	}

	private Map<Object, Integer> parseValues(){
		DataType dataType = requireDataType();

		Map<Object, Integer> result = new LinkedHashMap<>();

		int validIndex = 0;

		List<Value> pmmlValues = getValues();
		for(Value pmmlValue : pmmlValues){
			Object objectValue = pmmlValue.requireValue();

			Value.Property property = pmmlValue.getProperty();
			switch(property){
				case VALID:
					{
						validIndex++;

						Object value = TypeUtil.parseOrCast(dataType, objectValue);

						result.put(value, validIndex);
					}
					break;
				case INVALID:
				case MISSING:
					throw new InvalidAttributeException(pmmlValue, property);
				default:
					throw new UnsupportedAttributeException(pmmlValue, property);
			}
		}

		return result;
	}
}