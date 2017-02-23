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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.collect.ImmutableMap;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Value;
import org.jpmml.model.ReflectionUtil;

@XmlRootElement (
	name = "DataField"
)
public class RichDataField extends DataField implements HasParsedValueMapping<Value> {

	@XmlTransient
	private Map<FieldValue, Value> parsedValueMappings = null;


	public RichDataField(){
	}

	public RichDataField(DataField dataField){
		ReflectionUtil.copyState(dataField, this);
	}

	public Map<FieldValue, Value> getValueMapping(){
		return getValueMapping(getDataType(), getOpType());
	}

	@Override
	public Map<FieldValue, Value> getValueMapping(DataType dataType, OpType opType){

		if(this.parsedValueMappings == null){
			this.parsedValueMappings = ImmutableMap.copyOf(parseValues(dataType, opType));
		}

		return this.parsedValueMappings;
	}

	private Map<FieldValue, Value> parseValues(DataType dataType, OpType opType){
		Map<FieldValue, Value> result = new LinkedHashMap<>();

		List<Value> pmmlValues = getValues();
		for(Value pmmlValue : pmmlValues){
			Value.Property property = pmmlValue.getProperty();

			switch(property){
				case VALID:
					{
						FieldValue value = FieldValueUtil.create(dataType, opType, pmmlValue.getValue());

						result.put(value, pmmlValue);
					}
					break;
				case INVALID:
				case MISSING:
					{
						FieldValue value;

						try {
							value = FieldValueUtil.create(dataType, opType, pmmlValue.getValue());
						} catch(IllegalArgumentException | TypeCheckException e){
							continue;
						}

						result.put(value, pmmlValue);
					}
					break;
				default:
					throw new UnsupportedFeatureException(pmmlValue, property);
			}
		}

		return result;
	}
}