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

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

import com.google.common.math.DoubleMath;
import org.dmg.pmml.ComplexValue;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;

public class TypeUtil {

	private TypeUtil(){
	}

	static
	public String format(Object value){

		if(value instanceof ComplexValue){
			ComplexValue complexValue = (ComplexValue)value;

			value = complexValue.toSimpleValue();
		}

		return toString(value);
	}

	/**
	 * @throws IllegalArgumentException If the value is a String, and it cannot be parsed to the requested representation.
	 * @throws TypeCheckException If the value is an Object other than String, and it cannot be cast to the requested representation.
	 */
	static
	public Object parseOrCast(DataType dataType, Object value){

		if(value instanceof String){
			String string = (String)value;

			return parse(dataType, string);
		}

		return cast(dataType, value);
	}

	/**
	 * @throws IllegalArgumentException If the String representation of the value cannot be parsed to the requested representation.
	 */
	static
	public Object parse(DataType dataType, String value){

		switch(dataType){
			case STRING:
				return value;
			case INTEGER:
				return parseInteger(value);
			case FLOAT:
				return parseFloat(value);
			case DOUBLE:
				return parseDouble(value);
			case BOOLEAN:
				return parseBoolean(value);
			case DATE:
				return parseDate(value);
			case TIME:
				return parseTime(value);
			case DATE_TIME:
				return parseDateTime(value);
			case DATE_DAYS_SINCE_0:
				throw new NotImplementedException();
			case DATE_DAYS_SINCE_1960:
				return parseDaysSinceDate(Epochs.YEAR_1960, value);
			case DATE_DAYS_SINCE_1970:
				return parseDaysSinceDate(Epochs.YEAR_1970, value);
			case DATE_DAYS_SINCE_1980:
				return parseDaysSinceDate(Epochs.YEAR_1980, value);
			case TIME_SECONDS:
				return parseSecondsSinceMidnight(value);
			case DATE_TIME_SECONDS_SINCE_0:
				throw new NotImplementedException();
			case DATE_TIME_SECONDS_SINCE_1960:
				return parseSecondsSinceDate(Epochs.YEAR_1960, value);
			case DATE_TIME_SECONDS_SINCE_1970:
				return parseSecondsSinceDate(Epochs.YEAR_1970, value);
			case DATE_TIME_SECONDS_SINCE_1980:
				return parseSecondsSinceDate(Epochs.YEAR_1980, value);
			default:
				throw new IllegalArgumentException();
		}
	}

	static
	private Integer parseInteger(String value){

		try {
			long result = Long.parseLong(value);

			return parseInteger(value, result);
		} catch(NumberFormatException nfeInteger){

			try {
				double result = Double.parseDouble(value);

				if(DoubleMath.isMathematicalInteger(result)){
					return parseInteger(value, (long)result);
				}
			} catch(NumberFormatException nfeDouble){
				// Ignored
			} // End try

			try {
				return toInteger(parseFlag(value));
			} catch(IllegalArgumentException iae){
				// Ignored
			}

			throw nfeInteger;
		}
	}

	static
	private Integer parseInteger(String value, long parsedValue){

		try {
			return MathUtil.toIntExact(parsedValue);
		} catch(ArithmeticException ae){
			throw new IllegalArgumentException(value, ae);
		}
	}

	static
	private Float parseFloat(String value){

		if(value.length() <= 4){

			switch(value){
				case "-1":
				case "-1.0":
					return Numbers.FLOAT_MINUS_ONE;
				case "0":
				case "0.0":
					return Numbers.FLOAT_ZERO;
				case "1":
				case "1.0":
					return Numbers.FLOAT_ONE;
				default:
					break;
			}

			if(("NaN").equalsIgnoreCase(value)){
				return Float.NaN;
			} else

			if(("-INF").equalsIgnoreCase(value)){
				return Float.NEGATIVE_INFINITY;
			} else

			if(("INF").equalsIgnoreCase(value)){
				return Float.POSITIVE_INFINITY;
			}
		}

		try {
			// -0f + 0f = 0f
			return (Float.parseFloat(value) + 0f);
		} catch(NumberFormatException nfe){

			try {
				return toFloat(parseFlag(value));
			} catch(IllegalArgumentException iae){
				// Ignored
			}

			throw nfe;
		}
	}

