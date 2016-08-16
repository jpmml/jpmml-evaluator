/*
 * Copyright (c) 2013 Villu Ruusmann
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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.dmg.pmml.Entity;

public class EntityUtil {

	private EntityUtil(){
	}

	static
	public <E extends Entity> String getId(E entity, HasEntityRegistry<E> hasEntityRegistry){
		BiMap<String, E> entityRegistry = hasEntityRegistry.getEntityRegistry();

		return getId(entity, entityRegistry);
	}

	static
	public <E extends Entity> String getId(E entity, BiMap<String, E> entityRegistry){
		String id = entity.getId();

		if(id == null){
			BiMap<E, String> inversedEntityRegistry = entityRegistry.inverse();

			return inversedEntityRegistry.get(entity);
		}

		return id;
	}

	static
	public <E extends Entity> ImmutableBiMap<String, E> buildBiMap(List<E> entities){
		ImmutableBiMap.Builder<String, E> builder = new ImmutableBiMap.Builder<>();

		builder = putAll(entities, new AtomicInteger(1), builder);

		return builder.build();
	}

	static
	public <E extends Entity> ImmutableBiMap.Builder<String, E> put(E entity, AtomicInteger index, ImmutableBiMap.Builder<String, E> builder){
		String implicitId = String.valueOf(index.getAndIncrement());

		String id = entity.getId();
		if(id == null){
			id = implicitId;
		}

		return builder.put(id, entity);
	}

	static
	public <E extends Entity> ImmutableBiMap.Builder<String, E> putAll(List<E> entities, AtomicInteger index, ImmutableBiMap.Builder<String, E> builder){

		for(E entity : entities){
			builder = put(entity, index, builder);
		}

		return builder;
	}
}