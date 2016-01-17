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

import org.dmg.pmml.DataType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Years;
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

		try {
			TypeUtil.parse(DataType.INTEGER, Long.toString(Integer.MIN_VALUE - 1l));

			fail();
		} catch(EvaluationException ee){
			// Ignored
		}

		try {
			TypeUtil.parse(DataType.INTEGER, Double.toString(Integer.MIN_VALUE - 1d));

			fail();
		} catch(EvaluationException ee){
			// Ignored
		}

		assertEquals(Integer.MAX_VALUE, TypeUtil.parse(DataType.INTEGER, Integer.toString(Integer.MAX_VALUE)));

		try {
			TypeUtil.parse(DataType.INTEGER, Long.toString(Integer.MAX_VALUE + 1l));

			fail();
		} catch(EvaluationException ee){
			// Ignored
		}

		try {
			TypeUtil.parse(DataType.INTEGER, Double.toString(Integer.MAX_VALUE + 1d));

			fail();
		} catch(EvaluationException ee){
			// Ignored
		}
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

		try {
			TypeUtil.cast(DataType.INTEGER, Long.valueOf(Integer.MIN_VALUE - 1l));

			fail();
		} catch(EvaluationException ee){
			// Ignored
		}

		try {
			TypeUtil.cast(DataType.INTEGER, Long.valueOf(Integer.MAX_VALUE + 1l));

			fail();
		} catch(EvaluationException ee){
			// Ignored
		}

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

		try {
			TypeUtil.cast(DataType.INTEGER, Double.valueOf(Integer.MIN_VALUE - 1d));

			fail();
		} catch(EvaluationException ee){
			// Ignored
		}

		try {
			TypeUtil.cast(DataType.INTEGER, Double.valueOf(Integer.MAX_VALUE + 1d));

			fail();
		} catch(EvaluationException ee){
			// Ignored
		}
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

		assertEquals(DECADE, Years.yearsBetween(sixties.getEpoch(), seventies.getEpoch()));
		assertEquals(DECADE, Years.yearsBetween(seventies.getEpoch(), eighties.getEpoch()));

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

		assertEquals(0, countDaysSince1960("1960-01-01"));
		assertEquals(1, countDaysSince1960("1960-01-02"));
		assertEquals(31, countDaysSince1960("1960-02-01"));

		assertEquals(-1, countDaysSince1960("1959-12-31"));

		assertEquals(15796, countDaysSince1960("2003-04-01"));
	}

	@Test
	public void parseSecondsSinceMidnight(){
		SecondsSinceMidnight noon = (SecondsSinceMidnight)TypeUtil.parse(DataType.TIME_SECONDS, "12:00:00");

		assertEquals(DataType.TIME_SECONDS, TypeUtil.getDataType(noon));

		assertEquals(0, countSecondsSinceMidnight("0:00:00"));
		assertEquals(100, countSecondsSinceMidnight("0:01:40"));
		assertEquals(200, countSecondsSinceMidnight("0:03:20"));
		assertEquals(1000, countSecondsSinceMidnight("0:16:40"));
		assertEquals(86400, countSecondsSinceMidnight("24:00:00"));
		assertEquals(86401, countSecondsSinceMidnight("24:00:01"));
		assertEquals(100000, countSecondsSinceMidnight("27:46:40"));

		assertEquals(19410, countSecondsSinceMidnight("05:23:30"));
	}

	@Test
	public void parseSecondsSinceDate(){
		SecondsSinceDate sixties = (SecondsSinceDate)TypeUtil.parse(DataType.DATE_TIME_SECONDS_SINCE_1960, DATE_TIME);
		SecondsSinceDate seventies = (SecondsSinceDate)TypeUtil.parse(DataType.DATE_TIME_SECONDS_SINCE_1970, DATE_TIME);
		SecondsSinceDate eighties = (SecondsSinceDate)TypeUtil.parse(DataType.DATE_TIME_SECONDS_SINCE_1980, DATE_TIME);

		assertEquals(DataType.DATE_TIME_SECONDS_SINCE_1960, TypeUtil.getDataType(sixties));
		assertEquals(DataType.DATE_TIME_SECONDS_SINCE_1970, TypeUtil.getDataType(seventies));
		assertEquals(DataType.DATE_TIME_SECONDS_SINCE_1980, TypeUtil.getDataType(eighties));

		assertEquals(DECADE, Years.yearsBetween(sixties.getEpoch(), seventies.getEpoch()));
		assertEquals(DECADE, Years.yearsBetween(seventies.getEpoch(), eighties.getEpoch()));

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

		assertEquals(0, countSecondsSince1960("1960-01-01T00:00:00"));
		assertEquals(1, countSecondsSince1960("1960-01-01T00:00:01"));
		assertEquals(60, countSecondsSince1960("1960-01-01T00:01:00"));

		assertEquals(-1, countSecondsSince1960("1959-12-31T23:59:59"));

		assertEquals(185403, countSecondsSince1960("1960-01-03T03:30:03"));
	}

	@Test
	public void getResultDataType(){
		assertEquals(DataType.DOUBLE, getResultDataType(1d, 1f));
		assertEquals(DataType.DOUBLE, getResultDataType(1d, 1));

		assertEquals(DataType.DOUBLE, getResultDataType(1f, 1d));
		assertEquals(DataType.FLOAT, getResultDataType(1f, 1));

		assertEquals(DataType.DOUBLE, getResultDataType(1, 1d));
		assertEquals(DataType.FLOAT, getResultDataType(1, 1f));
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

	static
	private int countDaysSince1960(String string){
		DaysSinceDate period = (DaysSinceDate)TypeUtil.parse(DataType.DATE_DAYS_SINCE_1960, string);

		return period.intValue();
	}

	static
	private int countSecondsSinceMidnight(String string){
		SecondsSinceMidnight period = (SecondsSinceMidnight)TypeUtil.parse(DataType.TIME_SECONDS, string);

		return period.intValue();
	}

	static
	private int countSecondsSince1960(String string){
		SecondsSinceDate period = (SecondsSinceDate)TypeUtil.parse(DataType.DATE_TIME_SECONDS_SINCE_1960, string);

		return period.intValue();
	}

	static
	private DataType getResultDataType(Object left, Object right){
		return TypeUtil.getResultDataType(TypeUtil.getDataType(left), TypeUtil.getDataType(right));
	}

	// The date and time (UTC) of the first moon landing
	private static final String DATE = "1969-07-20";
	private static final String TIME = "20:17:40";
	private static final String DATE_TIME = (DATE + "T" + TIME);

	private static final Years DECADE = Years.years(10);
}