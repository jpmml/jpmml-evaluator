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

import org.dmg.pmml.Entity;
import org.jpmml.model.ToStringHelper;

abstract
public class EntityClassification<E extends Entity<String>, K, V extends Number> extends Classification<K, V> implements HasEntityId, HasEntityRegistry<E> {

	private E entity = null;

	private Value<V> entityValue = null;


	protected EntityClassification(Type type, ValueMap<K, V> values){
		super(type, values);
	}

	@Override
	protected ToStringHelper toStringHelper(){
		ToStringHelper helper = super.toStringHelper()
			.add("entityId", getEntityId());

		return helper;
	}

	public void put(E entity, K key, Value<V> value){
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