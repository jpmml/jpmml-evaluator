/*
 * Copyright (c) 2015 Villu Ruusmann
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
package org.jpmml.evaluator.visitors;

import java.lang.reflect.Field;
import java.util.List;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.InvalidFeatureException;
import org.jpmml.model.ReflectionUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class InvalidFeatureInspectorTest {

	@Test
	public void inspect() throws Exception {
		DataDictionary dataDictionary = new DataDictionary()
			.setNumberOfFields(1);

		Field field = ReflectionUtil.getField(DataDictionary.class, "dataFields");

		assertNull(ReflectionUtil.getFieldValue(field, dataDictionary));

		List<DataField> dataFields = dataDictionary.getDataFields();
		assertEquals(0, dataFields.size());

		assertNotNull(ReflectionUtil.getFieldValue(field, dataDictionary));

		PMML pmml = new PMML(null, null, dataDictionary);

		InvalidFeatureInspector inspector = new InvalidFeatureInspector();

		try {
			inspector.applyTo(pmml);

			fail();
		} catch(InvalidFeatureException ife){
			List<InvalidFeatureException> exceptions = inspector.getExceptions();

			String[] features = {"PMML@version", "PMML/Header", "DataDictionary", "DataDictionary/DataField"};

			assertEquals(features.length, exceptions.size());

			for(int i = 0; i < exceptions.size(); i++){
				InvalidFeatureException exception = exceptions.get(i);

				assertEquals(features[i], exception.getMessage());
			}

			assertEquals("PMML@version", ife.getMessage());
		}
	}
}