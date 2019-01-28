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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;
import org.apache.commons.math3.stat.descriptive.summary.Product;
import org.apache.commons.math3.stat.descriptive.summary.Sum;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.jpmml.evaluator.functions.AbstractFunction;
import org.jpmml.evaluator.functions.AbstractNumericFunction;
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

public interface Functions {

	ArithmeticFunction PLUS = new ArithmeticFunction("+"){

		@Override
		public Double evaluate(Number left, Number right){
			return Double.valueOf(left.doubleValue() + right.doubleValue());
		}
	};

	ArithmeticFunction MINUS = new ArithmeticFunction("-"){

		@Override
		public Double evaluate(Number left, Number right){
			return Double.valueOf(left.doubleValue() - right.doubleValue());
		}
	};

	ArithmeticFunction MULTIPLY = new ArithmeticFunction("*"){

		@Override
		public Double evaluate(Number left, Number right){
			return Double.valueOf(left.doubleValue() * right.doubleValue());
		}
	};

	ArithmeticFunction DIVIDE = new ArithmeticFunction("/"){

		@Override
		public Number evaluate(Number left, Number right){

			if(left instanceof Integer && right instanceof Integer){
				return Integer.valueOf(left.intValue() / right.intValue());
			}

			return Double.valueOf(left.doubleValue() / right.doubleValue());
		}
	};

	ArithmeticFunction MODULO = new ArithmeticFunction("x-modulo"){

		@Override
		public Number evaluate(Number left, Number right){

			if(left instanceof Integer && right instanceof Integer){
				return Integer.valueOf(left.intValue() % right.intValue());
			}

			return Double.valueOf(left.doubleValue() % right.doubleValue());
		}
	};

	AggregateFunction MIN = new AggregateFunction("min"){

		@Override
		public Min createStatistic(){
			return new Min();
		}
	};

	AggregateFunction MAX = new AggregateFunction("max"){

		@Override
		public Max createStatistic(){
			return new Max();
		}
	};

	AggregateFunction AVG = new AggregateFunction("avg"){

		@Override
		public Mean createStatistic(){
			return new Mean();
		}

		@Override
		public DataType getResultDataType(DataType dataType){

			if((DataType.INTEGER).equals(dataType)){
				return DataType.DOUBLE;
			}

			return dataType;
		}
	};

	AggregateFunction SUM = new AggregateFunction("sum"){

		@Override
		public Sum createStatistic(){
			return new Sum();
		}
	};

	AggregateFunction PRODUCT = new AggregateFunction("product"){

		@Override
		public Product createStatistic(){
			return new Product();
		}
	};

	FpMathFunction LOG10 = new FpMathFunction("log10"){

		@Override
		public Double evaluate(Number value){
			return Math.log10(value.doubleValue());
		}
	};

	FpMathFunction LN = new FpMathFunction("ln"){

		@Override
		public Double evaluate(Number value){
			return Math.log(value.doubleValue());
		}
	};

	FpMathFunction LN1P = new FpMathFunction("x-ln1p"){

		@Override
		public Double evaluate(Number value){
			return Math.log1p(value.doubleValue());
		}
	};

	FpMathFunction EXP = new FpMathFunction("exp"){

		@Override
		public Double evaluate(Number value){
			return Math.exp(value.doubleValue());
		}
	};

	FpMathFunction EXPM1 = new FpMathFunction("x-expm1"){

		@Override
		public Double evaluate(Number value){
			return Math.expm1(value.doubleValue());
		}
	};

	FpMathFunction SQRT = new FpMathFunction("sqrt"){

		@Override
		public Double evaluate(Number value){
			return Math.sqrt(value.doubleValue());
		}
	};

	MathFunction ABS = new MathFunction("abs"){

		@Override
		public Number evaluate(Number value){

			if(value instanceof Float){
				return Math.abs(value.floatValue());
			}

			return Math.abs(value.doubleValue());
		}
	};

	AbstractNumericFunction POW = new AbstractNumericFunction("pow"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkFixedArityArguments(arguments, 2);

			return evaluate(getRequiredArgument(arguments, 0), getRequiredArgument(arguments, 1));
		}