	static
	private Double parseDouble(String value){

		if(value.length() <= 4){

			switch(value){
				case "-1":
				case "-1.0":
					return Numbers.DOUBLE_MINUS_ONE;
				case "0":
				case "0.0":
					return Numbers.DOUBLE_ZERO;
				case "0.5":
					return Numbers.DOUBLE_ONE_HALF;
				case "1":
				case "1.0":
					return Numbers.DOUBLE_ONE;
				case "2":
				case "2.0":
					return Numbers.DOUBLE_TWO;
				default:
					break;
			}

			if(("NaN").equalsIgnoreCase(value)){
				return Double.NaN;
			} else

			if(("-INF").equalsIgnoreCase(value)){
				return Double.NEGATIVE_INFINITY;
			} else

			if(("INF").equalsIgnoreCase(value)){
				return Double.POSITIVE_INFINITY;
			}
		}

		try {
			// -0d + 0d = 0d
			return (Double.parseDouble(value) + 0d);
		} catch(NumberFormatException nfe){

			try {
				return toDouble(parseFlag(value));
			} catch(IllegalArgumentException iae){
				// Ignored
			}

			throw nfe;
		}
	}

	static
	private Boolean parseBoolean(String value){

		try {
			return parseFlag(value);
		} catch(IllegalArgumentException iae){

			try {
				return toBoolean(parseDouble(value));
			} catch(NumberFormatException nfe){
				// Ignored
			} catch(TypeCheckException tce){
				// Ignored
			}

			throw iae;
		}
	}

	static
	private boolean parseFlag(String value){

		if("true".equalsIgnoreCase(value)){
			return true;
		} else

		if("false".equalsIgnoreCase(value)){
			return false;
		}

		throw new IllegalArgumentException(value);
	}

	static
	private LocalDate parseDate(String value){

		try {
			return LocalDate.parse(value);
		} catch(DateTimeException dte){
			throw new IllegalArgumentException(value, dte);
		}
	}

	static
	private LocalTime parseTime(String value){

		try {
			return LocalTime.parse(value);
		} catch(DateTimeException dte){
			throw new IllegalArgumentException(value, dte);
		}
	}

	static
	private LocalDateTime parseDateTime(String value){

		try {
			return LocalDateTime.parse(value);
		} catch(DateTimeException dte){
			throw new IllegalArgumentException(value, dte);
		}
	}

	static
	public DaysSinceDate parseDaysSinceDate(LocalDate epoch, String value){
		return new DaysSinceDate(epoch, parseDate(value));
	}

	static
	private SecondsSinceMidnight parseSecondsSinceMidnight(String value){

		try {
			return SecondsSinceMidnight.parse(value);
		} catch(DateTimeException dte){
			throw new IllegalArgumentException(value, dte);
		}
	}

	static
	private SecondsSinceDate parseSecondsSinceDate(LocalDate epoch, String value){
		return new SecondsSinceDate(epoch, parseDateTime(value));
	}

	static
	public boolean equals(DataType dataType, Object value, Object referenceValue){

		try {
			return (parseOrCast(dataType, value)).equals(parseOrCast(dataType, referenceValue));
		} catch(IllegalArgumentException | TypeCheckException e){

			// The String representation of invalid or missing values (eg. "N/A") may not be parseable to the requested representation
			try {
				return (format(value)).equals(format(referenceValue));
			} catch(TypeCheckException tce){
				// Ignored
			}

			throw e;
		}
	}

