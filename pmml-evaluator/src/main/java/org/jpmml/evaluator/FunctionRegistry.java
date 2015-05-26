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
package org.jpmml.evaluator;

import java.util.IllegalFormatException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;
import org.apache.commons.math3.stat.descriptive.summary.Product;
import org.apache.commons.math3.stat.descriptive.summary.Sum;
import org.dmg.pmml.DataType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Seconds;
import org.jpmml.evaluator.functions.AbstractFunction;
import org.jpmml.evaluator.functions.AggregateFunction;
import org.jpmml.evaluator.functions.ArithmeticFunction;
import org.jpmml.evaluator.functions.BinaryBooleanFunction;
import org.jpmml.evaluator.functions.ComparisonFunction;
import org.jpmml.evaluator.functions.EqualityFunction;
import org.jpmml.evaluator.functions.FpMathFunction;
import org.jpmml.evaluator.functions.MathFunction;
import org.jpmml.evaluator.functions.StringFunction;
import org.jpmml.evaluator.functions.UnaryBooleanFunction;
import org.jpmml.evaluator.functions.ValueFunction;
import org.jpmml.evaluator.functions.ValueListFunction;

public class FunctionRegistry {

	private FunctionRegistry(){
	}

	static
	public Function getFunction(String name){
		Function function = FunctionRegistry.functions.get(name);
		if(function == null){
			function = loadJavaFunction(name);
		}

		return function;
	}

	static
	public void putFunction(Function function){
		putFunction(function.getName(), function);
	}

	static
	public void putFunction(String name, Function function){
		FunctionRegistry.functions.put(name, function);
	}

	static
	private Function loadJavaFunction(String name){
		Class<?> clazz;

		try {
			ClassLoader classLoader = (Thread.currentThread()).getContextClassLoader();
			if(classLoader == null){
				classLoader = (FunctionRegistry.class).getClassLoader();
			}

			clazz = classLoader.loadClass(name);
		} catch(ClassNotFoundException cnfe){
			return null;
		}

		if(!(Function.class).isAssignableFrom(clazz)){
			return null;
		}

		Function function;

		try {
			function = (Function)clazz.newInstance();
		} catch(Exception e){
			throw new EvaluationException();
		}

		return function;
	}

	private static final Map<String, Function> functions = new LinkedHashMap<>();

	static {
		putFunction(new ArithmeticFunction("+"){

			@Override
			public Double evaluate(Number left, Number right){
				return Double.valueOf(left.doubleValue() + right.doubleValue());
			}
		});

		putFunction(new ArithmeticFunction("-"){

			@Override
			public Double evaluate(Number left, Number right){
				return Double.valueOf(left.doubleValue() - right.doubleValue());
			}
		});

		putFunction(new ArithmeticFunction("*"){

			@Override
			public Double evaluate(Number left, Number right){
				return Double.valueOf(left.doubleValue() * right.doubleValue());
			}
		});

		putFunction(new ArithmeticFunction("/"){

			@Override
			public Number evaluate(Number left, Number right){

				if(left instanceof Integer && right instanceof Integer){
					return Integer.valueOf(left.intValue() / right.intValue());
				}

				return Double.valueOf(left.doubleValue() / right.doubleValue());
			}
		});
	}

	static {
		putFunction(new AggregateFunction("min"){

			@Override
			public Min createStatistic(){
				return new Min();
			}
		});

		putFunction(new AggregateFunction("max"){

			@Override
			public Max createStatistic(){
				return new Max();
			}
		});

		putFunction(new AggregateFunction("avg"){

			@Override
			public Mean createStatistic(){
				return new Mean();
			}

			@Override
			public DataType getResultType(DataType dataType){
				return integerToDouble(dataType);
			}
		});

		putFunction(new AggregateFunction("sum"){

			@Override
			public Sum createStatistic(){
				return new Sum();
			}
		});

		putFunction(new AggregateFunction("product"){

			@Override
			public Product createStatistic(){
				return new Product();
			}
		});
	}

