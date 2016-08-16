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
public class EntityClassification<E extends Entity> extends Classification implements HasEntityId, HasEntityRegistry<E> {

	private BiMap<String, E> entityRegistry = null;

	private E entity = null;

	private Double entityValue = null;


	protected EntityClassification(Type type, BiMap<String, E> entityRegistry){
		super(type);

		setEntityRegistry(entityRegistry);
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
		this.entityRegistry = entityRegistry;
	}

	@Override
	protected ToStringHelper toStringHelper(){
		ToStringHelper helper = super.toStringHelper()
			.add("entityId", getEntityId());

		return helper;
	}

	public Double put(E entity, String key, Double value){
		Type type = getType();

		if(this.entityValue == null || type.compare(value, this.entityValue) > 0){
			this.entityValue = value;

			setEntity(entity);
		}

		return put(key, value);
	}

	public E getEntity(){
		return this.entity;
	}

	protected void setEntity(E entity){
		this.entity = entity;
	}
}