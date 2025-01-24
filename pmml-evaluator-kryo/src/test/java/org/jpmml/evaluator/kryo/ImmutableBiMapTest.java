/*
 * Copyright (c) 2020 Villu Ruusmann
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

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import org.jpmml.model.kryo.KryoSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ImmutableBiMapTest extends KryoSerializerTest {

	@Test
	public void guavaBiMaps() throws Exception {
		KryoSerializer kryoSerializer = new KryoSerializer(super.kryo);

		Map<Integer, String> map = new LinkedHashMap<>();

		ImmutableBiMap<Integer, String> emptyBiMap = ImmutableBiMap.copyOf(map);

		ImmutableBiMap<Integer, String> clonedEmptyBiMap = cloneRaw(kryoSerializer, emptyBiMap);

		assertSame(emptyBiMap, clonedEmptyBiMap);

		map.put(0, "zero");

		ImmutableBiMap<Integer, String> singletonBiMap = ImmutableBiMap.copyOf(map);

		assertNotEquals(emptyBiMap.getClass(), singletonBiMap.getClass());

		checkedCloneRaw(kryoSerializer, singletonBiMap);

		map.put(1, "one");

		ImmutableMap<Integer, String> doubletonBiMap = ImmutableBiMap.copyOf(map);

		assertEquals(emptyBiMap.getClass(), doubletonBiMap.getClass());
		assertNotEquals(singletonBiMap.getClass(), doubletonBiMap.getClass());

		checkedCloneRaw(kryoSerializer, doubletonBiMap);

		map.put(2, "two");

		ImmutableBiMap<Integer, String> tripletonBiMap = ImmutableBiMap.copyOf(map);

		assertEquals(emptyBiMap.getClass(), tripletonBiMap.getClass());
		assertEquals(doubletonBiMap.getClass(), tripletonBiMap.getClass());

		checkedCloneRaw(kryoSerializer, tripletonBiMap);

		Map<Object, Object> jdkMap = new LinkedHashMap<>();

		for(int i = 0; i <= 1024; i++){
			jdkMap.put(new PreHashedValue(0, i), new PreHashedValue(1, String.valueOf(i)));
		}

		ImmutableBiMap<Object, Object> jdkBiMap = ImmutableBiMap.copyOf(jdkMap);

		assertNotEquals(emptyBiMap.getClass(), jdkBiMap.getClass());
		assertNotEquals(singletonBiMap.getClass(), jdkBiMap.getClass());

		checkedCloneRaw(kryoSerializer, jdkBiMap);
	}
}