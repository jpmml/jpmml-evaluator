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

import java.util.*;

import org.dmg.pmml.*;
import org.dmg.pmml.Interval.Closure;
import org.dmg.pmml.Value.Property;

import org.junit.*;

import com.google.common.collect.*;

import static org.junit.Assert.*;

public class ArgumentUtilTest {

	@Test
	public void prepare(){
		FieldName name = new FieldName("x");

		DataField dataField = new DataField(name, OpType.CONTINUOUS, DataType.DOUBLE);

		List<Value> fieldValues = dataField.getValues();
		List<Interval> fieldIntervals = dataField.getIntervals();

		MiningField miningField = new MiningField(name);

		miningField.setLowValue(1d);
		miningField.setHighValue(3d);

		assertEquals(1d, prepare(dataField, miningField, "1"));
		assertEquals(1d, prepare(dataField, miningField, 1));
		assertEquals(1d, prepare(dataField, miningField, 1f));
		assertEquals(1d, prepare(dataField, miningField, 1d));

		Value missingValue = createValue("N/A", Property.MISSING);

		fieldValues.add(missingValue);

		assertEquals(null, prepare(dataField, miningField, null));
		assertEquals(null, prepare(dataField, miningField, "N/A"));

		miningField.setMissingValueReplacement("0");

		assertEquals(0d, prepare(dataField, miningField, null));
		assertEquals(0d, prepare(dataField, miningField, "N/A"));

		fieldValues.clear();
		fieldIntervals.clear();

		fieldValues.add(missingValue);

		Interval validInterval = new Interval(Closure.CLOSED_CLOSED);
		validInterval.setLeftMargin(1d);
		validInterval.setRightMargin(3d);

		fieldIntervals.add(validInterval);

		miningField.setOutlierTreatment(OutlierTreatmentMethodType.AS_IS);
		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_IS);

		assertEquals(-1d, prepare(dataField, miningField, -1d));
		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(5d, prepare(dataField, miningField, 5d));

		miningField.setOutlierTreatment(OutlierTreatmentMethodType.AS_EXTREME_VALUES);
		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_IS);

		assertEquals(1d, prepare(dataField, miningField, -1d));
		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(3d, prepare(dataField, miningField, 5d));

		miningField.setOutlierTreatment(OutlierTreatmentMethodType.AS_IS);
		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_MISSING);

		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(0d, prepare(dataField, miningField, 5d));

		fieldValues.clear();
		fieldIntervals.clear();

		List<Value> validValues = Lists.newArrayList();
		validValues.add(createValue("1", Value.Property.VALID));
		validValues.add(createValue("2", Value.Property.VALID));
		validValues.add(createValue("3", Value.Property.VALID));

		fieldValues.add(missingValue);
		fieldValues.addAll(validValues);

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_IS);

		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(5d, prepare(dataField, miningField, 5d));

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_MISSING);

		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(0d, prepare(dataField, miningField, 5d));

		fieldValues.clear();
		fieldIntervals.clear();

		List<Value> invalidValues = Lists.newArrayList();
		invalidValues.add(createValue("1", Value.Property.INVALID));

		fieldValues.add(missingValue);
		fieldValues.addAll(invalidValues);

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_IS);

		assertEquals(1d, prepare(dataField, miningField, 1d));
		assertEquals(5d, prepare(dataField, miningField, 5d));

		miningField.setInvalidValueTreatment(InvalidValueTreatmentMethodType.AS_MISSING);

		assertEquals(0d, prepare(dataField, miningField, 1d));
		assertEquals(5d, prepare(dataField, miningField, 5d));
	}

	@Test
	public void isInvalid(){
		assertFalse(ArgumentUtil.isInvalid(null, null));
	}

	@Test
	public void isValid(){
		assertFalse(ArgumentUtil.isValid(null, null));
	}

	static
	private Object prepare(DataField dataField, MiningField miningField, Object value){
		FieldValue result = ArgumentUtil.prepare(dataField, miningField, value);

		return FieldValueUtil.getValue(result);
	}

	static
	private Value createValue(String value, Value.Property property){
		Value result = new Value(value);
		result.setProperty(property);

		return result;
	}
}