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
import java.util.Arrays;
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.math.IntMath;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;
import org.apache.commons.math3.stat.descriptive.summary.Product;
import org.apache.commons.math3.stat.descriptive.summary.Sum;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMMLFunctions;
import org.jpmml.evaluator.functions.AggregateMathFunction;
import org.jpmml.evaluator.functions.AggregateStringFunction;
import org.jpmml.evaluator.functions.ArithmeticFunction;
import org.jpmml.evaluator.functions.BinaryFunction;
import org.jpmml.evaluator.functions.ComparisonFunction;
import org.jpmml.evaluator.functions.DoubleUnaryMathFunction;
import org.jpmml.evaluator.functions.EqualityFunction;
import org.jpmml.evaluator.functions.LogicalFunction;
import org.jpmml.evaluator.functions.MultiaryFunction;
import org.jpmml.evaluator.functions.RoundingFunction;
import org.jpmml.evaluator.functions.TernaryFunction;
import org.jpmml.evaluator.functions.TrigonometricFunction;
import org.jpmml.evaluator.functions.UnaryBooleanFunction;
import org.jpmml.evaluator.functions.UnaryFunction;
import org.jpmml.evaluator.functions.UnaryMathFunction;
import org.jpmml.evaluator.functions.UnaryStringFunction;
import org.jpmml.evaluator.functions.ValueFunction;
import org.jpmml.evaluator.functions.ValueSpaceFunction;

public interface Functions {

	ArithmeticFunction ADD = new ArithmeticFunction(PMMLFunctions.ADD){

		@Override
		public Number evaluate(Number left, Number right){

			if(left instanceof Integer && right instanceof Integer){
				return IntMath.checkedAdd(left.intValue(), right.intValue());
			}

			return (left.doubleValue() + right.doubleValue());
		}
	};

	ArithmeticFunction SUBTRACT = new ArithmeticFunction(PMMLFunctions.SUBTRACT){

		@Override
		public Number evaluate(Number left, Number right){

			if(left instanceof Integer && right instanceof Integer){
				return IntMath.checkedSubtract(left.intValue(), right.intValue());
			}

			return (left.doubleValue() - right.doubleValue());
		}
	};

	ArithmeticFunction MULTIPLY = new ArithmeticFunction(PMMLFunctions.MULTIPLY){

		@Override
		public Number evaluate(Number left, Number right){

			if(left instanceof Integer && right instanceof Integer){
				return IntMath.checkedMultiply(left.intValue(), right.intValue());
			}

			return (left.doubleValue() * right.doubleValue());
		}
	};

	ArithmeticFunction DIVIDE = new ArithmeticFunction(PMMLFunctions.DIVIDE){

		@Override
		public Number evaluate(Number left, Number right){

			if(left instanceof Integer && right instanceof Integer){
				return (left.intValue() / right.intValue());
			}

			return (left.doubleValue() / right.doubleValue());
		}
	};

	ArithmeticFunction MODULO = new ArithmeticFunction(PMMLFunctions.MODULO){

		@Override
		public Number evaluate(Number left, Number right){

			if(left instanceof Integer && right instanceof Integer){
				int leftValue = left.intValue();
				int rightValue = right.intValue();

				int floorDivResult = (leftValue / rightValue);
				if((leftValue ^ rightValue) < 0 && (leftValue != floorDivResult * rightValue)){
					floorDivResult--;
				}

				return leftValue - floorDivResult * rightValue;
			}

			double leftValue = left.doubleValue();
			double rightValue = right.doubleValue();

			return leftValue - Math.floor(leftValue / rightValue) * rightValue;
		}
	};

	AggregateMathFunction MIN = new AggregateMathFunction(PMMLFunctions.MIN){

		@Override
		public Min createStatistic(){
			return new Min();
		}
	};

	AggregateMathFunction MAX = new AggregateMathFunction(PMMLFunctions.MAX){

		@Override
		public Max createStatistic(){
			return new Max();
		}
	};

