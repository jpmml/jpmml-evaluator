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
package org.jpmml.sas.functions;

import java.util.List;

import com.google.common.base.CharMatcher;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.FunctionException;
import org.jpmml.evaluator.functions.AbstractFunction;

public class StringNormalize extends AbstractFunction {

	public StringNormalize(){
		super("SAS-EM-String-Normalize");
	}

	@Override
	public FieldValue evaluate(List<FieldValue> arguments){
		checkArguments(arguments, 2);

		int length = (arguments.get(0)).asInteger();
		if(length < 0){
			throw new FunctionException(this, "Invalid length value " + length);
		}

		String string = (arguments.get(1)).asString();

		// Trim leading whitespace characters (but keep trailing whitespace characters)
		string = CharMatcher.WHITESPACE.trimLeadingFrom(string);

		// Truncate to a fixed length
		string = string.substring(0, Math.min(length, string.length()));

		// Convert to all uppercase characters
		string = string.toUpperCase();

		return FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, string);
	}
}