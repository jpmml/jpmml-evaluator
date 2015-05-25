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

import java.util.List;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Interval;
import org.dmg.pmml.Interval.Closure;
import org.dmg.pmml.InvalidValueTreatmentMethodType;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutlierTreatmentMethodType;
import org.dmg.pmml.Value;
import org.dmg.pmml.Value.Property;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ArgumentUtilTest {

	@Test
	public void prepare(){
		FieldName name = new FieldName("x");

		DataField dataField = new DataField(name, OpType.CONTINUOUS, DataType.DOUBLE);

		MiningField miningField = new MiningField(name);

		assertEquals(1d, prepare(dataField, miningField, "1"));
		assertEquals(1d, prepare(dataField, miningField, 1));
		assertEquals(1d, prepare(dataField, miningField, 1f));
		assertEquals(1d, prepare(dataField, miningField, 1d));

		Value missingValue = createValue("N/A", Property.MISSING);

		dataField.addValues(missingValue);

		assertEquals(null, prepare(dataField, miningField, null));
		assertEquals(null, prepare(dataField, miningField, "N/A"));

		miningField.setMissingValueReplacement("0");

		assertEquals(0d, prepare(dataField, miningField, null));
		assertEquals(0d, prepare(dataField, miningField, "N/A"));

		Interval validInterval = new Interval(Closure.CLOSED_CLOSED)
			.setLeftMargin(1d)
			.setRightMargin(3d);

		dataField.addIntervals(validInterval);

		miningField.setOutlierTreatment(OutlierTreatmentMethodType.AS_IS)
			.setInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_IS);

		assertEquals(-1d, prepare(dataField, miningField, -1d));
		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(5d, prepare(dataField, miningField, 5d));

		miningField.setOutlierTreatment(OutlierTreatmentMethodType.AS_MISSING_VALUES)
			.setInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_IS);

		assertEquals(0d, prepare(dataField, miningField, -1d));
		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(0d, prepare(dataField, miningField, 5d));

		miningField.setOutlierTreatment(OutlierTreatmentMethodType.AS_EXTREME_VALUES)
			.setInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_IS)
			.setLowValue(1d)
			.setHighValue(3d);

		assertEquals(1d, prepare(dataField, miningField, -1d));
		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(3d, prepare(dataField, miningField, 5d));

		miningField.setOutlierTreatment(OutlierTreatmentMethodType.AS_MISSING_VALUES)
			.setInvalidValueTreatment(InvalidValueTreatmentMethodType.RETURN_INVALID)
			.setLowValue(null)
			.setHighValue(null);

		try {
			prepare(dataField, miningField, -1d);

			fail();
		} catch(InvalidResultException ire){
			// Ignored
		}

		assertEquals(1d, prepare(dataField, miningField, 1d));

		try {
			prepare(dataField, miningField, 5d);

			fail();
		} catch(InvalidResultException ire){
			// Ignored
		}

		miningField.setOutlierTreatment(OutlierTreatmentMethodType.AS_IS)
			.setInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_MISSING);

		assertEquals(0d, prepare(dataField, miningField, -1d));
		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(0d, prepare(dataField, miningField, 5d));

		dataField = clearIntervaldAndValues(dataField);

		dataField.addValues(missingValue, createValue("1", Value.Property.VALID), createValue("2", Value.Property.VALID), createValue("3", Value.Property.VALID));

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_IS);

		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(5d, prepare(dataField, miningField, 5d));

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_MISSING);

		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(0d, prepare(dataField, miningField, 5d));

		dataField = clearIntervaldAndValues(dataField);

		dataField.addValues(missingValue, createValue("1", Value.Property.INVALID));

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_IS);

		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(5d, prepare(dataField, miningField, 5d));

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_MISSING);

		assertEquals(0d, prepare(dataField, miningField, 1d));
		assertEquals(5d, prepare(dataField, miningField, 5d));
	}

	@Test
	public void isOutlier(){
		FieldName name = new FieldName("x");

		DataField dataField = new DataField(name, OpType.CONTINUOUS, DataType.DOUBLE)
			.addIntervals(
				createInterval(Interval.Closure.CLOSED_CLOSED, -10d, -1d),
				createInterval(Interval.Closure.CLOSED_CLOSED, 1d, 10d)
			);

		MiningField miningField = new MiningField(name);

		assertTrue(ArgumentUtil.isOutlier(dataField, miningField, -15d));
		assertFalse(ArgumentUtil.isOutlier(dataField, miningField, 0d));
		assertTrue(ArgumentUtil.isOutlier(dataField, miningField, 15d));
	}

	@Test
	public void isInvalid(){
		assertFalse(ArgumentUtil.isInvalid(null, null, null));
	}

	@Test
	public void isValid(){
		assertFalse(ArgumentUtil.isValid(null, null, null));
	}

	static
	private Object prepare(DataField dataField, MiningField miningField, Object value){
		FieldValue result = ArgumentUtil.prepare(dataField, miningField, value);

		return FieldValueUtil.getValue(result);
	}

	static
	private DataField clearIntervaldAndValues(DataField dataField){
		List<Interval> intervals = dataField.getIntervals();
		intervals.clear();

		List<Value> values = dataField.getValues();
		values.clear();

		return dataField;
	}

	static
	private Interval createInterval(Interval.Closure closure, Double leftMargin, Double rightMargin){
		Interval result = new Interval(closure)
			.setLeftMargin(leftMargin)
			.setRightMargin(rightMargin);

		return result;
	}

	static
	private Value createValue(String value, Value.Property property){
		Value result = new Value(value)
			.setProperty(property);

		return result;
	}
}