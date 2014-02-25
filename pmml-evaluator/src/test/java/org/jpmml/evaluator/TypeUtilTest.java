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

import org.dmg.pmml.*;

import org.junit.*;

import org.joda.time.*;

import static org.junit.Assert.*;

public class TypeUtilTest {

	@Test
	public void parse(){
		assertEquals(1, TypeUtil.parse(DataType.INTEGER, "1"));
		assertEquals(1, TypeUtil.parse(DataType.INTEGER, "1.0"));
		assertEquals(1, TypeUtil.parse(DataType.INTEGER, "1e+0"));
	}

	@Test
	public void cast(){
		assertEquals("1", TypeUtil.cast(DataType.STRING, "1"));

		assertEquals("1", TypeUtil.cast(DataType.STRING, 1));
		assertEquals("1.0", TypeUtil.cast(DataType.STRING, 1f)); // XXX
		assertEquals("1.0", TypeUtil.cast(DataType.STRING, 1.0f));
		assertEquals("1.0", TypeUtil.cast(DataType.STRING, 1d)); // XXX
		assertEquals("1.0", TypeUtil.cast(DataType.STRING, 1.0d));

		assertEquals(1, TypeUtil.cast(DataType.INTEGER, 1));

		assertEquals(1f, TypeUtil.cast(DataType.FLOAT, 1));
		assertEquals(1f, TypeUtil.cast(DataType.FLOAT, 1f));

		assertEquals(1d, TypeUtil.cast(DataType.DOUBLE, 1));
		assertEquals(1d, TypeUtil.cast(DataType.DOUBLE, 1f));
		assertEquals(1d, TypeUtil.cast(DataType.DOUBLE, 1d));
	}

	@Test
	public void compareDateTime(){
		assertTrue(TypeUtil.compare(DataType.DATE, parseDate("1960-01-01"), parseDate("1960-01-01")) == 0);
		assertTrue(TypeUtil.compare(DataType.TIME, parseTime("00:00:00"), parseTime("00:00:00")) == 0);
		assertTrue(TypeUtil.compare(DataType.DATE_TIME, parseDateTime("1960-01-01T00:00:00"), parseDateTime("1960-01-01T00:00:00")) == 0);

		assertTrue(TypeUtil.compare(DataType.DATE_DAYS_SINCE_1960, parseDaysSince1960("1960-01-01"), parseDaysSince1960("1960-01-01")) == 0);
		assertTrue(TypeUtil.compare(DataType.TIME_SECONDS, parseSecondsSinceMidnight("00:00:00"), parseSecondsSinceMidnight("00:00:00")) == 0);
		assertTrue(TypeUtil.compare(DataType.DATE_TIME_SECONDS_SINCE_1960, parseSecondsSince1960("1960-01-01T00:00:00"), parseSecondsSince1960("1960-01-01T00:00:00")) == 0);
	}

	@Test
	public void parseDaysSinceDate(){
		assertEquals(0, countDaysSince1960("1960-01-01"));
		assertEquals(1, countDaysSince1960("1960-01-02"));
		assertEquals(31, countDaysSince1960("1960-02-01"));

		assertEquals(-1, countDaysSince1960("1959-12-31"));

		assertEquals(15796, countDaysSince1960("2003-04-01"));
	}

	@Test
	public void parseSecondsSinceMidnight(){
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
		assertEquals(0, countSecondsSince1960("1960-01-01T00:00:00"));
		assertEquals(1, countSecondsSince1960("1960-01-01T00:00:01"));
		assertEquals(60, countSecondsSince1960("1960-01-01T00:01:00"));

		assertEquals(-1, countSecondsSince1960("1959-12-31T23:59:59"));

		assertEquals(185403, countSecondsSince1960("1960-01-03T03:30:03"));
	}

	@Test
	public void getDataType(){
		assertEquals(DataType.STRING, TypeUtil.getDataType("value"));

		assertEquals(DataType.INTEGER, TypeUtil.getDataType(1));
		assertEquals(DataType.FLOAT, TypeUtil.getDataType(1f));
		assertEquals(DataType.DOUBLE, TypeUtil.getDataType(1d));
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
		assertEquals(DataType.FLOAT, TypeUtil.getConstantDataType("1.0"));
		assertEquals(DataType.FLOAT, TypeUtil.getConstantDataType("1.0E0"));
		assertEquals(DataType.STRING, TypeUtil.getConstantDataType("1.0X"));

		assertEquals(DataType.INTEGER, TypeUtil.getConstantDataType("1"));
		assertEquals(DataType.STRING, TypeUtil.getConstantDataType("1E0"));
		assertEquals(DataType.STRING, TypeUtil.getConstantDataType("1X"));
	}

	static
	private LocalDate parseDate(String string){
		return (LocalDate)TypeUtil.parse(DataType.DATE, string);
	}

	static
	private LocalTime parseTime(String string){
		return (LocalTime)TypeUtil.parse(DataType.TIME, string);
	}

	static
	private LocalDateTime parseDateTime(String string){
		return (LocalDateTime)TypeUtil.parse(DataType.DATE_TIME, string);
	}

	static
	private int countDaysSince1960(String string){
		DaysSinceDate period = parseDaysSince1960(string);

		return period.intValue();
	}

	static
	private DaysSinceDate parseDaysSince1960(String string){
		return (DaysSinceDate)TypeUtil.parse(DataType.DATE_DAYS_SINCE_1960, string);
	}

	static
	private int countSecondsSinceMidnight(String string){
		SecondsSinceMidnight period = parseSecondsSinceMidnight(string);

		return period.intValue();
	}

	static
	private SecondsSinceMidnight parseSecondsSinceMidnight(String string){
		return (SecondsSinceMidnight)TypeUtil.parse(DataType.TIME_SECONDS, string);
	}

	static
	private int countSecondsSince1960(String string){
		SecondsSinceDate period = parseSecondsSince1960(string);

		return period.intValue();
	}

	static
	private SecondsSinceDate parseSecondsSince1960(String string){
		return (SecondsSinceDate)TypeUtil.parse(DataType.DATE_TIME_SECONDS_SINCE_1960, string);
	}

	static
	private DataType getResultDataType(Object left, Object right){
		return TypeUtil.getResultDataType(TypeUtil.getDataType(left), TypeUtil.getDataType(right));
	}
}