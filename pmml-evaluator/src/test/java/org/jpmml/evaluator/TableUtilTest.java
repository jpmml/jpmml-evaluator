/*
 * Copyright (c) 2024 Villu Ruusmann
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TableUtilTest {

	@Test
	public void get(){
		List<String> values = new ArrayList<>();

		assertNull(TableUtil.get(values, 0));
		assertNull(TableUtil.get(values, 1));

		values.add("first");

		assertNotNull(TableUtil.get(values, 0));
		assertNull(TableUtil.get(values, 1));
	}

	@Test
	public void set(){
		List<String> values = new ArrayList<>();

		TableUtil.set(values, 2, "third");

		assertEquals(Arrays.asList(null, null, "third"), values);

		TableUtil.set(values, 0, "first");

		assertEquals(Arrays.asList("first", null, "third"), values);

		TableUtil.set(values, 1, "second");
		TableUtil.set(values, 3, "fourth");

		assertEquals(Arrays.asList("first", "second", "third", "fourth"), values);

		TableUtil.set(values, 3, null);

		assertEquals(Arrays.asList("first", "second", "third", null), values);
	}
}