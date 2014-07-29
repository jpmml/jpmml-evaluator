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
package org.jpmml.manager;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PMMLObjectUtilTest {

	@Test
	public void getElementName(){
		DataField dataField = new DataField();

		assertEquals("DataField", PMMLObjectUtil.getRootElementName(dataField));
	}

	@Test
	public void getAttributeName(){
		DataField dataField = new DataField();

		assertEquals("isCyclic", PMMLObjectUtil.getAttributeName(dataField, "cyclic"));
	}

	@Test
	public void getAttributeValue(){
		DataField dataField = new DataField();

		assertEquals(null, PMMLObjectUtil.getAttributeValue(dataField, "dataType"));

		dataField = dataField.withDataType(DataType.STRING);

		assertEquals(DataType.STRING, PMMLObjectUtil.getAttributeValue(dataField, "dataType"));
	}

	@Test
	public void getValue(){
		assertEquals("0", PMMLObjectUtil.getValue(DataField.Cyclic.ZERO));
		assertEquals("1", PMMLObjectUtil.getValue(DataField.Cyclic.ONE));
	}
}