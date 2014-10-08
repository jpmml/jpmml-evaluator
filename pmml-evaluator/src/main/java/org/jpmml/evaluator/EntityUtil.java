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

import com.google.common.collect.ImmutableBiMap;
import org.dmg.pmml.HasId;
import org.dmg.pmml.PMMLObject;
import org.jpmml.manager.InvalidFeatureException;

public class EntityUtil {

	private EntityUtil(){
	}

	static
	public <E extends PMMLObject & HasId> ImmutableBiMap<String, E> buildBiMap(List<E> entities){
		ImmutableBiMap.Builder<String, E> builder = new ImmutableBiMap.Builder<String, E>();

		builder = putAll(entities, builder);

		return builder.build();
	}

	static
	public <E extends PMMLObject & HasId> ImmutableBiMap.Builder<String, E> put(E entity, ImmutableBiMap.Builder<String, E> builder){
		String id = entity.getId();
		if(id == null){
			throw new InvalidFeatureException(entity);
		}

		return builder.put(id, entity);
	}

	static
	public <E extends PMMLObject & HasId> ImmutableBiMap.Builder<String, E> putAll(List<E> entities, ImmutableBiMap.Builder<String, E> builder){

		for(int i = 0, j = 1; i < entities.size(); i++, j++){
			E entity = entities.get(i);

			String id = entity.getId();

			// Generate an implicit identifier (ie. 1-based index) if the explicit identifier is missing
			if(id == null){
				id = String.valueOf(j);
			} // End if

			builder = builder.put(id, entity);
		}

		return builder;
	}
}