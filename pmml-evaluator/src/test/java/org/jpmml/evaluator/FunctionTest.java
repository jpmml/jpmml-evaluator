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

import com.google.common.collect.Lists;
import org.dmg.pmml.PMMLConstants;
import org.jpmml.model.temporals.Date;
import org.jpmml.model.temporals.DateTime;
import org.jpmml.model.temporals.Time;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FunctionTest implements Deltas {

	@Test
	public void evaluateArithmeticFunctions(){
		assertEquals(4, evaluate(Functions.ADD, 1, 3));
		assertEquals(-2, evaluate(Functions.SUBTRACT, 1, 3));
		assertEquals(3, evaluate(Functions.MULTIPLY, 1, 3));
		assertEquals(0, evaluate(Functions.DIVIDE, 1, 3));

		assertEquals(null, evaluate(Functions.ADD, 1, null));
		assertEquals(null, evaluate(Functions.ADD, null, 1));

		assertEquals(4d, evaluate(Functions.ADD, 1d, 3d));
		assertEquals(-2d, evaluate(Functions.SUBTRACT, 1d, 3d));
		assertEquals(3d, evaluate(Functions.MULTIPLY, 1d, 3d));
		assertEquals((1d / 3d), evaluate(Functions.DIVIDE, 1d, 3d));

		assertEquals(null, evaluate(Functions.ADD, 1d, null));
		assertEquals(null, evaluate(Functions.ADD, null, 1d));

		assertThrows(EvaluationException.class, () -> evaluate(Functions.ADD, true, true));

		assertEquals(1, evaluate(Functions.MULTIPLY, 1, 1));
		assertEquals(1f, evaluate(Functions.MULTIPLY, 1f, 1f));
		assertEquals(1d, evaluate(Functions.MULTIPLY, 1d, 1d));

		assertEquals(1, evaluate(Functions.DIVIDE, 1, 1));

		assertEquals(1f, evaluate(Functions.DIVIDE, 1, 1f));
		assertEquals(1f, evaluate(Functions.DIVIDE, 1f, 1f));

		assertEquals(1d, evaluate(Functions.DIVIDE, 1, 1d));
		assertEquals(1d, evaluate(Functions.DIVIDE, 1f, 1d));
		assertEquals(1d, evaluate(Functions.DIVIDE, 1d, 1d));

		assertEquals(0, evaluate(Functions.MODULO, Integer.MIN_VALUE, -1));

		assertEquals(2, evaluate(Functions.MODULO, 11, 3));
		assertEquals(-5, evaluate(Functions.MODULO, 9, -7));
		assertEquals(-4, evaluate(Functions.MODULO, -4, -9));
		assertEquals(0.3d, (Double)evaluate(Functions.MODULO, -17.2d, 0.5d), DOUBLE_EXACT);

		assertEquals(1, evaluate(Functions.MODULO, 10, 3));
		assertEquals(-2, evaluate(Functions.MODULO, 10, -3));
		assertEquals(2, evaluate(Functions.MODULO, -10, 3));
		assertEquals(-1, evaluate(Functions.MODULO, -10, -3));

		assertEquals(0, evaluate(Functions.MODULO, 6, 2));
		assertEquals(-0, evaluate(Functions.MODULO, 6, -2));
		assertEquals(0, evaluate(Functions.MODULO, -6, 2));
		assertEquals(-0, evaluate(Functions.MODULO, -6, -2));

		assertEquals(0.9d, (Double)evaluate(Functions.MODULO, 4.5d, 1.2d), DOUBLE_EXACT);
		assertEquals(-0.3d, (Double)evaluate(Functions.MODULO, 4.5d, -1.2d), DOUBLE_EXACT);
		assertEquals(0.3d, (Double)evaluate(Functions.MODULO, -4.5d, 1.2d), DOUBLE_EXACT);
		assertEquals(-0.9d, (Double)evaluate(Functions.MODULO, -4.5d, -1.2d), DOUBLE_EXACT);

		assertEquals(3.0e0d, (Double)evaluate(Functions.MODULO, 1.23e2d, 0.6e1d), DOUBLE_EXACT);
	}

	@Test
	public void evaluateDivisionByZero(){
		assertThrows(UndefinedResultException.class, () -> evaluate(Functions.DIVIDE, 1, 0));

		assertEquals(Float.NEGATIVE_INFINITY, evaluate(Functions.DIVIDE, -1f, 0));
		assertEquals(Float.POSITIVE_INFINITY, evaluate(Functions.DIVIDE, 1f, 0));

		assertEquals(Double.NEGATIVE_INFINITY, evaluate(Functions.DIVIDE, -1d, 0));
		assertEquals(Double.POSITIVE_INFINITY, evaluate(Functions.DIVIDE, 1d, 0));

		assertEquals(Float.POSITIVE_INFINITY, evaluate(Functions.DIVIDE, 1, -0f));
		assertEquals(Float.POSITIVE_INFINITY, evaluate(Functions.DIVIDE, 1, 0f));

		assertEquals(Double.POSITIVE_INFINITY, evaluate(Functions.DIVIDE, 1, -0d));
		assertEquals(Double.POSITIVE_INFINITY, evaluate(Functions.DIVIDE, 1, 0d));

		assertThrows(UndefinedResultException.class, () -> evaluate(Functions.MODULO, 1, 0));

		assertEquals(Float.NaN, evaluate(Functions.MODULO, 1f, 0));

		assertEquals(Double.NaN, evaluate(Functions.MODULO, 1d, 0));
	}

	@Test
	public void evaluateStatisticalFunctions(){
		List<Integer> values = Arrays.asList(null, 1, 2, 3);

		assertEquals(1, evaluate(Functions.MIN, values));
		assertEquals(3, evaluate(Functions.MAX, values));
		assertEquals(2d, evaluate(Functions.AVG, values));
		assertEquals(6, evaluate(Functions.SUM, values));
		assertEquals(6, evaluate(Functions.PRODUCT, values));

		values = Arrays.asList(null, null, null);

		assertEquals(null, evaluate(Functions.SUM, values));
	}

	@Test
	public void evaluateDoubleMathFunctions(){
		assertEquals(Double.NaN, evaluate(Functions.LOG10, -1));
		assertEquals(Double.NEGATIVE_INFINITY, evaluate(Functions.LOG10, 0));
		assertEquals(0d, evaluate(Functions.LOG10, 1));
		assertEquals(0d, evaluate(Functions.LOG10, 1f));
		assertEquals(Double.POSITIVE_INFINITY, evaluate(Functions.LOG10, Double.POSITIVE_INFINITY));

		assertEquals(Double.NaN, evaluate(Functions.LN, -1));
		assertEquals(Double.NEGATIVE_INFINITY, evaluate(Functions.LN, 0));
		assertEquals(0d, evaluate(Functions.LN, 1));
		assertEquals(0d, evaluate(Functions.LN, 1f));
		assertEquals(Double.POSITIVE_INFINITY, evaluate(Functions.LN, Double.POSITIVE_INFINITY));

		assertEquals(Double.NaN, evaluate(Functions.LN1P, -2));
		assertEquals(Double.NEGATIVE_INFINITY, evaluate(Functions.LN1P, -1));
		assertEquals(0d, evaluate(Functions.LN1P, 0));
		assertEquals(0d, evaluate(Functions.LN1P, 0f));
		assertEquals(Double.POSITIVE_INFINITY, evaluate(Functions.LN1P, Double.POSITIVE_INFINITY));

		assertEquals(0d, evaluate(Functions.EXP, Double.NEGATIVE_INFINITY));
		assertEquals(1d, evaluate(Functions.EXP, 0));
		assertEquals(1d, evaluate(Functions.EXP, 0f));
		assertEquals(Double.POSITIVE_INFINITY, evaluate(Functions.EXP, Double.POSITIVE_INFINITY));

		assertEquals(0d, evaluate(Functions.EXPM1, 0));
		assertEquals(0d, evaluate(Functions.EXPM1, 0f));

		assertEquals(Double.NaN, evaluate(Functions.SQRT, -1));
		assertEquals(0d, evaluate(Functions.SQRT, 0));
		assertEquals(1d, evaluate(Functions.SQRT, 1));
		assertEquals(1d, evaluate(Functions.SQRT, 1f));
	}

	@Test
	public void evaluateMathFunctions(){
		assertEquals(1, evaluate(Functions.ABS, -1));
		assertEquals(1f, evaluate(Functions.ABS, -1f));
		assertEquals(1d, evaluate(Functions.ABS, -1d));

		assertThrows(InvalidArgumentException.class, () -> evaluate(Functions.POW, -2, -2));

		assertEquals(1, evaluate(Functions.POW, -2, 0));
		assertEquals(4, evaluate(Functions.POW, -2, 2));
		assertEquals(-8, evaluate(Functions.POW, -2, 3));

		assertEquals(1, evaluate(Functions.POW, 0, 0));

		assertThrows(InvalidArgumentException.class, () -> evaluate(Functions.POW, 2, -2));

		assertEquals(1 / 4d, evaluate(Functions.POW, 2, -2d));
		assertEquals(1, evaluate(Functions.POW, 2, 0));
		assertEquals(1d, evaluate(Functions.POW, 2, 0d));
		assertEquals(4, evaluate(Functions.POW, 2, 2));
		assertEquals(4d, evaluate(Functions.POW, 2, 2d));

		assertEquals(1 / 4f, evaluate(Functions.POW, 2f, -2));
		assertEquals(1 / 4d, evaluate(Functions.POW, 2f, -2d));
		assertEquals(1f, evaluate(Functions.POW, 2f, 0));
		assertEquals(1d, evaluate(Functions.POW, 2f, 0d));
		assertEquals(4f, evaluate(Functions.POW, 2f, 2));
		assertEquals(4d, evaluate(Functions.POW, 2f, 2d));

		assertEquals(0, evaluate(Functions.THRESHOLD, 2, 3));
		assertEquals(0, evaluate(Functions.THRESHOLD, 3, 3));
		assertEquals(1, evaluate(Functions.THRESHOLD, 3, 2));

		assertEquals(0f, evaluate(Functions.THRESHOLD, 2f, 3f));
		assertEquals(0f, evaluate(Functions.THRESHOLD, 3f, 3f));
		assertEquals(1f, evaluate(Functions.THRESHOLD, 3f, 2f));

		assertEquals(1, evaluate(Functions.FLOOR, 1));
		assertEquals(1, evaluate(Functions.FLOOR, 1.99f));

		assertEquals(1, evaluate(Functions.CEIL, 1));
		assertEquals(1, evaluate(Functions.CEIL, 0.01f));

		assertEquals(2, evaluate(Functions.ROUND, 1.99f));
		assertEquals(0, evaluate(Functions.ROUND, 0.01f));

		assertEquals(Double.NaN, evaluate(Functions.RINT, Double.NaN));
		assertEquals(0d, evaluate(Functions.RINT, 0.01d));
		assertEquals(2d, evaluate(Functions.RINT, 1.99d));
	}

	@Test
	public void evaluateValueFunctions(){
		FieldValue value = FieldValues.MISSING_VALUE;

		assertEquals(true, evaluate(Functions.IS_MISSING, value));
		assertEquals(false, evaluate(Functions.IS_NOT_MISSING, value));
		assertEquals(false, evaluate(Functions.IS_VALID, value));
		assertEquals(false, evaluate(Functions.IS_NOT_VALID, value));

		value = FieldValue.create(TypeInfos.CATEGORICAL_STRING, "value");

		((ScalarValue)value).setValid(true);

		assertEquals(false, evaluate(Functions.IS_MISSING, value));
		assertEquals(true, evaluate(Functions.IS_NOT_MISSING, value));
		assertEquals(true, evaluate(Functions.IS_VALID, value));
		assertEquals(false, evaluate(Functions.IS_NOT_VALID, value));

		((ScalarValue)value).setValid(false);

		assertEquals(false, evaluate(Functions.IS_MISSING, value));
		assertEquals(true, evaluate(Functions.IS_NOT_MISSING, value));
		assertEquals(false, evaluate(Functions.IS_VALID, value));
		assertEquals(true, evaluate(Functions.IS_NOT_VALID, value));

		value = FieldValue.create(TypeInfos.CONTINUOUS_FLOAT, PMMLConstants.NOT_A_NUMBER);

		assertEquals(false, evaluate(Functions.IS_VALID, value));
		assertEquals(true, evaluate(Functions.IS_NOT_VALID, value));

		value = FieldValue.create(TypeInfos.CONTINUOUS_DOUBLE, PMMLConstants.NOT_A_NUMBER);

		assertEquals(false, evaluate(Functions.IS_VALID, value));
		assertEquals(true, evaluate(Functions.IS_NOT_VALID, value));
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
	public void evaluateValueSetFunctions(){
		assertEquals(true, evaluate(Functions.IS_IN, "3", "1", "2", "3"));
		assertEquals(true, evaluate(Functions.IS_NOT_IN, "0", "1", "2", "3"));

		assertEquals(false, evaluate(Functions.IS_IN, null, "1", "2", "3"));
		assertEquals(true, evaluate(Functions.IS_NOT_IN, null, "1", "2", "3"));

		assertEquals(true, evaluate(Functions.IS_IN, null, "1", null, "3"));
		assertEquals(false, evaluate(Functions.IS_NOT_IN, null, "1", null, "3"));

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

		assertEquals(0, evaluate(Functions.STRING_LENGTH, ""));
		assertEquals(5, evaluate(Functions.STRING_LENGTH, "aBc9x"));

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

		assertThrows(IllegalArgumentException.class, () -> evaluate(Functions.REPLACE, "BBBB", "B", "$"));

		assertEquals("$$$$", evaluate(Functions.REPLACE, "BBBB", "B", "$$"));

		assertThrows(IllegalArgumentException.class, () -> evaluate(Functions.REPLACE, "BBBB", "B", "\\"));

		assertEquals("\\\\\\\\", evaluate(Functions.REPLACE, "BBBB", "B", "\\\\"));

		assertEquals("10 USD", evaluate(Functions.REPLACE, "$10", "\\$(\\d+)", "$1 USD"));

		String word_repeated_pattern = "(\\w+)\\s+\\1";
		String word_repeated_replacement = "$1 (repeated)";

		assertEquals("Hello World", evaluate(Functions.REPLACE, "Hello World", word_repeated_pattern, word_repeated_replacement));
		assertEquals("Hello (repeated)", evaluate(Functions.REPLACE, "Hello Hello", word_repeated_pattern, word_repeated_replacement));
		assertEquals("Hello (repeated) World (repeated)", evaluate(Functions.REPLACE, "Hello Hello World World", word_repeated_pattern, word_repeated_replacement));

		// See https://www.w3.org/TR/xquery-operators/#func-replace
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

		// See https://www.w3.org/TR/xquery-operators/#func-matches
		assertEquals(true, evaluate(Functions.MATCHES, "abracadabra", "bra"));
		assertEquals(true, evaluate(Functions.MATCHES, "abracadabra", "^a.*a$"));
		assertEquals(false, evaluate(Functions.MATCHES, "abracadabra", "^bra"));

		assertEquals(true, evaluate(Functions.MATCHES, "Hello Hello", word_repeated_pattern));
		assertEquals(false, evaluate(Functions.MATCHES, "Hello World", word_repeated_pattern));
	}

	@Test
	public void evaluateFormatFunctions(){
		assertEquals("  2", evaluate(Functions.FORMAT_NUMBER, 2, "%3d"));

		assertEquals("08/20/04", evaluate(Functions.FORMAT_DATETIME, new Date(2004, 8, 20), "%m/%d/%y"));
	}

	@Test
	public void evaluateDateTimeFunctions(){
		assertEquals(15796, evaluate(Functions.DATE_DAYS_SINCE_YEAR, new Date(2003, 4, 1), 1960));
		assertEquals(15796, evaluate(Functions.DATE_DAYS_SINCE_YEAR, new DateTime(2003, 4, 1, 0, 0, 0), 1960));

		assertEquals(19410, evaluate(Functions.DATE_SECONDS_SINCE_MIDNIGHT, new Time(5, 23, 30)));
		assertEquals(19410, evaluate(Functions.DATE_SECONDS_SINCE_MIDNIGHT, new DateTime(1960, 1, 1, 5, 23, 30)));

		assertEquals(185403, evaluate(Functions.DATE_SECONDS_SINCE_YEAR, new DateTime(1960, 1, 3, 3, 30, 3), 1960));
	}

	@Test
	public void evaluateNormalDistributionFunctions(){
		assertEquals(0.30853754d, (Double)evaluate(Functions.NORMAL_CDF, 0d, 1d, 2d), DOUBLE_INEXACT);
		assertEquals(0.5, (Double)evaluate(Functions.STD_NORMAL_CDF, 0d), DOUBLE_INEXACT);

		assertEquals(0.17603266d, (Double)evaluate(Functions.NORMAL_PDF, 0d, 1d, 2d), DOUBLE_INEXACT);
		assertEquals(0.39894228d, (Double)evaluate(Functions.STD_NORMAL_PDF, 0d), DOUBLE_INEXACT);
	}

	@Test
	public void evaluateTrigonometricFunctions(){
		Double angle = Math.toRadians(30);

		assertEquals(0.5d, (Double)evaluate(Functions.SIN, angle), DOUBLE_INEXACT);
		assertEquals(angle, (Double)evaluate(Functions.ASIN, 0.5d), DOUBLE_INEXACT);
		assertEquals(Double.NaN, evaluate(Functions.ASIN, 2d));
		assertEquals(0.54785347d, (Double)evaluate(Functions.SINH, angle), DOUBLE_INEXACT);

		assertEquals(0.8660254d, (Double)evaluate(Functions.COS, angle), DOUBLE_INEXACT);
		assertEquals(angle, (Double)evaluate(Functions.ACOS, 0.8660254d), DOUBLE_INEXACT);
		assertEquals(Double.NaN, evaluate(Functions.ACOS, 2d));
		assertEquals(1.14023832d, (Double)evaluate(Functions.COSH, angle), DOUBLE_INEXACT);

		assertEquals(0.57735027d, (Double)evaluate(Functions.TAN, angle), DOUBLE_INEXACT);
		assertEquals(angle, (Double)evaluate(Functions.ATAN, 0.57735027d), DOUBLE_INEXACT);
		assertEquals(0.48047278d, (Double)evaluate(Functions.TANH, angle), DOUBLE_INEXACT);
	}

	static
	private Object evaluate(Function function, Object... arguments){
		return evaluate(function, Arrays.asList(arguments));
	}

	static
	private Object evaluate(Function function, List<?> arguments){
		FieldValue value = function.evaluate(Lists.transform(arguments, argument -> FieldValueUtil.create(argument)));

		return FieldValueUtil.getValue(value);
	}

	static
	private Object evaluate(Function function, FieldValue... arguments){
		FieldValue value = function.evaluate(Arrays.asList(arguments));

		return FieldValueUtil.getValue(value);
	}
}