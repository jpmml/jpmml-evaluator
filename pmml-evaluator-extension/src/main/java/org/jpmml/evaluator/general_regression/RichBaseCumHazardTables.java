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
package org.jpmml.evaluator.general_regression;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.collect.ImmutableMap;
import org.dmg.pmml.general_regression.BaseCumHazardTables;
import org.dmg.pmml.general_regression.BaselineStratum;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.HasParsedValueMapping;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.PMMLAttributes;
import org.jpmml.evaluator.TypeInfo;
import org.jpmml.model.ReflectionUtil;

@XmlRootElement (
	name = "BaseCumHazardTables"
)
public class RichBaseCumHazardTables extends BaseCumHazardTables implements HasParsedValueMapping<BaselineStratum> {

	@XmlTransient
	private Map<FieldValue, BaselineStratum> parsedValueMappings = null;


	public RichBaseCumHazardTables(){
	}

	public RichBaseCumHazardTables(BaseCumHazardTables baseCumHazardTables){
		ReflectionUtil.copyState(baseCumHazardTables, this);
	}

	@Override
	public Map<FieldValue, BaselineStratum> getValueMapping(TypeInfo typeInfo){

		if(this.parsedValueMappings == null){
			this.parsedValueMappings = ImmutableMap.copyOf(parseBaselineStrata(typeInfo));
		}

		return this.parsedValueMappings;
	}

	private Map<FieldValue, BaselineStratum> parseBaselineStrata(TypeInfo typeInfo){
		Map<FieldValue, BaselineStratum> result = new LinkedHashMap<>();

		List<BaselineStratum> baselineStrata = getBaselineStrata();
		for(BaselineStratum baselineStratum : baselineStrata){
			String category = baselineStratum.getValue();
			if(category == null){
				throw new MissingAttributeException(baselineStratum, PMMLAttributes.BASELINESTRATUM_VALUE);
			}

			FieldValue value = parse(typeInfo, category);

			result.put(value, baselineStratum);
		}

		return result;
	}
}