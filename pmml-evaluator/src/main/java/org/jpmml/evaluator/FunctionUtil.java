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

import java.util.*;

import org.jpmml.manager.*;

import org.apache.commons.math3.stat.descriptive.*;
import org.apache.commons.math3.stat.descriptive.moment.*;
import org.apache.commons.math3.stat.descriptive.rank.*;
import org.apache.commons.math3.stat.descriptive.summary.*;

import org.dmg.pmml.*;

import com.google.common.collect.*;

import org.joda.time.*;

public class FunctionUtil {

	private FunctionUtil(){
	}

	static
	public FieldValue evaluate(Apply apply, List<FieldValue> values, EvaluationContext context){
		String name = apply.getFunction();

		Function function = getFunction(name);
		if(function == null){
			DefineFunction defineFunction = context.resolveFunction(name);
			if(defineFunction == null){
				throw new UnsupportedFeatureException(apply);
			}

			return evaluate(defineFunction, values, context);
		}

		return function.evaluate(values);
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

		FunctionEvaluationContext functionContext = new FunctionEvaluationContext(context);

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

		return FieldValueUtil.refine(defineFunction.getDataType(), defineFunction.getOptype(), result);
	}

	static
	public Function getFunction(String name){
		return FunctionUtil.functions.get(name);
	}

	static
	public void putFunction(String name, Function function){
		FunctionUtil.functions.put(name, function);
	}

	static
	private void checkArguments(List<FieldValue> values, int size){
		checkArguments(values, size, false);
	}

	static
	private void checkArguments(List<FieldValue> values, int size, boolean allowNulls){
		boolean success = (values.size() == size) && (allowNulls ? true : !values.contains(null));
		if(!success){
			throw new EvaluationException();
		}
	}

	static
	private void checkVariableArguments(List<FieldValue> values, int size){
		checkVariableArguments(values, size, false);
	}

	static
	private void checkVariableArguments(List<FieldValue> values, int size, boolean allowNulls){
		boolean success = (values.size() >= size) && (allowNulls ? true : !values.contains(null));
		if(!success){
			throw new EvaluationException();
		}
	}

	static
	private Number cast(DataType dataType, Number number){

		switch(dataType){
			case INTEGER:
				if(number instanceof Integer){
					return number;
				}
				return Integer.valueOf(number.intValue());
			case FLOAT:
				if(number instanceof Float){
					return number;
				}
				return Float.valueOf(number.floatValue());
			case DOUBLE:
				if(number instanceof Double){
					return number;
				}
				return Double.valueOf(number.doubleValue());
			default:
				break;
		}

		throw new EvaluationException();
	}

	static
	private DataType integerToDouble(DataType dataType){

		switch(dataType){
			case INTEGER:
				return DataType.DOUBLE;
			default:
				break;
		}

		return dataType;
	}

	private static final Map<String, Function> functions = Maps.newLinkedHashMap();

	public interface Function {

		FieldValue evaluate(List<FieldValue> values);
	}

	static
	abstract
	public class ArithmeticFunction implements Function {

		abstract
		public Number evaluate(Number left, Number right);

		@Override
		public FieldValue evaluate(List<FieldValue> values){

			if(values.size() != 2){
				throw new EvaluationException();
			}

			FieldValue left = values.get(0);
			FieldValue right = values.get(1);

			// "If one of the input fields of a simple arithmetic function is a missing value, the result evaluates to missing value"
			if(left == null || right == null){
				return null;
			}

			DataType dataType = TypeUtil.getResultDataType(left.getDataType(), right.getDataType());

			Number result;

			try {
				result = evaluate(left.asNumber(), right.asNumber());
			} catch(ArithmeticException ae){
				throw new InvalidResultException(null);
			}

			return FieldValueUtil.create(cast(dataType, result));
		}
	}

