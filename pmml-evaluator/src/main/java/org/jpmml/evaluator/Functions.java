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
package org.jpmml.evaluator;

import java.util.IllegalFormatException;
import java.util.List;
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
import org.dmg.pmml.OpType;
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
import org.jpmml.evaluator.functions.TrigonometricFunction;
import org.jpmml.evaluator.functions.UnaryBooleanFunction;
import org.jpmml.evaluator.functions.ValueFunction;
import org.jpmml.evaluator.functions.ValueListFunction;

public class Functions {

	private Functions(){
	}

	public static final ArithmeticFunction PLUS = new ArithmeticFunction("+"){

		@Override
		public Double evaluate(Number left, Number right){
			return Double.valueOf(left.doubleValue() + right.doubleValue());
		}
	};

	public static final ArithmeticFunction MINUS = new ArithmeticFunction("-"){

		@Override
		public Double evaluate(Number left, Number right){
			return Double.valueOf(left.doubleValue() - right.doubleValue());
		}
	};

	public static final ArithmeticFunction MULTIPLY = new ArithmeticFunction("*"){

		@Override
		public Double evaluate(Number left, Number right){
			return Double.valueOf(left.doubleValue() * right.doubleValue());
		}
	};

	public static final ArithmeticFunction DIVIDE = new ArithmeticFunction("/"){

		@Override
		public Number evaluate(Number left, Number right){

			if(left instanceof Integer && right instanceof Integer){
				return Integer.valueOf(left.intValue() / right.intValue());
			}

			return Double.valueOf(left.doubleValue() / right.doubleValue());
		}
	};

	public static final AggregateFunction MIN = new AggregateFunction("min"){

		@Override
		public Min createStatistic(){
			return new Min();
		}
	};

	public static final AggregateFunction MAX = new AggregateFunction("max"){

		@Override
		public Max createStatistic(){
			return new Max();
		}
	};

	public static final AggregateFunction AVG = new AggregateFunction("avg"){

		@Override
		public Mean createStatistic(){
			return new Mean();
		}

		@Override
		public DataType getResultType(DataType dataType){
			return integerToDouble(dataType);
		}
	};

	public static final AggregateFunction SUM = new AggregateFunction("sum"){

		@Override
		public Sum createStatistic(){
			return new Sum();
		}
	};

	public static final AggregateFunction PRODUCT = new AggregateFunction("product"){

		@Override
		public Product createStatistic(){
			return new Product();
		}
	};

	public static final FpMathFunction LOG10 = new FpMathFunction("log10"){

		@Override
		public Double evaluate(Number value){
			return Math.log10(value.doubleValue());
		}
	};

	public static final FpMathFunction LN = new FpMathFunction("ln"){

		@Override
		public Double evaluate(Number value){
			return Math.log(value.doubleValue());
		}
	};

	public static final FpMathFunction LN1P = new FpMathFunction("x-ln1p"){

		@Override
		public Double evaluate(Number value){
			return Math.log1p(value.doubleValue());
		}
	};

	public static final FpMathFunction EXP = new FpMathFunction("exp"){

		@Override
		public Double evaluate(Number value){
			return Math.exp(value.doubleValue());
		}
	};

	public static final FpMathFunction EXPM1 = new FpMathFunction("x-expm1"){

		@Override
		public Double evaluate(Number value){
			return Math.expm1(value.doubleValue());
		}
	};

	public static final FpMathFunction SQRT = new FpMathFunction("sqrt"){

		@Override
		public Double evaluate(Number value){
			return Math.sqrt(value.doubleValue());
		}
	};

	public static final MathFunction ABS = new MathFunction("abs"){

		@Override
		public Double evaluate(Number value){
			return Math.abs(value.doubleValue());
		}
	};

