/*
 * Copyright (c) 2025 Villu Ruusmann
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
package org.jpmml.evaluator.kryo;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import org.jpmml.model.kryo.KryoSerializer;
import org.junit.jupiter.api.Test;

public class TableTest extends KryoSerializerTest {

	@Test
	public void guavaTable() throws Exception {
		KryoSerializer kryoSerializer = new KryoSerializer(super.kryo);

		Map<Integer, String> map = new LinkedHashMap<>();
		map.put(0, "zero");
		map.put(1, "one");
		map.put(2, "two");

		Table<Integer, Integer, String> table = ArrayTable.create(map.keySet(), Collections.singleton(0));

		Collection<Map.Entry<Integer, String>> entries = map.entrySet();
		for(Map.Entry<Integer, String> entry : entries){
			table.put(entry.getKey(), 0, entry.getValue());
		}

		checkedCloneRaw(kryoSerializer, ImmutableTable.copyOf(table));
	}
}