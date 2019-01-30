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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TypeUtilTest {

	@Test
	public void parseInteger(){
		assertEquals(0, TypeUtil.parse(DataType.INTEGER, "-0"));
		assertEquals(0, TypeUtil.parse(DataType.INTEGER, "0"));

		assertEquals(1, TypeUtil.parse(DataType.INTEGER, "1"));
		assertEquals(1, TypeUtil.parse(DataType.INTEGER, "1.0"));
		assertEquals(1, TypeUtil.parse(DataType.INTEGER, "1e+0"));
		assertEquals(1, TypeUtil.parse(DataType.INTEGER, "1.0e+0"));

		assertEquals(0, TypeUtil.parse(DataType.INTEGER, "false"));
		assertEquals(1, TypeUtil.parse(DataType.INTEGER, "true"));

		assertEquals(Integer.MIN_VALUE, TypeUtil.parse(DataType.INTEGER, Integer.toString(Integer.MIN_VALUE)));


		assertEquals(Integer.MIN_VALUE - 1L, TypeUtil.parse(DataType.INTEGER, Long.toString(Integer.MIN_VALUE - 1L)));
		assertEquals(Integer.MIN_VALUE - 1L, TypeUtil.parse(DataType.INTEGER, Double.toString(Integer.MIN_VALUE - 1L)));
		assertEquals(Integer.MAX_VALUE + 1L, TypeUtil.parse(DataType.INTEGER, Long.toString(Integer.MAX_VALUE + 1L)));
		assertEquals(Integer.MAX_VALUE + 1L, TypeUtil.parse(DataType.INTEGER, Double.toString(Integer.MAX_VALUE + 1L)));

		assertEquals(Integer.MAX_VALUE, TypeUtil.parse(DataType.INTEGER, Integer.toString(Integer.MAX_VALUE)));
	}

	@Test
	public void parseBoolean(){
		assertEquals(Boolean.TRUE, TypeUtil.parse(DataType.BOOLEAN, "true"));
		assertEquals(Boolean.TRUE, TypeUtil.parse(DataType.BOOLEAN, "TRUE"));

		assertEquals(Boolean.FALSE, TypeUtil.parse(DataType.BOOLEAN, "false"));
		assertEquals(Boolean.FALSE, TypeUtil.parse(DataType.BOOLEAN, "FALSE"));

		try {
			TypeUtil.parse(DataType.BOOLEAN, "yes");

			fail();
		} catch(IllegalArgumentException iae){
			// Ignored
		}

		assertEquals(Boolean.TRUE, TypeUtil.parse(DataType.BOOLEAN, "1"));
		assertEquals(Boolean.TRUE, TypeUtil.parse(DataType.BOOLEAN, "1.0"));

		try {
			TypeUtil.parse(DataType.BOOLEAN, "0.5");

			fail();
		} catch(IllegalArgumentException iae){
			// Ignored
		}

		assertEquals(Boolean.FALSE, TypeUtil.parse(DataType.BOOLEAN, "-0"));
		assertEquals(Boolean.FALSE, TypeUtil.parse(DataType.BOOLEAN, "-0.0"));
		assertEquals(Boolean.FALSE, TypeUtil.parse(DataType.BOOLEAN, "0"));
		assertEquals(Boolean.FALSE, TypeUtil.parse(DataType.BOOLEAN, "0.0"));
	}

	@Test
	public void cast(){
		assertEquals("1", TypeUtil.cast(DataType.STRING, "1"));

		assertEquals("1", TypeUtil.cast(DataType.STRING, (byte)1));
		assertEquals("1", TypeUtil.cast(DataType.STRING, (short)1));
		assertEquals("1", TypeUtil.cast(DataType.STRING, 1));
		assertEquals("1", TypeUtil.cast(DataType.STRING, 1l));
		assertEquals("1.0", TypeUtil.cast(DataType.STRING, 1f)); // XXX
		assertEquals("1.0", TypeUtil.cast(DataType.STRING, 1.0f));
		assertEquals("1.0", TypeUtil.cast(DataType.STRING, 1d)); // XXX
		assertEquals("1.0", TypeUtil.cast(DataType.STRING, 1.0d));

		assertEquals(1, TypeUtil.cast(DataType.INTEGER, (byte)1));
		assertEquals(1, TypeUtil.cast(DataType.INTEGER, (short)1));
		assertEquals(1, TypeUtil.cast(DataType.INTEGER, 1));
		assertEquals(1, TypeUtil.cast(DataType.INTEGER, 1l));
		assertEquals(1, TypeUtil.cast(DataType.INTEGER, true));

		assertEquals(1f, TypeUtil.cast(DataType.FLOAT, (byte)1));
		assertEquals(1f, TypeUtil.cast(DataType.FLOAT, (short)1));
		assertEquals(1f, TypeUtil.cast(DataType.FLOAT, 1));
		assertEquals(1f, TypeUtil.cast(DataType.FLOAT, 1l));
		assertEquals(1f, TypeUtil.cast(DataType.FLOAT, 1f));
		assertEquals(1f, TypeUtil.cast(DataType.FLOAT, true));

		assertEquals(1d, TypeUtil.cast(DataType.DOUBLE, (byte)1));
		assertEquals(1d, TypeUtil.cast(DataType.DOUBLE, (short)1));
		assertEquals(1d, TypeUtil.cast(DataType.DOUBLE, 1));
		assertEquals(1d, TypeUtil.cast(DataType.DOUBLE, 1l));
		assertEquals(1d, TypeUtil.cast(DataType.DOUBLE, 1f));
		assertEquals(1d, TypeUtil.cast(DataType.DOUBLE, 1d));
		assertEquals(1d, TypeUtil.cast(DataType.DOUBLE, true));
	}

	@Test
	public void castInteger(){

		assertEquals(-2147483649L, TypeUtil.cast(DataType.INTEGER, Integer.MIN_VALUE - 1L));
		assertEquals(2147483648L, TypeUtil.cast(DataType.INTEGER, Integer.MAX_VALUE + 1L));

		assertEquals(1, TypeUtil.cast(DataType.INTEGER, 1f));
		assertEquals(1, TypeUtil.cast(DataType.INTEGER, 1.0f));

		try {
			TypeUtil.cast(DataType.INTEGER, Math.nextUp(1f));

			fail();
		} catch(TypeCheckException tce){
			// Ignored
		}

		assertEquals(1, TypeUtil.cast(DataType.INTEGER, 1d));
		assertEquals(1, TypeUtil.cast(DataType.INTEGER, 1.0d));

		try {
			TypeUtil.cast(DataType.INTEGER, Math.nextUp(1d));

			fail();
		} catch(TypeCheckException tce){
			// Ignored
		}

		assertEquals(-2147483649L, TypeUtil.cast(DataType.INTEGER, Integer.MIN_VALUE - 1D));
		assertEquals(2147483648L, TypeUtil.cast(DataType.INTEGER, Integer.MAX_VALUE + 1D));
	}

	@Test
	public void castBoolean(){
		assertEquals(Boolean.TRUE, TypeUtil.cast(DataType.BOOLEAN, (byte)1));
		assertEquals(Boolean.TRUE, TypeUtil.cast(DataType.BOOLEAN, (short)1));
		assertEquals(Boolean.TRUE, TypeUtil.cast(DataType.BOOLEAN, 1));
		assertEquals(Boolean.TRUE, TypeUtil.cast(DataType.BOOLEAN, 1l));
		assertEquals(Boolean.TRUE, TypeUtil.cast(DataType.BOOLEAN, 1f));
		assertEquals(Boolean.TRUE, TypeUtil.cast(DataType.BOOLEAN, 1.0f));
		assertEquals(Boolean.TRUE, TypeUtil.cast(DataType.BOOLEAN, 1d));
		assertEquals(Boolean.TRUE, TypeUtil.cast(DataType.BOOLEAN, 1.0d));

		try {
			TypeUtil.cast(DataType.BOOLEAN, Math.nextUp(1f));

			fail();
		} catch(TypeCheckException tce){
			// Ignored
		}

		try {
			TypeUtil.cast(DataType.BOOLEAN, Math.nextUp(1d));

			fail();
		} catch(TypeCheckException tce){
			// Ignored
		}
	}

	@Test
	public void parseDate(){
		LocalDate date = (LocalDate)TypeUtil.parse(DataType.DATE, DATE);

		assertEquals(DataType.DATE, TypeUtil.getDataType(date));
	}

	@Test
	public void parseTime(){
		LocalTime time = (LocalTime)TypeUtil.parse(DataType.TIME, TIME);

		assertEquals(DataType.TIME, TypeUtil.getDataType(time));
	}

	@Test
	public void parseDateTime(){
		LocalDateTime dateTime = (LocalDateTime)TypeUtil.parse(DataType.DATE_TIME, DATE_TIME);

		assertEquals(DataType.DATE_TIME, TypeUtil.getDataType(dateTime));
	}

	@Test
	public void parseDaysSinceDate(){
		DaysSinceDate sixties = (DaysSinceDate)TypeUtil.parse(DataType.DATE_DAYS_SINCE_1960, DATE);
		DaysSinceDate seventies = (DaysSinceDate)TypeUtil.parse(DataType.DATE_DAYS_SINCE_1970, DATE);
		DaysSinceDate eighties = (DaysSinceDate)TypeUtil.parse(DataType.DATE_DAYS_SINCE_1980, DATE);

		assertEquals(DataType.DATE_DAYS_SINCE_1960, TypeUtil.getDataType(sixties));
		assertEquals(DataType.DATE_DAYS_SINCE_1970, TypeUtil.getDataType(seventies));
		assertEquals(DataType.DATE_DAYS_SINCE_1980, TypeUtil.getDataType(eighties));

		assertEquals(10, ChronoUnit.YEARS.between(sixties.getEpoch(), seventies.getEpoch()));
		assertEquals(10, ChronoUnit.YEARS.between(seventies.getEpoch(), eighties.getEpoch()));

		try {
			int diff = (sixties).compareTo(seventies);

			fail();
		} catch(ClassCastException cce){
			// Ignored
		}

		assertEquals(sixties, TypeUtil.cast(DataType.DATE_DAYS_SINCE_1960, seventies));
		assertEquals(sixties, TypeUtil.cast(DataType.DATE_DAYS_SINCE_1960, eighties));
		assertEquals(seventies, TypeUtil.cast(DataType.DATE_DAYS_SINCE_1970, sixties));
		assertEquals(seventies, TypeUtil.cast(DataType.DATE_DAYS_SINCE_1970, eighties));
		assertEquals(eighties, TypeUtil.cast(DataType.DATE_DAYS_SINCE_1980, sixties));
		assertEquals(eighties, TypeUtil.cast(DataType.DATE_DAYS_SINCE_1980, seventies));

		assertEquals(0L, countDaysSince1960("1960-01-01"));
		assertEquals(1L, countDaysSince1960("1960-01-02"));
		assertEquals(31L, countDaysSince1960("1960-02-01"));

		assertEquals(-1L, countDaysSince1960("1959-12-31"));

		assertEquals(15796L, countDaysSince1960("2003-04-01"));
	}

	@Test
	public void parseSecondsSinceMidnight(){
		SecondsSinceMidnight noon = (SecondsSinceMidnight)TypeUtil.parse(DataType.TIME_SECONDS, "12:00:00");

		assertEquals(DataType.TIME_SECONDS, TypeUtil.getDataType(noon));

		assertEquals(0L, countSecondsSinceMidnight("0:00:00"));
		assertEquals(100L, countSecondsSinceMidnight("0:01:40"));
		assertEquals(200L, countSecondsSinceMidnight("0:03:20"));
		assertEquals(1000L, countSecondsSinceMidnight("0:16:40"));
		assertEquals(86400L, countSecondsSinceMidnight("24:00:00"));
		assertEquals(86401L, countSecondsSinceMidnight("24:00:01"));
		assertEquals(100000L, countSecondsSinceMidnight("27:46:40"));

		assertEquals(19410L, countSecondsSinceMidnight("05:23:30"));

		assertEquals(-10L, countSecondsSinceMidnight("-0:00:10"));
		assertEquals(-100L, countSecondsSinceMidnight("-0:01:40"));
		assertEquals(-1000L, countSecondsSinceMidnight("-0:16:40"));
		assertEquals(-10000L, countSecondsSinceMidnight("-2:46:40"));
		assertEquals(-100000L, countSecondsSinceMidnight("-27:46:40"));
	}

	@Test
	public void parseSecondsSinceDate(){
		SecondsSinceDate sixties = (SecondsSinceDate)TypeUtil.parse(DataType.DATE_TIME_SECONDS_SINCE_1960, DATE_TIME);
		SecondsSinceDate seventies = (SecondsSinceDate)TypeUtil.parse(DataType.DATE_TIME_SECONDS_SINCE_1970, DATE_TIME);
		SecondsSinceDate eighties = (SecondsSinceDate)TypeUtil.parse(DataType.DATE_TIME_SECONDS_SINCE_1980, DATE_TIME);

		assertEquals(DataType.DATE_TIME_SECONDS_SINCE_1960, TypeUtil.getDataType(sixties));
		assertEquals(DataType.DATE_TIME_SECONDS_SINCE_1970, TypeUtil.getDataType(seventies));
		assertEquals(DataType.DATE_TIME_SECONDS_SINCE_1980, TypeUtil.getDataType(eighties));

		assertEquals(10, ChronoUnit.YEARS.between(sixties.getEpoch(), seventies.getEpoch()));
		assertEquals(10, ChronoUnit.YEARS.between(seventies.getEpoch(), eighties.getEpoch()));

		try {
			int diff = (sixties).compareTo(seventies);

			fail();
		} catch(ClassCastException cce){
			// Ignored
		}

		assertEquals(sixties, TypeUtil.cast(DataType.DATE_TIME_SECONDS_SINCE_1960, seventies));
		assertEquals(sixties, TypeUtil.cast(DataType.DATE_TIME_SECONDS_SINCE_1960, eighties));
		assertEquals(seventies, TypeUtil.cast(DataType.DATE_TIME_SECONDS_SINCE_1970, sixties));
		assertEquals(seventies, TypeUtil.cast(DataType.DATE_TIME_SECONDS_SINCE_1970, eighties));
		assertEquals(eighties, TypeUtil.cast(DataType.DATE_TIME_SECONDS_SINCE_1980, sixties));
		assertEquals(eighties, TypeUtil.cast(DataType.DATE_TIME_SECONDS_SINCE_1980, seventies));

		assertEquals(0L, countSecondsSince1960("1960-01-01T00:00:00"));
		assertEquals(1L, countSecondsSince1960("1960-01-01T00:00:01"));
		assertEquals(60L, countSecondsSince1960("1960-01-01T00:01:00"));

		assertEquals(-1L, countSecondsSince1960("1959-12-31T23:59:59"));

		assertEquals(185403L, countSecondsSince1960("1960-01-03T03:30:03"));
	}

	@Test
	public void getCommonDataType(){
		assertEquals(DataType.DOUBLE, TypeUtil.getCommonDataType(DataType.DOUBLE, DataType.DOUBLE));
		assertEquals(DataType.DOUBLE, TypeUtil.getCommonDataType(DataType.DOUBLE, DataType.FLOAT));
		assertEquals(DataType.DOUBLE, TypeUtil.getCommonDataType(DataType.DOUBLE, DataType.INTEGER));

		try {
			TypeUtil.getCommonDataType(DataType.DOUBLE, DataType.BOOLEAN);

			fail();
		} catch(EvaluationException ee){
			// Ignored
		}

		assertEquals(DataType.DOUBLE, TypeUtil.getCommonDataType(DataType.FLOAT, DataType.DOUBLE));
		assertEquals(DataType.FLOAT, TypeUtil.getCommonDataType(DataType.FLOAT, DataType.FLOAT));
		assertEquals(DataType.FLOAT, TypeUtil.getCommonDataType(DataType.FLOAT, DataType.INTEGER));

		assertEquals(DataType.DOUBLE, TypeUtil.getCommonDataType(DataType.INTEGER, DataType.DOUBLE));
		assertEquals(DataType.FLOAT, TypeUtil.getCommonDataType(DataType.INTEGER, DataType.FLOAT));
		assertEquals(DataType.INTEGER, TypeUtil.getCommonDataType(DataType.INTEGER, DataType.INTEGER));
	}

	@Test
	public void getConstantDataType(){
		assertEquals(DataType.STRING, TypeUtil.getConstantDataType(""));

		assertEquals(DataType.INTEGER, TypeUtil.getConstantDataType("-1"));
		assertEquals(DataType.INTEGER, TypeUtil.getConstantDataType("1"));
		assertEquals(DataType.INTEGER, TypeUtil.getConstantDataType("+1"));
		assertEquals(DataType.STRING, TypeUtil.getConstantDataType("1E0"));
		assertEquals(DataType.STRING, TypeUtil.getConstantDataType("1X"));

		assertEquals(DataType.FLOAT, TypeUtil.getConstantDataType("-1.0"));
		assertEquals(DataType.FLOAT, TypeUtil.getConstantDataType("1.0"));
		assertEquals(DataType.FLOAT, TypeUtil.getConstantDataType("+1.0"));
		assertEquals(DataType.FLOAT, TypeUtil.getConstantDataType("1.0E-1"));
		assertEquals(DataType.FLOAT, TypeUtil.getConstantDataType("1.0E1"));
		assertEquals(DataType.FLOAT, TypeUtil.getConstantDataType("1.0E+1"));
		assertEquals(DataType.STRING, TypeUtil.getConstantDataType("1.0X"));
	}

	@Test
	public void getOpType(){
		assertEquals(OpType.ORDINAL, TypeUtil.getOpType(DataType.DATE));
		assertEquals(OpType.ORDINAL, TypeUtil.getOpType(DataType.TIME));
		assertEquals(OpType.ORDINAL, TypeUtil.getOpType(DataType.DATE_TIME));

		assertEquals(OpType.CONTINUOUS, TypeUtil.getOpType(DataType.DATE_DAYS_SINCE_1970));
		assertEquals(OpType.CONTINUOUS, TypeUtil.getOpType(DataType.TIME_SECONDS));
		assertEquals(OpType.CONTINUOUS, TypeUtil.getOpType(DataType.DATE_TIME_SECONDS_SINCE_1970));
	}

	static
	private long countDaysSince1960(String string){
		DaysSinceDate period = (DaysSinceDate)TypeUtil.parse(DataType.DATE_DAYS_SINCE_1960, string);

		return period.getDays();
	}

	static
	private long countSecondsSinceMidnight(String string){
		SecondsSinceMidnight period = (SecondsSinceMidnight)TypeUtil.parse(DataType.TIME_SECONDS, string);

		return period.getSeconds();
	}

	static
	private long countSecondsSince1960(String string){
		SecondsSinceDate period = (SecondsSinceDate)TypeUtil.parse(DataType.DATE_TIME_SECONDS_SINCE_1960, string);

		return period.getSeconds();
	}

	// The date and time (UTC) of the first moon landing
	private static final String DATE = "1969-07-20";
	private static final String TIME = "20:17:40";
	private static final String DATE_TIME = (DATE + "T" + TIME);
}