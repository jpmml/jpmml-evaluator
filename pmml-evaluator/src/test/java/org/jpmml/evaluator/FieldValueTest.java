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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FieldValueTest {

 	@Test
	public void emptyList(){
		FieldValue list = FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, Arrays.asList());

		assertEquals(list.getDataType(), DataType.STRING);
		assertEquals(list.getOpType(), OpType.CATEGORICAL);
	}

	@Test
	public void categoricalString(){
		FieldValue zero = FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, "0");
		FieldValue one = FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, "1");

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
			zero.compareToValue(zero);

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
		FieldValue list = FieldValueUtil.create(TypeInfos.CATEGORICAL_STRING, Arrays.asList("1", "2", "3"));

		assertEquals(DataType.STRING, list.getDataType());
		assertEquals(OpType.CATEGORICAL, list.getOpType());
	}

	@Test
	public void ordinalString(){
		OrdinalValue loud = (OrdinalValue)FieldValueUtil.create(TypeInfos.ORDINAL_STRING, "loud");
		OrdinalValue louder = (OrdinalValue)FieldValueUtil.create(TypeInfos.ORDINAL_STRING, "louder");
		OrdinalValue insane = (OrdinalValue)FieldValueUtil.create(TypeInfos.ORDINAL_STRING, "insane");

		assertFalse(louder.equalsString("loud"));
		assertTrue(louder.equalsString("louder"));
		assertFalse(louder.equalsString("insane"));

		assertFalse(louder.equalsValue(loud));
		assertTrue(louder.equalsValue(louder));
		assertFalse(louder.equalsValue(insane));

		// Implicit (ie. lexicographic) ordering
		assertNull(loud.getOrdering());

		assertTrue(louder.compareToString("loud") > 0);
		assertTrue(louder.compareToString("louder") == 0);
		assertTrue(louder.compareToString("insane") > 0);

		assertTrue(louder.compareTo(loud) > 0);
		assertTrue(louder.compareTo(louder) == 0);
		assertTrue(louder.compareTo(insane) > 0);

		TypeInfo typeInfo = new SimpleTypeInfo(DataType.STRING, OpType.ORDINAL, Arrays.asList("loud", "louder", "insane"));

		loud = (OrdinalValue)FieldValueUtil.create(typeInfo, loud.getValue());
		louder = (OrdinalValue)FieldValueUtil.create(typeInfo, louder.getValue());
		insane = (OrdinalValue)FieldValueUtil.create(typeInfo, insane.getValue());

		// Explicit ordering
		assertNotNull(loud.getOrdering());

		assertTrue(louder.compareToString("loud") > 0);
		assertTrue(louder.compareToString("louder") == 0);
		assertTrue(louder.compareToString("insane") < 0);

		assertTrue(louder.compareTo(loud) > 0);
		assertTrue(louder.compareTo(louder) == 0);
		assertTrue(louder.compareTo(insane) < 0);
	}

	@Test
	public void continuousInteger(){
		FieldValue zero = FieldValueUtil.create(TypeInfos.CONTINUOUS_INTEGER, 0);
		FieldValue one = FieldValueUtil.create(TypeInfos.CONTINUOUS_INTEGER, 1);

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

		assertTrue(zero.compareToValue(zero) == 0);
		assertTrue(zero.compareToValue(one) < 0);

		try {
			TypeInfo typeInfo = new SimpleTypeInfo(zero.getDataType(), OpType.CATEGORICAL);

			FieldValue categoricalZero = zero.cast(typeInfo);

			((ScalarValue)zero).compareTo((ScalarValue)categoricalZero);

			fail();
		} catch(ClassCastException cce){
			// Ignored
		}

		try {
			TypeInfo typeInfo = new SimpleTypeInfo(DataType.DOUBLE, zero.getOpType());

			FieldValue doubleZero = zero.cast(typeInfo);

			((ScalarValue)zero).compareTo((ScalarValue)doubleZero);

			fail();
		} catch(ClassCastException cce){
			// Ignored
		}

		assertTrue(one.compareToValue(zero) > 0);
		assertTrue(one.compareToValue(one) == 0);
	}

	@Test
	public void continuousIntegerList(){
		FieldValue list = FieldValueUtil.create(TypeInfos.CONTINUOUS_INTEGER, Arrays.asList(1, 2, 3));

		assertEquals(DataType.INTEGER, list.getDataType());
		assertEquals(OpType.CONTINUOUS, list.getOpType());
	}

	@Test
	public void categoricalInteger(){
		FieldValue zero = FieldValueUtil.create(TypeInfos.CATEGORICAL_INTEGER, 0);

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
			zero.compareToValue(zero);

			fail();
		} catch(EvaluationException ee){
			// Ignored
		}
	}

	@Test
	public void ordinalInteger(){
		OrdinalValue zero = (OrdinalValue)FieldValueUtil.create(DataType.INTEGER, OpType.ORDINAL, 0);
		OrdinalValue one = (OrdinalValue)FieldValueUtil.create(DataType.INTEGER, OpType.ORDINAL, 1);

		assertTrue(zero.equalsString("0"));
		assertTrue(zero.equalsString("0.0"));

		assertTrue(zero.compareToString("-1") > 0);
		assertTrue(zero.compareToString("0") == 0);
		assertTrue(zero.compareToString("1") < 0);

		assertTrue(zero.compareTo(zero) == 0);
		assertTrue(zero.compareTo(one) < 0);

		assertTrue(one.compareTo(zero) > 0);
		assertTrue(one.compareTo(one) == 0);

		TypeInfo typeInfo = new SimpleTypeInfo(DataType.INTEGER, OpType.ORDINAL, Arrays.asList(1, 0));

		zero = (OrdinalValue)FieldValueUtil.create(typeInfo, zero.getValue());

		assertTrue(zero.compareTo(zero) == 0);
		assertTrue(zero.compareTo(one) > 0);

		one = (OrdinalValue)FieldValueUtil.create(typeInfo, one.getValue());

		assertTrue(one.compareTo(zero) < 0);
		assertTrue(one.compareTo(one) == 0);
	}

	@Test
	public void continuousFloat(){
		Float negativeZeroValue = Float.valueOf(-0f);
		Float positiveZeroValue = Float.valueOf(+0f);

		assertTrue(negativeZeroValue.floatValue() == positiveZeroValue.floatValue());

		assertFalse((negativeZeroValue).equals(positiveZeroValue));
		assertTrue((negativeZeroValue).compareTo(positiveZeroValue) < 0);

		FieldValue negativeZero = FieldValueUtil.create(TypeInfos.CONTINUOUS_FLOAT, negativeZeroValue);
		FieldValue positiveZero = FieldValueUtil.create(TypeInfos.CONTINUOUS_FLOAT, positiveZeroValue);

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

		assertTrue(negativeZero.compareToValue(positiveZero) == 0);
	}

	@Test
	public void continuousFloatList(){
		FieldValue list = FieldValueUtil.create(TypeInfos.CONTINUOUS_FLOAT, Arrays.asList(1f, 2f, 3f));

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

		FieldValue negativeZero = FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, negativeZeroValue);
		FieldValue positiveZero = FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, positiveZeroValue);

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

		assertTrue(negativeZero.compareToValue(positiveZero) == 0);
	}

	@Test
	public void continuousDoubleList(){
		FieldValue list = FieldValueUtil.create(TypeInfos.CONTINUOUS_DOUBLE, Arrays.asList(1d, 2d, 3d));

		assertEquals(DataType.DOUBLE, list.getDataType());
		assertEquals(OpType.CONTINUOUS, list.getOpType());
	}

	@Test
	public void categoricalBoolean(){
		FieldValue zero = FieldValueUtil.create(TypeInfos.CATEGORICAL_BOOLEAN, false);
		FieldValue one = FieldValueUtil.create(TypeInfos.CATEGORICAL_BOOLEAN, true);

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

		assertTrue(zero.compareToValue(zero) == 0);
		assertTrue(zero.compareToValue(one) < 0);

		assertTrue(one.compareToValue(zero) > 0);
		assertTrue(one.compareToValue(one) == 0);
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