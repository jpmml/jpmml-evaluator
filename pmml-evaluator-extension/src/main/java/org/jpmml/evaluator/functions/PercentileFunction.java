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

import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.jpmml.evaluator.ComplexDoubleVector;
import org.jpmml.evaluator.DoubleVector;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.FunctionException;
import org.jpmml.evaluator.TypeUtil;

/**
 * <p>
 * A Java UDF for calculating the n-th percentile of a collection of values.
 * </p>
 *
 * Pseudo-declaration of function:
 * <pre>
 *   &lt;DefineFunction name="..." dataType="double"&gt;
 *     &lt;ParameterField name="values" dataType="collection of numbers"/&gt;
 *     &lt;ParameterField name="percentile" dataType="integer"/&gt; &lt;-- 0 &lt; percentile &lt;= 100 --&gt;
 *   &lt;/DefineFunction&gt;
 * </pre>
 *
 * @see Percentile
 */
public class PercentileFunction extends AbstractFunction {

	public PercentileFunction(){
		this(PercentileFunction.class.getName());
	}

	public PercentileFunction(String name){
		super(name);
	}

	@Override
	public FieldValue evaluate(List<FieldValue> arguments){
		checkArguments(arguments, 2);

		Collection<?> values = FieldValueUtil.getValue(Collection.class, arguments.get(0));

		int percentile = (arguments.get(1)).asInteger();
		if(percentile < 1 || percentile > 100){
			throw new FunctionException(this, "Invalid percentile value " + percentile + ". Must be greater than 0 and equal or less than 100");
		}

		Double result = evaluate(values, percentile);

		return FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, result);
	}

	static
	private Double evaluate(Collection<?> values, int percentile){
		DoubleVector doubleValues = new ComplexDoubleVector(values.size());

		for(Object value : values){
			Double doubleValue = (Double)TypeUtil.parseOrCast(DataType.DOUBLE, value);

			doubleValues.add(doubleValue.doubleValue());
		}

		return doubleValues.doublePercentile(percentile);
	}
}