	static {
		putFunction("+", new ArithmeticFunction(){

			@Override
			public Double evaluate(Number left, Number right){
				return Double.valueOf(left.doubleValue() + right.doubleValue());
			}
		});

		putFunction("-", new ArithmeticFunction(){

			@Override
			public Double evaluate(Number left, Number right){
				return Double.valueOf(left.doubleValue() - right.doubleValue());
			}
		});

		putFunction("*", new ArithmeticFunction(){

			@Override
			public Double evaluate(Number left, Number right){
				return Double.valueOf(left.doubleValue() * right.doubleValue());
			}
		});

		putFunction("/", new ArithmeticFunction(){

			@Override
			public Number evaluate(Number left, Number right){

				if(left instanceof Integer && right instanceof Integer){
					return Integer.valueOf(left.intValue() / right.intValue());
				}

				return Double.valueOf(left.doubleValue() / right.doubleValue());
			}
		});
	}

	static
	abstract
	public class AggregateFunction implements Function {

		abstract
		public StorelessUnivariateStatistic createStatistic();

		public DataType getResultType(DataType dataType){
			return dataType;
		}

		@Override
		public FieldValue evaluate(List<FieldValue> values){
			StorelessUnivariateStatistic statistic = createStatistic();

			DataType dataType = null;

			for(FieldValue value : values){

				// "Missing values in the input to an aggregate function are simply ignored"
				if(value == null){
					continue;
				}

				statistic.increment((value.asNumber()).doubleValue());

				if(dataType != null){
					dataType = TypeUtil.getResultDataType(dataType, value.getDataType());
				} else

				{
					dataType = value.getDataType();
				}
			}

			if(statistic.getN() == 0){
				throw new MissingResultException(null);
			}

			Object result = cast(getResultType(dataType), statistic.getResult());

			return FieldValueUtil.create(result);
		}
	}

	static {
		putFunction("min", new AggregateFunction(){

			@Override
			public Min createStatistic(){
				return new Min();
			}
		});

		putFunction("max", new AggregateFunction(){

			@Override
			public Max createStatistic(){
				return new Max();
			}
		});

		putFunction("avg", new AggregateFunction(){

			@Override
			public Mean createStatistic(){
				return new Mean();
			}

			@Override
			public DataType getResultType(DataType dataType){
				return integerToDouble(dataType);
			}
		});

		putFunction("sum", new AggregateFunction(){

			@Override
			public Sum createStatistic(){
				return new Sum();
			}
		});

		putFunction("product", new AggregateFunction(){

			@Override
			public Product createStatistic(){
				return new Product();
			}
		});
	}

	static
	abstract
	public class MathFunction implements Function {

		abstract
		public Double evaluate(Number value);

		public DataType getResultType(DataType dataType){
			return dataType;
		}

		@Override
		public FieldValue evaluate(List<FieldValue> values){
			checkArguments(values, 1);

			FieldValue value = values.get(0);

			Number result = cast(getResultType(value.getDataType()), evaluate(value.asNumber()));

			return FieldValueUtil.create(result);
		}
	}

	static
	abstract
	public class FpMathFunction extends MathFunction {

		@Override
		public DataType getResultType(DataType dataType){
			return integerToDouble(dataType);
		}
	}

