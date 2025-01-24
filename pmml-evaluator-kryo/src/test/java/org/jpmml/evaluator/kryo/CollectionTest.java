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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import org.jpmml.model.kryo.KryoSerializer;
import org.junit.jupiter.api.Test;

public class CollectionTest extends KryoSerializerTest {

	@Test
	public void javaCollections() throws Exception {
		KryoSerializer kryoSerializer = new KryoSerializer(super.kryo);

		Map<Integer, String> map = new LinkedHashMap<>();
		map.put(0, "zero");
		map.put(1, "one");
		map.put(2, "two");

		checkedCloneRaw(kryoSerializer, Collections.unmodifiableSet(new HashSet<>(map.keySet())));
		checkedCloneRaw(kryoSerializer, Collections.unmodifiableSet(new LinkedHashSet<>(map.keySet())));
		checkedCloneRaw(kryoSerializer, Collections.unmodifiableList(new ArrayList<>(map.values())));

		checkedCloneRaw(kryoSerializer, Collections.unmodifiableMap(new HashMap<>(map)));
		checkedCloneRaw(kryoSerializer, Collections.unmodifiableMap(new LinkedHashMap<>(map)));
	}

	@Test
	public void guavaCollections() throws Exception {
		KryoSerializer kryoSerializer = new KryoSerializer(super.kryo);

		Map<Integer, String> map = new LinkedHashMap<>();
		map.put(0, "zero");
		map.put(1, "one");
		map.put(2, "two");

		checkedCloneRaw(kryoSerializer, ImmutableSet.copyOf(map.keySet()));
		checkedCloneRaw(kryoSerializer, ImmutableList.copyOf(new ArrayList<>(map.values())));

		checkedCloneRaw(kryoSerializer, ImmutableMap.copyOf(map));

		checkedCloneRaw(kryoSerializer, ImmutableListMultimap.copyOf(map.entrySet()));
		checkedCloneRaw(kryoSerializer, ImmutableSetMultimap.copyOf(map.entrySet()));
	}
}