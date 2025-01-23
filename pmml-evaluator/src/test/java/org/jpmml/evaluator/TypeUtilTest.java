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
import org.dmg.pmml.OpType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TypeUtilTest {

	@Test
	public void format(){
		assertEquals("1", TypeUtil.format("1"));

		assertEquals("1", TypeUtil.format((byte)1));
		assertEquals("1", TypeUtil.format((short)1));
		assertEquals("1", TypeUtil.format(1));
		assertEquals("1", TypeUtil.format(1l));
		assertEquals("1.0", TypeUtil.format(1f)); // XXX
		assertEquals("1.0", TypeUtil.format(1.0f));
		assertEquals("1.0", TypeUtil.format(1d)); // XXX
		assertEquals("1.0", TypeUtil.format(1.0d));
	}

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
		} catch(IllegalArgumentException iae){
			// Ignored
		}

		try {
			TypeUtil.parse(DataType.INTEGER, Double.toString(Integer.MIN_VALUE - 1d));

			fail();
		} catch(IllegalArgumentException iae){
			// Ignored
		}

		assertEquals(Integer.MAX_VALUE, TypeUtil.parse(DataType.INTEGER, Integer.toString(Integer.MAX_VALUE)));

		try {
			TypeUtil.parse(DataType.INTEGER, Long.toString(Integer.MAX_VALUE + 1l));

			fail();
		} catch(IllegalArgumentException iae){
			// Ignored
		}

		try {
			TypeUtil.parse(DataType.INTEGER, Double.toString(Integer.MAX_VALUE + 1d));

			fail();
		} catch(IllegalArgumentException iae){
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
		} catch(TypeCheckException tce){
			// Ignored
		}

		try {
			TypeUtil.cast(DataType.INTEGER, Long.valueOf(Integer.MAX_VALUE + 1l));

			fail();
		} catch(TypeCheckException tce){
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
		} catch(TypeCheckException tce){
			// Ignored
		}

		try {
			TypeUtil.cast(DataType.INTEGER, Double.valueOf(Integer.MAX_VALUE + 1d));

			fail();
		} catch(TypeCheckException tce){
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

		assertEquals(DataType.DOUBLE, TypeUtil.getConstantDataType("-1.0"));
		assertEquals(DataType.DOUBLE, TypeUtil.getConstantDataType("1.0"));
		assertEquals(DataType.DOUBLE, TypeUtil.getConstantDataType("+1.0"));
		assertEquals(DataType.DOUBLE, TypeUtil.getConstantDataType("1.0E-1"));
		assertEquals(DataType.DOUBLE, TypeUtil.getConstantDataType("1.0E1"));
		assertEquals(DataType.DOUBLE, TypeUtil.getConstantDataType("1.0E+1"));
		assertEquals(DataType.STRING, TypeUtil.getConstantDataType("1.0X"));
	}

	@Test
	public void getOpType(){
		assertEquals(OpType.ORDINAL, TypeUtil.getOpType(DataType.DATE));
		assertEquals(OpType.ORDINAL, TypeUtil.getOpType(DataType.TIME));
		assertEquals(OpType.ORDINAL, TypeUtil.getOpType(DataType.DATE_TIME));

		assertEquals(OpType.CONTINUOUS, TypeUtil.getOpType(DataType.DATE_DAYS_SINCE_1960));
		assertEquals(OpType.CONTINUOUS, TypeUtil.getOpType(DataType.TIME_SECONDS));
		assertEquals(OpType.CONTINUOUS, TypeUtil.getOpType(DataType.DATE_TIME_SECONDS_SINCE_1960));
	}
}