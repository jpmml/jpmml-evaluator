/*
 * Copyright (c) 2013 Villu Ruusmann
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
package org.jpmml.evaluator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.dmg.pmml.Apply;
import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.Expression;
import org.dmg.pmml.ParameterField;
import org.jpmml.model.ReflectionUtil;

public class FunctionUtil {

	private FunctionUtil(){
	}

	static
	public FieldValue evaluate(Apply apply, List<FieldValue> values, EvaluationContext context){
		String name = apply.getFunction();

		if(name == null){
			throw new InvalidFeatureException(apply);
		}

		Function builtInFunction = getFunction(name);
		if(builtInFunction != null){
			return builtInFunction.evaluate(values);
		}

		Function userDefinedFunction = FunctionRegistry.getFunction(name);
		if(userDefinedFunction != null){
			return userDefinedFunction.evaluate(values);
		}

		DefineFunction defineFunction = context.resolveDefineFunction(name);
		if(defineFunction != null){
			return evaluate(defineFunction, values, context);
		}

		throw new UnsupportedFeatureException(apply, ReflectionUtil.getField(apply, "function"), name);
	}

	static
	public FieldValue evaluate(DefineFunction defineFunction, List<FieldValue> values, EvaluationContext context){
		List<ParameterField> parameterFields = defineFunction.getParameterFields();

		if(parameterFields.size() < 1){
			throw new InvalidFeatureException(defineFunction);
		} // End if

		if(parameterFields.size() != values.size()){
			throw new EvaluationException();
		}

		DefineFunctionEvaluationContext functionContext = new DefineFunctionEvaluationContext(context);

		for(int i = 0; i < parameterFields.size(); i++){
			ParameterField parameterField = parameterFields.get(i);

			FieldValue value = FieldValueUtil.refine(parameterField, values.get(i));

			functionContext.declare(parameterField.getName(), value);
		}

		Expression expression = defineFunction.getExpression();
		if(expression == null){
			throw new InvalidFeatureException(defineFunction);
		}

		FieldValue result = ExpressionUtil.evaluate(expression, functionContext);

		return FieldValueUtil.refine(defineFunction.getDataType(), defineFunction.getOpType(), result);
	}

	static
	public Function getFunction(String name){
		return FunctionUtil.builtInFunctions.get(name);
	}

	private static final Map<String, Function> builtInFunctions;

	static {
		List<? extends Function> functions = Arrays.asList(
			Functions.PLUS, Functions.MINUS, Functions.MULTIPLY, Functions.DIVIDE,
			Functions.MIN, Functions.MAX, Functions.AVG, Functions.SUM, Functions.PRODUCT,
			Functions.LOG10, Functions.LN, Functions.EXP, Functions.SQRT, Functions.ABS, Functions.POW, Functions.THRESHOLD, Functions.FLOOR, Functions.CEIL, Functions.ROUND,
			Functions.IS_MISSING, Functions.IS_NOT_MISSING,
			Functions.EQUAL, Functions.NOT_EQUAL,
			Functions.LESS_THAN, Functions.LESS_OR_EQUAL, Functions.GREATER_THAN, Functions.GREATER_OR_EQUAL,
			Functions.AND, Functions.OR,
			Functions.NOT,
			Functions.IS_IN, Functions.IS_NOT_IN,
			Functions.IF,
			Functions.UPPERCASE, Functions.LOWERCASE, Functions.SUBSTRING, Functions.TRIM_BLANKS,
			Functions.CONCAT,
			Functions.REPLACE, Functions.MATCHES,
			Functions.FORMAT_NUMBER, Functions.FORMAT_DATETIME,
			Functions.DATE_DAYS_SINCE_YEAR, Functions.DATE_SECONDS_SINCE_MIDNIGHT, Functions.DATE_SECONDS_SINCE_YEAR
		);

		ImmutableMap.Builder<String, Function> builder = new ImmutableMap.Builder<>();

		for(Function function : functions){
			builder.put(function.getName(), function);
		}

		builtInFunctions = builder.build();
	}
}