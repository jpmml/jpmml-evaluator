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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.dmg.pmml.DataType;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.TypeInfos;
import org.jpmml.evaluator.TypeUtil;

/**
 * <p>
 * A Java UDF for calculating the standard deviation of a collection of values.
 * </p>
 *
 * Pseudo-declaration of function:
 * <pre>{@code
 * <DefineFunction name="..." dataType="double">
 *   <ParameterField name="values" dataType="collection of numbers"/>
 *   <!-- Optional; defaults to false -->
 *   <ParameterField name="biasCorrected" dataType="boolean"/>
 * </DefineFunction>
 * }</pre>
 *
 * @see StandardDeviation
 */
public class StandardDeviationFunction extends MultiaryFunction {

	public StandardDeviationFunction(){
		this(StandardDeviationFunction.class.getName());
	}

	public StandardDeviationFunction(String name){
		super(name, Arrays.asList("values", "biasCorrected"));
	}

	public Double evaluate(Collection<?> values, boolean biasCorrected){
		StandardDeviation statistic = new StandardDeviation();
		statistic.setBiasCorrected(biasCorrected);

		for(Object value : values){
			Number number = (Number)TypeUtil.parseOrCast(DataType.DOUBLE, value);

			statistic.increment(number.doubleValue());
		}

		return statistic.getResult();
	}

	@Override
	public FieldValue evaluate(List<FieldValue> arguments){
		checkVariableArityArguments(arguments, 1, 2);

		Double result;

		if(arguments.size() > 1){
			result = evaluate(getRequiredArgument(arguments, 0).asCollection(), getRequiredArgument(arguments, 1).asBoolean());
		} else

		{
			result = evaluate(getRequiredArgument(arguments, 0).asCollection(), Boolean.FALSE);
		}

		return FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, result);
	}
}
