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

import java.util.Collection;

import com.google.common.math.DoubleMath;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Seconds;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.DateTimeParserBucket;

public class TypeUtil {

	private TypeUtil(){
	}

	static
	public boolean equals(DataType dataType, Object left, Object right){
		left = cast(dataType, left);
		right = cast(dataType, right);

		return (left).equals(right);
	}

	@SuppressWarnings (
		value = {"rawtypes", "unchecked"}
	)
	static
	public int compare(DataType dataType, Object left, Object right){
		left = cast(dataType, left);
		right = cast(dataType, right);

		return ((Comparable)left).compareTo(right);
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
			case DATE_DAYS_SINCE_1960:
				return new DaysSinceDate(YEAR_1960, parseDate(value));
			case DATE_DAYS_SINCE_1970:
				return new DaysSinceDate(YEAR_1970, parseDate(value));
			case DATE_DAYS_SINCE_1980:
				return new DaysSinceDate(YEAR_1980, parseDate(value));
			case TIME_SECONDS:
				return new SecondsSinceMidnight(parseSeconds(value));
			case DATE_TIME_SECONDS_SINCE_1960:
				return new SecondsSinceDate(YEAR_1960, parseDateTime(value));
			case DATE_TIME_SECONDS_SINCE_1970:
				return new SecondsSinceDate(YEAR_1970, parseDateTime(value));
			case DATE_TIME_SECONDS_SINCE_1980:
				return new SecondsSinceDate(YEAR_1980, parseDateTime(value));
			default:
				throw new UnsupportedFeatureException();
		}
	}

	static
	private Integer parseInteger(String value){

		try {
			long result = Long.parseLong(value);

			return toInteger(result);
		} catch(NumberFormatException nfeInteger){

			try {
				double result = Double.parseDouble(value);

				if(DoubleMath.isMathematicalInteger(result)){
					return toInteger((long)result);
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
		return LocalDate.parse(value);
	}

	static
	private LocalTime parseTime(String value){
		return LocalTime.parse(value);
	}

	static
	private LocalDateTime parseDateTime(String value){
		return LocalDateTime.parse(value);
	}

	@SuppressWarnings (
		value = {"deprecation"}
	)
	static
	private Seconds parseSeconds(String value){
		DateTimeFormatter format = SecondsSinceMidnight.getFormat();

		DateTimeParser parser = format.getParser();

		DateTimeParserBucket bucket = new DateTimeParserBucket(0, null, null);
		bucket.setZone(null);

		int result = parser.parseInto(bucket, value, 0);
		if(result >= 0 && result >= value.length()){
			long millis = bucket.computeMillis(true);

			return Seconds.seconds((int)(millis / 1000L));
		}

		throw new IllegalArgumentException(value);
	}

	static
	public String format(Object value){

		if(value instanceof String){
			return (String)value;
		} // End if

		if(value != null){
			return String.valueOf(value);
		}

		throw new EvaluationException();
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

		throw new EvaluationException();
	}

	static
	public DataType getDataType(Collection<?> values){
		DataType result = null;

		for(Object value : values){
			DataType dataType = getDataType(value);

			if(result == null){
				result = dataType;
			} else

			{
				if(!(result).equals(dataType)){
					throw new EvaluationException();
				}
			}
		}

		if(result == null){
			throw new EvaluationException();
		}

		return result;
	}

	/**
	 * @return The least restrictive data type of the data types of two values.
	 */
	static
	public DataType getResultDataType(DataType left, DataType right){

		if((left).equals(right)){
			return left;
		}

		// "When the input parameters have multiple dataTypes, the least restrictive dataType will be inherited by default"
		for(int i = 0; i < inheritanceSequence.length; i++){
			DataType dataType = inheritanceSequence[i];

			if((dataType).equals(left) || (dataType).equals(right)){
				return dataType;
			}
		}

		throw new EvaluationException();
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
			case DATE_DAYS_SINCE_0:
			case DATE_DAYS_SINCE_1960:
			case DATE_DAYS_SINCE_1970:
			case DATE_DAYS_SINCE_1980:
			case TIME_SECONDS:
			case DATE_TIME_SECONDS_SINCE_0:
			case DATE_TIME_SECONDS_SINCE_1960:
			case DATE_TIME_SECONDS_SINCE_1970:
			case DATE_TIME_SECONDS_SINCE_1980:
				return OpType.ORDINAL;
			default:
				throw new UnsupportedFeatureException();
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
			case DATE_DAYS_SINCE_1960:
				return toDaysSinceDate(value, YEAR_1960);
			case DATE_DAYS_SINCE_1970:
				return toDaysSinceDate(value, YEAR_1970);
			case DATE_DAYS_SINCE_1980:
				return toDaysSinceDate(value, YEAR_1980);
			case TIME_SECONDS:
				return toSecondsSinceMidnight(value);
			case DATE_TIME_SECONDS_SINCE_1960:
				return toSecondsSinceDate(value, YEAR_1960);
			case DATE_TIME_SECONDS_SINCE_1970:
				return toSecondsSinceDate(value, YEAR_1970);
			case DATE_TIME_SECONDS_SINCE_1980:
				return toSecondsSinceDate(value, YEAR_1980);
			default:
				throw new UnsupportedFeatureException();
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
				return toInteger(number.longValue());
			}
		} else

		if(value instanceof Long){
			Long number = (Long)value;

			return toInteger(number.longValue());
		} else

		if((value instanceof Short) || (value instanceof Byte)){
			Number number = (Number)value;

			return Integer.valueOf(number.intValue());
		} else

		if(value instanceof Boolean){
			Boolean flag = (Boolean)value;

			return (flag.booleanValue() ? Numbers.INTEGER_ONE : Numbers.INTEGER_ZERO);
		} else

		if((value instanceof DaysSinceDate) || (value instanceof SecondsSinceDate) || (value instanceof SecondsSinceMidnight)){
			Number number = (Number)value;

			return Integer.valueOf(number.intValue());
		}

		throw new TypeCheckException(DataType.INTEGER, value);
	}

	static
	private Integer toInteger(long value){

		if(value < Integer.MIN_VALUE || value > Integer.MAX_VALUE){
			throw new EvaluationException();
		}

		return Integer.valueOf((int)value);
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

			if(number.doubleValue() == 1d){
				return Boolean.TRUE;
			} else

			if(number.doubleValue() == 0d){
				return Boolean.FALSE;
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
	private DaysSinceDate toDaysSinceDate(Object value, LocalDate epoch){

		if(value instanceof DaysSinceDate){
			DaysSinceDate period = (DaysSinceDate)value;

			if((period.getEpoch()).equals(epoch)){
				return period;
			}

			Days difference = Days.daysBetween(epoch, period.getEpoch()).plus(period.getDays());

			return new DaysSinceDate(epoch, difference);
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
	private SecondsSinceDate toSecondsSinceDate(Object value, LocalDate epoch){

		if(value instanceof SecondsSinceDate){
			SecondsSinceDate period = (SecondsSinceDate)value;

			if((period.getEpoch()).equals(epoch)){
				return period;
			}

			Seconds difference = Seconds.secondsBetween(toMidnight(epoch), toMidnight(period.getEpoch())).plus(period.getSeconds());

			return new SecondsSinceDate(epoch, difference);
		}

		throw new TypeCheckException(getSecondsDataType(epoch), value);
	}

	static
	public DataType getConstantDataType(String string){

		try {
			if(string.indexOf('.') > -1){
				Double.parseDouble(string);

				return DataType.FLOAT;
			} else

			{
				Long.parseLong(string);

				return DataType.INTEGER;
			}
		} catch(NumberFormatException nfe){
			return DataType.STRING;
		}
	}

	static
	private DataType getDaysDataType(LocalDate epoch){

		if((YEAR_1960).equals(epoch)){
			return DataType.DATE_DAYS_SINCE_1960;
		} else

		if((YEAR_1970).equals(epoch)){
			return DataType.DATE_DAYS_SINCE_1970;
		} else

		if((YEAR_1980).equals(epoch)){
			return DataType.DATE_DAYS_SINCE_1980;
		}

		throw new EvaluationException();
	}

	static
	private DataType getSecondsDataType(LocalDate epoch){

		if((YEAR_1960).equals(epoch)){
			return DataType.DATE_TIME_SECONDS_SINCE_1960;
		} else

		if((YEAR_1970).equals(epoch)){
			return DataType.DATE_TIME_SECONDS_SINCE_1970;
		} else

		if((YEAR_1980).equals(epoch)){
			return DataType.DATE_TIME_SECONDS_SINCE_1980;
		}

		throw new EvaluationException();
	}

	static
	LocalDateTime toMidnight(LocalDate date){
		return new LocalDateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 0, 0, 0);
	}

	private static final DataType[] inheritanceSequence = {DataType.STRING, DataType.DOUBLE, DataType.FLOAT, DataType.INTEGER};

	private static final LocalDate YEAR_1960 = new LocalDate(1960, 1, 1);
	private static final LocalDate YEAR_1970 = new LocalDate(1970, 1, 1);
	private static final LocalDate YEAR_1980 = new LocalDate(1980, 1, 1);
}