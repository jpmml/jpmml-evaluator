/*
 * Copyright (c) 2021 Villu Ruusmann
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

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FieldValueMapTest {

	@Test
	public void jdk8() throws Exception {
		// Assume that unit tests are never run in non-JDK8 compatible environment
		assertTrue(FieldValueMap.JDK8_API);

		assertNotNull(Map.class.getMethod("getOrDefault", Object.class, Object.class));

		try {
			Map.class.getMethod("getOrDefault", Object.class, FieldValue.class);

			fail();
		} catch(ReflectiveOperationException roe){
			// Ignored
		} // End try

		try {
			Map.class.getMethod("getOrDefault", String.class, FieldValue.class);

			fail();
		} catch(ReflectiveOperationException roe){
			// Ignored
		}

		assertNotNull(Map.class.getMethod("putIfAbsent", Object.class, Object.class));

		try {
			Map.class.getMethod("putIfAbsent", String.class, FieldValue.class);

			fail();
		} catch(ReflectiveOperationException roe){
			// Ignored
		}
	}
}