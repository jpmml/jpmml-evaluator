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
import java.util.Objects;

import javax.xml.bind.annotation.XmlTransient;

import com.google.common.collect.ImmutableMap;
import org.dmg.pmml.DataType;
import org.dmg.pmml.general_regression.BaseCumHazardTables;
import org.dmg.pmml.general_regression.BaselineStratum;
import org.dmg.pmml.general_regression.PMMLAttributes;
import org.jpmml.evaluator.MapHolder;
import org.jpmml.evaluator.MissingAttributeException;
import org.jpmml.evaluator.TypeUtil;
import org.jpmml.model.ReflectionUtil;

public class RichBaseCumHazardTables extends BaseCumHazardTables implements MapHolder<BaselineStratum> {

	@XmlTransient
	private DataType dataType = null;

	@XmlTransient
	private Map<?, BaselineStratum> baselineStratumMap = null;


	private RichBaseCumHazardTables(){
	}

	public RichBaseCumHazardTables(DataType dataType){
		setDataType(dataType);
	}

	public RichBaseCumHazardTables(DataType dataType, BaseCumHazardTables baseCumHazardTables){
		setDataType(dataType);

		ReflectionUtil.copyState(baseCumHazardTables, this);
	}

	@Override
	public Map<?, BaselineStratum> getMap(){

		if(this.baselineStratumMap == null){
			this.baselineStratumMap = ImmutableMap.copyOf(parseBaselineStrata());
		}

		return this.baselineStratumMap;
	}

	@Override
	public DataType getDataType(){
		return this.dataType;
	}

	private void setDataType(DataType dataType){
		this.dataType = Objects.requireNonNull(dataType);
	}

	private Map<?, BaselineStratum> parseBaselineStrata(){
		DataType dataType = getDataType();

		Map<Object, BaselineStratum> result = new LinkedHashMap<>();

		List<BaselineStratum> baselineStrata = getBaselineStrata();
		for(BaselineStratum baselineStratum : baselineStrata){
			Object category = baselineStratum.getValue();
			if(category == null){
				throw new MissingAttributeException(baselineStratum, PMMLAttributes.BASELINESTRATUM_VALUE);
			}

			Object value = TypeUtil.parseOrCast(dataType, category);

			result.put(value, baselineStratum);
		}

		return result;
	}
}