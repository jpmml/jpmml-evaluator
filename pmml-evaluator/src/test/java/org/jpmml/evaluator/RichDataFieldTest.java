/*
 * Copyright (c) 2017 Villu Ruusmann
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

import java.util.Map;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Value;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RichDataFieldTest {

	@Test
	public void getValueMapping(){
		Value invalidValue = createValue("0", Value.Property.INVALID);
		Value validValueOne = createValue("1", Value.Property.VALID);
		Value validValueTwo = createValue("2", null);
		Value validValueThree = createValue("3", null);
		Value missingValue = createValue("N/A", Value.Property.MISSING);

		DataField dataField = new DataField(FieldName.create("x"), OpType.CATEGORICAL, DataType.STRING)
			.addValues(invalidValue, validValueOne, validValueTwo, validValueThree, missingValue);

		RichDataField richDataField = new RichDataField(dataField);

		Map<?, Integer> valueMap = richDataField.getMap();

		assertEquals(5, valueMap.size());

		assertEquals(FieldValue.STATUS_UNKNOWN_INVALID, valueMap.get("0"));
		assertEquals((Integer)1, valueMap.get("1"));
		assertEquals((Integer)2, valueMap.get("2"));
		assertEquals((Integer)3, valueMap.get("3"));
		assertEquals(FieldValue.STATUS_MISSING, valueMap.get("N/A"));

		dataField.setDataType(DataType.INTEGER);

		richDataField = new RichDataField(dataField);

		valueMap = richDataField.getMap();

		assertEquals(4, valueMap.size());

		assertEquals(FieldValue.STATUS_UNKNOWN_INVALID, valueMap.get(0));
		assertEquals((Integer)1, valueMap.get(1));
		assertEquals((Integer)2, valueMap.get(2));
		assertEquals((Integer)3, valueMap.get(3));
	}

	static
	private Value createValue(String value, Value.Property property){
		Value result = new Value(value)
			.setProperty(property);

		return result;
	}
}