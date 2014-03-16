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

import java.util.*;
import java.util.regex.*;

import org.jpmml.evaluator.functions.*;

import org.apache.commons.math3.stat.descriptive.moment.*;
import org.apache.commons.math3.stat.descriptive.rank.*;
import org.apache.commons.math3.stat.descriptive.summary.*;

import org.dmg.pmml.*;

import com.google.common.collect.*;

import org.joda.time.*;

public class FunctionRegistry {

	private FunctionRegistry(){
	}

	static
	public Function getFunction(String name){
		return FunctionRegistry.functions.get(name);
	}

	static
	public void putFunction(String name, Function function){
		FunctionRegistry.functions.put(name, function);
	}

	private static final Map<String, Function> functions = Maps.newLinkedHashMap();

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

		putFunction("pow", new AbstractFunction(){

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

		putFunction("threshold", new AbstractFunction(){

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

	static {
		putFunction("not", new UnaryBooleanFunction(){

			@Override
			public Boolean evaluate(Boolean value){
				return Boolean.valueOf(!value.booleanValue());
			}
		});
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
		putFunction("if", new AbstractFunction(){

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

		putFunction("substring", new AbstractFunction(){

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
		putFunction("concat", new AbstractFunction(){

			@Override
			public FieldValue evaluate(List<FieldValue> values){
				checkVariableArguments(values, 2);

				StringBuilder sb = new StringBuilder();

				for(FieldValue value : values){
					String string = (String)TypeUtil.cast(DataType.STRING, value.getValue());

					sb.append(string);
				}

				return FieldValueUtil.create(sb.toString());
			}
		});
	}

	static {
		putFunction("replace", new AbstractFunction(){

			@Override
			public FieldValue evaluate(List<FieldValue> values){
				checkArguments(values, 3);

				String input = (values.get(0)).asString();
				String pattern = (values.get(1)).asString();
				String replacement = (values.get(2)).asString();

				Matcher matcher = Pattern.compile(pattern).matcher(input);

				String result = matcher.replaceAll(replacement);

				return FieldValueUtil.create(result);
			}
		});

		putFunction("matches", new AbstractFunction(){

			@Override
			public FieldValue evaluate(List<FieldValue> values){
				checkArguments(values, 2);

				String input = (values.get(0)).asString();
				String pattern = (values.get(1)).asString();

				Matcher matcher = Pattern.compile(pattern).matcher(input);

				// "the string is considered to match the pattern if any substring matches the pattern"
				Boolean result = Boolean.valueOf(matcher.find());

				return FieldValueUtil.create(result);
			}
		});
	}

	static {
		putFunction("formatNumber", new AbstractFunction(){

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

		putFunction("formatDatetime", new AbstractFunction(){

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
		putFunction("dateDaysSinceYear", new AbstractFunction(){

			@Override
			public FieldValue evaluate(List<FieldValue> values){
				checkArguments(values, 2);

				LocalDate instant = (values.get(0)).asLocalDate();

				int year = (values.get(1)).asInteger();

				DaysSinceDate period = new DaysSinceDate(year, instant);

				return FieldValueUtil.create(period.intValue());
			}
		});

		putFunction("dateSecondsSinceMidnight", new AbstractFunction(){

			@Override
			public FieldValue evaluate(List<FieldValue> values){
				checkArguments(values, 1);

				LocalTime instant = (values.get(0)).asLocalTime();

				Seconds seconds = Seconds.seconds(instant.getHourOfDay() * 60 * 60 + instant.getMinuteOfHour() * 60 + instant.getSecondOfMinute());

				SecondsSinceMidnight period = new SecondsSinceMidnight(seconds);

				return FieldValueUtil.create(period.intValue());
			}
		});

		putFunction("dateSecondsSinceYear", new AbstractFunction(){

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