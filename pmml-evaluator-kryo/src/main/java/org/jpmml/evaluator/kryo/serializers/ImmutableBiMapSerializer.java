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
package org.jpmml.evaluator.kryo.serializers;

import java.util.HashMap;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import org.jpmml.evaluator.kryo.PreHashedValue;

public class ImmutableBiMapSerializer extends Serializer<ImmutableBiMap<Object, ?>> {

	public ImmutableBiMapSerializer(){
		super(ImmutableBiMapSerializer.DOES_NOT_ACCEPT_NULL, ImmutableBiMapSerializer.IMMUTABLE);
	}

	@Override
	public void write(Kryo kryo, Output output, ImmutableBiMap<Object, ?> immutableBiMap) {
		kryo.writeObject(output, Maps.newHashMap(immutableBiMap));
	}

	@Override
	public ImmutableBiMap<Object, Object> read(Kryo kryo, Input input, Class<? extends ImmutableBiMap<Object, ?>> clazz){
		Map<?, ?> map = kryo.readObject(input, HashMap.class);

		return ImmutableBiMap.copyOf(map);
	}

	static
	public void registerSerializers(Kryo kryo){
		ImmutableBiMapSerializer serializer = new ImmutableBiMapSerializer();

		kryo.register(ImmutableBiMap.class, serializer);

		// empty
		kryo.register(ImmutableBiMap.of().getClass(), serializer);

		Object key1 = new Object();
		Object value1 = new Object();

		// singleton
		kryo.register(ImmutableBiMap.of(key1, value1).getClass(), serializer);

		Object key2 = new Object();
		Object value2 = new Object();

		// regular
		kryo.register(ImmutableBiMap.of(key1, value1, key2, value2).getClass(), serializer);

		ImmutableBiMap.Builder<Object, Object> jdkBiMapBuilder = ImmutableBiMap.builder();

		for(int i = 0; i <= 1024; i++){
			jdkBiMapBuilder.put(new PreHashedValue(0, i), new PreHashedValue(1, String.valueOf(i)));
		}

		ImmutableBiMap<?, ?> jdkBiMap = jdkBiMapBuilder.build();

		// regular, JDK-backed
		kryo.register(jdkBiMap.getClass(), serializer);
	}

	private static final boolean DOES_NOT_ACCEPT_NULL = true;
	private static final boolean IMMUTABLE = true;
}