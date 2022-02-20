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

import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.dmg.pmml.DataType;
import org.jpmml.evaluator.ComplexDoubleVector;
import org.jpmml.evaluator.DoubleVector;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.InvalidArgumentException;
import org.jpmml.evaluator.TypeInfos;
import org.jpmml.evaluator.TypeUtil;

/**
 * <p>
 * A Java UDF for calculating the n-th percentile of a collection of values.
 * </p>
 *
 * Pseudo-declaration of function:
 * <pre>{@code
 * <DefineFunction name="..." dataType="double">
 *   <ParameterField name="values" dataType="collection of numbers"/>
 *   <-- 0 < percentile <= 100 -->
 *   <ParameterField name="percentile" dataType="integer"/>
 * </DefineFunction>
 * }</pre>
 *
 * @see Percentile
 */
public class PercentileFunction extends BinaryFunction {

	public PercentileFunction(){
		this(PercentileFunction.class.getName());
	}

	public PercentileFunction(String name){
		super(name, Arrays.asList("values", "percentile"));
	}

	public Double evaluate(Collection<?> values, int percentile){

		if(percentile < 1 || percentile > 100){
			throw new InvalidArgumentException(this, InvalidArgumentException.formatMessage(this, "percentile", percentile) + ". Must be greater than 0, and less than or equal to 100");
		}

		DoubleVector doubleValues = new ComplexDoubleVector(values.size());

		for(Object value : values){
			Number number = (Number)TypeUtil.parseOrCast(DataType.DOUBLE, value);

			doubleValues.add(number.doubleValue());
		}

		return doubleValues.doublePercentile(percentile);
	}

	@Override
	public FieldValue evaluate(FieldValue first, FieldValue second){
		Double result = evaluate(first.asCollection(), second.asInteger());

		return FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, result);
	}
}