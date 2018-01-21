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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class XPathUtilTest {

	@Test
	public void formatXPath() throws Exception {
		Class<? extends DataField> dataFieldClazz = DataField.class;

		assertEquals("DataField", XPathUtil.formatElement(dataFieldClazz));

		Field nameField = dataFieldClazz.getDeclaredField("name");

		assertEquals("DataField@name", XPathUtil.formatElementOrAttribute(nameField));

		Field valuesField = dataFieldClazz.getDeclaredField("values");

		assertEquals("DataField/Value", XPathUtil.formatElementOrAttribute(valuesField));

		Field isCyclicField = dataFieldClazz.getDeclaredField("cyclic");

		assertEquals("DataField@isCyclic", XPathUtil.formatAttribute(isCyclicField, null));
		assertEquals("DataField@isCyclic=0", XPathUtil.formatAttribute(isCyclicField, "0"));
	}
}