	AggregateMathFunction AVG = new AggregateMathFunction(PMMLFunctions.AVG){

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

	AggregateMathFunction SUM = new AggregateMathFunction(PMMLFunctions.SUM){

		@Override
		public Sum createStatistic(){
			return new Sum();
		}
	};

	AggregateMathFunction PRODUCT = new AggregateMathFunction(PMMLFunctions.PRODUCT){

		@Override
		public Product createStatistic(){
			return new Product();
		}
	};

	DoubleUnaryMathFunction LOG10 = new DoubleUnaryMathFunction(PMMLFunctions.LOG10){

		@Override
		public Double evaluate(Number value){
			return Math.log10(value.doubleValue());
		}
	};

	DoubleUnaryMathFunction LN = new DoubleUnaryMathFunction(PMMLFunctions.LN){

		@Override
		public Double evaluate(Number value){
			return Math.log(value.doubleValue());
		}
	};

	DoubleUnaryMathFunction LN1P = new DoubleUnaryMathFunction(PMMLFunctions.LN1P){

		@Override
		public Double evaluate(Number value){
			return Math.log1p(value.doubleValue());
		}
	};

	DoubleUnaryMathFunction EXP = new DoubleUnaryMathFunction(PMMLFunctions.EXP){

		@Override
		public Double evaluate(Number value){
			return Math.exp(value.doubleValue());
		}
	};

	DoubleUnaryMathFunction EXPM1 = new DoubleUnaryMathFunction(PMMLFunctions.EXPM1){

		@Override
		public Double evaluate(Number value){
			return Math.expm1(value.doubleValue());
		}
	};

	DoubleUnaryMathFunction SQRT = new DoubleUnaryMathFunction(PMMLFunctions.SQRT){

		@Override
		public Double evaluate(Number value){
			return Math.sqrt(value.doubleValue());
		}
	};

	UnaryMathFunction ABS = new UnaryMathFunction(PMMLFunctions.ABS){

		@Override
		public Number evaluate(Number value){

			if(value instanceof Float){
				return Math.abs(value.floatValue());
			}

			return Math.abs(value.doubleValue());
		}
	};

	BinaryFunction POW = new BinaryFunction(PMMLFunctions.POW){

		public Double evaluate(Number x, Number y){
			return Math.pow(x.doubleValue(), y.doubleValue());
		}

		@Override
		public FieldValue evaluate(FieldValue first, FieldValue second){
			DataType dataType = TypeUtil.getCommonDataType(first.getDataType(), second.getDataType());

			Double result = evaluate(first.asNumber(), second.asNumber());

			return FieldValueUtil.create(dataType, OpType.CONTINUOUS, result);
		}
	};

	BinaryFunction THRESHOLD = new BinaryFunction(PMMLFunctions.THRESHOLD){

		public Integer evaluate(Number x, Number y){
			int order = Double.compare(x.doubleValue(), y.doubleValue());

			return (order > 0) ? Numbers.INTEGER_ONE : Numbers.INTEGER_ZERO;
		}

		@Override
		public FieldValue evaluate(FieldValue first, FieldValue second){
			DataType dataType = TypeUtil.getCommonDataType(first.getDataType(), second.getDataType());

			Integer result = evaluate(first.asNumber(), second.asNumber());

			return FieldValueUtil.create(dataType, OpType.CONTINUOUS, result);
		}
	};

	RoundingFunction FLOOR = new RoundingFunction(PMMLFunctions.FLOOR){

		@Override
		public Integer evaluate(Number number){
			long result = (long)Math.floor(number.doubleValue());

			return MathUtil.toIntExact(result);
		}
	};

	RoundingFunction CEIL = new RoundingFunction(PMMLFunctions.CEIL){

		@Override
		public Integer evaluate(Number number){
			long result = (long)Math.ceil(number.doubleValue());

			return MathUtil.toIntExact(result);
		}
	};

	RoundingFunction ROUND = new RoundingFunction(PMMLFunctions.ROUND){

		@Override
		public Integer evaluate(Number number){
			long result;

			if(number instanceof Float){
				result = (long)Math.round(number.floatValue());
			} else

			{
				result = (long)Math.round(number.doubleValue());
			}

			return MathUtil.toIntExact(result);
		}
	};

	UnaryMathFunction RINT = new UnaryMathFunction(PMMLFunctions.RINT){

		@Override
		public Number evaluate(Number number){

			if(number instanceof Float){
				return Math.rint(number.floatValue());
			}

			return Math.rint(number.doubleValue());
		}
	};

	ValueFunction IS_MISSING = new ValueFunction(PMMLFunctions.ISMISSING){

		@Override
		public FieldValue evaluate(FieldValue value){
			Boolean result = FieldValueUtil.isMissing(value);

			return FieldValue.create(TypeInfos.CATEGORICAL_BOOLEAN, result);
		}
	};

	ValueFunction IS_NOT_MISSING = new ValueFunction(PMMLFunctions.ISNOTMISSING){

		@Override
		public FieldValue evaluate(FieldValue value){
			Boolean result = !FieldValueUtil.isMissing(value);

			return FieldValue.create(TypeInfos.CATEGORICAL_BOOLEAN, result);
		}
	};

	ValueFunction IS_VALID = new ValueFunction(PMMLFunctions.ISVALID){

		@Override
		public FieldValue evaluate(FieldValue value){
			Boolean result = !FieldValueUtil.isMissing(value) && value.isValid();

			return FieldValue.create(TypeInfos.CATEGORICAL_BOOLEAN, result);
		}
	};

	ValueFunction IS_NOT_VALID = new ValueFunction(PMMLFunctions.ISNOTVALID){

		@Override
		public FieldValue evaluate(FieldValue value){
			Boolean result = !FieldValueUtil.isMissing(value) && !value.isValid();

			return FieldValue.create(TypeInfos.CATEGORICAL_BOOLEAN, result);
		}
	};

	EqualityFunction EQUAL = new EqualityFunction(PMMLFunctions.EQUAL){

		@Override
		public Boolean evaluate(boolean equals){
			return Boolean.valueOf(equals);
		}
	};

	EqualityFunction NOT_EQUAL = new EqualityFunction(PMMLFunctions.NOTEQUAL){

		@Override
		public Boolean evaluate(boolean equals){
			return Boolean.valueOf(!equals);
		}
	};

	ComparisonFunction LESS_THAN = new ComparisonFunction(PMMLFunctions.LESSTHAN){

		@Override
		public Boolean evaluate(int order){
			return Boolean.valueOf(order < 0);
		}
	};

	ComparisonFunction LESS_OR_EQUAL = new ComparisonFunction(PMMLFunctions.LESSOREQUAL){

		@Override
		public Boolean evaluate(int order){
			return Boolean.valueOf(order <= 0);
		}
	};

	ComparisonFunction GREATER_THAN = new ComparisonFunction(PMMLFunctions.GREATERTHAN){

		@Override
		public Boolean evaluate(int order){
			return Boolean.valueOf(order > 0);
		}
	};

	ComparisonFunction GREATER_OR_EQUAL = new ComparisonFunction(PMMLFunctions.GREATEROREQUAL){

		@Override
		public Boolean evaluate(int order){
			return Boolean.valueOf(order >= 0);
		}
	};

	LogicalFunction AND = new LogicalFunction(PMMLFunctions.AND){

		@Override
		public Boolean evaluate(Boolean left, Boolean right){
			return Boolean.valueOf(left.booleanValue() & right.booleanValue());
		}
	};

	LogicalFunction OR = new LogicalFunction(PMMLFunctions.OR){

		@Override
		public Boolean evaluate(Boolean left, Boolean right){
			return Boolean.valueOf(left.booleanValue() | right.booleanValue());
		}
	};

	UnaryBooleanFunction NOT = new UnaryBooleanFunction(PMMLFunctions.NOT){

		@Override
		public Boolean evaluate(Boolean value){
			return Boolean.valueOf(!value.booleanValue());
		}
	};

	ValueSpaceFunction IS_IN = new ValueSpaceFunction(PMMLFunctions.ISIN){

		@Override
		public Boolean evaluate(boolean isIn){
			return Boolean.valueOf(isIn);
		}
	};

	ValueSpaceFunction IS_NOT_IN = new ValueSpaceFunction(PMMLFunctions.ISNOTIN){

		@Override
		public Boolean evaluate(boolean isIn){
			return Boolean.valueOf(!isIn);
		}
	};

	MultiaryFunction IF = new MultiaryFunction(PMMLFunctions.IF){

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

	UnaryStringFunction UPPERCASE = new UnaryStringFunction(PMMLFunctions.UPPERCASE){

		@Override
		public String evaluate(String value){
			return value.toUpperCase();
		}
	};

	UnaryStringFunction LOWERCASE = new UnaryStringFunction(PMMLFunctions.LOWERCASE){

		@Override
		public String evaluate(String value){
			return value.toLowerCase();
		}
	};

	UnaryFunction STRING_LENGTH = new UnaryFunction(PMMLFunctions.STRINGLENGTH){

		@Override
		public FieldValue evaluate(FieldValue value){
			String string = value.asString();

			return FieldValueUtil.create(TypeInfos.CONTINUOUS_INTEGER, string.length());
		}
	};

	TernaryFunction SUBSTRING = new TernaryFunction(PMMLFunctions.SUBSTRING, Arrays.asList("input", "startPos", "length")){

		public String evaluate(String string, int position, int length){

			if(position < 1){
				throw new FunctionException(this, "Invalid \"startPos\" value " + position + ". Must be equal or greater than 1");
			} // End if

			if(length < 0){
				throw new FunctionException(this, "Invalid \"length\" value " + length);
			}

			// "The first character of a string is located at position 1 (not position 0)"
			int javaPosition = Math.min(position - 1, string.length());

			int javaLength = Math.min(length, (string.length() - javaPosition));

			// This expression must never throw a StringIndexOutOfBoundsException
			return string.substring(javaPosition, javaPosition + javaLength);
		}

		@Override
		public FieldValue evaluate(FieldValue first, FieldValue second, FieldValue third){
			String result = evaluate(first.asString(), second.asInteger(), third.asInteger());

			return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, result);
		}
	};

	UnaryStringFunction TRIM_BLANKS = new UnaryStringFunction(PMMLFunctions.TRIMBLANKS){

		@Override
		public String evaluate(String value){
			return value.trim();
		}
	};

	AggregateStringFunction CONCAT = new AggregateStringFunction(PMMLFunctions.CONCAT){

		@Override
		public FieldValue evaluate(List<FieldValue> arguments){
			checkVariableArityArguments(arguments, 2);

			StringBuilder sb = new StringBuilder();

			for(int i = 0; i < arguments.size(); i++){
				FieldValue value = getOptionalArgument(arguments, i);

				if(FieldValueUtil.isMissing(value)){
					continue;
				}

				sb.append(value.asString());
			}

			String result = sb.toString();

			return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, result);
		}
	};

