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

import java.util.*;

import org.jpmml.evaluator.*;
import org.jpmml.evaluator.FieldValue;

import org.apache.commons.math3.stat.descriptive.moment.*;

import org.dmg.pmml.*;

/**
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
	public FieldValue evaluate(List<FieldValue> values){

		if(values.size() < 1 || values.size() > 2){
			throw new FunctionException(getName(), "Expected 1 or 2 arguments, but got " + values.size() + " arguments");
		}

		Object data = FieldValueUtil.getValue(values.get(0));
		if(data == null){
			throw new FunctionException(getName(), "Missing arguments");
		} // End if

		if(!(data instanceof Collection)){
			throw new TypeCheckException(Collection.class, data);
		}

		Boolean biasCorrected = Boolean.FALSE;
		if(values.size() > 1){
			biasCorrected = (values.get(1)).asBoolean();
		} // End if

		if(biasCorrected == null){
			throw new FunctionException(getName(), "Missing arguments");
		}

		Double result = evaluate((Collection<?>)data, biasCorrected);

		return FieldValueUtil.create(result);
	}

	static
	private Double evaluate(Collection<?> values, boolean biasCorrected){
		StandardDeviation statistic = new StandardDeviation();
		statistic.setBiasCorrected(biasCorrected);

		for(Object value : values){
			Double doubleValue = (Double)TypeUtil.parseOrCast(DataType.DOUBLE, value);

			statistic.increment(doubleValue);
		}

		return statistic.getResult();
	}
}