	/**
	 * @return The data type of the value.
	 */
	static
	public DataType getDataType(Object value){

		if(value instanceof String){
			return DataType.STRING;
		} else

		if(value instanceof Integer){
			return DataType.INTEGER;
		} else

		if(value instanceof Float){
			return DataType.FLOAT;
		} else

		if(value instanceof Double){
			return DataType.DOUBLE;
		} else

		if(value instanceof Boolean){
			return DataType.BOOLEAN;
		} else

		if(value instanceof LocalDate){
			return DataType.DATE;
		} else

		if(value instanceof LocalTime){
			return DataType.TIME;
		} else

		if(value instanceof LocalDateTime){
			return DataType.DATE_TIME;
		} else

		if(value instanceof DaysSinceDate){
			DaysSinceDate period = (DaysSinceDate)value;

			return getDaysDataType(period.getEpoch());
		} else

		if(value instanceof SecondsSinceMidnight){
			return DataType.TIME_SECONDS;
		} else

		if(value instanceof SecondsSinceDate){
			SecondsSinceDate period = (SecondsSinceDate)value;

			return getSecondsDataType(period.getEpoch());
		}

		throw new EvaluationException("No PMML data type for Java data type " + (value != null ? (value.getClass()).getName() : null));
	}

	static
	public DataType getDataType(Collection<?> values){
		DataType result = null;

		for(Object value : values){

			if(value == null){
				continue;
			}

			DataType dataType = getDataType(value);

			if(result == null){
				result = dataType;
			} else

			{
				if(!(result).equals(dataType)){
					throw new TypeCheckException(result, value);
				}
			}
		}

		if(result == null){
			result = DataType.STRING;
		}

		return result;
	}

	/**
	 * @return The least restrictive data type of two numeric data types.
	 *
	 * @see DataType#INTEGER
	 * @see DataType#FLOAT
	 * @see DataType#DOUBLE
	 */
	static
	public DataType getCommonDataType(DataType left, DataType right){

		if((left).equals(right)){

			switch(left){
				case DOUBLE:
				case FLOAT:
				case INTEGER:
					return left;
				default:
					break;
			}
		} else

		if((DataType.DOUBLE).equals(left)){

			if((DataType.FLOAT).equals(right) || (DataType.INTEGER).equals(right)){
				return left;
			}
		} else

		if((DataType.FLOAT).equals(left)){

			if((DataType.DOUBLE).equals(right)){
				return right;
			} else

			if((DataType.INTEGER).equals(right)){
				return left;
			}
		} else

		if((DataType.INTEGER).equals(left)){

			if((DataType.DOUBLE).equals(right) || (DataType.FLOAT).equals(right)){
				return right;
			}
		}

		throw new EvaluationException("No PMML data type for the intersection of PMML data types " + left.value() + " and " + right.value());
	}

	static
	public OpType getOpType(DataType dataType){

		switch(dataType){
			case STRING:
				return OpType.CATEGORICAL;
			case INTEGER:
			case FLOAT:
			case DOUBLE:
				return OpType.CONTINUOUS;
			case BOOLEAN:
				return OpType.CATEGORICAL;
			case DATE:
			case TIME:
			case DATE_TIME:
				return OpType.ORDINAL;
			case DATE_DAYS_SINCE_0:
			case DATE_DAYS_SINCE_1960:
			case DATE_DAYS_SINCE_1970:
			case DATE_DAYS_SINCE_1980:
			case TIME_SECONDS:
			case DATE_TIME_SECONDS_SINCE_0:
			case DATE_TIME_SECONDS_SINCE_1960:
			case DATE_TIME_SECONDS_SINCE_1970:
			case DATE_TIME_SECONDS_SINCE_1980:
				return OpType.CONTINUOUS;
			default:
				throw new IllegalArgumentException();
		}
	}