	static {
		putFunction("log10", new FpMathFunction(){

			@Override
			public Double evaluate(Number value){
				return Math.log10(value.doubleValue());
			}
		});

		putFunction("ln", new FpMathFunction(){

			@Override
			public Double evaluate(Number value){
				return Math.log(value.doubleValue());
			}
		});

		putFunction("exp", new FpMathFunction(){

			@Override
			public Double evaluate(Number value){
				return Math.exp(value.doubleValue());
			}
		});

		putFunction("sqrt", new FpMathFunction(){

			@Override
			public Double evaluate(Number value){
				return Math.sqrt(value.doubleValue());
			}
		});

		putFunction("abs", new MathFunction(){

			@Override
			public Double evaluate(Number value){
				return Math.abs(value.doubleValue());
			}
		});

		putFunction("pow", new Function(){

			@Override
			public FieldValue evaluate(List<FieldValue> values){
				checkArguments(values, 2);

				FieldValue left = values.get(0);
				FieldValue right = values.get(1);

				DataType dataType = TypeUtil.getResultDataType(left.getDataType(), right.getDataType());

				Double result = Math.pow((left.asNumber()).doubleValue(), (right.asNumber()).doubleValue());

				return FieldValueUtil.create(cast(dataType, result));
			}
		});

		putFunction("threshold", new Function(){

			@Override
			public FieldValue evaluate(List<FieldValue> values){
				checkArguments(values, 2);

				FieldValue left = values.get(0);
				FieldValue right = values.get(1);

				DataType dataType = TypeUtil.getResultDataType(left.getDataType(), right.getDataType());

				Integer result = ((left.asNumber()).doubleValue() > (right.asNumber()).doubleValue()) ? 1 : 0;

				return FieldValueUtil.create(cast(dataType, result));
			}
		});

		putFunction("floor", new MathFunction(){

			@Override
			public Double evaluate(Number number){
				return Math.floor(number.doubleValue());
			}
		});

		putFunction("ceil", new MathFunction(){

			@Override
			public Double evaluate(Number number){
				return Math.ceil(number.doubleValue());
			}
		});

		putFunction("round", new MathFunction(){

			@Override
			public Double evaluate(Number number){
				return (double)Math.round(number.doubleValue());
			}
		});
	}

	static
	abstract
	public class ValueFunction implements Function {

		abstract
		public Boolean evaluate(FieldValue value);

		@Override
		public FieldValue evaluate(List<FieldValue> values){
			checkArguments(values, 1, true);

			FieldValue value = values.get(0);

			Boolean result = evaluate(value);

			return FieldValueUtil.create(result);
		}
	}

	static {
		putFunction("isMissing", new ValueFunction(){

			@Override
			public Boolean evaluate(FieldValue value){
				return Boolean.valueOf(value == null);
			}
		});

		putFunction("isNotMissing", new ValueFunction(){

			@Override
			public Boolean evaluate(FieldValue value){
				return Boolean.valueOf(value != null);
			}
		});
	}

	static
	abstract
	public class EqualityFunction implements Function {

		abstract
		public Boolean evaluate(boolean equals);

		@Override
		public FieldValue evaluate(List<FieldValue> values){
			checkArguments(values, 2);

			FieldValue left = values.get(0);
			FieldValue right = values.get(1);

			Boolean result = evaluate((left).equalsValue(right));

			return FieldValueUtil.create(result);
		}
	}

	static {
		putFunction("equal", new EqualityFunction(){

			@Override
			public Boolean evaluate(boolean equals){
				return Boolean.valueOf(equals);
			}
		});

		putFunction("notEqual", new EqualityFunction(){

			@Override
			public Boolean evaluate(boolean equals){
				return Boolean.valueOf(!equals);
			}
		});

	}

	static
	abstract
	public class ComparisonFunction implements Function {

		abstract
		public Boolean evaluate(int order);

		@Override
		public FieldValue evaluate(List<FieldValue> values){
			checkArguments(values, 2);

			FieldValue left = values.get(0);
			FieldValue right = values.get(1);

			Boolean result = evaluate((left).compareToValue(right));

			return FieldValueUtil.create(result);
		}
	}

	static {
		putFunction("lessThan", new ComparisonFunction(){

			@Override
			public Boolean evaluate(int order){
				return Boolean.valueOf(order < 0);
			}
		});

		putFunction("lessOrEqual", new ComparisonFunction(){

			@Override
			public Boolean evaluate(int order){
				return Boolean.valueOf(order <= 0);
			}
		});

		putFunction("greaterThan", new ComparisonFunction(){

			@Override
			public Boolean evaluate(int order){
				return Boolean.valueOf(order > 0);
			}
		});

		putFunction("greaterOrEqual", new ComparisonFunction(){

			@Override
			public Boolean evaluate(int order){
				return Boolean.valueOf(order >= 0);
			}
		});
	}