		private FieldValue evaluate(FieldValue left, FieldValue right){
			DataType dataType = TypeUtil.getCommonDataType(left.getDataType(), right.getDataType());

			Double result = Math.pow((left.asNumber()).doubleValue(), (right.asNumber()).doubleValue());

			return FieldValueUtil.create(dataType, OpType.CONTINUOUS, result);
		}
	};

	AbstractNumericFunction THRESHOLD = new AbstractNumericFunction("threshold"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkFixedArityArguments(arguments, 2);

			return evaluate(getRequiredArgument(arguments, 0), getRequiredArgument(arguments, 1));
		}

		private FieldValue evaluate(FieldValue left, FieldValue right){
			DataType dataType = TypeUtil.getCommonDataType(left.getDataType(), right.getDataType());

			Integer result = ((left.asNumber()).doubleValue() > (right.asNumber()).doubleValue()) ? Numbers.INTEGER_ONE : Numbers.INTEGER_ZERO;

			return FieldValueUtil.create(dataType, OpType.CONTINUOUS, result);
		}
	};

	MathFunction FLOOR = new MathFunction("floor"){

		@Override
		public Number evaluate(Number number){
			return Math.floor(number.doubleValue());
		}
	};

	MathFunction CEIL = new MathFunction("ceil"){

		@Override
		public Number evaluate(Number number){
			return Math.ceil(number.doubleValue());
		}
	};

	MathFunction ROUND = new MathFunction("round"){

		@Override
		public Number evaluate(Number number){

			if(number instanceof Float){
				Math.round(number.floatValue());
			}

			return Math.round(number.doubleValue());
		}
	};

	MathFunction RINT = new MathFunction("x-rint"){

		@Override
		public Number evaluate(Number number){
			return Math.rint(number.doubleValue());
		}
	};

	ValueFunction IS_MISSING = new ValueFunction("isMissing"){

		@Override
		public Boolean evaluate(boolean isMissing){
			return Boolean.valueOf(isMissing);
		}
	};

	ValueFunction IS_NOT_MISSING = new ValueFunction("isNotMissing"){

		@Override
		public Boolean evaluate(boolean isMissing){
			return Boolean.valueOf(!isMissing);
		}
	};

	EqualityFunction EQUAL = new EqualityFunction("equal"){

		@Override
		public Boolean evaluate(boolean equals){
			return Boolean.valueOf(equals);
		}
	};

	EqualityFunction NOT_EQUAL = new EqualityFunction("notEqual"){

		@Override
		public Boolean evaluate(boolean equals){
			return Boolean.valueOf(!equals);
		}
	};

	ComparisonFunction LESS_THAN = new ComparisonFunction("lessThan"){

		@Override
		public Boolean evaluate(int order){
			return Boolean.valueOf(order < 0);
		}
	};

	ComparisonFunction LESS_OR_EQUAL = new ComparisonFunction("lessOrEqual"){

		@Override
		public Boolean evaluate(int order){
			return Boolean.valueOf(order <= 0);
		}
	};

	ComparisonFunction GREATER_THAN = new ComparisonFunction("greaterThan"){

		@Override
		public Boolean evaluate(int order){
			return Boolean.valueOf(order > 0);
		}
	};

	ComparisonFunction GREATER_OR_EQUAL = new ComparisonFunction("greaterOrEqual"){

		@Override
		public Boolean evaluate(int order){
			return Boolean.valueOf(order >= 0);
		}
	};

	BinaryBooleanFunction AND = new BinaryBooleanFunction("and"){

		@Override
		public Boolean evaluate(Boolean left, Boolean right){
			return Boolean.valueOf(left.booleanValue() & right.booleanValue());
		}
	};

	BinaryBooleanFunction OR = new BinaryBooleanFunction("or"){

		@Override
		public Boolean evaluate(Boolean left, Boolean right){
			return Boolean.valueOf(left.booleanValue() | right.booleanValue());
		}
	};

	UnaryBooleanFunction NOT = new UnaryBooleanFunction("not"){

		@Override
		public Boolean evaluate(Boolean value){
			return Boolean.valueOf(!value.booleanValue());
		}
	};

	ValueListFunction IS_IN = new ValueListFunction("isIn"){

		@Override
		public Boolean evaluate(boolean isIn){
			return Boolean.valueOf(isIn);
		}
	};

	ValueListFunction IS_NOT_IN = new ValueListFunction("isNotIn"){

		@Override
		public Boolean evaluate(boolean isIn){
			return Boolean.valueOf(!isIn);
		}
	};

	AbstractFunction IF = new AbstractFunction("if"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkVariableArityArguments(arguments, 2, 3);

			Boolean flag = getRequiredArgument(arguments, 0).asBoolean();

			if(flag){
				return getOptionalArgument(arguments, 1);
			} else

			{
				// "The ELSE part is optional. If the ELSE part is absent, then a missing value is returned"
				if(arguments.size() > 2){
					return getOptionalArgument(arguments, 2);
				}

				return FieldValues.MISSING_VALUE;
			}
		}
	};

	StringFunction UPPERCASE = new StringFunction("uppercase"){

		@Override
		public String evaluate(String value){
			return value.toUpperCase();
		}
	};

	StringFunction LOWERCASE = new StringFunction("lowercase"){

		@Override
		public String evaluate(String value){
			return value.toLowerCase();
		}
	};

	AbstractFunction SUBSTRING = new AbstractFunction("substring"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkFixedArityArguments(arguments, 3);

			String string = getRequiredArgument(arguments, 0, "input").asString();

			int position = getRequiredArgument(arguments, 1, "startPos").asInteger();
			if(position < 1){
				throw new FunctionException(this, "Invalid \'startPos\' value " + position + ". Must be equal or greater than 1");
			}

			// "The first character of a string is located at position 1 (not position 0)"
			int javaPosition = Math.min(position - 1, string.length());

			int length = getRequiredArgument(arguments, 2, "length").asInteger();
			if(length < 0){
				throw new FunctionException(this, "Invalid \'length\' value " + length);
			}

			int javaLength = Math.min(length, (string.length() - javaPosition));

			// This expression must never throw a StringIndexOutOfBoundsException
			String result = string.substring(javaPosition, javaPosition + javaLength);

			return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, result);
		}
	};

	StringFunction TRIM_BLANKS = new StringFunction("trimBlanks"){

		@Override
		public String evaluate(String value){
			return value.trim();
		}
	};

	AbstractFunction CONCAT = new AbstractFunction("concat"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkVariableArityArguments(arguments, 2);

			String result = arguments.stream()
				.filter(Objects::nonNull)
				.map(value -> (String)TypeUtil.cast(DataType.STRING, value.getValue()))
				.collect(Collectors.joining());

			return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, result);
		}
	};

	AbstractFunction REPLACE = new AbstractFunction("replace"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkFixedArityArguments(arguments, 3);

			String input = getRequiredArgument(arguments, 0, "input").asString();

			String regex = getRequiredArgument(arguments, 1, "pattern").asString();

			Pattern pattern = RegExUtil.compile(regex, null);

			Matcher matcher = pattern.matcher(input);

			String replacement = getRequiredArgument(arguments, 2, "replacement").asString();

			String result = matcher.replaceAll(replacement);

			return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, result);
		}
	};

	AbstractFunction MATCHES = new AbstractFunction("matches"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkFixedArityArguments(arguments, 2);

			String input = getRequiredArgument(arguments, 0, "input").asString();

			String pattern = getRequiredArgument(arguments, 1, "pattern").asString();

			Matcher matcher = Pattern.compile(pattern).matcher(input);

			// "The string is considered to match the pattern if any substring matches the pattern"
			Boolean result = Boolean.valueOf(matcher.find());

			return FieldValueUtil.create(TypeInfos.CATEGORICAL_BOOLEAN, result);
		}
	};

	AbstractFunction FORMAT_NUMBER = new AbstractFunction("formatNumber"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkFixedArityArguments(arguments, 2);

			Number number = getRequiredArgument(arguments, 0, "input").asNumber();

			String pattern = getRequiredArgument(arguments, 1, "pattern").asString();

			String result;

			// According to the java.util.Formatter javadoc, Java formatting is more strict than C's printf formatting.
			// For example, in Java, if a conversion is incompatible with a flag, an exception will be thrown. In C's printf, inapplicable flags are silently ignored.
			try {
				result = String.format(pattern, number);
			} catch(IllegalFormatException ife){
				throw new FunctionException(this, "Invalid \'pattern\' value")
					.initCause(ife);
			}

			return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, result);
		}
	};

	AbstractFunction FORMAT_DATETIME = new AbstractFunction("formatDatetime"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkFixedArityArguments(arguments, 2);

			ZonedDateTime zonedDateTime = getRequiredArgument(arguments, 0, "input").asZonedDateTime(ZoneId.systemDefault());

			Date date = Date.from(zonedDateTime.toInstant());

			String pattern = translatePattern(getRequiredArgument(arguments, 1, "pattern").asString());

			String result;

			try {
				result = String.format(pattern, date);
			} catch(IllegalFormatException ife){
				throw new FunctionException(this, "Invalid \'pattern\' value")
					.initCause(ife);
			}

			return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, result);
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

	AbstractFunction DATE_DAYS_SINCE_YEAR = new AbstractFunction("dateDaysSinceYear"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkFixedArityArguments(arguments, 2);

			LocalDate instant = getRequiredArgument(arguments, 0, "input").asLocalDate();

			int year = getRequiredArgument(arguments, 1, "referenceYear").asInteger();

			DaysSinceDate period = new DaysSinceDate(LocalDate.of(year, 1, 1), instant);

			return FieldValueUtil.create(TypeInfos.CONTINUOUS_INTEGER, period.intValue());
		}
	};

	AbstractFunction DATE_SECONDS_SINCE_MIDNIGHT = new AbstractFunction("dateSecondsSinceMidnight"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkFixedArityArguments(arguments, 1);

			LocalTime instant = getRequiredArgument(arguments, 0, "input").asLocalTime();

			SecondsSinceMidnight period = new SecondsSinceMidnight(instant.toSecondOfDay());

			return FieldValueUtil.create(TypeInfos.CONTINUOUS_INTEGER, period.intValue());
		}
	};

	AbstractFunction DATE_SECONDS_SINCE_YEAR = new AbstractFunction("dateSecondsSinceYear"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkFixedArityArguments(arguments, 2);

			LocalDateTime instant = getRequiredArgument(arguments, 0, "input").asLocalDateTime();

			int year = getRequiredArgument(arguments, 1, "referenceYear").asInteger();

			SecondsSinceDate period = new SecondsSinceDate(LocalDate.of(year, 1, 1), instant);

			return FieldValueUtil.create(TypeInfos.CONTINUOUS_INTEGER, period.intValue());
		}
	};

	AbstractNumericFunction HYPOT = new AbstractNumericFunction("x-hypot"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkFixedArityArguments(arguments, 2);

			Number x = getRequiredArgument(arguments, 0).asNumber();

			Number y = getRequiredArgument(arguments, 1).asNumber();

			Double result = Math.hypot(x.doubleValue(), y.doubleValue());

			return FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, result);
		}
	};

	TrigonometricFunction SIN = new TrigonometricFunction("x-sin"){

		@Override
		public Double evaluate(Number value){
			return Math.sin(value.doubleValue());
		}
	};

	TrigonometricFunction COS = new TrigonometricFunction("x-cos"){

		@Override
		public Double evaluate(Number value){
			return Math.cos(value.doubleValue());
		}
	};

	TrigonometricFunction TAN = new TrigonometricFunction("x-tan"){

		@Override
		public Double evaluate(Number value){
			return Math.tan(value.doubleValue());
		}
	};

	TrigonometricFunction ASIN = new TrigonometricFunction("x-asin"){

		@Override
		public Double evaluate(Number value){
			return Math.asin(value.doubleValue());
		}
	};

	TrigonometricFunction ACOS = new TrigonometricFunction("x-acos"){

		@Override
		public Double evaluate(Number value){
			return Math.acos(value.doubleValue());
		}
	};

	TrigonometricFunction ATAN = new TrigonometricFunction("x-atan"){

		@Override
		public Double evaluate(Number value){
			return Math.atan(value.doubleValue());
		}
	};

	AbstractNumericFunction ATAN2 = new AbstractNumericFunction("x-atan2"){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkFixedArityArguments(arguments, 2);

			Number y = getRequiredArgument(arguments, 0).asNumber();

			Number x = getRequiredArgument(arguments, 1).asNumber();

			Double result = Math.atan2(y.doubleValue(), x.doubleValue());
			if(result.isNaN()){
				throw new NaNResultException();
			}

			return FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, result);
		}
	};

	TrigonometricFunction SINH = new TrigonometricFunction("x-sinh"){

		@Override
		public Double evaluate(Number value){
			return Math.sinh(value.doubleValue());
		}
	};

	TrigonometricFunction COSH = new TrigonometricFunction("x-cosh"){

		@Override
		public Double evaluate(Number value){
			return Math.cosh(value.doubleValue());
		}
	};

	TrigonometricFunction TANH = new TrigonometricFunction("x-tanh"){

		@Override
		public Double evaluate(Number value){
			return Math.tanh(value.doubleValue());
		}
	};
}