	/**
	 * @throws TypeCheckException If the value cannot be cast to the requested representation.
	 */
	static
	public Object cast(DataType dataType, Object value){

		switch(dataType){
			case STRING:
				return toString(value);
			case INTEGER:
				return toInteger(value);
			case FLOAT:
				return toFloat(value);
			case DOUBLE:
				return toDouble(value);
			case BOOLEAN:
				return toBoolean(value);
			case DATE:
				return toDate(value);
			case TIME:
				return toTime(value);
			case DATE_TIME:
				return toDateTime(value);
			case DATE_DAYS_SINCE_0:
				throw new NotImplementedException();
			case DATE_DAYS_SINCE_1960:
				return toDaysSinceDate(Epochs.YEAR_1960, value);
			case DATE_DAYS_SINCE_1970:
				return toDaysSinceDate(Epochs.YEAR_1970, value);
			case DATE_DAYS_SINCE_1980:
				return toDaysSinceDate(Epochs.YEAR_1980, value);
			case TIME_SECONDS:
				return toSecondsSinceMidnight(value);
			case DATE_TIME_SECONDS_SINCE_0:
				throw new NotImplementedException();
			case DATE_TIME_SECONDS_SINCE_1960:
				return toSecondsSinceDate(Epochs.YEAR_1960, value);
			case DATE_TIME_SECONDS_SINCE_1970:
				return toSecondsSinceDate(Epochs.YEAR_1970, value);
			case DATE_TIME_SECONDS_SINCE_1980:
				return toSecondsSinceDate(Epochs.YEAR_1980, value);
			default:
				throw new IllegalArgumentException();
		}
	}

	static
	public <V> V cast(Class<? extends V> clazz, Object value){

		if(!clazz.isInstance(value)){
			throw new TypeCheckException(clazz, value);
		}

		return clazz.cast(value);
	}

	/**
	 * <p>
	 * Casts the specified value to String data type.
	 * </p>
	 *
	 * @see DataType#STRING
	 */
	static
	private String toString(Object value){

		if(value instanceof String){
			return (String)value;
		} else

		if((value instanceof Double) || (value instanceof Float) || (value instanceof Long) || (value instanceof Integer) || (value instanceof Short) || (value instanceof Byte)){
			Number number = (Number)value;

			return number.toString();
		} else

		if(value instanceof Boolean){
			Boolean flag = (Boolean)value;

			return (flag.booleanValue() ? "true" : "false");
		}

		throw new TypeCheckException(DataType.STRING, value);
	}

	/**
	 * <p>
	 * Casts the specified value to Integer data type.
	 * </p>
	 *
	 * @see DataType#INTEGER
	 */
	static
	private Integer toInteger(Object value){

		if(value instanceof Integer){
			return (Integer)value;
		} else

		if((value instanceof Double) || (value instanceof Float)){
			Number number = (Number)value;

			if(DoubleMath.isMathematicalInteger(number.doubleValue())){
				return toInteger(number);
			}
		} else

		if(value instanceof Long){
			Long number = (Long)value;

			return toInteger(number);
		} else

		if((value instanceof Short) || (value instanceof Byte)){
			Number number = (Number)value;

			return number.intValue();
		} else

		if(value instanceof Boolean){
			Boolean flag = (Boolean)value;

			return (flag.booleanValue() ? Numbers.INTEGER_ONE : Numbers.INTEGER_ZERO);
		} else

		if((value instanceof DaysSinceDate) || (value instanceof SecondsSinceDate) || (value instanceof SecondsSinceMidnight)){
			Number number = (Number)value;

			return toInteger(number);
		}

		throw new TypeCheckException(DataType.INTEGER, value);
	}

	static
	private Integer toInteger(Number value){

		try {
			return MathUtil.toIntExact(value.longValue());
		} catch(ArithmeticException ae){
			throw new TypeCheckException(DataType.INTEGER, value)
				.initCause(ae);
		}
	}