	static {
		putFunction(new FpMathFunction("log10"){

			@Override
			public Double evaluate(Number value){
				return Math.log10(value.doubleValue());
			}
		});

		putFunction(new FpMathFunction("ln"){

			@Override
			public Double evaluate(Number value){
				return Math.log(value.doubleValue());
			}
		});

		putFunction(new FpMathFunction("exp"){

			@Override
			public Double evaluate(Number value){
				return Math.exp(value.doubleValue());
			}
		});

		putFunction(new FpMathFunction("sqrt"){

			@Override
			public Double evaluate(Number value){
				return Math.sqrt(value.doubleValue());
			}
		});

		putFunction(new MathFunction("abs"){

			@Override
			public Double evaluate(Number value){
				return Math.abs(value.doubleValue());
			}
		});

		putFunction(new AbstractFunction("pow"){

			@Override
			public FieldValue evaluate(List<FieldValue> arguments){
				checkArguments(arguments, 2);

				FieldValue left = arguments.get(0);
				FieldValue right = arguments.get(1);

				DataType dataType = TypeUtil.getResultDataType(left.getDataType(), right.getDataType());

				Double result = Math.pow((left.asNumber()).doubleValue(), (right.asNumber()).doubleValue());

				return FieldValueUtil.create(cast(dataType, result));
			}
		});

		putFunction(new AbstractFunction("threshold"){

			@Override
			public FieldValue evaluate(List<FieldValue> arguments){
				checkArguments(arguments, 2);

				FieldValue left = arguments.get(0);
				FieldValue right = arguments.get(1);

				DataType dataType = TypeUtil.getResultDataType(left.getDataType(), right.getDataType());

				Integer result = ((left.asNumber()).doubleValue() > (right.asNumber()).doubleValue()) ? 1 : 0;

				return FieldValueUtil.create(cast(dataType, result));
			}
		});

		putFunction(new MathFunction("floor"){

			@Override
			public Double evaluate(Number number){
				return Math.floor(number.doubleValue());
			}
		});

		putFunction(new MathFunction("ceil"){

			@Override
			public Double evaluate(Number number){
				return Math.ceil(number.doubleValue());
			}
		});

		putFunction(new MathFunction("round"){

			@Override
			public Double evaluate(Number number){
				return (double)Math.round(number.doubleValue());
			}
		});
	}

	static {
		putFunction(new ValueFunction("isMissing"){

			@Override
			public Boolean evaluate(FieldValue value){
				return Boolean.valueOf(value == null);
			}
		});

		putFunction(new ValueFunction("isNotMissing"){

			@Override
			public Boolean evaluate(FieldValue value){
				return Boolean.valueOf(value != null);
			}
		});
	}

	static {
		putFunction(new EqualityFunction("equal"){

			@Override
			public Boolean evaluate(boolean equals){
				return Boolean.valueOf(equals);
			}
		});

		putFunction(new EqualityFunction("notEqual"){

			@Override
			public Boolean evaluate(boolean equals){
				return Boolean.valueOf(!equals);
			}
		});

	}

	static {
		putFunction(new ComparisonFunction("lessThan"){

			@Override
			public Boolean evaluate(int order){
				return Boolean.valueOf(order < 0);
			}
		});

		putFunction(new ComparisonFunction("lessOrEqual"){

			@Override
			public Boolean evaluate(int order){
				return Boolean.valueOf(order <= 0);
			}
		});

		putFunction(new ComparisonFunction("greaterThan"){

			@Override
			public Boolean evaluate(int order){
				return Boolean.valueOf(order > 0);
			}
		});

		putFunction(new ComparisonFunction("greaterOrEqual"){

			@Override
			public Boolean evaluate(int order){
				return Boolean.valueOf(order >= 0);
			}
		});
	}

	static {
		putFunction(new BinaryBooleanFunction("and"){

			@Override
			public Boolean evaluate(Boolean left, Boolean right){
				return Boolean.valueOf(left.booleanValue() & right.booleanValue());
			}
		});

		putFunction(new BinaryBooleanFunction("or"){

			@Override
			public Boolean evaluate(Boolean left, Boolean right){
				return Boolean.valueOf(left.booleanValue() | right.booleanValue());
			}
		});
	}

	static {
		putFunction(new UnaryBooleanFunction("not"){

			@Override
			public Boolean evaluate(Boolean value){
				return Boolean.valueOf(!value.booleanValue());
			}
		});
	}

	static {
		putFunction(new ValueListFunction("isIn"){

			@Override
			public Boolean evaluate(FieldValue value, List<FieldValue> values){
				return value.equalsAnyValue(values);
			}
		});

		putFunction(new ValueListFunction("isNotIn"){

			@Override
			public Boolean evaluate(FieldValue value, List<FieldValue> values){
				return !value.equalsAnyValue(values);
			}
		});
	}

	static {
		putFunction(new AbstractFunction("if"){

			@Override
			public FieldValue evaluate(List<FieldValue> arguments){

				if((arguments.size() < 2 || arguments.size() > 3)){
					throw new FunctionException(getName(), "Expected 2 or 3 arguments, but got " + arguments.size() + " arguments");
				}

				FieldValue flag = arguments.get(0);
				if(flag == null){
					throw new FunctionException(getName(), "Missing arguments");
				} // End if

				if(flag.asBoolean()){
					FieldValue trueValue = arguments.get(1);

					// "The THEN part is required"
					if(trueValue == null){
						throw new FunctionException(getName(), "Missing arguments");
					}

					return trueValue;
				} else

				{
					FieldValue falseValue = (arguments.size() > 2 ? arguments.get(2) : null);

					// "The ELSE part is optional. If the ELSE part is absent, then a missing value is returned"
					if(falseValue == null){
						return null;
					}

					return falseValue;
				}
			}
		});
	}

