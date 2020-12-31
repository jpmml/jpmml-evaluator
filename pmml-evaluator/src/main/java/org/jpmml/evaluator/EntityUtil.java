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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.dmg.pmml.Entity;
import org.jpmml.model.XPathUtil;

public class EntityUtil {

	private EntityUtil(){
	}

	static
	public <E extends Entity<?>> String getId(E entity, HasEntityRegistry<E> hasEntityRegistry){
		BiMap<String, E> entityRegistry = hasEntityRegistry.getEntityRegistry();

		return getId(entity, entityRegistry);
	}

	static
	public <E extends Entity<?>> String getId(E entity, BiMap<String, E> entityRegistry){
		Object id = entity.getId();

		if(id == null){
			BiMap<E, String> inversedEntityRegistry = entityRegistry.inverse();

			return inversedEntityRegistry.get(entity);
		}

		return TypeUtil.format(id);
	}

	static
	public <E extends Entity<?>> BiMap<String, E> buildBiMap(List<E> entities){
		BiMap<String, E> result = HashBiMap.create(entities.size());

		for(int i = 0; i < entities.size(); i++){
			E entity = entities.get(i);

			String key = String.valueOf(i + 1);

			Object id = entity.getId();
			if(id != null){
				key = TypeUtil.format(id);
			} // End if

			if(result.containsKey(key)){
				throw new InvalidAttributeException(InvalidAttributeException.formatMessage(XPathUtil.formatElement(entity.getClass()) + "@id=" + key), entity);
			}

			result.put(key, entity);
		}

		return result;
	}
}