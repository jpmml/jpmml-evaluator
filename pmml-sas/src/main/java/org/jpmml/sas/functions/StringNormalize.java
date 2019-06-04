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

import java.util.Arrays;

import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.FunctionException;
import org.jpmml.evaluator.TypeInfos;
import org.jpmml.evaluator.functions.BinaryFunction;

public class StringNormalize extends BinaryFunction {

	public StringNormalize(){
		super("SAS-EM-String-Normalize", Arrays.asList("length", null));
	}

	public String evaluate(int length, String string){

		if(length < 0){
			throw new FunctionException(this, "Invalid \"length\" value " + length);
		}

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

		return string;
	}

	@Override
	public FieldValue evaluate(FieldValue first, FieldValue second){
		String result = evaluate(first.asInteger(), second.asString());

		return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, result);
	}
}