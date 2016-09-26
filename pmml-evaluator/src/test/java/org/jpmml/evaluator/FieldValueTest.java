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

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FieldValueTest {

 	@Test
	public void emptyList(){
		FieldValue list = FieldValueUtil.create(null, null, Arrays.asList());

		assertEquals(list.getDataType(), DataType.STRING);
		assertEquals(list.getOpType(), OpType.CATEGORICAL);
	}

	@Test
	public void categoricalString(){
		FieldValue zero = FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, "0");
		FieldValue one = FieldValueUtil.create(DataType.STRING, OpType.CATEGORICAL, "1");

		assertTrue(zero.equalsString("0"));
		assertFalse(zero.equalsString("0.0"));

		assertTrue(one.equalsString("1"));
		assertFalse(one.equalsString("1.0"));

		assertTrue(zero.equalsValue(zero));
		assertFalse(zero.equalsValue(one));

		assertFalse(one.equalsValue(zero));
		assertTrue(one.equalsValue(one));

		try {
			zero.compareToString("0");

			fail();
		} catch(EvaluationException ee){
			// Ignored
		}

		try {
			zero.compareTo(zero);

			fail();
		} catch(EvaluationException ee){
			// Ignored
		}

		assertEquals((Integer)0, zero.asInteger());
		assertEquals(Boolean.FALSE, zero.asBoolean());

		assertEquals((Integer)1, one.asInteger());
		assertEquals(Boolean.TRUE, one.asBoolean());
	}

	@Test
	public void categoricalStringList(){
		FieldValue list = FieldValueUtil.create(null, null, Arrays.asList("1", "2", "3"));

		assertEquals(DataType.STRING, list.getDataType());
		assertEquals(OpType.CATEGORICAL, list.getOpType());
	}

	@Test
	public void ordinalString(){
		OrdinalValue loud = (OrdinalValue)FieldValueUtil.create(DataType.STRING, OpType.ORDINAL, "loud");
		OrdinalValue louder = (OrdinalValue)FieldValueUtil.create(DataType.STRING, OpType.ORDINAL, "louder");
		OrdinalValue insane = (OrdinalValue)FieldValueUtil.create(DataType.STRING, OpType.ORDINAL, "insane");

		assertFalse(louder.equalsString("loud"));
		assertTrue(louder.equalsString("louder"));
		assertFalse(louder.equalsString("insane"));

		assertFalse(louder.equalsValue(loud));
		assertTrue(louder.equalsValue(louder));
		assertFalse(louder.equalsValue(insane));

		// Implicit (ie. lexicographic) ordering
		louder.setOrdering(null);

		assertTrue(louder.compareToString("loud") > 0);
		assertTrue(louder.compareToString("louder") == 0);
		assertTrue(louder.compareToString("insane") > 0);

		assertTrue(louder.compareTo(loud) > 0);
		assertTrue(louder.compareTo(louder) == 0);
		assertTrue(louder.compareTo(insane) > 0);

		// Explicit ordering
		louder.setOrdering(Arrays.asList(loud.getValue(), louder.getValue(), insane.getValue()));

		assertTrue(louder.compareToString("loud") > 0);
		assertTrue(louder.compareToString("louder") == 0);
		assertTrue(louder.compareToString("insane") < 0);

		assertTrue(louder.compareTo(loud) > 0);
		assertTrue(louder.compareTo(louder) == 0);
		assertTrue(louder.compareTo(insane) < 0);
	}

	@Test
	public void continuousInteger(){
		FieldValue zero = FieldValueUtil.create(DataType.INTEGER, OpType.CONTINUOUS, 0);
		FieldValue one = FieldValueUtil.create(DataType.INTEGER, OpType.CONTINUOUS, 1);

		assertTrue(zero.equalsString("-0"));
		assertTrue(zero.equalsString("-0.0"));
		assertTrue(zero.equalsString("0"));
		assertTrue(zero.equalsString("0.0"));
		assertTrue(zero.equalsString("false"));
		assertFalse(zero.equalsString("true"));

		assertTrue(one.equalsString("1"));
		assertTrue(one.equalsString("1.0"));
		assertFalse(one.equalsString("false"));
		assertTrue(one.equalsString("true"));

		assertTrue(zero.equalsValue(zero));
		assertFalse(zero.equalsValue(one));

		assertFalse(one.equalsValue(zero));
		assertTrue(one.equalsValue(one));

		assertTrue(zero.compareToString("-1") > 0);
		assertTrue(zero.compareToString("-1.5") > 0);
		assertTrue(zero.compareToString("-0") == 0);
		assertTrue(zero.compareToString("-0.0") == 0);
		assertTrue(zero.compareToString("0") == 0);
		assertTrue(zero.compareToString("0.0") == 0);
		assertTrue(zero.compareToString("1") < 0);
		assertTrue(zero.compareToString("1.5") < 0);
		assertTrue(zero.compareToString("false") == 0);
		assertTrue(zero.compareToString("true") < 0);

		assertTrue(one.compareToString("0") > 0);
		assertTrue(one.compareToString("1") == 0);
		assertTrue(one.compareToString("1.0") == 0);
		assertTrue(one.compareToString("2") < 0);
		assertTrue(one.compareToString("false") > 0);
		assertTrue(one.compareToString("true") == 0);

		assertTrue(zero.compareTo(zero) == 0);
		assertTrue(zero.compareTo(one) < 0);

		try {
			FieldValue categoricalZero = FieldValueUtil.refine(zero.getDataType(), OpType.CATEGORICAL, zero);

			zero.compareTo(categoricalZero);

			fail();
		} catch(ClassCastException cce){
			// Ignored
		}

		try {
			FieldValue doubleZero = FieldValueUtil.refine(DataType.DOUBLE, zero.getOpType(), zero);

			zero.compareTo(doubleZero);

			fail();
		} catch(ClassCastException cce){
			// Ignored
		}

		assertTrue(one.compareTo(zero) > 0);
		assertTrue(one.compareTo(one) == 0);
	}

	@Test
	public void continuousIntegerList(){
		FieldValue list = FieldValueUtil.create(null, null, Arrays.asList(1, 2, 3));

		assertEquals(DataType.INTEGER, list.getDataType());
		assertEquals(OpType.CONTINUOUS, list.getOpType());
	}

	@Test
	public void categoricalInteger(){
		FieldValue zero = FieldValueUtil.create(DataType.INTEGER, OpType.CATEGORICAL, 0);

		assertTrue(zero.equalsString("-0"));
		assertTrue(zero.equalsString("-0.0"));
		assertTrue(zero.equalsString("0"));
		assertTrue(zero.equalsString("0.0"));
		assertTrue(zero.equalsString("false"));
		assertFalse(zero.equalsString("true"));

		try {
			zero.compareToString("0");

			fail();
		} catch(EvaluationException ee){
			// Ignored
		}

		try {
			zero.compareTo(zero);

			fail();
		} catch(EvaluationException ee){
			// Ignored
		}
	}

	@Test
	public void continuousFloat(){
		Float negativeZeroValue = Float.valueOf(-0f);
		Float positiveZeroValue = Float.valueOf(+0f);

		assertTrue(negativeZeroValue.floatValue() == positiveZeroValue.floatValue());

		assertFalse((negativeZeroValue).equals(positiveZeroValue));
		assertTrue((negativeZeroValue).compareTo(positiveZeroValue) < 0);

		FieldValue negativeZero = FieldValueUtil.create(DataType.FLOAT, OpType.CONTINUOUS, negativeZeroValue);
		FieldValue positiveZero = FieldValueUtil.create(DataType.FLOAT, OpType.CONTINUOUS, positiveZeroValue);

		assertEquals(negativeZero, positiveZero);

		assertTrue(negativeZero.equalsString("-0"));
		assertTrue(negativeZero.equalsString("-0.0"));
		assertTrue(negativeZero.equalsString("0"));
		assertTrue(negativeZero.equalsString("0.0"));
		assertTrue(negativeZero.equalsString("false"));

		assertTrue(negativeZero.compareToString("0") == 0);
		assertTrue(negativeZero.compareToString("0.0") == 0);

		assertTrue(positiveZero.equalsString("-0"));
		assertTrue(positiveZero.equalsString("-0.0"));

		assertTrue(positiveZero.compareToString("-0") == 0);
		assertTrue(positiveZero.compareToString("-0.0") == 0);

		assertTrue(negativeZero.equalsValue(positiveZero));

		assertTrue(negativeZero.compareTo(positiveZero) == 0);
	}

	@Test
	public void continuousFloatList(){
		FieldValue list = FieldValueUtil.create(null, null, Arrays.asList(1f, 2f, 3f));

		assertEquals(DataType.FLOAT, list.getDataType());
		assertEquals(OpType.CONTINUOUS, list.getOpType());
	}

	@Test
	public void continuousDouble(){
		Double negativeZeroValue = Double.valueOf(-0d);
		Double positiveZeroValue = Double.valueOf(+0d);

		assertTrue(negativeZeroValue.doubleValue() == positiveZeroValue.doubleValue());

		assertFalse((negativeZeroValue).equals(positiveZeroValue));
		assertTrue((negativeZeroValue).compareTo(positiveZeroValue) < 0);

		FieldValue negativeZero = FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, negativeZeroValue);
		FieldValue positiveZero = FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, positiveZeroValue);

		assertEquals(negativeZero, positiveZero);

		assertTrue(negativeZero.equalsString("-0"));
		assertTrue(negativeZero.equalsString("-0.0"));
		assertTrue(negativeZero.equalsString("0"));
		assertTrue(negativeZero.equalsString("0.0"));
		assertTrue(negativeZero.equalsString("false"));

		assertTrue(negativeZero.compareToString("0") == 0);
		assertTrue(negativeZero.compareToString("0.0") == 0);

		assertTrue(positiveZero.equalsString("-0"));
		assertTrue(positiveZero.equalsString("-0.0"));

		assertTrue(positiveZero.compareToString("-0") == 0);
		assertTrue(positiveZero.compareToString("-0.0") == 0);

		assertTrue(negativeZero.equalsValue(positiveZero));

		assertTrue(negativeZero.compareTo(positiveZero) == 0);
	}

	@Test
	public void continuousDoubleList(){
		FieldValue list = FieldValueUtil.create(null, null, Arrays.asList(1d, 2d, 3d));

		assertEquals(DataType.DOUBLE, list.getDataType());
		assertEquals(OpType.CONTINUOUS, list.getOpType());
	}

	@Test
	public void categoricalBoolean(){
		FieldValue zero = FieldValueUtil.create(DataType.BOOLEAN, OpType.CATEGORICAL, false);
		FieldValue one = FieldValueUtil.create(DataType.BOOLEAN, OpType.CATEGORICAL, true);

		assertTrue(zero.compareToString("-0") == 0);
		assertTrue(zero.compareToString("-0.0") == 0);
		assertTrue(zero.compareToString("0") == 0);
		assertTrue(zero.compareToString("0.0") == 0);
		assertTrue(zero.compareToString("1") < 0);
		assertTrue(zero.compareToString("1.0") < 0);
		assertTrue(zero.compareToString("false") == 0);
		assertTrue(zero.compareToString("true") < 0);

		assertTrue(one.compareToString("0") > 0);
		assertTrue(one.compareToString("0.0") > 0);
		assertTrue(one.compareToString("1") == 0);
		assertTrue(one.compareToString("1.0") == 0);
		assertTrue(one.compareToString("false") > 0);
		assertTrue(one.compareToString("true") == 0);

		assertTrue(zero.compareTo(zero) == 0);
		assertTrue(zero.compareTo(one) < 0);

		assertTrue(one.compareTo(zero) > 0);
		assertTrue(one.compareTo(one) == 0);
	}

	@Test
	public void categoricalDaysSinceDate(){
		FieldValue period = FieldValueUtil.create(DataType.DATE_DAYS_SINCE_1960, OpType.CATEGORICAL, "1960-01-03");

		assertEquals((Integer)2, period.asInteger());
	}

	@Test
	public void categoricalSecondsSinceDate(){
		FieldValue period = FieldValueUtil.create(DataType.DATE_TIME_SECONDS_SINCE_1960, OpType.CATEGORICAL, "1960-01-03T03:30:03");

		assertEquals((Integer)185403, period.asInteger());
	}
}