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

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;

abstract
public class ComparisonFunction extends AbstractFunction {

	public ComparisonFunction(String name){
		super(name);
	}

	abstract
	public Boolean evaluate(int order);

	@Override
	public FieldValue evaluate(List<FieldValue> arguments){
		checkArguments(arguments, 2);

		FieldValue left = arguments.get(0);
		FieldValue right = arguments.get(1);

		Boolean result = evaluate((left).compareToValue(right));

		return FieldValueUtil.create(DataType.BOOLEAN, OpType.CATEGORICAL, result);
	}
}