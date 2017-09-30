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

import com.google.common.base.Objects.ToStringHelper;
import com.google.common.collect.BiMap;
import org.dmg.pmml.Entity;

abstract
public class EntityClassification<E extends Entity, V extends Number> extends Classification<V> implements HasEntityId, HasEntityRegistry<E> {

	private BiMap<String, E> entityRegistry = null;

	private E entity = null;

	private Value<V> entityValue = null;


	protected EntityClassification(Type type, ValueMap<String, V> values, BiMap<String, E> entityRegistry){
		super(type, values);

		setEntityRegistry(entityRegistry);
	}

	@Override
	protected ToStringHelper toStringHelper(){
		ToStringHelper helper = super.toStringHelper()
			.add("entityId", getEntityId());

		return helper;
	}

	public void put(E entity, Value<V> value){
		BiMap<String, E> entityRegistry = getEntityRegistry();

		String id = EntityUtil.getId(entity, entityRegistry);

		put(entity, id, value);
	}

	public void put(E entity, String key, Value<V> value){
		Type type = getType();

		if(this.entityValue == null || type.compareValues(value, this.entityValue) > 0){
			setEntity(entity);

			this.entityValue = value;
		}

		put(key, value);
	}

	@Override
	public String getEntityId(){
		E entity = getEntity();

		return EntityUtil.getId(entity, this);
	}

	@Override
	public BiMap<String, E> getEntityRegistry(){
		return this.entityRegistry;
	}

	private void setEntityRegistry(BiMap<String, E> entityRegistry){

		if(entityRegistry == null){
			throw new IllegalArgumentException();
		}

		this.entityRegistry = entityRegistry;
	}

	public E getEntity(){
		return this.entity;
	}

	protected void setEntity(E entity){

		if(entity == null){
			throw new IllegalArgumentException();
		}

		this.entity = entity;
	}
}