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

import com.google.common.math.DoubleMath;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
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
		return (cast(dataType, left)).equals(cast(dataType, right));
	}

	@SuppressWarnings (
		value = {"rawtypes", "unchecked"}
	)
	static
	public int compare(DataType dataType, Object left, Object right){
		return ((Comparable)cast(dataType, left)).compareTo(cast(dataType, right));
	}

	static
	public Object parseOrCast(DataType dataType, Object value){

		if(value instanceof String){
			String string = (String)value;

			return parse(dataType, string);
		}

		return cast(dataType, value);
	}

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
				break;
		}

		throw new EvaluationException();
	}

	static
	private Integer parseInteger(String value){

		try {
			return Integer.valueOf(value);
		} catch(NumberFormatException nfeInteger){

			try {
				Double result = parseDouble(value);

				if(DoubleMath.isMathematicalInteger(result)){
					return result.intValue();
				}
			} catch(NumberFormatException nfeDouble){
				// Ignored
			}

			throw nfeInteger;
		}
	}

	static
	private Float parseFloat(String value){
		return Float.valueOf(value);
	}

	static
	private Double parseDouble(String value){
		return Double.valueOf(value);
	}

	static
	private Boolean parseBoolean(String value){
		return Boolean.valueOf(value);
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

		throw new EvaluationException();
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

			LocalDate epoch = period.getEpoch();

			if((epoch).equals(YEAR_1960)){
				return DataType.DATE_DAYS_SINCE_1960;
			} else

			if((epoch).equals(YEAR_1970)){
				return DataType.DATE_DAYS_SINCE_1970;
			} else

			if((epoch).equals(YEAR_1980)){
				return DataType.DATE_DAYS_SINCE_1980;
			}

			throw new EvaluationException();
		} else

		if(value instanceof SecondsSinceMidnight){
			return DataType.TIME_SECONDS;
		} else

		if(value instanceof SecondsSinceDate){
			SecondsSinceDate period = (SecondsSinceDate)value;

			LocalDate epoch = period.getEpoch();

			if((epoch).equals(YEAR_1960)){
				return DataType.DATE_TIME_SECONDS_SINCE_1960;
			} else

			if((epoch).equals(YEAR_1970)){
				return DataType.DATE_TIME_SECONDS_SINCE_1970;
			} else

			if((epoch).equals(YEAR_1980)){
				return DataType.DATE_TIME_SECONDS_SINCE_1980;
			}

			throw new EvaluationException();
		}

		throw new EvaluationException();
	}

	/**
	 * @return The least restrictive data type of the data types of two values
	 */
	static
	public DataType getResultDataType(DataType left, DataType right){

		if((left).equals(right)){
			return left;
		}

		// "When the input parameters have multiple dataTypes, the least restrictive dataType will be inherited by default."
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
			case DATE_TIME_SECONDS_SINCE_0:
			case DATE_TIME_SECONDS_SINCE_1960:
			case DATE_TIME_SECONDS_SINCE_1970:
			case DATE_TIME_SECONDS_SINCE_1980:
				return OpType.ORDINAL;
			default:
				break;
		}

		throw new EvaluationException();
	}

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
				break;
		}

		throw new EvaluationException();
	}

	/**
	 * Casts the specified value to String data type.
	 *
	 * @see DataType#STRING
	 */
	static
	private String toString(Object value){

		if(value instanceof String){
			return (String)value;
		} else

		if((value instanceof Double) || (value instanceof Float) || (value instanceof Integer)){
			Number number = (Number)value;

			return number.toString();
		}

		throw new TypeCheckException(DataType.STRING, value);
	}

	/**
	 * Casts the specified value to Integer data type.
	 *
	 * @see DataType#INTEGER
	 */
	static
	private Integer toInteger(Object value){

		if(value instanceof Integer){
			return (Integer)value;
		}

		throw new TypeCheckException(DataType.INTEGER, value);
	}

	/**
	 * Casts the specified value to Float data type.
	 *
	 * @see DataType#FLOAT
	 */
	static
	private Float toFloat(Object value){

		if(value instanceof Float){
			return (Float)value;
		} else

		if(value instanceof Integer){
			Number number = (Number)value;

			return Float.valueOf(number.floatValue());
		}

		throw new TypeCheckException(DataType.FLOAT, value);
	}

	/**
	 * Casts the specified value to Double data type.
	 *
	 * @see DataType#DOUBLE
	 */
	static
	private Double toDouble(Object value){

		if(value instanceof Double){
			return (Double)value;
		} else

		if((value instanceof Float) || (value instanceof Integer)){
			Number number = (Number)value;

			return Double.valueOf(number.doubleValue());
		}

		throw new TypeCheckException(DataType.DOUBLE, value);
	}

	/**
	 * @see DataType#BOOLEAN
	 */
	static
	private Boolean toBoolean(Object value){

		if(value instanceof Boolean){
			return (Boolean)value;
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
		}

		throw new TypeCheckException(DataType.DATE_DAYS_SINCE_1970, value);
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
		}

		throw new TypeCheckException(DataType.DATE_TIME_SECONDS_SINCE_1970, value);
	}

	static
	public DataType getConstantDataType(String string){

		try {
			if(string.indexOf('.') > -1){
				Float.parseFloat(string);

				return DataType.FLOAT;
			} else

			{
				Integer.parseInt(string);

				return DataType.INTEGER;
			}
		} catch(NumberFormatException nfe){
			return DataType.STRING;
		}
	}

	private static final DataType[] inheritanceSequence = {DataType.STRING, DataType.DOUBLE, DataType.FLOAT, DataType.INTEGER};

	private static final LocalDate YEAR_1960 = new LocalDate(1960, 1, 1);
	private static final LocalDate YEAR_1970 = new LocalDate(1970, 1, 1);
	private static final LocalDate YEAR_1980 = new LocalDate(1980, 1, 1);

}