	public static final AbstractFunction POW = new AbstractFunction("pow"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkArguments(arguments, 2);

			FieldValue left = arguments.get(0);
			FieldValue right = arguments.get(1);

			DataType dataType = TypeUtil.getResultDataType(left.getDataType(), right.getDataType());

			Double result = Math.pow((left.asNumber()).doubleValue(), (right.asNumber()).doubleValue());

			return FieldValueUtil.create(dataType, OpType.CONTINUOUS, result);
		}
	};

	public static final AbstractFunction THRESHOLD = new AbstractFunction("threshold"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkArguments(arguments, 2);

			FieldValue left = arguments.get(0);
			FieldValue right = arguments.get(1);

			DataType dataType = TypeUtil.getResultDataType(left.getDataType(), right.getDataType());

			Integer result = ((left.asNumber()).doubleValue() > (right.asNumber()).doubleValue()) ? Numbers.INTEGER_ONE : Numbers.INTEGER_ZERO;

			return FieldValueUtil.create(dataType, OpType.CONTINUOUS, result);
		}
	};

	public static final MathFunction FLOOR = new MathFunction("floor"){

		@Override
		public Double evaluate(Number number){
			return Math.floor(number.doubleValue());
		}
	};

	public static final MathFunction CEIL = new MathFunction("ceil"){

		@Override
		public Double evaluate(Number number){
			return Math.ceil(number.doubleValue());
		}
	};

	public static final MathFunction ROUND = new MathFunction("round"){

		@Override
		public Double evaluate(Number number){
			return (double)Math.round(number.doubleValue());
		}
	};

	public static final MathFunction RINT = new MathFunction("x-rint"){

		@Override
		public Double evaluate(Number number){
			return Math.rint(number.doubleValue());
		}
	};

	public static final ValueFunction IS_MISSING = new ValueFunction("isMissing"){

		@Override
		public Boolean evaluate(FieldValue value){
			return Boolean.valueOf(value == null);
		}
	};

	public static final ValueFunction IS_NOT_MISSING = new ValueFunction("isNotMissing"){

		@Override
		public Boolean evaluate(FieldValue value){
			return Boolean.valueOf(value != null);
		}
	};

	public static final EqualityFunction EQUAL = new EqualityFunction("equal"){

		@Override
		public Boolean evaluate(boolean equals){
			return Boolean.valueOf(equals);
		}
	};

	public static final EqualityFunction NOT_EQUAL = new EqualityFunction("notEqual"){

		@Override
		public Boolean evaluate(boolean equals){
			return Boolean.valueOf(!equals);
		}
	};

	public static final ComparisonFunction LESS_THAN = new ComparisonFunction("lessThan"){

		@Override
		public Boolean evaluate(int order){
			return Boolean.valueOf(order < 0);
		}
	};

	public static final ComparisonFunction LESS_OR_EQUAL = new ComparisonFunction("lessOrEqual"){

		@Override
		public Boolean evaluate(int order){
			return Boolean.valueOf(order <= 0);
		}
	};

	public static final ComparisonFunction GREATER_THAN = new ComparisonFunction("greaterThan"){

		@Override
		public Boolean evaluate(int order){
			return Boolean.valueOf(order > 0);
		}
	};

	public static final ComparisonFunction GREATER_OR_EQUAL = new ComparisonFunction("greaterOrEqual"){

		@Override
		public Boolean evaluate(int order){
			return Boolean.valueOf(order >= 0);
		}
	};

	public static final BinaryBooleanFunction AND = new BinaryBooleanFunction("and"){

		@Override
		public Boolean evaluate(Boolean left, Boolean right){
			return Boolean.valueOf(left.booleanValue() & right.booleanValue());
		}
	};

	public static final BinaryBooleanFunction OR = new BinaryBooleanFunction("or"){

		@Override
		public Boolean evaluate(Boolean left, Boolean right){
			return Boolean.valueOf(left.booleanValue() | right.booleanValue());
		}
	};

	public static final UnaryBooleanFunction NOT = new UnaryBooleanFunction("not"){

		@Override
		public Boolean evaluate(Boolean value){
			return Boolean.valueOf(!value.booleanValue());
		}
	};

	public static final ValueListFunction IS_IN = new ValueListFunction("isIn"){

		@Override
		public Boolean evaluate(int index){
			return Boolean.valueOf(index >= 0);
		}
	};

	public static final ValueListFunction IS_NOT_IN = new ValueListFunction("isNotIn"){

		@Override
		public Boolean evaluate(int index){
			return Boolean.valueOf(index < 0);
		}
	};

	public static final AbstractFunction IF = new AbstractFunction("if"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){

			if((arguments.size() < 2 || arguments.size() > 3)){
				throw new FunctionException(this, "Expected 2 or 3 arguments, but got " + arguments.size() + " arguments");
			}

			FieldValue flag = arguments.get(0);
			if(flag == null){
				throw new FunctionException(this, "Missing arguments");
			} // End if

			if(flag.asBoolean()){
				FieldValue trueValue = arguments.get(1);

				// "The THEN part is required"
				if(trueValue == null){
					throw new FunctionException(this, "Missing arguments");
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
	};

	public static final StringFunction UPPERCASE = new StringFunction("uppercase"){

		@Override
		public String evaluate(String value){
			return value.toUpperCase();
		}
	};

	public static final StringFunction LOWERCASE = new StringFunction("lowercase"){

		@Override
		public String evaluate(String value){
			return value.toLowerCase();
		}
	};

	public static final AbstractFunction SUBSTRING = new AbstractFunction("substring"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkArguments(arguments, 3);

			String string = (arguments.get(0)).asString();

			int position = (arguments.get(1)).asInteger();
			if(position < 1){
				throw new FunctionException(this, "Invalid position value " + position + ". Must be equal or greater than 1");
			}

			// "The first character of a string is located at position 1 (not position 0)"
			int javaPosition = Math.min(position - 1, string.length());

			int length = (arguments.get(2)).asInteger();
			if(length < 0){
				throw new FunctionException(this, "Invalid length value " + length);
			}

			int javaLength = Math.min(length, (string.length() - javaPosition));

			// This expression must never throw a StringIndexOutOfBoundsException
			String result = string.substring(javaPosition, javaPosition + javaLength);

			return FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, result);
		}
	};

	public static final StringFunction TRIM_BLANKS = new StringFunction("trimBlanks"){

		@Override
		public String evaluate(String value){
			return value.trim();
		}
	};

	public static final AbstractFunction CONCAT = new AbstractFunction("concat"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkVariableArguments(arguments, 2, true);

			StringBuilder sb = new StringBuilder();

			Iterable<FieldValue> values = Iterables.filter(arguments, Predicates.notNull());
			for(FieldValue value : values){
				String string = (String)TypeUtil.cast(DataType.STRING, value.getValue());

				sb.append(string);
			}

			return FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, sb.toString());
		}
	};

	public static final AbstractFunction REPLACE = new AbstractFunction("replace"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkArguments(arguments, 3);

			String input = (arguments.get(0)).asString();
			String regex = (arguments.get(1)).asString();
			String replacement = (arguments.get(2)).asString();

			Pattern pattern = RegExUtil.compile(regex, null);

			Matcher matcher = pattern.matcher(input);

			String result = matcher.replaceAll(replacement);

			return FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, result);
		}
	};

	public static final AbstractFunction MATCHES = new AbstractFunction("matches"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkArguments(arguments, 2);

			String input = (arguments.get(0)).asString();
			String pattern = (arguments.get(1)).asString();

			Matcher matcher = Pattern.compile(pattern).matcher(input);

			// "The string is considered to match the pattern if any substring matches the pattern"
			Boolean result = Boolean.valueOf(matcher.find());

			return FieldValueUtil.create(DataType.BOOLEAN, OpType.CATEGORICAL, result);
		}
	};

	public static final AbstractFunction FORMAT_NUMBER = new AbstractFunction("formatNumber"){

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
				throw new FunctionException(this, formatMessage("Invalid format value \"" + pattern.asString() + "\"", ife));
			}

			return FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, result);
		}
	};

	public static final AbstractFunction FORMAT_DATETIME = new AbstractFunction("formatDatetime"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkArguments(arguments, 2);

			FieldValue value = arguments.get(0);
			FieldValue pattern = arguments.get(1);

			String result;

			try {
				result = String.format(translatePattern(pattern.asString()), (value.asDateTime()).toDate());
			} catch(IllegalFormatException ife){
				throw new FunctionException(this, formatMessage("Invalid format value \"" + pattern.asString() + "\"", ife));
			}

			return FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, result);
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
	};

	public static final AbstractFunction DATE_DAYS_SINCE_YEAR = new AbstractFunction("dateDaysSinceYear"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkArguments(arguments, 2);

			LocalDate instant = (arguments.get(0)).asLocalDate();

			int year = (arguments.get(1)).asInteger();

			DaysSinceDate period = new DaysSinceDate(year, instant);

			return FieldValueUtil.create(DataType.INTEGER, OpType.CONTINUOUS, period.intValue());
		}
	};

	public static final AbstractFunction DATE_SECONDS_SINCE_MIDNIGHT = new AbstractFunction("dateSecondsSinceMidnight"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkArguments(arguments, 1);

			LocalTime instant = (arguments.get(0)).asLocalTime();

			Seconds seconds = Seconds.seconds(instant.getHourOfDay() * 60 * 60 + instant.getMinuteOfHour() * 60 + instant.getSecondOfMinute());

			SecondsSinceMidnight period = new SecondsSinceMidnight(seconds);

			return FieldValueUtil.create(DataType.INTEGER, OpType.CONTINUOUS, period.intValue());
		}
	};

	public static final AbstractFunction DATE_SECONDS_SINCE_YEAR = new AbstractFunction("dateSecondsSinceYear"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkArguments(arguments, 2);

			LocalDateTime instant = (arguments.get(0)).asLocalDateTime();

			int year = (arguments.get(1)).asInteger();

			SecondsSinceDate period = new SecondsSinceDate(year, instant);

			return FieldValueUtil.create(DataType.INTEGER, OpType.CONTINUOUS, period.intValue());
		}
	};

	public static final AbstractFunction HYPOT = new AbstractFunction("x-hypot"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkArguments(arguments, 2);

			Number x = (arguments.get(0)).asNumber();
			Number y = (arguments.get(1)).asNumber();

			Double result = Math.hypot(x.doubleValue(), y.doubleValue());

			return FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, result);
		}
	};

	public static final TrigonometricFunction SIN = new TrigonometricFunction("x-sin"){

		@Override
		public Double evaluate(Number value){
			return Math.sin(value.doubleValue());
		}
	};

	public static final TrigonometricFunction COS = new TrigonometricFunction("x-cos"){

		@Override
		public Double evaluate(Number value){
			return Math.cos(value.doubleValue());
		}
	};

	public static final TrigonometricFunction TAN = new TrigonometricFunction("x-tan"){

		@Override
		public Double evaluate(Number value){
			return Math.tan(value.doubleValue());
		}
	};

	public static final TrigonometricFunction ASIN = new TrigonometricFunction("x-asin"){

		@Override
		public Double evaluate(Number value){
			return Math.asin(value.doubleValue());
		}
	};

	public static final TrigonometricFunction ACOS = new TrigonometricFunction("x-acos"){

		@Override
		public Double evaluate(Number value){
			return Math.acos(value.doubleValue());
		}
	};

	public static final TrigonometricFunction ATAN = new TrigonometricFunction("x-atan"){

		@Override
		public Double evaluate(Number value){
			return Math.atan(value.doubleValue());
		}
	};

	public static final AbstractFunction ATAN2 = new AbstractFunction("x-atan2"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkArguments(arguments, 2);

			Number y = (arguments.get(0)).asNumber();
			Number x = (arguments.get(1)).asNumber();

			Double result = Math.atan2(y.doubleValue(), x.doubleValue());
			if(result.isNaN()){
				throw new InvalidResultException(null);
			}

			return FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, result);
		}
	};

	public static final TrigonometricFunction SINH = new TrigonometricFunction("x-sinh"){

		@Override
		public Double evaluate(Number value){
			return Math.sinh(value.doubleValue());
		}
	};

	public static final TrigonometricFunction COSH = new TrigonometricFunction("x-cosh"){

		@Override
		public Double evaluate(Number value){
			return Math.cosh(value.doubleValue());
		}
	};

	public static final TrigonometricFunction TANH = new TrigonometricFunction("x-tanh"){

		@Override
		public Double evaluate(Number value){
			return Math.tanh(value.doubleValue());
		}
	};

	static
	private String formatMessage(String message, Exception cause){
		String causeMessage = cause.getMessage();

		if(causeMessage != null){
			message += " (" + causeMessage + ")";
		}

		return message;
	}
}