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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class RichDataFieldTest {

	@Test
	public void getValueMapping(){
		Value invalidValue = createValue("0", Value.Property.INVALID);
		Value validValueOne = createValue("1", null);
		Value validValueTwo = createValue("2", null);
		Value validValueThree = createValue("3", null);
		Value missingValue = createValue("N/A", Value.Property.MISSING);

		DataField dataField = new DataField(FieldName.create("x"), OpType.CATEGORICAL, DataType.INTEGER)
			.addValues(invalidValue, validValueOne, validValueTwo, validValueThree, missingValue);

		RichDataField richDataField = new RichDataField(dataField);

		Map<FieldValue, Value> valueMappings = richDataField.getValueMapping();

		assertEquals(4, valueMappings.size());

		assertSame(invalidValue, valueMappings.get(FieldValueUtil.create(DataType.INTEGER, OpType.CATEGORICAL, 0)));

		assertSame(validValueOne, FieldValueUtil.getValidValue(richDataField, true));
		assertSame(validValueTwo, FieldValueUtil.getValidValue(richDataField, 2d));
		assertSame(validValueThree, FieldValueUtil.getValidValue(richDataField, "3"));

		try {
			valueMappings.get(FieldValueUtil.create(DataType.INTEGER, OpType.CATEGORICAL, "N/A"));

			fail();
		} catch(NumberFormatException nfe){
			// Ignored
		}
	}

	static
	private Value createValue(String value, Value.Property property){
		Value result = new Value(value)
			.setProperty(property);

		return result;
	}
}