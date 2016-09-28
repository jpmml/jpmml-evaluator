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

import org.dmg.pmml.OpType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.junit.Assert;
import org.junit.Test;

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

		assertEquals(1, evaluate(Functions.MULTIPLY, 1, 1));
		assertEquals(1f, evaluate(Functions.MULTIPLY, 1f, 1f));
		assertEquals(1d, evaluate(Functions.MULTIPLY, 1d, 1d));

		assertEquals(1, evaluate(Functions.DIVIDE, 1, 1));

		assertEquals(1f, evaluate(Functions.DIVIDE, 1, 1f));
		assertEquals(1f, evaluate(Functions.DIVIDE, 1f, 1f));

		assertEquals(1d, evaluate(Functions.DIVIDE, 1, 1d));
		assertEquals(1d, evaluate(Functions.DIVIDE, 1f, 1d));
		assertEquals(1d, evaluate(Functions.DIVIDE, 1d, 1d));
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

		assertEquals(1, evaluate(Functions.MIN, values));
		assertEquals(3, evaluate(Functions.MAX, values));
		assertEquals(2d, evaluate(Functions.AVG, values));
		assertEquals(6, evaluate(Functions.SUM, values));
		assertEquals(6, evaluate(Functions.PRODUCT, values));
	}

	@Test
	public void evaluateMathFunctions(){
		assertEquals(0d, evaluate(Functions.LOG10, 1));
		assertEquals(0f, evaluate(Functions.LOG10, 1f));

		assertEquals(0d, evaluate(Functions.LN, 1));
		assertEquals(0f, evaluate(Functions.LN, 1f));

		assertEquals(1d, evaluate(Functions.EXP, 0));
		assertEquals(1f, evaluate(Functions.EXP, 0f));

		assertEquals(1d, evaluate(Functions.SQRT, 1));
		assertEquals(1f, evaluate(Functions.SQRT, 1f));

		assertEquals(1, evaluate(Functions.ABS, -1));
		assertEquals(1f, evaluate(Functions.ABS, -1f));
		assertEquals(1d, evaluate(Functions.ABS, -1d));

		assertEquals(1, evaluate(Functions.POW, 1, 1));
		assertEquals(1f, evaluate(Functions.POW, 1f, 1f));

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
		assertEquals(true, evaluate(Functions.IS_MISSING, (String)null));
		assertEquals(false, evaluate(Functions.IS_MISSING, "value"));

		assertEquals(true, evaluate(Functions.IS_NOT_MISSING, "value"));
		assertEquals(false, evaluate(Functions.IS_NOT_MISSING, (String)null));
	}

	@Test
	public void evaluateEqualityFunctions(){
		assertEquals(true, evaluate(Functions.EQUAL, 1, 1d));
		assertEquals(true, evaluate(Functions.EQUAL, 1d, 1d));

		assertEquals(true, evaluate(Functions.NOT_EQUAL, 1d, 3d));
		assertEquals(true, evaluate(Functions.NOT_EQUAL, 1, 3));
	}

	@Test
	public void evaluateComparisonFunctions(){
		assertEquals(true, evaluate(Functions.LESS_THAN, 1d, 3d));
		assertEquals(true, evaluate(Functions.LESS_THAN, 1, 3d));

		assertEquals(true, evaluate(Functions.LESS_OR_EQUAL, 1d, 1d));
		assertEquals(true, evaluate(Functions.LESS_OR_EQUAL, 1, 1d));

		assertEquals(true, evaluate(Functions.GREATER_THAN, 3d, 1d));
		assertEquals(true, evaluate(Functions.GREATER_THAN, 3, 1d));

		assertEquals(true, evaluate(Functions.GREATER_OR_EQUAL, 3d, 3d));
		assertEquals(true, evaluate(Functions.GREATER_OR_EQUAL, 3, 3d));
	}

	@Test
	public void evaluateBooleanComparisonFunctions(){
		assertEquals(true, evaluate(Functions.LESS_THAN, false, 0.5d));
		assertEquals(false, evaluate(Functions.LESS_THAN, true, 0.5d));

		assertEquals(true, evaluate(Functions.LESS_OR_EQUAL, false, 0d));
		assertEquals(false, evaluate(Functions.LESS_OR_EQUAL, true, 0d));

		assertEquals(false, evaluate(Functions.GREATER_THAN, false, 0.5d));
		assertEquals(true, evaluate(Functions.GREATER_THAN, true, 0.5d));

		assertEquals(false, evaluate(Functions.GREATER_OR_EQUAL, false, 1d));
		assertEquals(true, evaluate(Functions.GREATER_OR_EQUAL, true, 1d));
	}

	@Test
	public void evaluateBinaryFunctions(){
		assertEquals(true, evaluate(Functions.AND, true, true));
		assertEquals(true, evaluate(Functions.AND, true, true, true));

		assertEquals(false, evaluate(Functions.AND, true, false));
		assertEquals(false, evaluate(Functions.AND, false, true));

		assertEquals(true, evaluate(Functions.OR, false, true));
		assertEquals(true, evaluate(Functions.OR, false, false, true));

		assertEquals(false, evaluate(Functions.OR, false, false));
	}

	@Test
	public void evaluateUnaryFunction(){
		assertEquals(true, evaluate(Functions.NOT, false));
		assertEquals(false, evaluate(Functions.NOT, true));
	}

	@Test
	public void evaluateValueListFunctions(){
		assertEquals(true, evaluate(Functions.IS_IN, "3", "1", "2", "3"));
		assertEquals(true, evaluate(Functions.IS_NOT_IN, "0", "1", "2", "3"));

		assertEquals(true, evaluate(Functions.IS_IN, 3, 1, 2, 3));
		assertEquals(true, evaluate(Functions.IS_NOT_IN, 0, 1, 2, 3));

		assertEquals(true, evaluate(Functions.IS_IN, 3d, 1d, 2d, 3d));
		assertEquals(true, evaluate(Functions.IS_NOT_IN, 0d, 1d, 2d, 3d));
	}

	@Test
	public void evaluateIfFunction(){
		assertEquals("left", evaluate(Functions.IF, true, "left"));
		assertEquals("left", evaluate(Functions.IF, true, "left", "right"));

		assertEquals(null, evaluate(Functions.IF, false, "left"));
		assertEquals("right", evaluate(Functions.IF, false, "left", "right"));
	}

	@Test
	public void evaluateStringFunctions(){
		assertEquals("VALUE", evaluate(Functions.UPPERCASE, "Value"));
		assertEquals("value", evaluate(Functions.LOWERCASE, "Value"));

		assertEquals("Bc9", evaluate(Functions.SUBSTRING, "aBc9x", 2, 3));

		assertEquals("", evaluate(Functions.SUBSTRING, "", 1, 0));
		assertEquals("", evaluate(Functions.SUBSTRING, "", 1, Integer.MAX_VALUE));
		assertEquals("", evaluate(Functions.SUBSTRING, "", Integer.MAX_VALUE, 0));
		assertEquals("", evaluate(Functions.SUBSTRING, "", Integer.MAX_VALUE, Integer.MAX_VALUE));

		assertEquals("", evaluate(Functions.SUBSTRING, "value", 1, 0));
		assertEquals("valu", evaluate(Functions.SUBSTRING, "value", 1, 4));
		assertEquals("value", evaluate(Functions.SUBSTRING, "value", 1, 5));
		assertEquals("value", evaluate(Functions.SUBSTRING, "value", 1, Integer.MAX_VALUE));
		assertEquals("alue", evaluate(Functions.SUBSTRING, "value", 2, 4));
		assertEquals("alue", evaluate(Functions.SUBSTRING, "value", 2, Integer.MAX_VALUE));
		assertEquals("", evaluate(Functions.SUBSTRING, "value", 6, Integer.MAX_VALUE));
		assertEquals("", evaluate(Functions.SUBSTRING, "value", Integer.MAX_VALUE, Integer.MAX_VALUE));

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
			boolean matches = (i == 0 || i == 1 || i == 4);

			assertEquals(matches, evaluate(Functions.MATCHES, monthNames[i], "ar?y"));
		}

		// See http://www.w3.org/TR/xquery-operators/#func-matches
		assertEquals(true, evaluate(Functions.MATCHES, "abracadabra", "bra"));
		assertEquals(true, evaluate(Functions.MATCHES, "abracadabra", "^a.*a$"));
		assertEquals(false, evaluate(Functions.MATCHES, "abracadabra", "^bra"));
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
	public void evaluateTrigonometricFunctions(){
		Double angle = Math.toRadians(30);

		assertEquals(0.5d, evaluate(Functions.SIN, angle), 1e-6);
		assertEquals(angle, evaluate(Functions.ASIN, 0.5d), 1e-6);
		assertEquals(0.54785347d, evaluate(Functions.SINH, angle), 1e-6);

		assertEquals(0.8660254d, evaluate(Functions.COS, angle), 1e-6);
		assertEquals(angle, evaluate(Functions.ACOS, 0.8660254d), 1e-6);
		assertEquals(1.14023832d, evaluate(Functions.COSH, angle), 1e-6);

		assertEquals(0.57735027d, evaluate(Functions.TAN, angle), 1e-6);
		assertEquals(angle, evaluate(Functions.ATAN, 0.57735027d), 1e-6);
		assertEquals(0.48047278d, evaluate(Functions.TANH, angle), 1e-6);

		try {
			evaluate(Functions.ASIN, 2d);

			fail();
		} catch(InvalidResultException ire){
			// Ignored
		}
	}

	static
	private void assertEquals(Object expected, FieldValue actual){
		Assert.assertEquals(FieldValueUtil.create(null, null, expected), actual);
	}

	static
	private void assertEquals(Number expected, FieldValue actual, double delta){
		Assert.assertEquals(FieldValueUtil.create(null, OpType.CONTINUOUS, expected).asDouble(), actual.asDouble(), delta);
	}

	static
	private FieldValue evaluate(Function function, Object... arguments){
		return evaluate(function, Arrays.asList(arguments));
	}

	static
	private FieldValue evaluate(Function function, List<?> arguments){
		return function.evaluate(FieldValueUtil.createAll(null, null, arguments));
	}
}