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
 * <pre>{@code
 * <DefineFunction name="..." dataType="double">
 *   <ParameterField name="values" dataType="collection of numbers"/>
 * </DefineFunction>
 * }</pre>
 *
 * @see Mean
 */
public class MeanFunction extends UnaryFunction {

	public MeanFunction(){
		this(MeanFunction.class.getName());
	}

	public MeanFunction(String name){
		super(name, Arrays.asList("values"));
	}

	public Double evaluate(Collection<?> values){
		Mean statistic = new Mean();

		for(Object value : values){
			Number number = (Number)TypeUtil.parseOrCast(DataType.DOUBLE, value);

			statistic.increment(number.doubleValue());
		}

		return statistic.getResult();
	}

	@Override
	public FieldValue evaluate(FieldValue value){
		Double result = evaluate(value.asCollection());

		return FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, result);
	}
}