	TernaryFunction REPLACE = new TernaryFunction(PMMLFunctions.REPLACE, Arrays.asList("input", "pattern", "replacement")){

		public String evaluate(String input, String regex, String replacement){
			Pattern pattern = RegExUtil.compile(regex, null);

			Matcher matcher = pattern.matcher(input);

			return matcher.replaceAll(replacement);
		}

		@Override
		public FieldValue evaluate(FieldValue first, FieldValue second, FieldValue third){
			String result = evaluate(first.asString(), second.asString(), third.asString());

			return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, result);
		}
	};

	BinaryFunction MATCHES = new BinaryFunction(PMMLFunctions.MATCHES, Arrays.asList("input", "pattern")){

		public Boolean evaluate(String input, String regex){
			Pattern pattern = RegExUtil.compile(regex, null);

			Matcher matcher = pattern.matcher(input);

			// "The string is considered to match the pattern if any substring matches the pattern"
			return Boolean.valueOf(matcher.find());
		}

		@Override
		public FieldValue evaluate(FieldValue first, FieldValue second){
			Boolean result = evaluate(first.asString(), second.asString());

			return FieldValueUtil.create(TypeInfos.CATEGORICAL_BOOLEAN, result);
		}
	};

	BinaryFunction FORMAT_NUMBER = new BinaryFunction(PMMLFunctions.FORMATNUMBER, Arrays.asList("input", "pattern")){

		public String evaluate(Number input, String pattern){

			// According to the java.util.Formatter javadoc, Java formatting is more strict than C's printf formatting.
			// For example, in Java, if a conversion is incompatible with a flag, an exception will be thrown. In C's printf, inapplicable flags are silently ignored.
			try {
				return String.format(pattern, input);
			} catch(IllegalFormatException ife){
				throw new FunctionException(this, "Invalid \"pattern\" value")
					.initCause(ife);
			}
		}

		@Override
		public FieldValue evaluate(FieldValue first, FieldValue second){
			String result = evaluate(first.asNumber(), second.asString());

			return FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, result);
		}
	};

	BinaryFunction FORMAT_DATETIME = new BinaryFunction(PMMLFunctions.FORMATDATETIME, Arrays.asList("input", "pattern")){

		public String evaluate(Date input, String pattern){
			pattern = translatePattern(pattern);

			try {
				return String.format(pattern, input);
			} catch(IllegalFormatException ife){
				throw new FunctionException(this, "Invalid \"pattern\" value")
					.initCause(ife);
			}
		}

		@Override
		public FieldValue evaluate(FieldValue first, FieldValue second){
			ZonedDateTime zonedDateTime = first.asZonedDateTime(ZoneId.systemDefault());

			Date date = Date.from(zonedDateTime.toInstant());

			String result = evaluate(date, second.asString());

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

	BinaryFunction DATE_DAYS_SINCE_YEAR = new BinaryFunction(PMMLFunctions.DATEDAYSSINCEYEAR, Arrays.asList("input", "referenceYear")){

		public DaysSinceDate evaluate(LocalDate input, int year){
			return new DaysSinceDate(LocalDate.of(year, 1, 1), input);
		}

		@Override
		public FieldValue evaluate(FieldValue first, FieldValue second){
			DaysSinceDate result = evaluate(first.asLocalDate(), second.asInteger());

			return FieldValueUtil.create(TypeInfos.CONTINUOUS_INTEGER, result);
		}
	};

	UnaryFunction DATE_SECONDS_SINCE_MIDNIGHT = new UnaryFunction(PMMLFunctions.DATESECONDSSINCEMIDNIGHT, Arrays.asList("input")){

		public SecondsSinceMidnight evaluate(LocalTime input){
			return new SecondsSinceMidnight(input.toSecondOfDay());
		}

		@Override
		public FieldValue evaluate(FieldValue value){
			SecondsSinceMidnight result = evaluate(value.asLocalTime());

			return FieldValueUtil.create(TypeInfos.CONTINUOUS_INTEGER, result);
		}
	};

	BinaryFunction DATE_SECONDS_SINCE_YEAR = new BinaryFunction(PMMLFunctions.DATESECONDSSINCEYEAR, Arrays.asList("input", "referenceYear")){

		public SecondsSinceDate evaluate(LocalDateTime input, int year){
			return new SecondsSinceDate(LocalDate.of(year, 1, 1), input);
		}

		@Override
		public FieldValue evaluate(FieldValue first, FieldValue second){
			SecondsSinceDate result = evaluate(first.asLocalDateTime(), second.asInteger());

			return FieldValueUtil.create(TypeInfos.CONTINUOUS_INTEGER, result);
		}
	};

	BinaryFunction HYPOT = new BinaryFunction(PMMLFunctions.HYPOT){

		public Double evaluate(Number x, Number y){
			return Math.hypot(x.doubleValue(), y.doubleValue());
		}

		@Override
		public FieldValue evaluate(FieldValue first, FieldValue second){
			Double result = evaluate(first.asNumber(), second.asNumber());

			return FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, result);
		}
	};

	TrigonometricFunction SIN = new TrigonometricFunction(PMMLFunctions.SIN){

		@Override
		public Double evaluate(Number value){
			return Math.sin(value.doubleValue());
		}
	};

	TrigonometricFunction COS = new TrigonometricFunction(PMMLFunctions.COS){

		@Override
		public Double evaluate(Number value){
			return Math.cos(value.doubleValue());
		}
	};

	TrigonometricFunction TAN = new TrigonometricFunction(PMMLFunctions.TAN){

		@Override
		public Double evaluate(Number value){
			return Math.tan(value.doubleValue());
		}
	};

	TrigonometricFunction ASIN = new TrigonometricFunction(PMMLFunctions.ASIN){

		@Override
		public Double evaluate(Number value){
			return Math.asin(value.doubleValue());
		}
	};

	TrigonometricFunction ACOS = new TrigonometricFunction(PMMLFunctions.ACOS){

		@Override
		public Double evaluate(Number value){
			return Math.acos(value.doubleValue());
		}
	};

	TrigonometricFunction ATAN = new TrigonometricFunction(PMMLFunctions.ATAN){

		@Override
		public Double evaluate(Number value){
			return Math.atan(value.doubleValue());
		}
	};

	BinaryFunction ATAN2 = new BinaryFunction(PMMLFunctions.ATAN2){

		public Double evaluate(Number y, Number x){
			return Math.atan2(y.doubleValue(), x.doubleValue());
		}

		@Override
		public FieldValue evaluate(FieldValue first, FieldValue second){
			Double result = evaluate(first.asNumber(), second.asNumber());
			if(result.isNaN()){
				throw new NaNResultException();
			}

			return FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, result);
		}
	};

	TrigonometricFunction SINH = new TrigonometricFunction(PMMLFunctions.SINH){

		@Override
		public Double evaluate(Number value){
			return Math.sinh(value.doubleValue());
		}
	};

	TrigonometricFunction COSH = new TrigonometricFunction(PMMLFunctions.COSH){

		@Override
		public Double evaluate(Number value){
			return Math.cosh(value.doubleValue());
		}
	};

	TrigonometricFunction TANH = new TrigonometricFunction(PMMLFunctions.TANH){

		@Override
		public Double evaluate(Number value){
			return Math.tanh(value.doubleValue());
		}
	};
}