	static
	abstract
	public class BinaryBooleanFunction implements Function {

		abstract
		public Boolean evaluate(Boolean left, Boolean right);

		@Override
		public FieldValue evaluate(List<FieldValue> values){
			checkVariableArguments(values, 2);

			Boolean result = (values.get(0)).asBoolean();

			for(int i = 1; i < values.size(); i++){
				result = evaluate(result, (values.get(i)).asBoolean());
			}

			return FieldValueUtil.create(result);
		}
	}

	static {
		putFunction("and", new BinaryBooleanFunction(){

			@Override
			public Boolean evaluate(Boolean left, Boolean right){
				return Boolean.valueOf(left.booleanValue() & right.booleanValue());
			}
		});

		putFunction("or", new BinaryBooleanFunction(){

			@Override
			public Boolean evaluate(Boolean left, Boolean right){
				return Boolean.valueOf(left.booleanValue() | right.booleanValue());
			}
		});
	}

	static
	abstract
	public class UnaryBooleanFunction implements Function {

		abstract
		public Boolean evaluate(Boolean value);

		@Override
		public FieldValue evaluate(List<FieldValue> values){
			checkArguments(values, 1);

			FieldValue value = values.get(0);

			Boolean result = evaluate(value.asBoolean());

			return FieldValueUtil.create(result);
		}
	}

	static {
		putFunction("not", new UnaryBooleanFunction(){

			@Override
			public Boolean evaluate(Boolean value){
				return Boolean.valueOf(!value.booleanValue());
			}
		});
	}

	static
	abstract
	public class ValueListFunction implements Function {

		abstract
		public Boolean evaluate(FieldValue value, List<FieldValue> values);

		@Override
		public FieldValue evaluate(List<FieldValue> values){
			checkVariableArguments(values, 2);

			Boolean result = evaluate(values.get(0), values.subList(1, values.size()));

			return FieldValueUtil.create(result);
		}
	}

	static {
		putFunction("isIn", new ValueListFunction(){

			@Override
			public Boolean evaluate(FieldValue value, List<FieldValue> values){
				return value.equalsAnyValue(values);
			}
		});

		putFunction("isNotIn", new ValueListFunction(){

			@Override
			public Boolean evaluate(FieldValue value, List<FieldValue> values){
				return !value.equalsAnyValue(values);
			}
		});
	}

	static {
		putFunction("if", new Function(){

			@Override
			public FieldValue evaluate(List<FieldValue> values){

				if((values.size() < 2 || values.size() > 3)){
					throw new EvaluationException();
				}

				FieldValue flag = values.get(0);
				if(flag == null){
					throw new EvaluationException();
				} // End if

				if(flag.asBoolean()){
					FieldValue trueValue = values.get(1);

					// "The THEN part is required"
					if(trueValue == null){
						throw new EvaluationException();
					}

					return trueValue;
				} else

				{
					FieldValue falseValue = (values.size() > 2 ? values.get(2) : null);

					// "The ELSE part is optional. If the ELSE part is absent then a missing value is returned"
					if(falseValue == null){
						return null;
					}

					return falseValue;
				}
			}
		});
	}

	static
	abstract
	public class StringFunction implements Function {

		abstract
		public String evaluate(String value);

		@Override
		public FieldValue evaluate(List<FieldValue> values){
			checkArguments(values, 1);

			FieldValue value = values.get(0);

			String result = evaluate(value.asString());

			return FieldValueUtil.create(result);
		}
	}