	/**
	 * <p>
	 * Casts the specified value to Float data type.
	 * </p>
	 *
	 * @see DataType#FLOAT
	 */
	static
	private Float toFloat(Object value){

		if(value instanceof Float){
			return (Float)value;
		} else

		if(value instanceof Double){
			Number number = (Number)value;

			return toFloat(number.floatValue());
		} else

		if((value instanceof Long) || (value instanceof Integer) || (value instanceof Short) || (value instanceof Byte)){
			Number number = (Number)value;

			return toFloat(number.floatValue());
		} else

		if(value instanceof Boolean){
			Boolean flag = (Boolean)value;

			return (flag.booleanValue() ? Numbers.FLOAT_ONE : Numbers.FLOAT_ZERO);
		} else

		if((value instanceof DaysSinceDate) || (value instanceof SecondsSinceDate) || (value instanceof SecondsSinceMidnight)){
			Number number = (Number)value;

			return toFloat(number.floatValue());
		}

		throw new TypeCheckException(DataType.FLOAT, value);
	}

	static
	private Float toFloat(float value){

		if(value == -1f){
			return Numbers.FLOAT_MINUS_ONE;
		} else

		if(value == 0f){
			return Numbers.FLOAT_ZERO;
		} else

		if(value == 1f){
			return Numbers.FLOAT_ONE;
		}

		return value;
	}

	/**
	 * <p>
	 * Casts the specified value to Double data type.
	 * </p>
	 *
	 * @see DataType#DOUBLE
	 */
	static
	private Double toDouble(Object value){

		if(value instanceof Double){
			return (Double)value;
		} else

		if((value instanceof Float) || (value instanceof Long) || (value instanceof Integer) || (value instanceof Short) || (value instanceof Byte)){
			Number number = (Number)value;

			return toDouble(number.doubleValue());
		} else

		if(value instanceof Boolean){
			Boolean flag = (Boolean)value;

			return (flag.booleanValue() ? Numbers.DOUBLE_ONE : Numbers.DOUBLE_ZERO);
		} else

		if((value instanceof DaysSinceDate) || (value instanceof SecondsSinceDate) || (value instanceof SecondsSinceMidnight)){
			Number number = (Number)value;

			return toDouble(number.doubleValue());
		}

		throw new TypeCheckException(DataType.DOUBLE, value);
	}

	static
	private Double toDouble(double value){

		if(value == -1d){
			return Numbers.DOUBLE_MINUS_ONE;
		} else

		if(value == 0d){
			return Numbers.DOUBLE_ZERO;
		} else

		if(value == 0.5d){
			return Numbers.DOUBLE_ONE_HALF;
		} else

		if(value == 1d){
			return Numbers.DOUBLE_ONE;
		} else

		if(value == 2d){
			return Numbers.DOUBLE_TWO;
		}

		return value;
	}

	/**
	 * @see DataType#BOOLEAN
	 */
	static
	private Boolean toBoolean(Object value){

		if(value instanceof Boolean){
			return (Boolean)value;
		} else

		if((value instanceof Double) || (value instanceof Float) || (value instanceof Long) || (value instanceof Integer) || (value instanceof Short) || (value instanceof Byte)){
			Number number = (Number)value;

			if(number.doubleValue() == 0d){
				return Boolean.FALSE;
			} else

			if(number.doubleValue() == 1d){
				return Boolean.TRUE;
			}
		}

		throw new TypeCheckException(DataType.BOOLEAN, value);
	}

	/**
	 * @see DataType#DATE
	 */
	static
	private LocalDate toDate(Object value){

		if(value instanceof LocalDate){
			return (LocalDate)value;
		} else

		if(value instanceof LocalDateTime){
			LocalDateTime instant = (LocalDateTime)value;

			return instant.toLocalDate();
		}

		throw new TypeCheckException(DataType.DATE, value);
	}

	/**
	 * @see DataType#TIME
	 */
	static
	private LocalTime toTime(Object value){

		if(value instanceof LocalTime){
			return (LocalTime)value;
		} else

		if(value instanceof LocalDateTime){
			LocalDateTime instant = (LocalDateTime)value;

			return instant.toLocalTime();
		}

		throw new TypeCheckException(DataType.TIME, value);
	}

