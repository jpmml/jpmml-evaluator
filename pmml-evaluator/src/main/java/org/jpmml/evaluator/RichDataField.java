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

import javax.xml.bind.annotation.XmlTransient;

import com.google.common.collect.ImmutableMap;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.PMMLAttributes;
import org.dmg.pmml.Value;
import org.jpmml.model.ReflectionUtil;

public class RichDataField extends DataField implements ValueStatusHolder {

	@XmlTransient
	private Map<?, Integer> valueMap = null;

	@XmlTransient
	private boolean hasValidValues = false;


	public RichDataField(){
	}

	public RichDataField(DataField dataField){
		ReflectionUtil.copyState(dataField, this);
	}

	@Override
	public DataType getDataType(){
		DataType dataType = super.getDataType();
		if(dataType == null){
			throw new MissingAttributeException(this, PMMLAttributes.DATAFIELD_DATATYPE);
		}

		return dataType;
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
		return this.hasValidValues;
	}

	private Map<Object, Integer> parseValues(){
		DataType dataType = getDataType();

		Map<Object, Integer> result = new LinkedHashMap<>();

		int validIndex = 0;

		List<Value> pmmlValues = getValues();
		for(Value pmmlValue : pmmlValues){
			Object objectValue = pmmlValue.getValue();
			if(objectValue == null){
				throw new MissingAttributeException(pmmlValue, PMMLAttributes.VALUE_VALUE);
			}

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
					{
						Object value;

						try {
							value = TypeUtil.parseOrCast(dataType, objectValue);
						} catch(IllegalArgumentException | TypeCheckException e){
							continue;
						}

						result.put(value, FieldValue.STATUS_UNKNOWN_INVALID);
					}
					break;
				case MISSING:
					{
						Object value;

						try {
							value = TypeUtil.parseOrCast(dataType, objectValue);
						} catch(IllegalArgumentException | TypeCheckException e){
							continue;
						}

						result.put(value, FieldValue.STATUS_MISSING);
					}
					break;
				default:
					throw new UnsupportedAttributeException(pmmlValue, property);
			}
		}

		this.hasValidValues = (validIndex > 0);

		return result;
	}
}