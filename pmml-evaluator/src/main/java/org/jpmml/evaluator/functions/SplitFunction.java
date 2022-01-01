/*
 * Copyright (c) 2019 Villu Ruusmann
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
import java.util.List;
import java.util.regex.Pattern;

import org.jpmml.evaluator.CollectionValue;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.RegExUtil;
import org.jpmml.evaluator.TypeInfos;

/**
 * <p>
 * A Java UDF for splitting a scalar string value to a collection of string values.
 * </p>
 *
 * Pseudo-declaration of function:
 * <pre>{@code
 * <DefineFunction name="..." dataType="collection of strings">
 *   <ParameterField name="input" dataType="string"/>
 *   <ParameterField name="pattern" dataType="string"/>
 * </DefineFunction>
 * }</pre>
 *
 * @see Pattern#split(CharSequence, int)
 */
public class SplitFunction extends BinaryFunction {

	public SplitFunction(){
		this(SplitFunction.class.getName());
	}

	public SplitFunction(String name){
		super(name, Arrays.asList("input", "pattern"));
	}

	public List<String> evaluate(String input, String regex){
		Pattern pattern = RegExUtil.compile(regex, null);

		String[] values = pattern.split(input, -1);

		return Arrays.asList(values);
	}

	@Override
	public FieldValue evaluate(FieldValue first, FieldValue second){
		List<String> values = evaluate(first.asString(), second.asString());

		return CollectionValue.create(TypeInfos.CATEGORICAL_STRING, values);
	}
}