	static {
		putFunction("uppercase", new StringFunction(){

			@Override
			public String evaluate(String value){
				return value.toUpperCase();
			}
		});

		putFunction("lowercase", new StringFunction(){

			@Override
			public String evaluate(String value){
				return value.toLowerCase();
			}
		});

		putFunction("substring", new Function(){

			@Override
			public FieldValue evaluate(List<FieldValue> values){
				checkArguments(values, 3);

				String string = (values.get(0)).asString();

				int position = (values.get(1)).asInteger();
				int length = (values.get(2)).asInteger();

				// "The first character of a string is located at position 1 (not position 0)"
				if(position <= 0 || length < 0){
					throw new EvaluationException();
				}

				String result = string.substring(position - 1, (position + length) - 1);

				return FieldValueUtil.create(result);
			}
		});

		putFunction("trimBlanks", new StringFunction(){

			@Override
			public String evaluate(String value){
				return value.trim();
			}
		});
	}

	static {
		putFunction("formatNumber", new Function(){

			@Override
			public FieldValue evaluate(List<FieldValue> values){
				checkArguments(values, 2);

				FieldValue value = values.get(0);
				FieldValue pattern = values.get(1);

				String result;

				// According to the java.util.Formatter javadoc, Java formatting is more strict than C's printf formatting.
				// For example, in Java, if a conversion is incompatible with a flag, an exception will be thrown. In C's printf, inapplicable flags are silently ignored.
				try {
					result = String.format(pattern.asString(), value.asNumber());
				} catch(IllegalFormatException ife){
					throw ife;
				}

				return FieldValueUtil.create(result);
			}
		});

		putFunction("formatDatetime", new Function(){

			@Override
			public FieldValue evaluate(List<FieldValue> values){
				checkArguments(values, 2);

				FieldValue value = values.get(0);
				FieldValue pattern = values.get(1);

				String result;

				try {
					result = String.format(translatePattern(pattern.asString()), (value.asDateTime()).toDate());
				} catch(IllegalFormatException ife){
					throw ife;
				}

				return FieldValueUtil.create(result);
			}

			private String translatePattern(String pattern){
				StringBuilder sb = new StringBuilder();

				for(int i = 0; i < pattern.length(); i++){
					char c = pattern.charAt(i);

					sb.append(c);

					if(c == '%'){

						// Every %[conversion] has to become %1$t[conversion]
						// Here, "1$" denotes the first argument, and "t" denotes the prefix for date and time conversion characters
						if(i < (pattern.length() - 1) && pattern.charAt(i + 1) != '%'){
							sb.append("1$t");
						}
					}
				}

				return sb.toString();
			}
		});
	}

	static {
		putFunction("dateDaysSinceYear", new Function(){

			@Override
			public FieldValue evaluate(List<FieldValue> values){
				checkArguments(values, 2);

				LocalDate instant = (values.get(0)).asLocalDate();

				int year = (values.get(1)).asInteger();

				DaysSinceDate period = new DaysSinceDate(year, instant);

				return FieldValueUtil.create(period.intValue());
			}
		});

		putFunction("dateSecondsSinceMidnight", new Function(){

			@Override
			public FieldValue evaluate(List<FieldValue> values){
				checkArguments(values, 1);

				LocalTime instant = (values.get(0)).asLocalTime();

				Seconds seconds = Seconds.seconds(instant.getHourOfDay() * 60 * 60 + instant.getMinuteOfHour() * 60 + instant.getSecondOfMinute());

				SecondsSinceMidnight period = new SecondsSinceMidnight(seconds);

				return FieldValueUtil.create(period.intValue());
			}
		});

		putFunction("dateSecondsSinceYear", new Function(){

			@Override
			public FieldValue evaluate(List<FieldValue> values){
				checkArguments(values, 2);

				LocalDateTime instant = (values.get(0)).asLocalDateTime();

				int year = (values.get(1)).asInteger();

				SecondsSinceDate period = new SecondsSinceDate(year, instant);

				return FieldValueUtil.create(period.intValue());
			}
		});
	}
}