	/**
	 * @see DataType#DATE_TIME
	 */
	static
	private LocalDateTime toDateTime(Object value){

		if(value instanceof LocalDateTime){
			return (LocalDateTime)value;
		}

		throw new TypeCheckException(DataType.DATE_TIME, value);
	}

	/**
	 * @see DataType#DATE_DAYS_SINCE_1960
	 * @see DataType#DATE_DAYS_SINCE_1970
	 * @see DataType#DATE_DAYS_SINCE_1980
	 */
	static
	private DaysSinceDate toDaysSinceDate(LocalDate epoch, Object value){

		if(value instanceof DaysSinceDate){
			DaysSinceDate period = (DaysSinceDate)value;

			if((epoch).equals(period.getEpoch())){
				return period;
			}

			long days = ChronoUnit.DAYS.between(epoch, period.getEpoch()) + period.getDays();

			return new DaysSinceDate(epoch, days);
		}

		throw new TypeCheckException(getDaysDataType(epoch), value);
	}

	/**
	 * @see DataType#TIME_SECONDS
	 */
	static
	private SecondsSinceMidnight toSecondsSinceMidnight(Object value){

		if(value instanceof SecondsSinceMidnight){
			return (SecondsSinceMidnight)value;
		}

		throw new TypeCheckException(DataType.TIME_SECONDS, value);
	}

	/**
	 * @see DataType#DATE_TIME_SECONDS_SINCE_1960
	 * @see DataType#DATE_TIME_SECONDS_SINCE_1970
	 * @see DataType#DATE_TIME_SECONDS_SINCE_1980
	 */
	static
	private SecondsSinceDate toSecondsSinceDate(LocalDate epoch, Object value){

		if(value instanceof SecondsSinceDate){
			SecondsSinceDate period = (SecondsSinceDate)value;

			if((epoch).equals(period.getEpoch())){
				return period;
			}

			long seconds = ChronoUnit.SECONDS.between(epoch.atStartOfDay(), (period.getEpoch()).atStartOfDay()) + period.getSeconds();

			return new SecondsSinceDate(epoch, seconds);
		}

		throw new TypeCheckException(getSecondsDataType(epoch), value);
	}

	static
	public DataType getConstantDataType(Object value){

		if(value instanceof String){
			String string = (String)value;

			return getConstantDataType(string);
		}

		return TypeUtil.getDataType(value);
	}

	static
	public DataType getConstantDataType(String value){

		if(("").equals(value)){
			return DataType.STRING;
		} else

		if(("NaN").equalsIgnoreCase(value) || ("INF").equalsIgnoreCase(value) || ("-INF").equalsIgnoreCase(value)){
			return DataType.DOUBLE;
		}

		try {
			if(value.indexOf('.') > -1){
				Double.parseDouble(value);

				return DataType.DOUBLE;
			} else

			{
				Long.parseLong(value);

				return DataType.INTEGER;
			}
		} catch(NumberFormatException nfe){
			return DataType.STRING;
		}
	}

	static
	private DataType getDaysDataType(LocalDate epoch){

		if((Epochs.YEAR_1960).equals(epoch)){
			return DataType.DATE_DAYS_SINCE_1960;
		} else

		if((Epochs.YEAR_1970).equals(epoch)){
			return DataType.DATE_DAYS_SINCE_1970;
		} else

		if((Epochs.YEAR_1980).equals(epoch)){
			return DataType.DATE_DAYS_SINCE_1980;
		}

		throw new EvaluationException("Non-standard epoch " + epoch);
	}

	static
	private DataType getSecondsDataType(LocalDate epoch){

		if((Epochs.YEAR_1960).equals(epoch)){
			return DataType.DATE_TIME_SECONDS_SINCE_1960;
		} else

		if((Epochs.YEAR_1970).equals(epoch)){
			return DataType.DATE_TIME_SECONDS_SINCE_1970;
		} else

		if((Epochs.YEAR_1980).equals(epoch)){
			return DataType.DATE_TIME_SECONDS_SINCE_1980;
		}

		throw new EvaluationException("Non-standard epoch " + epoch);
	}
}