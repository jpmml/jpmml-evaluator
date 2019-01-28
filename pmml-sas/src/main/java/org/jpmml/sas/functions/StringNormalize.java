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

import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.FunctionException;
import org.jpmml.evaluator.TypeInfos;
import org.jpmml.evaluator.functions.AbstractFunction;

public class StringNormalize extends AbstractFunction {

	public StringNormalize(){
		super("SAS-EM-String-Normalize");
	}

	@Override
	public FieldValue evaluate(List<FieldValue> arguments){
		checkFixedArityArguments(arguments, 2);

		int length = getRequiredArgument(arguments, 0, "length").asInteger();
		if(length < 0){
			throw new FunctionException(this, "Invalid \'length\' value " + length);
		}

		String string = getRequiredArgument(arguments, 1).asString();

		int offset = 0;

		// Trim leading whitespace characters (but keep trailing whitespace characters)
		for(int i = 0; i < string.length(); i++){
			char c = string.charAt(i);

			if(c > 32){
				break;
			}

			offset++;
		}

		// Truncate to fixed length
		string = string.substring(offset, offset + Math.min(length, string.length() - offset));

		// Convert to all uppercase characters
		string = string.toUpperCase();

		return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, string);
	}
}