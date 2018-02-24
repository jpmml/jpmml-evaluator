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

import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.TypeUtil;

/**
 * <p>
 * A Java UDF for calculating the standard deviation of a collection of values.
 * </p>
 *
 * Pseudo-declaration of function:
 * <pre>
 *   &lt;DefineFunction name="..." dataType="double"&gt;
 *     &lt;ParameterField name="values" dataType="collection of numbers"/&gt;
 *     &lt;ParameterField name="biasCorrected" dataType="boolean"/&gt; &lt;!-- Optional; defaults to false --&gt;
 *   &lt;/DefineFunction&gt;
 * </pre>
 *
 * @see StandardDeviation
 */
public class StandardDeviationFunction extends AbstractFunction {

	public StandardDeviationFunction(){
		this(StandardDeviationFunction.class.getName());
	}

	public StandardDeviationFunction(String name){
		super(name);
	}

	@Override
	public FieldValue evaluate(List<FieldValue> arguments){
		checkVariableArityArguments(arguments, 1, 2);

		Collection<?> values = FieldValueUtil.getValue(Collection.class, getRequiredArgument(arguments, 0, "values"));

		Boolean biasCorrected = Boolean.FALSE;
		if(arguments.size() > 1){
			biasCorrected = getRequiredArgument(arguments, 1, "biasCorrected").asBoolean();
		}

		Double result = evaluate(values, biasCorrected);

		return FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, result);
	}

	static
	private Double evaluate(Collection<?> values, boolean biasCorrected){
		StandardDeviation statistic = new StandardDeviation();
		statistic.setBiasCorrected(biasCorrected);

		for(Object value : values){
			Number number = (Number)TypeUtil.parseOrCast(DataType.DOUBLE, value);

			statistic.increment(number.doubleValue());
		}

		return statistic.getResult();
	}
}