	static {
		putFunction(new StringFunction("uppercase"){

			@Override
			public String evaluate(String value){
				return value.toUpperCase();
			}
		});

		putFunction(new StringFunction("lowercase"){

			@Override
			public String evaluate(String value){
				return value.toLowerCase();
			}
		});

		putFunction(new AbstractFunction("substring"){

			@Override
			public FieldValue evaluate(List<FieldValue> arguments){
				checkArguments(arguments, 3);

				String string = (arguments.get(0)).asString();

				int position = (arguments.get(1)).asInteger();
				int length = (arguments.get(2)).asInteger();

				// "The first character of a string is located at position 1 (not position 0)"
				if(position <= 0 || length < 0){
					throw new FunctionException(getName(), "Invalid arguments");
				}

				String result = string.substring(position - 1, (position + length) - 1);

				return FieldValueUtil.create(result);
			}
		});

		putFunction(new StringFunction("trimBlanks"){

			@Override
			public String evaluate(String value){
				return value.trim();
			}
		});
	}

	static {
		putFunction(new AbstractFunction("concat"){

			@Override
			public FieldValue evaluate(List<FieldValue> arguments){
				checkVariableArguments(arguments, 2, true);

				StringBuilder sb = new StringBuilder();

				Iterable<FieldValue> values = Iterables.filter(arguments, Predicates.notNull());
				for(FieldValue value : values){
					String string = (String)TypeUtil.cast(DataType.STRING, value.getValue());

					sb.append(string);
				}

				return FieldValueUtil.create(sb.toString());
			}
		});
	}

	static {
		putFunction(new AbstractFunction("replace"){

			@Override
			public FieldValue evaluate(List<FieldValue> arguments){
				checkArguments(arguments, 3);

				String input = (arguments.get(0)).asString();
				String pattern = (arguments.get(1)).asString();
				String replacement = (arguments.get(2)).asString();

				Matcher matcher = Pattern.compile(pattern).matcher(input);

				String result = matcher.replaceAll(replacement);

				return FieldValueUtil.create(result);
			}
		});

		putFunction(new AbstractFunction("matches"){

			@Override
			public FieldValue evaluate(List<FieldValue> arguments){
				checkArguments(arguments, 2);

				String input = (arguments.get(0)).asString();
				String pattern = (arguments.get(1)).asString();

				Matcher matcher = Pattern.compile(pattern).matcher(input);

				// "The string is considered to match the pattern if any substring matches the pattern"
				Boolean result = Boolean.valueOf(matcher.find());

				return FieldValueUtil.create(result);
			}
		});
	}

	static {
		putFunction(new AbstractFunction("formatNumber"){

			@Override
			public FieldValue evaluate(List<FieldValue> arguments){
				checkArguments(arguments, 2);

				FieldValue value = arguments.get(0);
				FieldValue pattern = arguments.get(1);

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

		putFunction(new AbstractFunction("formatDatetime"){

			@Override
			public FieldValue evaluate(List<FieldValue> arguments){
				checkArguments(arguments, 2);

				FieldValue value = arguments.get(0);
				FieldValue pattern = arguments.get(1);

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
		putFunction(new AbstractFunction("dateDaysSinceYear"){

			@Override
			public FieldValue evaluate(List<FieldValue> arguments){
				checkArguments(arguments, 2);

				LocalDate instant = (arguments.get(0)).asLocalDate();

				int year = (arguments.get(1)).asInteger();

				DaysSinceDate period = new DaysSinceDate(year, instant);

				return FieldValueUtil.create(period.intValue());
			}
		});

		putFunction(new AbstractFunction("dateSecondsSinceMidnight"){

			@Override
			public FieldValue evaluate(List<FieldValue> arguments){
				checkArguments(arguments, 1);

				LocalTime instant = (arguments.get(0)).asLocalTime();

				Seconds seconds = Seconds.seconds(instant.getHourOfDay() * 60 * 60 + instant.getMinuteOfHour() * 60 + instant.getSecondOfMinute());

				SecondsSinceMidnight period = new SecondsSinceMidnight(seconds);

				return FieldValueUtil.create(period.intValue());
			}
		});

		putFunction(new AbstractFunction("dateSecondsSinceYear"){

			@Override
			public FieldValue evaluate(List<FieldValue> arguments){
				checkArguments(arguments, 2);

				LocalDateTime instant = (arguments.get(0)).asLocalDateTime();

				int year = (arguments.get(1)).asInteger();

				SecondsSinceDate period = new SecondsSinceDate(year, instant);

				return FieldValueUtil.create(period.intValue());
			}
		});
	}
}