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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.google.common.collect.ArrayTable;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import org.junit.Before;
import org.junit.Test;

public class KryoUtilTest {

	private Kryo kryo = null;


	@Before
	public void setUp(){
		Kryo kryo = new Kryo();

		KryoUtil.init(kryo);
		KryoUtil.register(kryo);

		this.kryo = kryo;
	}

	@Test
	public void javaCollections(){
		Map<Integer, String> map = new LinkedHashMap<>();
		map.put(0, "zero");
		map.put(1, "one");
		map.put(2, "two");

		KryoUtil.clone(this.kryo, (Serializable)Collections.unmodifiableSet(new HashSet<>(map.keySet())));
		KryoUtil.clone(this.kryo, (Serializable)Collections.unmodifiableSet(new LinkedHashSet<>(map.keySet())));
		KryoUtil.clone(this.kryo, (Serializable)Collections.unmodifiableList(new ArrayList<>(map.values())));
		KryoUtil.clone(this.kryo, (Serializable)Collections.unmodifiableMap(new HashMap<>(map)));
		KryoUtil.clone(this.kryo, (Serializable)Collections.unmodifiableMap(new LinkedHashMap<>(map)));
	}

	@Test
	public void guavaCollections(){
		Map<Integer, String> map = new LinkedHashMap<>();
		map.put(0, "zero");
		map.put(1, "one");
		map.put(2, "two");

		KryoUtil.clone(this.kryo, ImmutableSet.copyOf(map.keySet()));
		KryoUtil.clone(this.kryo, ImmutableList.copyOf(new ArrayList<>(map.values())));
		KryoUtil.clone(this.kryo, ImmutableMap.copyOf(map));
		KryoUtil.clone(this.kryo, ImmutableBiMap.copyOf(map));

		KryoUtil.clone(this.kryo, ImmutableListMultimap.copyOf(map.entrySet()));
		KryoUtil.clone(this.kryo, ImmutableSetMultimap.copyOf(map.entrySet()));

		Table<Integer, Integer, String> table = ArrayTable.create(map.keySet(), Collections.singleton(0));

		Collection<Map.Entry<Integer, String>> entries = map.entrySet();
		for(Map.Entry<Integer, String> entry : entries){
			table.put(entry.getKey(), 0, entry.getValue());
		}

		KryoUtil.clone(this.kryo, ImmutableTable.copyOf(table));
	}
}