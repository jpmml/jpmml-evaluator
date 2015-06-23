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

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.dmg.pmml.DataType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.jpmml.evaluator.functions.EchoFunction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FunctionTest {

	@Test
	public void evaluateArithmeticFunctions(){
		assertEquals(4d, evaluate(Functions.PLUS, 1d, 3d));
		assertEquals(-2d, evaluate(Functions.MINUS, 1d, 3d));
		assertEquals(3d, evaluate(Functions.MULTIPLY, 1d, 3d));
		assertEquals((1d / 3d), evaluate(Functions.DIVIDE, 1d, 3d));

		assertEquals(null, evaluate(Functions.PLUS, 1d, null));
		assertEquals(null, evaluate(Functions.PLUS, null, 1d));

		assertEquals(DataType.INTEGER, TypeUtil.getDataType(evaluate(Functions.MULTIPLY, 1, 1)));
		assertEquals(DataType.FLOAT, TypeUtil.getDataType(evaluate(Functions.MULTIPLY, 1f, 1f)));
		assertEquals(DataType.DOUBLE, TypeUtil.getDataType(evaluate(Functions.MULTIPLY, 1d, 1d)));

		assertEquals(DataType.INTEGER, TypeUtil.getDataType(evaluate(Functions.DIVIDE, 1, 1)));

		assertEquals(DataType.FLOAT, TypeUtil.getDataType(evaluate(Functions.DIVIDE, 1, 1f)));
		assertEquals(DataType.FLOAT, TypeUtil.getDataType(evaluate(Functions.DIVIDE, 1f, 1f)));

		assertEquals(DataType.DOUBLE, TypeUtil.getDataType(evaluate(Functions.DIVIDE, 1, 1d)));
		assertEquals(DataType.DOUBLE, TypeUtil.getDataType(evaluate(Functions.DIVIDE, 1f, 1d)));
		assertEquals(DataType.DOUBLE, TypeUtil.getDataType(evaluate(Functions.DIVIDE, 1d, 1d)));
	}

	@Test
	public void evaluateDivisionByZero(){

		try {
			evaluate(Functions.DIVIDE, 1, 0);

			fail();
		} catch(InvalidResultException ire){
			// Ignored
		}

		assertEquals(Float.NEGATIVE_INFINITY, evaluate(Functions.DIVIDE, -1f, 0));
		assertEquals(Float.POSITIVE_INFINITY, evaluate(Functions.DIVIDE, 1f, 0));

		assertEquals(Double.NEGATIVE_INFINITY, evaluate(Functions.DIVIDE, -1d, 0));
		assertEquals(Double.POSITIVE_INFINITY, evaluate(Functions.DIVIDE, 1d, 0));
	}

	@Test
	public void evaluateAggregateFunctions(){
		List<Integer> values = Arrays.asList(1, 2, 3);

		Object min = evaluate(Functions.MIN, values);
		assertEquals(1, min);
		assertEquals(DataType.INTEGER, TypeUtil.getDataType(min));

		Object max = evaluate(Functions.MAX, values);
		assertEquals(3, max);
		assertEquals(DataType.INTEGER, TypeUtil.getDataType(max));

		Object average = evaluate(Functions.AVG, values);
		assertEquals(2d, average);
		assertEquals(DataType.DOUBLE, TypeUtil.getDataType(average));

		Object sum = evaluate(Functions.SUM, values);
		assertEquals(6, sum);
		assertEquals(DataType.INTEGER, TypeUtil.getDataType(sum));

		Object product = evaluate(Functions.PRODUCT, values);
		assertEquals(6, product);
		assertEquals(DataType.INTEGER, TypeUtil.getDataType(product));
	}

	@Test
	public void evaluateMathFunctions(){
		assertEquals(DataType.DOUBLE, TypeUtil.getDataType(evaluate(Functions.LOG10, 1)));
		assertEquals(DataType.FLOAT, TypeUtil.getDataType(evaluate(Functions.LOG10, 1f)));

		assertEquals(DataType.DOUBLE, TypeUtil.getDataType(evaluate(Functions.LN, 1)));
		assertEquals(DataType.FLOAT, TypeUtil.getDataType(evaluate(Functions.LN, 1f)));

		assertEquals(DataType.DOUBLE, TypeUtil.getDataType(evaluate(Functions.EXP, 1)));
		assertEquals(DataType.FLOAT, TypeUtil.getDataType(evaluate(Functions.EXP, 1f)));

		assertEquals(DataType.DOUBLE, TypeUtil.getDataType(evaluate(Functions.SQRT, 1)));
		assertEquals(DataType.FLOAT, TypeUtil.getDataType(evaluate(Functions.SQRT, 1f)));

		assertEquals(1, evaluate(Functions.ABS, -1));
		assertEquals(1f, evaluate(Functions.ABS, -1f));
		assertEquals(1d, evaluate(Functions.ABS, -1d));

		assertEquals(DataType.INTEGER, TypeUtil.getDataType(evaluate(Functions.POW, 1, 1)));
		assertEquals(DataType.FLOAT, TypeUtil.getDataType(evaluate(Functions.POW, 1f, 1f)));

		assertEquals(0, evaluate(Functions.THRESHOLD, 2, 3));
		assertEquals(0, evaluate(Functions.THRESHOLD, 3, 3));
		assertEquals(1, evaluate(Functions.THRESHOLD, 3, 2));

		assertEquals(0f, evaluate(Functions.THRESHOLD, 2f, 3f));
		assertEquals(0f, evaluate(Functions.THRESHOLD, 3f, 3f));
		assertEquals(1f, evaluate(Functions.THRESHOLD, 3f, 2f));

		assertEquals(1, evaluate(Functions.FLOOR, 1));
		assertEquals(1, evaluate(Functions.CEIL, 1));

		assertEquals(1f, evaluate(Functions.FLOOR, 1.99f));
		assertEquals(2f, evaluate(Functions.ROUND, 1.99f));

		assertEquals(1f, evaluate(Functions.CEIL, 0.01f));
		assertEquals(0f, evaluate(Functions.ROUND, 0.01f));
	}

	@Test
	public void evaluateValueFunctions(){
		assertEquals(Boolean.TRUE, evaluate(Functions.IS_MISSING, (String)null));
		assertEquals(Boolean.FALSE, evaluate(Functions.IS_MISSING, "value"));

		assertEquals(Boolean.TRUE, evaluate(Functions.IS_NOT_MISSING, "value"));
		assertEquals(Boolean.FALSE, evaluate(Functions.IS_NOT_MISSING, (String)null));
	}

	@Test
	public void evaluateEqualityFunctions(){
		assertEquals(Boolean.TRUE, evaluate(Functions.EQUAL, 1, 1d));
		assertEquals(Boolean.TRUE, evaluate(Functions.EQUAL, 1d, 1d));

		assertEquals(Boolean.TRUE, evaluate(Functions.NOT_EQUAL, 1d, 3d));
		assertEquals(Boolean.TRUE, evaluate(Functions.NOT_EQUAL, 1, 3));
	}

	@Test
	public void evaluateComparisonFunctions(){
		assertEquals(Boolean.TRUE, evaluate(Functions.LESS_THAN, 1d, 3d));
		assertEquals(Boolean.TRUE, evaluate(Functions.LESS_THAN, 1, 3d));

		assertEquals(Boolean.TRUE, evaluate(Functions.LESS_OR_EQUAL, 1d, 1d));
		assertEquals(Boolean.TRUE, evaluate(Functions.LESS_OR_EQUAL, 1, 1d));

		assertEquals(Boolean.TRUE, evaluate(Functions.GREATER_THAN, 3d, 1d));
		assertEquals(Boolean.TRUE, evaluate(Functions.GREATER_THAN, 3, 1d));

		assertEquals(Boolean.TRUE, evaluate(Functions.GREATER_OR_EQUAL, 3d, 3d));
		assertEquals(Boolean.TRUE, evaluate(Functions.GREATER_OR_EQUAL, 3, 3d));
	}

	@Test
	public void evaluateBooleanComparisonFunctions(){

		try {
			evaluate(Functions.LESS_OR_EQUAL, false, "false");

			fail();
		} catch(TypeCheckException tce){
			// Ignored
		}

		assertEquals(Boolean.TRUE, evaluate(Functions.LESS_THAN, false, 0.5d));
		assertEquals(Boolean.FALSE, evaluate(Functions.LESS_THAN, true, 0.5d));

		assertEquals(Boolean.TRUE, evaluate(Functions.LESS_OR_EQUAL, false, 0d));
		assertEquals(Boolean.FALSE, evaluate(Functions.LESS_OR_EQUAL, true, 0d));

		assertEquals(Boolean.FALSE, evaluate(Functions.GREATER_THAN, false, 0.5d));
		assertEquals(Boolean.TRUE, evaluate(Functions.GREATER_THAN, true, 0.5d));

		assertEquals(Boolean.FALSE, evaluate(Functions.GREATER_OR_EQUAL, false, 1d));
		assertEquals(Boolean.TRUE, evaluate(Functions.GREATER_OR_EQUAL, true, 1d));

		try {
			evaluate(Functions.LESS_OR_EQUAL, false, false);

			fail();
		} catch(TypeCheckException tce){
			// Ignored
		}
	}

	@Test
	public void evaluateBinaryFunctions(){
		assertEquals(Boolean.TRUE, evaluate(Functions.AND, Boolean.TRUE, Boolean.TRUE));
		assertEquals(Boolean.TRUE, evaluate(Functions.AND, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE));

		assertEquals(Boolean.FALSE, evaluate(Functions.AND, Boolean.TRUE, Boolean.FALSE));
		assertEquals(Boolean.FALSE, evaluate(Functions.AND, Boolean.FALSE, Boolean.TRUE));

		assertEquals(Boolean.TRUE, evaluate(Functions.OR, Boolean.FALSE, Boolean.TRUE));
		assertEquals(Boolean.TRUE, evaluate(Functions.OR, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE));

		assertEquals(Boolean.FALSE, evaluate(Functions.OR, Boolean.FALSE, Boolean.FALSE));
	}

	@Test
	public void evaluateUnaryFunction(){
		assertEquals(Boolean.TRUE, evaluate(Functions.NOT, Boolean.FALSE));
		assertEquals(Boolean.FALSE, evaluate(Functions.NOT, Boolean.TRUE));
	}

	@Test
	public void evaluateValueListFunctions(){
		assertEquals(Boolean.TRUE, evaluate(Functions.IS_IN, "3", "1", "2", "3"));
		assertEquals(Boolean.TRUE, evaluate(Functions.IS_NOT_IN, "0", "1", "2", "3"));

		assertEquals(Boolean.TRUE, evaluate(Functions.IS_IN, 3, 1, 2, 3));
		assertEquals(Boolean.TRUE, evaluate(Functions.IS_NOT_IN, 0, 1, 2, 3));

		assertEquals(Boolean.TRUE, evaluate(Functions.IS_IN, 3d, 1d, 2d, 3d));
		assertEquals(Boolean.TRUE, evaluate(Functions.IS_NOT_IN, 0d, 1d, 2d, 3d));
	}

	@Test
	public void evaluateIfFunction(){
		assertEquals("left", evaluate(Functions.IF, Boolean.TRUE, "left"));
		assertEquals("left", evaluate(Functions.IF, Boolean.TRUE, "left", "right"));

		assertEquals(null, evaluate(Functions.IF, Boolean.FALSE, "left"));
		assertEquals("right", evaluate(Functions.IF, Boolean.FALSE, "left", "right"));
	}

	@Test
	public void evaluateStringFunctions(){
		assertEquals("VALUE", evaluate(Functions.UPPERCASE, "Value"));
		assertEquals("value", evaluate(Functions.LOWERCASE, "Value"));

		assertEquals("", evaluate(Functions.SUBSTRING, "value", 1, 0));
		assertEquals("value", evaluate(Functions.SUBSTRING, "value", 1, 5));

		assertEquals("alue", evaluate(Functions.SUBSTRING, "value", 2, 4));
		assertEquals("valu", evaluate(Functions.SUBSTRING, "value", 1, 4));

		assertEquals("value", evaluate(Functions.TRIM_BLANKS, "\tvalue\t"));
	}

	@Test
	public void evaluateConcatenationFunction(){
		assertEquals("2-2000", evaluate(Functions.CONCAT, "2", "-", "2000"));
		assertEquals("2-2000", evaluate(Functions.CONCAT, "2", null, "-", null, "2000"));

		assertEquals("2-2000", evaluate(Functions.CONCAT, 2, "-", 2000));
	}

	@Test
	public void evaluateRegularExpressionFunctions(){
		assertEquals("c", evaluate(Functions.REPLACE, "BBBB", "B+", "c"));
		assertEquals("cccc", evaluate(Functions.REPLACE, "BBBB", "B+?", "c"));

		// See http://www.w3.org/TR/xquery-operators/#func-replace
		assertEquals("a*cada*", evaluate(Functions.REPLACE, "abracadabra", "bra", "*"));
		assertEquals("*", evaluate(Functions.REPLACE, "abracadabra", "a.*a", "*"));
		assertEquals("*c*bra", evaluate(Functions.REPLACE, "abracadabra", "a.*?a", "*"));
		assertEquals("brcdbr", evaluate(Functions.REPLACE, "abracadabra", "a", ""));
		assertEquals("abbraccaddabbra", evaluate(Functions.REPLACE, "abracadabra", "a(.)", "a$1$1"));

		String[] monthNames = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
		for(int i = 0; i < monthNames.length; i++){
			Boolean matches = Boolean.valueOf(i == 0 || i == 1 || i == 4);

			assertEquals(matches, evaluate(Functions.MATCHES, monthNames[i], "ar?y"));
		}

		// See http://www.w3.org/TR/xquery-operators/#func-matches
		assertEquals(Boolean.TRUE, evaluate(Functions.MATCHES, "abracadabra", "bra"));
		assertEquals(Boolean.TRUE, evaluate(Functions.MATCHES, "abracadabra", "^a.*a$"));
		assertEquals(Boolean.FALSE, evaluate(Functions.MATCHES, "abracadabra", "^bra"));
	}

	@Test
	public void evaluateFormatFunctions(){
		assertEquals("  2", evaluate(Functions.FORMAT_NUMBER, 2, "%3d"));

		assertEquals("08/20/04", evaluate(Functions.FORMAT_DATETIME, new LocalDate(2004, 8, 20), "%m/%d/%y"));
	}

	@Test
	public void evaluateDateTimeFunctions(){
		assertEquals(15796, evaluate(Functions.DATE_DAYS_SINCE_YEAR, new LocalDate(2003, 4, 1), 1960));
		assertEquals(15796, evaluate(Functions.DATE_DAYS_SINCE_YEAR, new LocalDateTime(2003, 4, 1, 0, 0, 0), 1960));

		assertEquals(19410, evaluate(Functions.DATE_SECONDS_SINCE_MIDNIGHT, new LocalTime(5, 23, 30)));
		assertEquals(19410, evaluate(Functions.DATE_SECONDS_SINCE_MIDNIGHT, new LocalDateTime(1960, 1, 1, 5, 23, 30)));

		assertEquals(185403, evaluate(Functions.DATE_SECONDS_SINCE_YEAR, new LocalDateTime(1960, 1, 3, 3, 30, 3), 1960));
	}

	@Test
	public void evaluateEchoFunction(){
		Function function = new EchoFunction();

		try {
			evaluate(function);

			fail();
		} catch(FunctionException fe){
			// Ignored
		}

		assertEquals("Hello World!", evaluate(function, "Hello World!"));

		try {
			evaluate(function, "Hello World!", "Hello World!");

			fail();
		} catch(FunctionException fe){
			// Ignored
		}
	}

	static
	private Object evaluate(Function function, Object... values){
		return evaluate(function, Arrays.asList(values));
	}

	static
	private Object evaluate(Function function, List<?> values){
		com.google.common.base.Function<Object, FieldValue> transformer = new com.google.common.base.Function<Object, FieldValue>(){

			@Override
			public FieldValue apply(Object object){
				return FieldValueUtil.create(object);
			}
		};

		FieldValue result = function.evaluate(Lists.newArrayList(Iterables.transform(values, transformer)));

		return FieldValueUtil.getValue(result);
	}
}