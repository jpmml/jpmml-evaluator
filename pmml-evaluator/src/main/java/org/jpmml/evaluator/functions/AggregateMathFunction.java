/*
 * Copyright (c) 2014 Villu Ruusmann
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
package org.jpmml.evaluator.functions;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.StorelessUnivariateStatistic;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.FieldValues;
import org.jpmml.evaluator.TypeUtil;

abstract
public class AggregateMathFunction extends AggregateFunction {

	public AggregateMathFunction(String name){
		super(name);
	}

	abstract
	public StorelessUnivariateStatistic createStatistic();

	@Override
	public FieldValue evaluate(List<FieldValue> arguments){
		StorelessUnivariateStatistic statistic = createStatistic();

		DataType dataType = null;

		for(int i = 0; i < arguments.size(); i++){
			FieldValue value = getOptionalArgument(arguments, i);

			// "Missing values in the input to an aggregate function are simply ignored"
			if(FieldValueUtil.isMissing(value)){
				continue;
			}

			statistic.increment((value.asNumber()).doubleValue());

			if(dataType != null){
				dataType = TypeUtil.getCommonDataType(dataType, value.getDataType());
			} else

			{
				dataType = value.getDataType();
			}
		}

		// "If all inputs are missing, then the result evaluates to a missing value"
		if(statistic.getN() == 0){
			return FieldValues.MISSING_VALUE;
		}

		Double result = statistic.getResult();

		return FieldValueUtil.create(getResultDataType(dataType), OpType.CONTINUOUS, result);
	}

	protected DataType getResultDataType(DataType dataType){
		return dataType;
	}
}