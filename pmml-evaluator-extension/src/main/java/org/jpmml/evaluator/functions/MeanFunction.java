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

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.dmg.pmml.DataType;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.TypeInfos;
import org.jpmml.evaluator.TypeUtil;

/**
 * <p>
 * A Java UDF for calculating the mean of a collection of values.
 * </p>
 *
 * Pseudo-declaration of function:
 * <pre>
 *   &lt;DefineFunction name="..." dataType="double"&gt;
 *     &lt;ParameterField name="values" dataType="collection of numbers"/&gt;
 *   &lt;/DefineFunction&gt;
 * </pre>
 *
 * @see Mean
 */
public class MeanFunction extends AbstractFunction {

	public MeanFunction(){
		this(MeanFunction.class.getName());
	}

	public MeanFunction(String name){
		super(name);
	}

	@Override
	public FieldValue evaluate(List<FieldValue> arguments){
		checkFixedArityArguments(arguments, 1);

		Collection<?> values = FieldValueUtil.getValue(Collection.class, getRequiredArgument(arguments, 0, "values"));

		Double result = evaluate(values);

		return FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, result);
	}

	static
	private Double evaluate(Collection<?> values){
		Mean statistic = new Mean();

		for(Object value : values){
			Number number = (Number)TypeUtil.parseOrCast(DataType.DOUBLE, value);

			statistic.increment(number.doubleValue());
		}

		return statistic.getResult();
	}
}