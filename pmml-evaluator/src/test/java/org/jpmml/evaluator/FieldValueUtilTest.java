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
import org.dmg.pmml.InvalidValueTreatmentMethod;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutlierTreatmentMethod;
import org.dmg.pmml.Value;
import org.dmg.pmml.Value.Property;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FieldValueUtilTest {

	@Test
	public void prepareContinuousInputValue(){
		FieldName name = FieldName.create("x");

		DataField dataField = new DataField(name, OpType.CONTINUOUS, DataType.DOUBLE);

		MiningField miningField = new MiningField(name);

		assertEquals(1d, prepare(dataField, miningField, "1"));
		assertEquals(1d, prepare(dataField, miningField, 1));
		assertEquals(1d, prepare(dataField, miningField, 1f));
		assertEquals(1d, prepare(dataField, miningField, 1d));

		try {
			prepare(dataField, miningField, "one");

			fail();
		} catch(InvalidResultException ire){
			// Ignored
		}

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethod.AS_IS);

		try {
			prepare(dataField, miningField, "one");

			fail();
		} catch(NumberFormatException nfe){
			// Ignored
		}

		Value missingValue = createValue("N/A", Property.MISSING);

		dataField.addValues(missingValue);

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethod.AS_MISSING);

		assertEquals(null, prepare(dataField, miningField, null));
		assertEquals(null, prepare(dataField, miningField, "N/A"));

		assertEquals(null, prepare(dataField, miningField, "one"));

		miningField.setMissingValueReplacement("0");

		assertEquals(0d, prepare(dataField, miningField, null));
		assertEquals(0d, prepare(dataField, miningField, "N/A"));

		assertEquals(0d, prepare(dataField, miningField, "one"));

		miningField.setOutlierTreatment(OutlierTreatmentMethod.AS_IS)
			.setLowValue(1d)
			.setHighValue(3d);

		assertEquals(-1d, prepare(dataField, miningField, -1d));
		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(5d, prepare(dataField, miningField, 5d));

		miningField.setOutlierTreatment(OutlierTreatmentMethod.AS_MISSING_VALUES);

		assertEquals(0d, prepare(dataField, miningField, -1d));
		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(0d, prepare(dataField, miningField, 5d));

		miningField.setOutlierTreatment(OutlierTreatmentMethod.AS_EXTREME_VALUES);

		assertEquals(1d, prepare(dataField, miningField, -1d));
		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(3d, prepare(dataField, miningField, 5d));

		miningField.setOutlierTreatment(null)
			.setLowValue(null)
			.setHighValue(null);

		Interval validInterval = new Interval(Closure.CLOSED_CLOSED)
			.setLeftMargin(1d)
			.setRightMargin(3d);

		dataField.addIntervals(validInterval);

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethod.RETURN_INVALID);

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

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethod.AS_MISSING);

		assertEquals(0d, prepare(dataField, miningField, -1d));
		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(0d, prepare(dataField, miningField, 5d));

		clearIntervalsAndValues(dataField);

		dataField.addValues(missingValue, createValue("1", Value.Property.VALID), createValue("2", Value.Property.VALID), createValue("3", Value.Property.VALID));

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethod.AS_IS);

		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(5d, prepare(dataField, miningField, 5d));

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethod.AS_MISSING);

		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(0d, prepare(dataField, miningField, 5d));

		clearIntervalsAndValues(dataField);

		dataField.addValues(missingValue, createValue("1", Value.Property.INVALID));

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethod.AS_IS);

		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(5d, prepare(dataField, miningField, 5d));

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethod.AS_MISSING);

		assertEquals(0d, prepare(dataField, miningField, 1d));
		assertEquals(5d, prepare(dataField, miningField, 5d));
	}

	@Test
	public void prepareCategoricalInputValue(){
		FieldName name = FieldName.create("x");

		DataField dataField = new DataField(name, OpType.CATEGORICAL, DataType.INTEGER);

		MiningField miningField = new MiningField(name);

		assertEquals(1, prepare(dataField, miningField, "1"));
		assertEquals(1, prepare(dataField, miningField, 1));

		try {
			prepare(dataField, miningField, "one");

			fail();
		} catch(InvalidResultException ire){
			// Ignored
		}

		try {
			prepare(dataField, miningField, 1.5d);

			fail();
		} catch(InvalidResultException ire){
			// Ignored
		}

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethod.AS_IS);

		try {
			prepare(dataField, miningField, "one");

			fail();
		} catch(NumberFormatException nfe){
			// Ignored
		}

		try {
			prepare(dataField, miningField, 1.5d);

			fail();
		} catch(TypeCheckException tce){
			// Ignored
		}

		Value missingValue = createValue("-999", Property.MISSING);

		dataField.addValues(missingValue);

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethod.AS_MISSING);

		assertEquals(null, prepare(dataField, miningField, null));
		assertEquals(null, prepare(dataField, miningField, "-999"));
		assertEquals(null, prepare(dataField, miningField, -999));

		assertEquals(null, prepare(dataField, miningField, "one"));
		assertEquals(null, prepare(dataField, miningField, 1.5d));

		miningField.setMissingValueReplacement("0");

		assertEquals(0, prepare(dataField, miningField, null));
		assertEquals(0, prepare(dataField, miningField, "-999"));
		assertEquals(0, prepare(dataField, miningField, -999));

		assertEquals(0, prepare(dataField, miningField, "one"));
		assertEquals(0, prepare(dataField, miningField, 1.5d));
	}

	static
	private Object prepare(DataField dataField, MiningField miningField, Object value){
		FieldValue result = FieldValueUtil.prepareInputValue(dataField, miningField, value);

		return FieldValueUtil.getValue(result);
	}

	static
	private void clearIntervalsAndValues(DataField dataField){
		List<Interval> intervals = dataField.getIntervals();
		intervals.clear();

		List<Value> values = dataField.getValues();
		values.clear();
	}

	static
	private Value createValue(String value, Value.Property property){
		Value result = new Value(value)
			.setProperty(property);

		return result;
	}
}