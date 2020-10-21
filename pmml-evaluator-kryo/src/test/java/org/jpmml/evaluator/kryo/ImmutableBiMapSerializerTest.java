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

import com.esotericsoftware.kryo.Kryo;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ImmutableBiMapSerializerTest {

	private Kryo kryo = null;


	@Before
	public void setUp(){
		Kryo kryo = new Kryo();

		KryoUtil.init(kryo);
		KryoUtil.register(kryo);

		this.kryo = kryo;
	}

	@Test
	public void kryoClone(){
		Map<Integer, String> map = new LinkedHashMap<>();

		ImmutableBiMap<Integer, String> emptyBiMap = ImmutableBiMap.copyOf(map);

		ImmutableBiMap<Integer, String> clonedEmptyBiMap = KryoUtil.clone(this.kryo, emptyBiMap);

		assertEquals(emptyBiMap, clonedEmptyBiMap);

		map.put(0, "zero");

		ImmutableBiMap<Integer, String> singletonBiMap = ImmutableBiMap.copyOf(map);

		ImmutableBiMap<Integer, String> clonedSingletonBiMap = KryoUtil.clone(this.kryo, singletonBiMap);

		assertNotEquals(emptyBiMap.getClass(), singletonBiMap.getClass());

		assertEquals(singletonBiMap, clonedSingletonBiMap);

		map.put(1, "one");

		ImmutableMap<Integer, String> doubletonBiMap = ImmutableBiMap.copyOf(map);

		ImmutableMap<Integer, String> clonedDoubletonBiMap = KryoUtil.clone(this.kryo, doubletonBiMap);

		assertEquals(emptyBiMap.getClass(), doubletonBiMap.getClass());
		assertNotEquals(singletonBiMap.getClass(), doubletonBiMap.getClass());

		assertEquals(doubletonBiMap, clonedDoubletonBiMap);

		map.put(2, "two");

		ImmutableBiMap<Integer, String> tripletonBiMap = ImmutableBiMap.copyOf(map);

		ImmutableBiMap<Integer, String> clonedTripletonBiMap = KryoUtil.clone(this.kryo, tripletonBiMap);

		assertEquals(emptyBiMap.getClass(), tripletonBiMap.getClass());
		assertEquals(doubletonBiMap.getClass(), tripletonBiMap.getClass());

		assertEquals(tripletonBiMap, clonedTripletonBiMap);

		Map<Object, Object> jdkMap = new LinkedHashMap<>();

		for(int i = 0; i <= 1024; i++){
			jdkMap.put(new PreHashedValue(0, i), new PreHashedValue(1, String.valueOf(i)));
		}

		ImmutableBiMap<Object, Object> jdkBiMap = ImmutableBiMap.copyOf(jdkMap);

		ImmutableBiMap<Object, Object> clonedJdkBiMap = KryoUtil.clone(this.kryo, jdkBiMap);

		assertNotEquals(emptyBiMap.getClass(), jdkBiMap.getClass());
		assertNotEquals(singletonBiMap.getClass(), jdkBiMap.getClass());

		assertEquals(jdkBiMap, clonedJdkBiMap);
	}
}