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
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UnsupportedFeatureExceptionTest {

	@Test
	public void getMessage(){
		DataField dataField = new DataField(new FieldName("x"), OpType.CONTINUOUS, DataType.DOUBLE);

		assertEquals("DataField", new UnsupportedFeatureException(dataField).getMessage());

		assertEquals("DataField@isCyclic", new UnsupportedFeatureException(dataField, "cyclic").getMessage());

		assertEquals("DataField@isCyclic=0", new UnsupportedFeatureException(dataField, DataField.Cyclic.ZERO).getMessage());
		assertEquals("DataField@isCyclic", new UnsupportedFeatureException(dataField, "cyclic", null).getMessage());
		assertEquals("DataField@isCyclic=0", new UnsupportedFeatureException(dataField, "cyclic", "0").getMessage());
	}
}