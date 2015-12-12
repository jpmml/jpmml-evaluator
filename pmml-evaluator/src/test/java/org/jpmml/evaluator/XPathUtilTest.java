/*
 * Copyright (c) 2014 Villu Ruusmann
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

import java.lang.reflect.Field;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Value;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class XPathUtilTest {

	@Test
	public void formatXPath() throws Exception {
		DataField dataField = new DataField(FieldName.create("x"), OpType.CATEGORICAL, DataType.DOUBLE)
			.addValues(new Value("0"), new Value("1"));

		assertEquals("DataField", XPathUtil.formatXPath(dataField));

		Field valuesField = DataField.class.getDeclaredField("values");

		assertEquals("DataField/Value", XPathUtil.formatXPath(dataField, valuesField));

		Field isCyclicField = DataField.class.getDeclaredField("cyclic");

		assertEquals("DataField@isCyclic", XPathUtil.formatXPath(dataField, isCyclicField));

		assertEquals("DataField@isCyclic", XPathUtil.formatXPath(dataField, isCyclicField, null));
		assertEquals("DataField@isCyclic=0", XPathUtil.formatXPath(dataField, isCyclicField, "0"));
	}
}